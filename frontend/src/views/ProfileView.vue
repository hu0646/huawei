<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { tagApi } from '@/api'
import type { Tag } from '@/api/types'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const store = useUserStore()
const { show } = useToast()

const tags = ref<Tag[]>([])
const selected = ref<number[]>([])
const budgetMin = ref<number | string | null>(null)
const budgetMax = ref<number | string | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')

const avatarText = () => (store.nickname || '同学').slice(0, 1)

function toBudget(value: number | string | null): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const num = Number(value)
  return Number.isFinite(num) && num >= 0 ? num : null
}

function toggleTag(tagId: number) {
  const index = selected.value.indexOf(tagId)
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push(tagId)
  }
}

async function save() {
  error.value = ''
  const min = toBudget(budgetMin.value)
  const max = toBudget(budgetMax.value)
  if (min !== null && max !== null && min > max) {
    error.value = '预算下限不能大于上限'
    return
  }
  saving.value = true
  try {
    await store.savePreference({
      tagIds: selected.value,
      budgetMin: min,
      budgetMax: max
    })
    show('口味与预算已更新')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败，请稍后再试'
  } finally {
    saving.value = false
  }
}

async function logout() {
  await store.logout()
  await router.replace('/login')
}

onMounted(async () => {
  try {
    if (!store.user) {
      await store.fetchProfile()
    }
    selected.value = store.user?.preferences.map((tag) => tag.id) ?? []
    budgetMin.value = store.user?.budgetMin ?? null
    budgetMax.value = store.user?.budgetMax ?? null
    tags.value = await tagApi.list()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '资料加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="profile-view">
    <section class="profile-card">
      <div class="profile-card__avatar">{{ avatarText() }}</div>
      <div class="profile-card__info">
        <h1>{{ store.nickname }}</h1>
        <p>@{{ store.user?.username }}</p>
        <span class="profile-card__badge">
          {{ store.user?.onboardingDone === 1 ? '已完成口味引导' : '待完成口味引导' }}
        </span>
      </div>
    </section>

    <section class="panel">
      <div class="panel__head">
        <div>
          <h2>口味偏好</h2>
          <p>多选你喜欢的口味，推荐分会按命中比例计算。</p>
        </div>
        <span class="panel__count">已选 {{ selected.length }} 项</span>
      </div>

      <div v-if="loading" class="skeleton-line"></div>
      <div v-else class="tag-grid tag-grid--compact">
        <button
          v-for="tag in tags"
          :key="tag.id"
          type="button"
          class="tag-option"
          :class="{ 'is-selected': selected.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }}
          <span v-if="selected.includes(tag.id)" class="tag-option__check">✓</span>
        </button>
      </div>
    </section>

    <section class="panel">
      <div class="panel__head">
        <div>
          <h2>每餐预算</h2>
          <p>留空表示不限制。下限与上限至少填一个即可。</p>
        </div>
      </div>

      <div class="budget-custom">
        <label class="field field--inline">
          <span>最低</span>
          <input v-model.number="budgetMin" type="number" min="0" placeholder="不限" />
        </label>
        <span class="budget-custom__dash">—</span>
        <label class="field field--inline">
          <span>最高</span>
          <input v-model.number="budgetMax" type="number" min="0" placeholder="不限" />
        </label>
        <span class="budget-custom__unit">元</span>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <div class="panel__actions">
        <router-link to="/" class="btn btn--ghost">去看推荐</router-link>
        <button class="btn btn--primary" type="button" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存偏好' }}
        </button>
      </div>
    </section>

    <section class="panel panel--danger">
      <div class="panel__head">
        <div>
          <h2>账号</h2>
          <p>退出后需要重新登录才能查看收藏与推荐。</p>
        </div>
      </div>
      <button class="btn btn--danger" type="button" @click="logout">退出登录</button>
    </section>
  </div>
</template>