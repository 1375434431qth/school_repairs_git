/**
 * 命令式反馈 API 统一出口。
 * 从 element-plus 子路径导入，避免从桶文件导入把整包打进产物。
 */
export { ElMessage } from 'element-plus/es/components/message/index'
export { ElMessageBox } from 'element-plus/es/components/message-box/index'
export { ElNotification } from 'element-plus/es/components/notification/index'
