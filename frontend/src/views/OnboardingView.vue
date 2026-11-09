<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { tagApi } from '@/api'
import type { Tag } from '@/api/types'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'

interface BudgetPreset {
  key: string
  label: string
  min: number | null
  max: number | null
}

const router = useRouter()
const store = useUserStore()
const { show } = useToast()

const tags = ref<Tag[]>([])
const selected = ref<number[]>([])
const step = ref(1)
const loading = ref(false)
const error = ref('')
const budgetPreset = ref('any')
const budgetMin = ref<number | string | null>(null)
const budgetMax = ref<number | string | null>(null)

const budgetPresets: BudgetPreset[] = [
  { key: 'any', label: '不限', min: null, max: null },
  { key: 'under10', label: '10 元内', min: null, max: 10 },
  { key: '10-20', label: '10-20 元', min: 10, max: 20 },
  { key: '20-30', label: '20-30 元', min: 20, max: 30 },
  { key: 'over30', label: '30 元以上', min: 30, max: null }
]

onMounted(async () => {
  try {
    tags.value = await tagApi.list()
  } catch (e) {
    error.value = e instanceof Error ? e.message : '口味标签加载失败'
  }
})

function toggleTag(tagId: number) {
  const index = selected.value.indexOf(tagId)
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push(tagId)
  }
}

function applyPreset(preset: BudgetPreset) {
  budgetPreset.value = preset.key
  budgetMin.value = preset.min
  budgetMax.value = preset.max
}

function toBudget(value: number | string | null): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  const num = Number(value)
  return Number.isFinite(num) && num >= 0 ? num : null
}

async function finish(skipBudget = false) {
  error.value = ''
  const min = skipBudget ? null : toBudget(budgetMin.value)
  const max = skipBudget ? null : toBudget(budgetMax.value)
  if (min !== null && max !== null && min > max) {
    error.value = '预算下限不能大于上限'
    return
  }
  loading.value = true
  try {
    await store.savePreference({
      tagIds: selected.value,
      budgetMin: min,
      budgetMax: max
    })
    show('偏好已保存，开始为你推荐')
    await router.replace('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '保存失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="onboarding">
    <header class="onboarding__head">
      <div class="onboarding__brand">
        <span class="onboarding__mark">🍜</span>
        <span>校园食光</span>
      </div>
      <button class="text-btn" type="button" @click="finish(true)">跳过，直接推荐</button>
    </header>

    <div class="steps">
      <div class="steps__item" :class="{ 'is-current': step === 1, 'is-done': step > 1 }">
        <b>1</b>
        <span>选择口味</span>
      </div>
      <div class="steps__line" :class="{ 'is-done': step > 1 }"></div>
      <div class="steps__item" :class="{ 'is-current': step === 2 }">
        <b>2</b>
        <span>设置预算</span>
      </div>
    </div>

    <section v-if="step === 1" class="onboarding__panel">
      <h1>先说说你喜欢什么口味</h1>
      <p class="onboarding__lead">可以多选，选得越准，推荐越合胃口。</p>

      <div class="tag-grid">
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

      <p class="hint">
        <template v-if="selected.length === 0">还没选择口味，完成后将按评分与热度推荐。</template>
        <template v-else>已选择 {{ selected.length }} 种口味</template>
      </p>

      <p v-if="error" class="form-error">{{ error }}</p>

      <div class="onboarding__actions">
        <button class="btn btn--ghost" type="button" @click="finish(true)">跳过</button>
        <button class="btn btn--primary" type="button" @click="step = 2">
          下一步：设置预算
        </button>
      </div>
    </section>

    <section v-else class="onboarding__panel">
      <h1>每顿大概想花多少？</h1>
      <p class="onboarding__lead">预算可以跳过，跳过时不做价格约束。</p>

      <div class="budget-presets">
        <button
          v-for="preset in budgetPresets"
          :key="preset.key"
          type="button"
          class="budget-chip"
          :class="{ 'is-selected': budgetPreset === preset.key }"
          @click="applyPreset(preset)"
        >
          {{ preset.label }}
        </button>
      </div>

      <div class="budget-custom">
        <label class="field field--inline">
          <span>最低</span>
          <input
            v-model.number="budgetMin"
            type="number"
            min="0"
            inputmode="decimal"
            placeholder="不限"
            @input="budgetPreset = 'custom'"
          />
        </label>
        <span class="budget-custom__dash">—</span>
        <label class="field field--inline">
          <span>最高</span>
          <input
            v-model.number="budgetMax"
            type="number"
            min="0"
            inputmode="decimal"
            placeholder="不限"
            @input="budgetPreset = 'custom'"
          />
        </label>
        <span class="budget-custom__unit">元</span>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <div class="onboarding__actions">
        <button class="btn btn--ghost" type="button" @click="step = 1">上一步</button>
        <button class="btn btn--ghost" type="button" @click="finish(true)">跳过预算</button>
        <button class="btn btn--primary" type="button" :disabled="loading" @click="finish(false)">
          {{ loading ? '保存中…' : '完成，开始推荐' }}
        </button>
      </div>
    </section>
  </div>
</template>