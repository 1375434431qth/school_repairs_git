<template>
  <div class="page-container">
    <PageHeader title="提交完工" :subtitle="detail ? `工单 ${detail.orderNo} · ${detail.buildingName} ${detail.roomNo}` : ''">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交完工</el-button>
      </template>
    </PageHeader>

    <Panel v-loading="loading" title="完工信息">
      <el-alert
        v-if="detail && !detail.repairRecord"
        title="尚未保存维修过程"
        type="warning"
        :closable="false"
        show-icon
        description="提交完工前请先在“填写维修过程”中保存维修记录与配件。"
        class="mb-12"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" style="max-width: 760px">
        <el-form-item label="完工说明" prop="resultDesc">
          <el-input
            v-model="form.resultDesc"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请说明处理结果与现场恢复情况（如试水无渗漏、已清理现场）"
          />
        </el-form-item>
        <el-form-item label="完工照片" required>
          <div>
            <ImageUploader v-model="images" biz-type="AFTER" :max="6" />
            <div class="form-tip mt-12">至少 1 张、最多 6 张，支持 jpg / png / webp，单张不超过 5MB</div>
          </div>
        </el-form-item>
      </el-form>
    </Panel>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import ImageUploader from '@/components/business/ImageUploader.vue'
import { finishOrder, getRepairOrderDetail } from '@/api/dispatch'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)
const images = ref([])
const orderId = computed(() => route.params.id)

const form = reactive({ resultDesc: '' })

const rules = {
  resultDesc: [
    { required: true, message: '请填写完工说明', trigger: 'blur' },
    { max: 500, message: '完工说明不能超过 500 字', trigger: 'blur' }
  ]
}

const uploading = computed(() => images.value.some((item) => item.status === 'uploading'))

async function load() {
  loading.value = true
  try {
    detail.value = await getRepairOrderDetail(orderId.value)
    if (detail.value?.status !== 'ARRIVED') {
      await ElMessageBox.alert('仅“维修中”的工单可提交完工', '提示', { type: 'warning' })
      router.replace(`/repairer/orders/${orderId.value}`)
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  const fileIds = images.value.filter((item) => item.fileId).map((item) => item.fileId)
  if (!fileIds.length) {
    ElMessage.warning('请至少上传 1 张完工照片')
    return
  }
  if (uploading.value) {
    ElMessage.warning('图片上传中，请稍候')
    return
  }
  submitting.value = true
  try {
    await finishOrder(orderId.value, {
      resultDesc: form.resultDesc.trim(),
      afterImageFileIds: fileIds
    })
    ElMessage.success('已提交完工，等待学生确认')
    router.replace(`/repairer/orders/${orderId.value}`)
  } catch (error) {
    if (error?.code === 40009) {
      ElMessage.warning('工单状态已变化，请返回列表刷新')
      router.replace('/repairer/orders')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>
