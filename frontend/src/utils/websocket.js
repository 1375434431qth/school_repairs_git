/**
 * WebSocket 客户端：心跳 30s + 指数退避重连（1s→2s→4s→…→最长 30s）。
 * 后端地址：/ws/notice?token=xxx，握手由 JWT + Redis token 校验。
 */
export function createWsClient({ onMessage, onOpen, onClose } = {}) {
  let socket = null
  let heartbeatTimer = null
  let reconnectTimer = null
  let reconnectCount = 0
  let manualClosed = false

  const clearTimers = () => {
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    heartbeatTimer = null
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  const startHeartbeat = () => {
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    heartbeatTimer = setInterval(() => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000)
  }

  const connect = () => {
    const token = sessionStorage.getItem('token')
    if (!token) return
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const url = `${protocol}://${window.location.host}/ws/notice?token=${encodeURIComponent(token)}`
    socket = new WebSocket(url)

    socket.onopen = () => {
      reconnectCount = 0
      startHeartbeat()
      onOpen && onOpen()
    }

    socket.onmessage = (event) => {
      let payload = null
      try {
        payload = JSON.parse(event.data)
      } catch (e) {
        return
      }
      if (payload && payload.type === 'pong') return
      onMessage && onMessage(payload)
    }

    socket.onclose = () => {
      clearTimers()
      onClose && onClose()
      if (manualClosed) return
      reconnectCount += 1
      const delay = Math.min(1000 * 2 ** (reconnectCount - 1), 30000)
      reconnectTimer = setTimeout(connect, delay)
    }

    socket.onerror = () => {
      if (socket) socket.close()
    }
  }

  const close = () => {
    manualClosed = true
    clearTimers()
    if (socket) socket.close()
    socket = null
  }

  return { connect, close }
}
