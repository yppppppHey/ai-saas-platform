import { request } from './request'

// 会话列表
export function getConversations(params?: Record<string, any>) {
  return request({ url: '/v1/conversations', method: 'get', params })
}

// 创建会话
export function createConversation(data: Record<string, any>) {
  return request({ url: '/v1/conversations', method: 'post', data })
}

// 删除会话
export function deleteConversation(id: string) {
  return request({ url: `/v1/conversations/${id}`, method: 'delete' })
}

// 会话消息列表（后端为 GET /api/v1/messages/conversation/{conversationId}）
export function getMessages(conversationId: string, params?: Record<string, any>) {
  return request({ url: `/v1/messages/conversation/${conversationId}`, method: 'get', params })
}

// 发送消息（非流式）
export function sendMessage(data: Record<string, any>) {
  return request({ url: '/v1/messages/send', method: 'post', data })
}

// 流式对话（SSE）- 使用 fetch + ReadableStream
export function streamChat(
  data: Record<string, any>,
  onMessage: (chunk: string) => void,
  onDone?: () => void,
  onError?: (error: Error) => void,
) {
  const token = localStorage.getItem('token')
  fetch('/api/v1/ai-chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(data),
  })
    .then(async (response) => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body?.getReader()
      if (!reader) throw new Error('No response body')
      const decoder = new TextDecoder()
      let buffer = ''
      let currentEvent = ''
      let finished = false
      const finish = (fn?: () => void) => {
        if (finished) return
        finished = true
        fn?.()
      }
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          const trimmed = line.trim()
          if (!trimmed) continue
          if (trimmed.startsWith('event:')) {
            currentEvent = trimmed.slice(6).trim()
            continue
          }
          if (!trimmed.startsWith('data:')) continue
          const payload = trimmed.slice(5).trim()
          if (currentEvent === 'error') {
            try {
              const obj = JSON.parse(payload)
              onError?.(new Error(obj?.message || '生成失败'))
            } catch {
              onError?.(new Error('生成失败'))
            }
            finish()
            return
          }
          if (currentEvent === 'complete') {
            finish(onDone)
            return
          }
          // start / delta 事件
          try {
            const obj = JSON.parse(payload)
            if (obj?.type === 'delta' && obj.content) {
              onMessage(String(obj.content))
            } else if (obj?.type === 'complete' || obj?.type === 'done') {
              finish(onDone)
              return
            } else if (obj?.type === 'error') {
              onError?.(new Error(obj?.message || '生成失败'))
              finish()
              return
            }
          } catch {
            // 非标准 data 原样按文本处理
            if (payload && payload !== '[DONE]') onMessage(payload)
          }
        }
      }
      finish(onDone)
    })
    .catch((error) => onError?.(error))
}

// 中止生成
export function abortChat(messageId: string | number) {
  return request({ url: '/v1/ai-chat/stop', method: 'post', data: { messageId } })
}

// 提示词模板列表
export function getPromptTemplates(params?: Record<string, any>) {
  return request({ url: '/v1/prompt-templates', method: 'get', params })
}
