import { reactive } from 'vue'

/** 列表分页状态：默认 10 条，切换筛选时回到第 1 页 */
export function usePagination(initialSize = 10) {
  const pagination = reactive({
    pageNum: 1,
    pageSize: initialSize,
    total: 0
  })

  const resetPage = () => {
    pagination.pageNum = 1
  }

  const apply = (pageResult) => {
    pagination.total = Number(pageResult?.total || 0)
    pagination.pageNum = Number(pageResult?.pageNum || pagination.pageNum)
    pagination.pageSize = Number(pageResult?.pageSize || pagination.pageSize)
  }

  return { pagination, resetPage, apply }
}
