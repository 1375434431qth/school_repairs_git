import request from './request'

export const pageManagerOrders = (params) => request.get('/manager/orders', { params })
export const getManagerOrderDetail = (id) => request.get(`/manager/orders/${id}`)
export const assignOrder = (id, data) => request.put(`/manager/orders/${id}/assign`, data)
export const changePriority = (id, data) => request.put(`/manager/orders/${id}/priority`, data)
export const releaseOrder = (id, reason) => request.put(`/manager/orders/${id}/release`, { reason })
export const markTimeout = (id, data) => request.put(`/manager/orders/${id}/timeout`, data)
export const urgeOrder = (id) => request.put(`/manager/orders/${id}/urge`)
export const getBoard = (params) => request.get('/manager/orders/board', { params })
export const getTimeoutList = (params) => request.get('/manager/orders/timeout-list', { params })
export const getRepairerLoad = (params) => request.get('/manager/repairers/load', { params })
