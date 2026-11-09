<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { favoriteApi } from '@/api'
import type { Food } from '@/api/types'
import { useToast } from '@/composables/useToast'
import FoodCard from '@/components/FoodCard.vue'

const { show } = useToast()

const foods = ref<Food[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    foods.value = await favoriteApi.list()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '收藏加载失败'
  } finally {
    loading.value = false
  }
}

async function removeFavorite(food: Food) {
  try {
    await favoriteApi.remove(food.id)
    foods.value = foods.value.filter((item) => item.id !== food.id)
    show('已取消收藏')
  } catch (e) {
    show(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="favorites-view">
    <header class="page-head">
      <span class="page-head__eyebrow">我的收藏</span>
      <h1>想再吃一次的，都在这里</h1>
      <p>收藏会保存在账号里，换个设备登录也能找回。</p>
    </header>

    <div v-if="loading" class="food-grid">
      <div v-for="i in 3" :key="i" class="skeleton-card"></div>
    </div>
    <p v-else-if="error" class="empty-state">
      {{ error }}
      <button class="text-btn" type="button" @click="load">重试</button>
    </p>
    <template v-else-if="foods.length">
      <div class="section-head">
        <h2>已收藏</h2>
        <span class="section-head__note">{{ foods.length }} 份</span>
      </div>
      <div class="food-grid">
        <FoodCard
          v-for="food in foods"
          :key="food.id"
          :food="food"
          @toggle-favorite="removeFavorite"
        />
      </div>
    </template>
    <div v-else class="empty-card">
      <span class="empty-card__emoji">🍽️</span>
      <h2>还没有收藏</h2>
      <p>在推荐或美食广场里点亮小心心，就能在这里找到了。</p>
      <router-link to="/foods" class="btn btn--primary">去逛逛美食广场</router-link>
    </div>
  </div>
</template>