<template>
  <div class="page-container">
    <PageHeader title="基础数据" subtitle="维护楼栋、房间与故障类型字典，保存后立即刷新缓存" />

    <Panel body-padding="0">
      <el-tabs v-model="activeTab" class="data-tabs" @tab-change="onTabChange">
        <!-- 楼栋 -->
        <el-tab-pane label="楼栋管理" name="building">
          <div class="tab-body">
            <div class="toolbar">
              <el-button type="primary" @click="openBuilding()">
                <AppIcon name="plus" :size="14" /> 新增楼栋
              </el-button>
              <span class="spacer"></span>
              <span class="muted">共 {{ buildings.length }} 栋</span>
            </div>
            <el-table v-loading="loading" :data="buildings" border stripe>
              <el-table-column prop="name" label="楼栋名称" min-width="160" />
              <el-table-column prop="code" label="楼栋编号" width="130">
                <template #default="{ row }">{{ row.code || '-' }}</template>
              </el-table-column>
              <el-table-column prop="area" label="所属区域" width="140">
                <template #default="{ row }">{{ row.area || '-' }}</template>
              </el-table-column>
              <el-table-column prop="floors" label="楼层数" width="100">
                <template #default="{ row }">{{ row.floors ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="180">
                <template #default="{ row }">{{ row.remark || '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openBuilding(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeBuildingRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 房间 -->
        <el-tab-pane label="房间管理" name="room">
          <div class="tab-body">
            <div class="toolbar">
              <el-select v-model="currentBuildingId" placeholder="请选择楼栋" style="width: 200px" @change="loadRooms">
                <el-option v-for="item in buildings" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-button type="primary" :disabled="!currentBuildingId" @click="openRoom()">
                <AppIcon name="plus" :size="14" /> 新增房间
              </el-button>
              <span class="spacer"></span>
              <span class="muted">共 {{ rooms.length }} 间</span>
            </div>
            <el-table v-loading="loading" :data="rooms" border stripe>
              <el-table-column prop="roomNo" label="房间号" min-width="160" />
              <el-table-column prop="floor" label="楼层" width="120">
                <template #default="{ row }">{{ row.floor ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openRoom(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeRoomRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <EmptyState v-if="!loading && currentBuildingId && !rooms.length" description="该楼栋暂无房间" />
          </div>
        </el-tab-pane>

        <!-- 故障类型 -->
        <el-tab-pane label="故障类型" name="fault">
          <div class="tab-body">
            <div class="toolbar">
              <el-button type="primary" @click="openFault(null)">
                <AppIcon name="plus" :size="14" /> 新增一级分类
              </el-button>
              <span class="spacer"></span>
              <span class="muted">共 {{ faultTypes.length }} 个一级分类</span>
            </div>
            <el-table v-loading="loading" :data="faultTypes" row-key="id" border default-expand-all>
              <el-table-column prop="name" label="名称" min-width="180" />
              <el-table-column label="分类" width="120">
                <template #default="{ row }">{{ row.categoryName || CATEGORY_MAP[row.category] || '-' }}</template>
              </el-table-column>
              <el-table-column label="默认优先级" width="110">
                <template #default="{ row }">
                  <span :style="{ color: PRIORITY_MAP[row.defaultPriority]?.color }">
                    {{ PRIORITY_MAP[row.defaultPriority]?.label || '-' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="期望时长" width="100">
                <template #default="{ row }">{{ row.expectHours ? `${row.expectHours}h` : '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="230" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="!row.parentId" link type="primary" @click="openFault(null, row)">加子类</el-button>
                  <el-button link type="primary" @click="openFault(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeFaultRow(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </Panel>

    <!-- 楼栋表单 -->
    <el-dialog v-model="buildingVisible" :title="buildingForm.id ? '编辑楼栋' : '新增楼栋'" width="480px">
      <el-form ref="buildingRef" :model="buildingForm" :rules="buildingRules" label-width="90px">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="buildingForm.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="楼栋编号">
          <el-input v-model="buildingForm.code" maxlength="20" />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="buildingForm.area" maxlength="50" />
        </el-form-item>
        <el-form-item label="楼层数">
          <el-input-number v-model="buildingForm.floors" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="buildingForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBuilding">保存</el-button>
      </template>
    </el-dialog>

    <!-- 房间表单 -->
    <el-dialog v-model="roomVisible" :title="roomForm.id ? '编辑房间' : '新增房间'" width="440px">
      <el-form ref="roomRef" :model="roomForm" :rules="roomRules" label-width="90px">
        <el-form-item label="所属楼栋">{{ buildingNameOf(roomForm.buildingId) }}</el-form-item>
        <el-form-item label="房间号" prop="roomNo">
          <el-input v-model="roomForm.roomNo" maxlength="10" placeholder="如 302" />
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="roomForm.floor" :min="1" :max="99" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRoom">保存</el-button>
      </template>
    </el-dialog>

    <!-- 故障类型表单 -->
    <el-dialog v-model="faultVisible" :title="faultForm.id ? '编辑故障类型' : '新增故障类型'" width="520px">
      <el-form ref="faultRef" :model="faultForm" :rules="faultRules" label-width="100px">
        <el-form-item label="上级分类">
          <el-select v-model="faultForm.parentId" style="width: 100%">
            <el-option label="一级分类（无上级）" :value="0" />
            <el-option
              v-for="item in faultTypes"
              :key="item.id"
              :label="item.name"
              :value="item.id"
              :disabled="item.id === faultForm.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="faultForm.name" maxlength="30" />
        </el-form-item>
        <el-form-item label="故障分类" prop="category">
          <el-select v-model="faultForm.category" style="width: 100%">
            <el-option v-for="(label, value) in CATEGORY_MAP" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认优先级">
          <PrioritySelect v-model="faultForm.defaultPriority" />
        </el-form-item>
        <el-form-item label="期望时长(h)">
          <el-input-number v-model="faultForm.expectHours" :min="1" :max="720" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="faultForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="faultForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitFault">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import PrioritySelect from '@/components/business/PrioritySelect.vue'
import { CATEGORY_MAP, PRIORITY_MAP } from '@/composables/useOrderStatus'
import { useDictStore } from '@/stores/dict'
import {
  createBuilding,
  createFaultType,
  createRoom,
  listFaultTypes,
  updateBuilding,
  updateFaultType,
  updateRoom,
  removeBuilding,
  removeFaultType,
  removeRoom
} from '@/api/basedata'

const dictStore = useDictStore()
const activeTab = ref('building')
const loading = ref(false)
const submitting = ref(false)
const buildings = ref([])
const rooms = ref([])
const faultTypes = ref([])
const currentBuildingId = ref(null)

const buildingVisible = ref(false)
const buildingRef = ref(null)
const buildingForm = reactive({ id: null, name: '', code: '', area: '', floors: 6, remark: '' })
const buildingRules = {
  name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }]
}

const roomVisible = ref(false)
const roomRef = ref(null)
const roomForm = reactive({ id: null, buildingId: null, roomNo: '', floor: null })
const roomRules = {
  roomNo: [{ required: true, message: '请输入房间号', trigger: 'blur' }]
}

const faultVisible = ref(false)
const faultRef = ref(null)
const faultForm = reactive({
  id: null,
  parentId: 0,
  name: '',
  category: 'OTHER',
  defaultPriority: 3,
  expectHours: 24,
  sort: 0,
  status: 1
})
const faultRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

function buildingNameOf(id) {
  return buildings.value.find((item) => item.id === id)?.name || '-'
}

async function loadBuildings() {
  loading.value = true
  try {
    buildings.value = (await dictStore.loadBuildings(true)) || []
    if (!currentBuildingId.value && buildings.value.length) {
      currentBuildingId.value = buildings.value[0].id
    }
  } finally {
    loading.value = false
  }
}

async function loadRooms() {
  if (!currentBuildingId.value) return
  loading.value = true
  try {
    rooms.value = (await dictStore.roomsOf(currentBuildingId.value, true)) || []
  } finally {
    loading.value = false
  }
}

async function loadFaults() {
  loading.value = true
  try {
    faultTypes.value = (await listFaultTypes()) || []
    dictStore.faultLoaded = false
  } finally {
    loading.value = false
  }
}

function onTabChange(name) {
  if (name === 'room') loadRooms()
  if (name === 'fault') loadFaults()
}

function openBuilding(row) {
  buildingForm.id = row?.id || null
  buildingForm.name = row?.name || ''
  buildingForm.code = row?.code || ''
  buildingForm.area = row?.area || ''
  buildingForm.floors = row?.floors ?? 6
  buildingForm.remark = row?.remark || ''
  buildingVisible.value = true
}

async function submitBuilding() {
  await buildingRef.value.validate()
  submitting.value = true
  try {
    if (buildingForm.id) {
      await updateBuilding(buildingForm.id, { ...buildingForm })
      ElMessage.success('楼栋已更新')
    } else {
      await createBuilding({ ...buildingForm })
      ElMessage.success('楼栋已新增')
    }
    buildingVisible.value = false
    dictStore.invalidate()
    await loadBuildings()
    if (activeTab.value === 'room') loadRooms()
  } finally {
    submitting.value = false
  }
}

async function removeBuildingRow(row) {
  try {
    await ElMessageBox.confirm(`确认删除楼栋「${row.name}」？删除后不影响历史工单。`, '删除楼栋', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await removeBuilding(row.id)
    ElMessage.success('楼栋已删除')
    dictStore.invalidate()
    await loadBuildings()
    if (currentBuildingId.value === row.id) {
      rooms.value = []
      currentBuildingId.value = buildings.value[0]?.id || null
      if (currentBuildingId.value) loadRooms()
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

function openRoom(row) {
  if (!currentBuildingId.value) {
    ElMessage.warning('请先选择楼栋')
    return
  }
  roomForm.id = row?.id || null
  roomForm.buildingId = row?.buildingId || currentBuildingId.value
  roomForm.roomNo = row?.roomNo || ''
  roomForm.floor = row?.floor ?? null
  roomVisible.value = true
}

async function submitRoom() {
  await roomRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      buildingId: roomForm.buildingId,
      roomNo: roomForm.roomNo.trim(),
      floor: roomForm.floor || undefined
    }
    if (roomForm.id) {
      await updateRoom(roomForm.id, payload)
      ElMessage.success('房间已更新')
    } else {
      await createRoom(payload)
      ElMessage.success('房间已新增')
    }
    roomVisible.value = false
    dictStore.invalidate()
    loadRooms()
  } finally {
    submitting.value = false
  }
}

async function removeRoomRow(row) {
  try {
    await ElMessageBox.confirm(`确认删除房间「${row.roomNo}」？`, '删除房间', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await removeRoom(row.id)
    ElMessage.success('房间已删除')
    loadRooms()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

function openFault(row, parent) {
  faultForm.id = row?.id || null
  faultForm.parentId = row?.parentId ?? parent?.id ?? 0
  faultForm.name = row?.name || ''
  faultForm.category = row?.category || parent?.category || 'OTHER'
  faultForm.defaultPriority = row?.defaultPriority ?? parent?.defaultPriority ?? 3
  faultForm.expectHours = row?.expectHours ?? parent?.expectHours ?? 24
  faultForm.sort = row?.sort ?? 0
  faultForm.status = row?.status ?? 1
  faultVisible.value = true
}

async function submitFault() {
  await faultRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...faultForm }
    delete payload.id
    if (faultForm.id) {
      await updateFaultType(faultForm.id, payload)
      ElMessage.success('故障类型已更新')
    } else {
      await createFaultType(payload)
      ElMessage.success('故障类型已新增')
    }
    faultVisible.value = false
    loadFaults()
  } finally {
    submitting.value = false
  }
}

async function removeFaultRow(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.name}」？`, '删除故障类型', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await removeFaultType(row.id)
    ElMessage.success('故障类型已删除')
    loadFaults()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
  }
}

onMounted(async () => {
  await loadBuildings()
})
</script>

<style scoped>
.data-tabs {
  padding: 0 16px 16px;
}

.data-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.tab-body {
  padding-top: 8px;
}
</style>
