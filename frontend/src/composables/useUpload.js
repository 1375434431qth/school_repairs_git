import { reactive } from 'vue'
import { ElMessage } from '@/utils/feedback'
import { uploadFile, removeFile } from '@/api/file'

const ACCEPT_TYPES = ['image/jpeg', 'image/png', 'image/webp']
const MAX_SIZE = 5 * 1024 * 1024
const MAX_PER_GROUP = 6

/**
 * 图片上传：选择即上传、逐张进度、失败可重试、可删除。
 * modelValue 结构：[{ fileId, url, status }]
 */
export function useUpload(bizType, modelValue) {
  const beforeUpload = (file) => {
    if (!ACCEPT_TYPES.includes(file.type)) {
      ElMessage.error('仅支持 jpg / png / webp 图片')
      return false
    }
    if (file.size > MAX_SIZE) {
      ElMessage.error('图片不能超过 5MB')
      return false
    }
    if (modelValue.value.length >= MAX_PER_GROUP) {
      ElMessage.error(`最多上传 ${MAX_PER_GROUP} 张图片`)
      return false
    }
    return true
  }

  const doUpload = async (file) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('bizType', bizType)
    // 必须是响应式对象：push 进数组后直接改原始对象不会触发视图更新
    const item = reactive({ fileId: '', url: '', status: 'uploading', name: file.name, raw: file, uid: file.uid })
    modelValue.value.push(item)
    try {
      const data = await uploadFile(formData)
      item.fileId = data.fileId
      item.url = data.url
      item.status = 'done'
    } catch (e) {
      item.status = 'failed'
    }
  }

  const retry = (item) => {
    if (!item.raw) return
    item.status = 'uploading'
    const formData = new FormData()
    formData.append('file', item.raw)
    formData.append('bizType', bizType)
    uploadFile(formData)
      .then((data) => {
        item.fileId = data.fileId
        item.url = data.url
        item.status = 'done'
      })
      .catch(() => {
        item.status = 'failed'
      })
  }

  const remove = async (index) => {
    const item = modelValue.value[index]
    if (!item) return
    modelValue.value.splice(index, 1)
    if (item.fileId) {
      try {
        await removeFile(item.fileId)
      } catch (e) {
        // 已提交过的图片删除失败不影响前端展示
      }
    }
  }

  const fileIds = () => modelValue.value.filter((item) => item.fileId).map((item) => item.fileId)
  const uploading = () => modelValue.value.some((item) => item.status === 'uploading')

  return { beforeUpload, doUpload, retry, remove, fileIds, uploading }
}
