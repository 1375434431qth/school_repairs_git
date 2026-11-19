/**
 * 工单状态 / 优先级 / 故障分类 / 超时环节 的统一字典。
 * 三处（列表、详情、看板）必须共用本文件，避免标签与按钮不一致。
 */
export const STATUS_LIST = [
  { value: 'CREATED', label: '待派单', color: '#909399', bg: '#f4f4f5' },
  { value: 'ASSIGNED', label: '待接单', color: '#409eff', bg: '#ecf5ff' },
  { value: 'ACCEPTED', label: '待上门', color: '#13c2c2', bg: '#e6fffb' },
  { value: 'ARRIVED', label: '维修中', color: '#fa8c16', bg: '#fff7e6' },
  { value: 'FINISHED', label: '待确认', color: '#9254de', bg: '#f9f0ff' },
  { value: 'REWORKING', label: '返修中', color: '#fa541c', bg: '#fff2e8' },
  { value: 'CONFIRMED', label: '待评价', color: '#52c41a', bg: '#f6ffed' },
  { value: 'CLOSED', label: '已完成', color: '#237804', bg: '#f0f9eb' },
  { value: 'CANCELLED', label: '已取消', color: '#bfbfbf', bg: '#fafafa' }
]

export const STATUS_MAP = STATUS_LIST.reduce((acc, item) => {
  acc[item.value] = item
  return acc
}, {})

export const REPAIRER_STATUS_LIST = STATUS_LIST.filter((item) => item.value !== 'CREATED')

export const PRIORITY_LIST = [
  { value: 1, label: '紧急', color: '#f5222d', hours: 4 },
  { value: 2, label: '高', color: '#fa8c16', hours: 8 },
  { value: 3, label: '普通', color: '#1ba3e0', hours: 24 },
  { value: 4, label: '低', color: '#8b94a3', hours: 72 }
]

export const PRIORITY_MAP = PRIORITY_LIST.reduce((acc, item) => {
  acc[item.value] = item
  return acc
}, {})

export const CATEGORY_MAP = {
  WATER_ELEC: '水电类',
  DOOR_WINDOW: '门窗类',
  NETWORK: '网络类',
  FURNITURE: '家具类',
  OTHER: '其他'
}

export const TIMEOUT_TYPE_MAP = {
  ACCEPT: '接单超时',
  ARRIVE: '到场超时',
  FINISH: '完工超时'
}

export const DISPATCH_TYPE_MAP = {
  ASSIGN: '管理员派单',
  GRAB: '维修工抢单',
  REJECT: '拒单',
  RELEASE: '管理员释放',
  CHANGE_PRIORITY: '调整优先级'
}

/** 流转轨迹动作编码 → 中文（后端 order_track.action） */
export const TRACK_ACTION_MAP = {
  CREATE: '提交报修',
  ASSIGN: '派单',
  GRAB: '抢单',
  ACCEPT: '接单',
  REJECT: '拒单',
  RELEASE: '确认弃单',
  CHANGE_PRIORITY: '调整优先级',
  TIMEOUT: '标记超时',
  URGE: '催单',
  ARRIVE: '到场',
  REPAIR_RECORD: '更新维修过程',
  FINISH: '提交完工',
  CANCEL: '取消工单',
  CONFIRM: '确认完工',
  EVALUATE: '提交评价',
  REOPEN: '反馈未修好',
  AUTO_CONFIRM: '系统自动确认',
  AUTO_CLOSE: '系统自动关闭'
}

export function trackActionLabel(value) {
  return TRACK_ACTION_MAP[value] || value || '状态变更'
}

export function statusLabel(value) {
  return STATUS_MAP[value]?.label || value || '-'
}

export function priorityLabel(value) {
  return PRIORITY_MAP[value]?.label || '-'
}

/** 当前状态 + 角色下可执行的动作，用于按钮置灰的提示性判断 */
export function actionsFor(status, role) {
  const actions = []
  if (role === 'STUDENT') {
    if (status === 'CREATED') actions.push('cancel')
    if (status === 'FINISHED') actions.push('confirm', 'reopen')
    if (status === 'CONFIRMED') actions.push('evaluate')
    if (status === 'CLOSED') actions.push('reopen')
  }
  if (role === 'REPAIRER') {
    if (status === 'ASSIGNED') actions.push('accept', 'reject')
    if (status === 'ACCEPTED') actions.push('arrive')
    if (status === 'REWORKING') actions.push('arrive')
    if (status === 'ARRIVED') actions.push('repair', 'finish')
  }
  if (role === 'MANAGER') {
    if (status === 'CREATED') actions.push('assign', 'priority')
    if (status === 'REWORKING') actions.push('assign', 'urge', 'timeout', 'release')
    if (status === 'ASSIGNED' || status === 'ACCEPTED' || status === 'ARRIVED') {
      actions.push('priority', 'urge', 'timeout')
    }
    if (status === 'ACCEPTED') actions.push('release')
  }
  return actions
}

export function useOrderStatus() {
  return {
    STATUS_LIST,
    STATUS_MAP,
    REPAIRER_STATUS_LIST,
    PRIORITY_LIST,
    PRIORITY_MAP,
    CATEGORY_MAP,
    TIMEOUT_TYPE_MAP,
    DISPATCH_TYPE_MAP,
    statusLabel,
    priorityLabel,
    actionsFor
  }
}
