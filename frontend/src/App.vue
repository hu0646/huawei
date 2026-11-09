<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'
import AgentChat from '@/components/AgentChat.vue'

const route = useRoute()
const store = useUserStore()
const { toast } = useToast()

const isAuthPage = computed(() =>
  ['login', 'register', 'onboarding'].includes(String(route.name))
)

const navItems = [
  { name: 'home', label: '推荐', icon: '🍽️', to: '/' },
  { name: 'foods', label: '广场', icon: '🧭', to: '/foods' },
  { name: 'favorites', label: '收藏', icon: '♥', to: '/favorites' },
  { name: 'profile', label: '我的', icon: '👤', to: '/profile' }
]

const avatarText = computed(() => (store.nickname || '同学').slice(0, 1))
</script>

<template>
  <div class="app-shell">
    <header v-if="!isAuthPage" class="topbar">
      <router-link to="/" class="brand">
        <span class="brand__mark">🍜</span>
        <span class="brand__text">
          <strong>校园食光</strong>
          <small>校园美食推荐小助手</small>
        </span>
      </router-link>

      <nav class="topnav">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="item.to"
          class="topnav__link"
          :class="{ 'is-active': route.name === item.name }"
        >
          <span class="topnav__icon">{{ item.icon }}</span>{{ item.label }}
        </router-link>
      </nav>

      <div class="topbar__user">
        <span class="avatar">{{ avatarText }}</span>
        <span class="topbar__name">{{ store.nickname }}</span>
      </div>
    </header>

    <main class="page" :class="{ 'page--shell': !isAuthPage }">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <nav v-if="!isAuthPage" class="bottom-nav">
      <router-link
        v-for="item in navItems"
        :key="item.name"
        :to="item.to"
        class="bottom-nav__item"
        :class="{ 'is-active': route.name === item.name }"
      >
        <span class="bottom-nav__icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>

    <AgentChat v-if="!isAuthPage" />

    <transition name="toast">
      <div v-if="toast" class="toast">{{ toast }}</div>
    </transition>
  </div>
</template>