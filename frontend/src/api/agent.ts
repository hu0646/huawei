import http from './http'
import type { AgentChatResponse, AgentConversation, AgentConversationDetail } from './types'

export const agentApi = {
  chat: (payload: { conversationId?: number; message: string }) =>
    http.post<unknown, AgentChatResponse>('/agent/chat', payload),
  conversations: () => http.get<unknown, AgentConversation[]>('/agent/conversations'),
  conversation: (id: number) =>
    http.get<unknown, AgentConversationDetail>(`/agent/conversations/${id}`)
}