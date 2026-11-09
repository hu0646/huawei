<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { favoriteApi, foodApi } from '@/api'
import type { Food } from '@/api/types'
import { useToast } from '@/composables/useToast'
import FoodCard from '@/components/FoodCard.vue'

const { show } = useToast()

const keyword = ref('')
const category = ref('')
const categories = ref<string[]>([])
const foods = ref<Food[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    foods.value = await foodApi.list({
      keyword: keyword.value.trim() || undefined,
      category: category.value || undefined
    })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '美食加载失败'
  } finally {
    loading.value = false
  }
}

function selectCategory(next: string) {
  category.value = category.value === next ? '' : next
  load()
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  load()
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

onMounted(async () => {
  try {
    categories.value = await foodApi.categories()
  } catch {
    categories.value = []
  }
  await load()
})
</script>

<template>
  <div class="foods-view">
    <header class="page-head">
      <span class="page-head__eyebrow">美食广场</span>
      <h1>把校园里的好味道逛个遍</h1>
      <p>支持按名称、食堂、分类检索，收藏喜欢的下一顿。</p>
    </header>

    <form class="search-bar" @submit.prevent="load">
      <span class="search-bar__icon">🔍</span>
      <input v-model="keyword" placeholder="搜索美食、食堂或口味关键词" />
      <button class="btn btn--primary" type="submit">搜索</button>
    </form>

    <div class="filter-row">
      <button
        class="budget-chip"
        :class="{ 'is-selected': category === '' }"
        type="button"
        @click="resetFilters"
      >
        全部
      </button>
      <button
        v-for="item in categories"
        :key="item"
        class="budget-chip"
        :class="{ 'is-selected': category === item }"
        type="button"
        @click="selectCategory(item)"
      >
        {{ item }}
      </button>
    </div>

    <div class="section-head">
      <h2>美食清单</h2>
      <span v-if="!loading" class="section-head__note">{{ foods.length }} 份</span>
    </div>

    <div v-if="loading" class="food-grid">
      <div v-for="i in 6" :key="i" class="skeleton-card"></div>
    </div>
    <p v-else-if="error" class="empty-state">
      {{ error }}
      <button class="text-btn" type="button" @click="load">重试</button>
    </p>
    <div v-else-if="foods.length" class="food-grid">
      <FoodCard
        v-for="food in foods"
        :key="food.id"
        :food="food"
        @toggle-favorite="toggleFavorite"
      />
    </div>
    <p v-else class="empty-state">没有找到符合条件的美食，换个关键词试试。</p>
  </div>
</template>