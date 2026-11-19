import request from './request'

export const listRepairerRatings = () => request.get('/ratings/repairers')
export const pageEvaluations = (params) => request.get('/ratings/evaluations', { params })
