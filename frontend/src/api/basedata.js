import request from './request'

export const listBuildings = () => request.get('/base/buildings')
export const listRooms = (buildingId) => request.get('/base/rooms', { params: { buildingId } })
export const listFaultTypes = () => request.get('/base/fault-types')
export const listRepairers = (buildingId) =>
  request.get('/base/repairers', { params: buildingId ? { buildingId } : {} })

export const createBuilding = (data) => request.post('/base/buildings', data)
export const updateBuilding = (id, data) => request.put(`/base/buildings/${id}`, data)
export const removeBuilding = (id) => request.delete(`/base/buildings/${id}`)

export const createRoom = (data) => request.post('/base/rooms', data)
export const updateRoom = (id, data) => request.put(`/base/rooms/${id}`, data)
export const removeRoom = (id) => request.delete(`/base/rooms/${id}`)

export const createFaultType = (data) => request.post('/base/fault-types', data)
export const updateFaultType = (id, data) => request.put(`/base/fault-types/${id}`, data)
export const removeFaultType = (id) => request.delete(`/base/fault-types/${id}`)
