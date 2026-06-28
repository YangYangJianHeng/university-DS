import request from './request'

// 会话
export function createSession() {
  return request.post('/ai/sessions')
}

export function getSessions(): Promise<any> {
  return request.get('/ai/sessions')
}

export function deleteSession(sessionId: number) {
  return request.delete(`/ai/sessions/${sessionId}`)
}

// 消息
export function getMessages(sessionId: number): Promise<any> {
  return request.get(`/ai/sessions/${sessionId}/messages`)
}

// 同步发送消息
export function sendMessage(sessionId: number, content: string): Promise<any> {
  return request.post('/ai/chat', { sessionId, content })
}

// SSE 流式发送消息
export function sendMessageStream(
  sessionId: number,
  content: string,
  onChunk: (text: string) => void,
  onDone: () => void,
  onError: (err: any) => void
) {
  const token = localStorage.getItem('token')
  // 使用 fetch 实现 SSE
  fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
    },
    body: JSON.stringify({ sessionId, content }),
  })
    .then(async (response) => {
      if (!response.ok) {
        const text = await response.text()
        throw new Error(text || `HTTP ${response.status}`)
      }
      const reader = response.body?.getReader()
      if (!reader) throw new Error('No response body')

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE 事件流
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 未完成的行保留

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (data === '[DONE]') {
              onDone()
              return
            }
            onChunk(data)
          }
        }
      }
      onDone()
    })
    .catch((err) => {
      console.error('SSE error:', err)
      onError(err)
    })
}
