import request from './request'

export const uploadFile = (formData) =>
  request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

export const removeFile = (fileId) => request.delete(`/files/${fileId}`)
