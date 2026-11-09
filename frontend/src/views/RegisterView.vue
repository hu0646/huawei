<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', nickname: '', password: '', confirm: '' })
const error = ref('')
const loading = ref(false)

function validate() {
  if (!form.username.trim()) {
    return '请输入用户名'
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(form.username.trim())) {
    return '用户名需为 3-20 位字母、数字或下划线'
  }
  if (form.password.length < 6 || form.password.length > 32) {
    return '密码长度需为 6-32 位'
  }
  if (form.password !== form.confirm) {
    return '两次输入的密码不一致'
  }
  return ''
}

async function submit() {
  error.value = validate()
  if (error.value) {
    return
  }
  loading.value = true
  try {
    await store.register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined
    })
    await router.replace('/onboarding')
  } catch (e) {
    error.value = e instanceof Error ? e.message : '注册失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page auth-page--reverse">
    <section class="auth-card">
      <h2>创建账号</h2>
      <p class="auth-card__sub">注册后选择口味，马上获得专属推荐</p>

      <form @submit.prevent="submit">
        <label class="field">
          <span>用户名</span>
          <input
            v-model="form.username"
            autocomplete="username"
            placeholder="3-20 位字母、数字或下划线"
          />
        </label>

        <label class="field">
          <span>昵称 <em>选填</em></span>
          <input
            v-model="form.nickname"
            autocomplete="nickname"
            maxlength="50"
            placeholder="想让别人怎么称呼你"
          />
        </label>

        <label class="field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="6-32 位密码"
          />
        </label>

        <label class="field">
          <span>确认密码</span>
          <input
            v-model="form.confirm"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入密码"
          />
        </label>

        <p v-if="error" class="form-error">{{ error }}</p>

        <button class="btn btn--primary btn--block" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注册并开始' }}
        </button>
      </form>

      <p class="auth-card__switch">
        已经有账号了？
        <router-link to="/login">去登录</router-link>
      </p>
    </section>

    <section class="auth-hero auth-hero--right">
      <span class="auth-hero__badge">三步吃到心仪的一餐</span>
      <h1>让每一顿，<br />都更合你的胃口。</h1>
      <p class="auth-hero__lead">口味、预算、评分、热度一起参与计算，推荐结果有据可依。</p>

      <div class="auth-hero__steps">
        <div><b>01</b><span>选择喜欢的口味标签</span></div>
        <div><b>02</b><span>设置每餐预算（可跳过）</span></div>
        <div><b>03</b><span>查看带推荐理由的清单</span></div>
      </div>
    </section>
  </div>
</template>