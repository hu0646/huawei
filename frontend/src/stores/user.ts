import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'
import type { PreferencePayload, RegisterPayload, User } from '@/api/types'

const TOKEN_KEY = 'campus_food_token'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: null as User | null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    needsOnboarding: (state) =>
      Boolean(state.token && state.user && state.user.onboardingDone !== 1),
    nickname: (state) => state.user?.nickname || state.user?.username || ''
  },
  actions: {
    setSession(token: string, user: User) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
    },
    async login(username: string, password: string) {
      const data = await authApi.login({ username, password })
      this.setSession(data.token, data.user)
    },
    async register(payload: RegisterPayload) {
      const data = await authApi.register(payload)
      this.setSession(data.token, data.user)
    },
    async fetchProfile() {
      this.user = await userApi.profile()
    },
    async savePreference(payload: PreferencePayload) {
      this.user = await userApi.savePreference(payload)
    },
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    },
    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 本地登出不依赖服务端结果
      }
      this.clearAuth()
    }
  }
})