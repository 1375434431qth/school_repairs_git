import request from './request'

/** 首页轮播（仅启用项，按 sort 升序） */
export const listCarousels = () => request.get('/carousels')

/** 管理端轮播列表（含停用） */
export const listManagerCarousels = () => request.get('/manager/carousels')
export const createCarousel = (data) => request.post('/manager/carousels', data)
export const updateCarousel = (id, data) => request.put(`/manager/carousels/${id}`, data)
export const deleteCarousel = (id) => request.delete(`/manager/carousels/${id}`)
export const sortCarousels = (ids) => request.put('/manager/carousels/sort', { ids })
