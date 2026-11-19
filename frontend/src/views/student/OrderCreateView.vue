<template>
  <div class="page-container">
    <PageHeader title="我要报修" subtitle="请如实填写故障信息并上传现场照片，提交后可在“我的工单”跟踪进度" />

    <Panel title="报修信息">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" style="max-width: 760px">
        <el-form-item label="报修楼栋" prop="buildingId">
          <el-select
            v-model="form.buildingId"
            placeholder="请选择楼栋"
            style="width: 260px"
            @change="onBuildingChange"
          >
            <el-option v-for="item in dictStore.buildings" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号" prop="roomId">
          <el-select v-model="form.roomId" placeholder="请选择房间" style="width: 260px" :disabled="!form.buildingId">
            <el-option v-for="item in rooms" :key="item.id" :label="item.roomNo" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障类型" prop="faultTypeId">
          <el-cascader
            v-model="form.faultTypeId"
            :options="dictStore.faultOptions"
            :props="{ value: 'value', label: 'label', children: 'children', checkStrictly: true, emitPath: false }"
            placeholder="请选择故障类型"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="选填，便于维修工联系" maxlength="11" style="width: 260px" />
        </el-form-item>
        <el-form-item label="故障描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述故障现象（10~500 字），例如：水龙头关不紧，持续滴水"
          />
        </el-form-item>
        <el-form-item label="现场照片" required>
          <div>
            <ImageUploader v-model="images" biz-type="BEFORE" :max="6" />
            <div class="form-tip mt-12">支持 jpg / png / webp，单张不超过 5MB，最多 6 张</div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交报修</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </Panel>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import ImageUploader from '@/components/business/ImageUploader.vue'
import { useDictStore } from '@/stores/dict'
import { createOrder } from '@/api/order'

const router = useRouter()
const dictStore = useDictStore()
const formRef = ref(null)
const submitting = ref(false)
const rooms = ref([])
const images = ref([])

const form = reactive({
  buildingId: null,
  roomId: null,
  faultTypeId: null,
  contactPhone: '',
  description: ''
})

const rules = {
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择房间', trigger: 'change' }],
  faultTypeId: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  description: [
    { required: true, message: '请填写故障描述', trigger: 'blur' },
    { min: 10, max: 500, message: '故障描述需 10~500 字', trigger: 'blur' }
  ],
  contactPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const uploading = computed(() => images.value.some((item) => item.status === 'uploading'))

async function onBuildingChange(buildingId) {
  form.roomId = null
  rooms.value = []
  if (buildingId) {
    rooms.value = await dictStore.roomsOf(buildingId)
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  const fileIds = images.value.filter((item) => item.fileId).map((item) => item.fileId)
  if (!fileIds.length) {
    ElMessage.warning('请至少上传 1 张现场照片')
    return
  }
  if (uploading.value) {
    ElMessage.warning('图片上传中，请稍候')
    return
  }
  if (fileIds.length > 6) {
    ElMessage.warning('现场照片最多 6 张')
    return
  }
  submitting.value = true
  try {
    const data = await createOrder({
      buildingId: form.buildingId,
      roomId: form.roomId,
      faultTypeId: form.faultTypeId,
      description: form.description.trim(),
      contactPhone: form.contactPhone || undefined,
      imageFileIds: fileIds
    })
    ElMessage.success(`报修提交成功，工单号 ${data.orderNo}`)
    router.replace(`/student/orders/${data.orderId}`)
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value.resetFields()
  images.value = []
  rooms.value = []
}

onMounted(async () => {
  await Promise.all([dictStore.loadBuildings(), dictStore.loadFaultTypes()])
})
</script>
