<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { favoriteApi, recommendApi } from '@/api'
import type { Food, Recommendation } from '@/api/types'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'
import FoodCard from '@/components/FoodCard.vue'

const store = useUserStore()
const { show } = useToast()

const recommendations = ref<Recommendation[]>([])
const loading = ref(true)
const error = ref('')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const hasPreference = computed(() => (store.user?.preferences?.length ?? 0) > 0)

const preferenceText = computed(
  () => store.user?.preferences?.map((tag) => tag.name).join('、') || ''
)

const budgetText = computed(() => {
  const min = store.user?.budgetMin
  const max = store.user?.budgetMax
  if (min == null && max == null) return '预算不限'
  if (min == null) return `${max} 元以内`
  if (max == null) return `${min} 元以上`
  return `${min}-${max} 元`
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    recommendations.value = await recommendApi.list(12)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '推荐加载失败'
  } finally {
    loading.value = false
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

onMounted(load)
</script>

<template>
  <div class="home-view">
    <section class="hero">
      <div class="hero__copy">
        <span class="hero__eyebrow">{{ greeting }}，{{ store.nickname }}</span>
        <h1>根据你的口味，<br />今天推荐这些。</h1>
        <p v-if="hasPreference">
          已按「{{ preferenceText }}」与{{ budgetText }}参与计算推荐分。
        </p>
        <p v-else>
          你还没有选择口味，当前按评分与热度推荐。补充偏好后结果会更合胃口。
        </p>

        <div class="hero__meta">
          <span v-for="tag in store.user?.preferences" :key="tag.id" class="tag">
            {{ tag.name }}
          </span>
          <span class="hero__budget">{{ budgetText }}</span>
        </div>

        <div class="hero__actions">
          <button class="btn btn--primary" type="button" :disabled="loading" @click="load">
            {{ loading ? '刷新中…' : '刷新推荐' }}
          </button>
          <router-link v-if="!hasPreference" to="/profile" class="btn btn--ghost">
            设置口味
          </router-link>
        </div>
      </div>

      <div class="hero__art" aria-hidden="true">
        <span class="hero__art-main">🍱</span>
        <span class="hero__float hero__float--a">🌶️</span>
        <span class="hero__float hero__float--b">🥢</span>
        <span class="hero__float hero__float--c">🍋</span>
      </div>
    </section>

    <div class="section-head">
      <h2>为你推荐</h2>
      <span v-if="recommendations.length" class="section-head__note">
        {{ recommendations.length }} 份 · 按推荐分排序
      </span>
    </div>

    <div v-if="loading" class="food-grid">
      <div v-for="i in 4" :key="i" class="skeleton-card"></div>
    </div>
    <p v-else-if="error" class="empty-state">
      {{ error }}
      <button class="text-btn" type="button" @click="load">重试</button>
    </p>
    <div v-else-if="recommendations.length" class="food-grid">
      <FoodCard
        v-for="(item, index) in recommendations"
        :key="item.food.id"
        :food="item.food"
        :recommendation="item"
        :rank="index < 3 ? index + 1 : undefined"
        @toggle-favorite="toggleFavorite"
      />
    </div>
    <p v-else class="empty-state">暂时没有推荐结果，稍后再试试。</p>
  </div>
</template>