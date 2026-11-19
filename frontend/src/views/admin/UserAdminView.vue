<template>
  <div class="page-container">
    <PageHeader title="用户管理" subtitle="新建账号、维护基础信息、启停账号与管理员楼栋授权">
      <template #actions>
        <el-button type="primary" @click="openCreate">
          <AppIcon name="plus" :size="14" /> 新建用户
        </el-button>
      </template>
    </PageHeader>

    <Panel>
      <div class="toolbar">
        <el-select v-model="filters.role" placeholder="全部角色" clearable style="width: 140px" @change="handleSearch">
          <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="账号 / 姓名 / 手机号"
          clearable
          style="width: 230px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ pagination.total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">{{ row.roleLabel || row.role }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="buildingName" label="所属楼栋" width="130">
          <template #default="{ row }">{{ row.buildingName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="roomNo" label="房间号" width="90">
          <template #default="{ row }">{{ row.roomNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="技能标签" min-width="150">
          <template #default="{ row }">
            <template v-if="row.skillTags?.length">
              <el-tag v-for="tag in row.skillTags" :key="tag" size="small" class="skill-tag">{{ tag }}</el-tag>
            </template>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="statusLoadingId === row.id"
              @change="(value) => handleStatusChange(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.role === 'MANAGER'" link type="warning" @click="openBuildings(row)">
              楼栋授权
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="!loading && !list.length" description="暂无用户" />

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadList"
          @size-change="handleSearch"
        />
      </div>
    </Panel>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑用户' : '新建用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <template v-if="!editing">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" placeholder="3~32 位字母、数字或下划线" maxlength="32" />
          </el-form-item>
          <el-form-item label="初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="6~20 位" maxlength="20" />
          </el-form-item>
        </template>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="!editing" label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%" @change="onRoleChange">
            <el-option v-for="item in ROLE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属楼栋">
          <el-select v-model="form.buildingId" clearable style="width: 100%" @change="onFormBuildingChange">
            <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.role === 'STUDENT'" label="房间号">
          <el-select v-model="form.roomId" clearable :disabled="!form.buildingId" style="width: 100%">
            <el-option v-for="item in formRooms" :key="item.id" :label="item.roomNo" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.role === 'REPAIRER'" label="技能标签">
          <el-input v-model="form.skillTagsText" placeholder="多个标签用英文逗号分隔，如：水电,门锁" />
        </el-form-item>
        <el-form-item v-if="form.role === 'MANAGER'" label="授权楼栋">
          <el-select v-model="form.managedBuildingIds" multiple style="width: 100%">
            <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!editing" label="账号状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 楼栋授权 -->
    <el-dialog v-model="buildingVisible" title="管理员楼栋授权" width="480px">
      <div class="muted mb-12">{{ currentRow?.realName }}（{{ currentRow?.username }}）</div>
      <el-select v-model="buildingForm.buildingIds" multiple style="width: 100%" placeholder="请选择授权楼栋">
        <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <div class="form-tip mt-12">保存后该管理员只能看到授权楼栋内的工单。</div>
      <template #footer>
        <el-button @click="buildingVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBuildings">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import { usePagination } from '@/composables/usePagination'
import { useDictStore } from '@/stores/dict'
import {
  createUser,
  getManagedBuildings,
  pageUsers,
  saveManagedBuildings,
  updateStatus,
  updateUser
} from '@/api/user'
import { formatDateTime } from '@/utils/format'

const ROLE_OPTIONS = [
  { value: 'STUDENT', label: '学生' },
  { value: 'REPAIRER', label: '维修工' },
  { value: 'MANAGER', label: '宿舍管理员' }
]

const dictStore = useDictStore()
const { pagination } = usePagination()
const loading = ref(false)
const submitting = ref(false)
const statusLoadingId = ref(null)
const list = ref([])
const filters = reactive({ role: '', status: null, keyword: '' })

const formVisible = ref(false)
const editing = ref(false)
const formRef = ref(null)
const formRooms = ref([])
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  role: 'STUDENT',
  buildingId: null,
  roomId: null,
  status: 1,
  skillTagsText: '',
  managedBuildingIds: []
})

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{3,32}$/, message: '3~32 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const buildingVisible = ref(false)
const currentRow = ref(null)
const buildingForm = reactive({ buildingIds: [] })

async function loadList() {
  loading.value = true
  try {
    const data = await pageUsers({
      role: filters.role || undefined,
      status: filters.status === null || filters.status === '' ? undefined : filters.status,
      keyword: filters.keyword || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    list.value = data?.records || []
    pagination.total = Number(data?.total || 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadList()
}

function handleReset() {
  filters.role = ''
  filters.status = null
  filters.keyword = ''
  handleSearch()
}

function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.phone = ''
  form.role = 'STUDENT'
  form.buildingId = null
  form.roomId = null
  form.status = 1
  form.skillTagsText = ''
  form.managedBuildingIds = []
  formRooms.value = []
}

function openCreate() {
  editing.value = false
  resetForm()
  formVisible.value = true
}

async function openEdit(row) {
  editing.value = true
  resetForm()
  form.id = row.id
  form.role = row.role
  form.realName = row.realName
  form.phone = row.phone || ''
  form.buildingId = row.buildingId
  form.roomId = row.roomId
  form.skillTagsText = (row.skillTags || []).join(',')
  if (row.buildingId) formRooms.value = await dictStore.roomsOf(row.buildingId)
  formVisible.value = true
}

async function onRoleChange() {
  form.roomId = null
  if (form.role === 'MANAGER') {
    form.managedBuildingIds = []
  }
}

async function onFormBuildingChange(buildingId) {
  form.roomId = null
  formRooms.value = buildingId ? await dictStore.roomsOf(buildingId) : []
}

function parseTags(text) {
  return (text || '')
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

async function submitForm() {
  await formRef.value.validate()
  if (form.role === 'STUDENT' && (!form.buildingId || !form.roomId)) {
    ElMessage.warning('学生账号必须选择楼栋与房间')
    return
  }
  if (form.role === 'REPAIRER' && !form.buildingId) {
    ElMessage.warning('维修工账号必须选择负责楼栋')
    return
  }
  submitting.value = true
  try {
    if (editing.value) {
      await updateUser(form.id, {
        realName: form.realName,
        phone: form.phone || undefined,
        buildingId: form.buildingId || undefined,
        roomId: form.roomId || undefined,
        skillTags: form.role === 'REPAIRER' ? parseTags(form.skillTagsText) : undefined
      })
      ElMessage.success('用户信息已更新')
    } else {
      await createUser({
        username: form.username.trim(),
        password: form.password,
        realName: form.realName,
        phone: form.phone || undefined,
        role: form.role,
        buildingId: form.buildingId || undefined,
        roomId: form.roomId || undefined,
        status: form.status,
        skillTags: form.role === 'REPAIRER' ? parseTags(form.skillTagsText) : undefined,
        managedBuildingIds: form.role === 'MANAGER' ? form.managedBuildingIds : undefined
      })
      ElMessage.success('用户创建成功')
    }
    formVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row, enabled) {
  statusLoadingId.value = row.id
  try {
    await updateStatus(row.id, enabled ? 1 : 0)
    ElMessage.success(enabled ? '账号已启用' : '账号已禁用')
    loadList()
  } catch (error) {
    loadList()
  } finally {
    statusLoadingId.value = null
  }
}

async function openBuildings(row) {
  currentRow.value = row
  buildingForm.buildingIds = (await getManagedBuildings(row.id)) || []
  buildingVisible.value = true
}

async function submitBuildings() {
  submitting.value = true
  try {
    await saveManagedBuildings(currentRow.value.id, buildingForm.buildingIds)
    ElMessage.success('楼栋授权已保存')
    buildingVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  dictStore.loadBuildings()
  loadList()
})
</script>

<style scoped>
.skill-tag {
  margin-right: 6px;
  margin-bottom: 4px;
}
</style>
