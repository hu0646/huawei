<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const form = reactive({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await store.login(form.username.trim(), form.password)
    if (store.needsOnboarding) {
      await router.replace('/onboarding')
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <section class="auth-hero">
      <span class="auth-hero__badge">校园美食推荐小助手</span>
      <h1>今天吃什么？<br />让口味替你决定。</h1>
      <p class="auth-hero__lead">选几个喜欢的口味，填上预算，剩下的我们帮你挑。</p>

      <div class="auth-hero__grid">
        <span>🌶️</span>
        <span>🍜</span>
        <span>🍗</span>
        <span>🍣</span>
        <span>🍰</span>
        <span>🥟</span>
      </div>

      <ul class="auth-hero__points">
        <li><b>口味匹配</b><span>按你喜欢的标签排序</span></li>
        <li><b>预算适配</b><span>不超支也能吃得好</span></li>
        <li><b>理由可查</b><span>每一份推荐都说得清</span></li>
      </ul>
    </section>

    <section class="auth-card">
      <h2>欢迎回来</h2>
      <p class="auth-card__sub">登录后继续发现校园里的好味道</p>

      <form @submit.prevent="submit">
        <label class="field">
          <span>用户名</span>
          <input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
        </label>

        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <p v-if="error" class="form-error">{{ error }}</p>

        <button class="btn btn--primary btn--block" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </form>

      <p class="auth-card__switch">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
    </section>
  </div>
</template>