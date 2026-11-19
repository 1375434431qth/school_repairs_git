import request from './request'

export const pageNotices = (params) => request.get('/notices', { params })
export const markNoticeRead = (id) => request.put(`/notices/${id}/read`)
export const markAllNoticesRead = () => request.put('/notices/read-all')
export const unreadCount = () => request.get('/notices/unread-count')
/** 管理员：全部通知分页 */
export const pageAllNotices = (params) => request.get('/manager/notices', { params })
