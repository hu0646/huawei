export interface Tag {
  id: number
  name: string
  code: string
}

export interface Food {
  id: number
  name: string
  description: string
  canteen: string
  category: string
  price: number
  rating: number
  popularity: number
  emoji: string
  tags: Tag[]
  favorite: boolean
}

export interface Recommendation {
  food: Food
  score: number
  reasons: string[]
}

export interface User {
  id: number
  username: string
  nickname: string
  budgetMin: number | null
  budgetMax: number | null
  onboardingDone: number
  preferences: Tag[]
}

export interface LoginResult {
  token: string
  user: User
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname?: string
}

export interface PreferencePayload {
  tagIds: number[]
  budgetMin: number | null
  budgetMax: number | null
}

export interface AgentStep {
  name: string
  arguments: string
  success: boolean
  durationMs: number
  summary: string
}

export interface AgentConversation {
  id: number
  title: string
  updatedAt: string
}

export interface AgentMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  createdAt?: string
}

export interface AgentConversationDetail {
  id: number
  title: string
  messages: AgentMessage[]
}

export interface AgentChatResponse {
  conversationId: number
  reply: string
  steps: AgentStep[]
  recommendations: Recommendation[]
  foods: Food[]
  createdConversation: boolean
  toolCallCount: number
}