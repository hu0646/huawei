<script setup lang="ts">
import type { Food, Recommendation } from '@/api/types'

defineProps<{
  food: Food
  recommendation?: Recommendation
  rank?: number
}>()

const emit = defineEmits<{
  (event: 'toggle-favorite', food: Food): void
}>()

function money(value: number) {
  const num = Number(value)
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}
</script>

<template>
  <article class="food-card">
    <div class="food-card__head">
      <div class="food-card__emoji">{{ food.emoji || '🍽️' }}</div>
      <div class="food-card__title">
        <h3 class="food-card__name">
          {{ food.name }}
          <span v-if="rank" class="food-card__rank">TOP {{ rank }}</span>
        </h3>
        <p class="food-card__canteen">{{ food.canteen }}</p>
      </div>
      <button
        class="fav-btn"
        :class="{ 'is-active': food.favorite }"
        :aria-label="food.favorite ? '取消收藏' : '收藏'"
        @click="emit('toggle-favorite', food)"
      >
        {{ food.favorite ? '♥' : '♡' }}
      </button>
    </div>

    <p class="food-card__desc">{{ food.description }}</p>

    <div class="food-card__tags">
      <span v-for="tag in food.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
    </div>

    <div v-if="recommendation" class="recommend-box">
      <div class="recommend-box__score">
        <strong>{{ recommendation.score }}</strong>
        <span>推荐分</span>
      </div>
      <ul class="recommend-box__reasons">
        <li v-for="(reason, index) in recommendation.reasons" :key="index">{{ reason }}</li>
      </ul>
    </div>

    <div class="food-card__meta">
      <span class="price">¥{{ money(food.price) }}</span>
      <span class="rating">★ {{ money(food.rating) }}</span>
      <span class="popularity">月售 {{ food.popularity }}+</span>
    </div>
  </article>
</template>