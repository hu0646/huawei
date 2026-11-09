import http from './http'
import type {
  Food,
  LoginPayload,
  LoginResult,
  PreferencePayload,
  Recommendation,
  RegisterPayload,
  Tag,
  User
} from './types'

export const authApi = {
  register: (payload: RegisterPayload) => http.post<unknown, LoginResult>('/auth/register', payload),
  login: (payload: LoginPayload) => http.post<unknown, LoginResult>('/auth/login', payload),
  logout: () => http.post<unknown, void>('/auth/logout')
}

export const tagApi = {
  list: () => http.get<unknown, Tag[]>('/tags')
}

export const userApi = {
  profile: () => http.get<unknown, User>('/user/profile'),
  savePreference: (payload: PreferencePayload) =>
    http.post<unknown, User>('/user/preference', payload)
}

export const foodApi = {
  list: (params?: { keyword?: string; category?: string }) =>
    http.get<unknown, Food[]>('/foods', { params }),
  detail: (id: number) => http.get<unknown, Food>(`/foods/${id}`),
  categories: () => http.get<unknown, string[]>('/food-categories')
}

export const recommendApi = {
  list: (limit = 12) =>
    http.get<unknown, Recommendation[]>('/recommendations', { params: { limit } })
}

export const favoriteApi = {
  list: () => http.get<unknown, Food[]>('/favorites'),
  add: (foodId: number) => http.post<unknown, void>(`/favorites/${foodId}`),
  remove: (foodId: number) => http.delete<unknown, void>(`/favorites/${foodId}`)
}