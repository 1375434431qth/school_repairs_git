import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/** 按角色计算按钮可见性（仅提示性，最终以服务端状态与错误码为准） */
export function usePermission() {
  const userStore = useUserStore()
  const role = computed(() => userStore.role)
  const isStudent = computed(() => role.value === 'STUDENT')
  const isRepairer = computed(() => role.value === 'REPAIRER')
  const isManager = computed(() => role.value === 'MANAGER')

  const hasRole = (roles = []) => roles.includes(role.value)

  return { role, isStudent, isRepairer, isManager, hasRole }
}
