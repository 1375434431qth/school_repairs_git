import request from './request'

export const createOrder = (data) => request.post('/student/orders', data)
export const pageMyOrders = (params) => request.get('/student/orders', { params })
export const getOrderDetail = (id) => request.get(`/student/orders/${id}`)
export const cancelOrder = (id, reason) => request.put(`/student/orders/${id}/cancel`, { reason })
export const confirmOrder = (id) => request.put(`/student/orders/${id}/confirm`)
export const evaluateOrder = (id, data) => request.post(`/student/orders/${id}/evaluate`, data)
export const reopenOrder = (id, data) => request.post(`/student/orders/${id}/reopen`, data)
