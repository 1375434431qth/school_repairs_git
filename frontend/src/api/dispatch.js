import request from './request'

export const poolOrders = (params) => request.get('/repairer/orders/pool', { params })
export const grabOrder = (id) => request.put(`/repairer/orders/${id}/grab`)
export const pageMyRepairOrders = (params) => request.get('/repairer/orders', { params })
export const getRepairOrderDetail = (id) => request.get(`/repairer/orders/${id}`)
export const acceptOrder = (id) => request.put(`/repairer/orders/${id}/accept`)
export const rejectOrder = (id, reason) => request.put(`/repairer/orders/${id}/reject`, { reason })
export const arriveOrder = (id) => request.put(`/repairer/orders/${id}/arrive`)
export const saveRepairRecord = (id, data) => request.post(`/repairer/orders/${id}/repair-record`, data)
export const finishOrder = (id, data) => request.put(`/repairer/orders/${id}/finish`, data)
export const getMyStat = (params) => request.get('/repairer/stat/mine', { params })
