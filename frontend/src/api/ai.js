import { redirectToLogin } from '@/api/request'

const STREAM_URL = '/api/v1/ai/chat/stream'

/**
 * 流式对话：使用 fetch + ReadableStream 解析 SSE。
 *
 * @param {Object} payload 请求体
 * @param {Object} handlers 事件回调：meta/links/delta/done/error
 * @param {AbortSignal} signal 中止信号
 */
export async function chatStream(payload, handlers = {}, signal) {
  const token = sessionStorage.getItem('token')
  const response = await fetch(STREAM_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify(payload),
    signal
  })

  if (!response.ok) {
    let body = {}
    try {
      body = await response.json()
    } catch (e) {
      // 非 JSON 错误体忽略
    }
    if (response.status === 401) {
      redirectToLogin()
      const error = new Error('登录已过期')
      error.code = 401
      error.handled = true
      throw error
    }
    const error = new Error(body.message || '请求失败')
    error.code = body.code
    error.status = response.status
    throw error
  }

  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const frame = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        dispatchFrame(frame, handlers)
        boundary = buffer.indexOf('\n\n')
      }
    }
    if (buffer.trim()) {
      dispatchFrame(buffer, handlers)
    }
  } finally {
    try {
      reader.releaseLock()
    } catch (e) {
      // 忽略释放异常
    }
  }
}

/**
 * 解析单个 SSE 帧并分发事件。
 */
function dispatchFrame(frame, handlers) {
  let event = 'message'
  const dataLines = []
  for (const rawLine of frame.split('\n')) {
    const line = rawLine.trimEnd()
    if (!line || line.startsWith(':')) continue
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  if (!dataLines.length) return
  const dataText = dataLines.join('\n')
  let data = dataText
  try {
    data = JSON.parse(dataText)
  } catch (e) {
    // 非 JSON 数据按字符串处理
  }
  const handler = handlers[event]
  if (typeof handler === 'function') {
    handler(data)
  }
}
