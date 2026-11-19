import request from './request'

export const getOverview = () => request.get('/manager/stat/overview')
export const getTypeDist = (params) => request.get('/manager/stat/type-dist', { params })
export const getAvgDuration = (params) => request.get('/manager/stat/avg-duration', { params })
export const getReworkRate = (params) => request.get('/manager/stat/rework-rate', { params })
export const getRepairerRank = (params) => request.get('/manager/stat/repairer-rank', { params })
export const refreshStat = () => request.post('/manager/stat/refresh')
