import request from './request'

export const updateProfile = (data) => request.put('/users/profile', data)
export const updateStatus = (id, status) => request.put(`/users/${id}/status`, { status })

export const pageUsers = (params) => request.get('/users', { params })
export const createUser = (data) => request.post('/users', data)
export const updateUser = (id, data) => request.put(`/users/${id}`, data)
export const getManagedBuildings = (id) => request.get(`/users/${id}/buildings`)
export const saveManagedBuildings = (id, buildingIds) =>
  request.put(`/users/${id}/buildings`, { buildingIds })
