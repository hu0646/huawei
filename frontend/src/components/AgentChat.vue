<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { agentApi } from '@/api/agent'
import { favoriteApi } from '@/api'
import type { AgentConversation, AgentStep, Food, Recommendation } from '@/api/types'
import FoodCard from '@/components/FoodCard.vue'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

interface ChatMessage {
  key: string
  role: 'user' | 'assistant'
  content: string
  steps?: AgentStep[]
  recommendations?: Recommendation[]
  foods?: Food[]
}

const store = useUserStore()
const { show } = useToast()

const open = ref(false)
const loading = ref(false)
const input = ref('')
const error = ref('')
const conversationId = ref<number | null>(null)
const conversations = ref<AgentConversation[]>([])
const showHistory = ref(false)
const messages = ref<ChatMessage[]>([])
const messageList = ref<HTMLElement | null>(null)

const quickQuestions = [
  '我的口味和预算是多少？',
  '推荐 15 元以内的麻辣美食',
  '帮我收藏第一道',
  '查看我的收藏'
]

const stepLabels: Record<string, string> = {
  get_my_preferences: '读取口味偏好',
  recommend_foods: '按条件推荐',
  get_food_detail: '查询美食详情',
  add_favorite: '加入收藏',
  list_favorites: '查看收藏'
}

const welcome: ChatMessage = {
  key: 'welcome',
  role: 'assistant',
  content:
    '你好，我是校园食光助手。可以帮你查口味预算、按条件推荐美食、看详情和收藏。试试下面的快捷问题。'
}

const visibleMessages = computed(() => (messages.value.length ? messages.value : [welcome]))

function scrollToBottom() {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

async function loadConversations() {
  try {
    conversations.value = await agentApi.conversations()
  } catch {
    conversations.value = []
  }
}

function toggleOpen() {
  if (!store.isLoggedIn) {
    return
  }
  open.value = !open.value
  if (open.value) {
    scrollToBottom()
    loadConversations()
  }
}

function newConversation() {
  conversationId.value = null
  messages.value = []
  error.value = ''
  showHistory.value = false
}

async function openConversation(id: number) {
  try {
    showHistory.value = false
    const detail = await agentApi.conversation(id)
    conversationId.value = detail.id
    messages.value = detail.messages.map((message) => ({
      key: 'history-' + message.id,
      role: message.role,
      content: message.content
    }))
    scrollToBottom()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '历史会话加载失败'
  }
}

async function send(text?: string) {
  const content = (text ?? input.value).trim()
  if (!content || loading.value) {
    return
  }
  input.value = ''
  error.value = ''
  messages.value.push({ key: 'u-' + Date.now(), role: 'user', content })
  loading.value = true
  scrollToBottom()
  try {
    const result = await agentApi.chat({
      conversationId: conversationId.value ?? undefined,
      message: content
    })
    conversationId.value = result.conversationId
    messages.value.push({
      key: 'a-' + Date.now(),
      role: 'assistant',
      content: result.reply,
      steps: result.steps,
      recommendations: result.recommendations,
      foods: result.foods
    })
    await loadConversations()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '助手暂时不可用'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    send()
  }
}

async function toggleFavorite(food: Food) {
  try {
    if (food.favorite) {
      await favoriteApi.remove(food.id)
      food.favorite = false
      show('已取消收藏')
    } else {
      await favoriteApi.add(food.id)
      food.favorite = true
      show('已加入收藏')
    }
  } catch (e) {
    show(e instanceof Error ? e.message : '操作失败')
  }
}

function stepLabel(name: string) {
  return stepLabels[name] || name
}
</script>

<template>
  <template v-if="store.isLoggedIn">
    <button
      v-if="!open"
      class="agent-fab"
      type="button"
      aria-label="打开美食助手"
      @click="toggleOpen"
    >
      <span class="agent-fab__icon">💬</span>
    </button>

    <section v-else class="agent-panel" aria-label="美食助手">
      <header class="agent-head">
        <span class="agent-head__mark">🍜</span>
        <div class="agent-head__text">
          <strong>美食助手</strong>
          <span>查口味 · 推荐 · 收藏</span>
        </div>
        <button class="agent-head__btn" type="button" title="新对话" @click="newConversation">
          ＋
        </button>
        <button class="agent-head__btn" type="button" title="历史会话" @click="showHistory = !showHistory">
          ☰
        </button>
        <button class="agent-head__btn" type="button" title="关闭" @click="toggleOpen">×</button>
      </header>

      <div v-if="showHistory" class="agent-history">
        <button
          v-for="item in conversations"
          :key="item.id"
          class="agent-history__item"
          :class="{ 'is-active': item.id === conversationId }"
          type="button"
          @click="openConversation(item.id)"
        >
          {{ item.title || '未命名会话' }}
        </button>
        <p v-if="!conversations.length" class="agent-history__empty">还没有历史会话</p>
      </div>

      <div ref="messageList" class="agent-messages">
        <div
          v-for="message in visibleMessages"
          :key="message.key"
          class="agent-msg"
          :class="message.role === 'user' ? 'agent-msg--user' : 'agent-msg--assistant'"
        >
          <div class="agent-msg__body">
            <div class="agent-bubble">{{ message.content }}</div>

            <div v-if="message.steps?.length" class="agent-steps">
              <details v-for="(step, index) in message.steps" :key="index" class="agent-step">
                <summary>
                  <span class="agent-step__dot" :class="{ 'is-fail': !step.success }"></span>
                  {{ stepLabel(step.name) }}
                  <span class="agent-step__time">{{ step.durationMs }}ms</span>
                  <span class="agent-step__status" :class="{ 'is-fail': !step.success }">
                    {{ step.success ? '完成' : '失败' }}
                  </span>
                </summary>
                <p class="agent-step__detail">{{ step.summary }}</p>
                <code class="agent-step__args">{{ step.arguments }}</code>
              </details>
            </div>

            <div v-if="message.recommendations?.length || message.foods?.length" class="agent-foods">
              <FoodCard
                v-for="item in message.recommendations"
                :key="'rec-' + item.food.id"
                :food="item.food"
                :recommendation="item"
                @toggle-favorite="toggleFavorite"
              />
              <FoodCard
                v-for="food in message.foods"
                :key="'food-' + food.id"
                :food="food"
                @toggle-favorite="toggleFavorite"
              />
            </div>
          </div>
        </div>

        <div v-if="loading" class="agent-msg agent-msg--assistant">
          <div class="agent-typing"><span></span><span></span><span></span></div>
        </div>

        <p v-if="error" class="agent-error">{{ error }}</p>
      </div>

      <div v-if="!messages.length" class="agent-quick">
        <button
          v-for="question in quickQuestions"
          :key="question"
          type="button"
          @click="send(question)"
        >
          {{ question }}
        </button>
      </div>

      <div class="agent-input">
        <textarea
          v-model="input"
          rows="1"
          placeholder="问我推荐、详情或收藏…"
          @keydown="handleKeydown"
        ></textarea>
        <button type="button" :disabled="loading || !input.trim()" aria-label="发送" @click="send()">
          ↑
        </button>
      </div>
    </section>
  </template>
</template>