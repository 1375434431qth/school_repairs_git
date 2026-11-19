<template>
  <div class="page-container">
    <PageHeader title="填写维修过程" :subtitle="detail ? `工单 ${detail.orderNo} · ${detail.buildingName} ${detail.roomNo}` : ''">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存维修记录</el-button>
        <el-button type="success" @click="router.push(`/repairer/orders/${orderId}/finish`)">去提交完工</el-button>
      </template>
    </PageHeader>

    <Panel v-loading="loading" title="维修过程">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" style="max-width: 820px">
        <el-form-item label="维修过程" prop="processDesc">
          <el-input
            v-model="form.processDesc"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请描述排查过程与处理方式（10~1000 字）"
          />
        </el-form-item>

        <el-form-item label="配件情况">
          <el-switch v-model="form.noParts" active-text="无需更换配件" inactive-text="使用配件" />
        </el-form-item>

        <template v-if="!form.noParts">
          <el-form-item v-for="(part, index) in form.parts" :key="index" :label="index === 0 ? '配件明细' : ''">
            <div class="part-row">
              <el-input v-model="part.partName" placeholder="配件名称" style="width: 200px" maxlength="50" />
              <el-input-number v-model="part.quantity" :min="0.01" :precision="2" :step="1" style="width: 120px" />
              <el-input v-model="part.unit" placeholder="单位" style="width: 90px" maxlength="10" />
              <el-input-number v-model="part.unitPrice" :min="0" :precision="2" :step="1" style="width: 130px" />
              <el-button link type="danger" @click="removePart(index)">删除</el-button>
            </div>
          </el-form-item>
          <el-form-item label=" ">
            <el-button plain @click="addPart">+ 添加配件</el-button>
          </el-form-item>
        </template>

        <el-form-item>
          <span class="form-tip">保存后仍可修改；提交完工前请确认维修过程与配件已保存。</span>
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
import { getRepairOrderDetail, saveRepairRecord } from '@/api/dispatch'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const detail = ref(null)
const orderId = computed(() => route.params.id)

const form = reactive({
  processDesc: '',
  noParts: false,
  parts: []
})

const rules = {
  processDesc: [
    { required: true, message: '请填写维修过程', trigger: 'blur' },
    { min: 10, max: 1000, message: '维修过程需 10~1000 字', trigger: 'blur' }
  ]
}

function addPart() {
  form.parts.push({ partName: '', quantity: 1, unit: '个', unitPrice: null })
}

function removePart(index) {
  form.parts.splice(index, 1)
}

async function load() {
  loading.value = true
  try {
    detail.value = await getRepairOrderDetail(orderId.value)
    if (!['ACCEPTED', 'ARRIVED'].includes(detail.value?.status)) {
      await ElMessageBox.alert('当前工单状态不可录入维修过程', '提示', { type: 'warning' })
      router.replace(`/repairer/orders/${orderId.value}`)
      return
    }
    const record = detail.value.repairRecord
    if (record) {
      form.processDesc = record.processDesc || ''
      form.noParts = record.noPartsFlag === 1
      form.parts = (record.parts || []).map((part) => ({
        partName: part.partName,
        quantity: Number(part.quantity),
        unit: part.unit || '个',
        unitPrice: part.unitPrice === null ? null : Number(part.unitPrice)
      }))
    }
    if (!form.parts.length && !form.noParts) addPart()
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  if (!form.noParts) {
    const invalid = form.parts.some((part) => !part.partName.trim() || !part.quantity)
    if (!form.parts.length) {
      ElMessage.warning('请至少添加一项配件，或勾选“无需更换配件”')
      return
    }
    if (invalid) {
      ElMessage.warning('请完整填写配件名称与数量')
      return
    }
  }
  submitting.value = true
  try {
    await saveRepairRecord(orderId.value, {
      processDesc: form.processDesc.trim(),
      noPartsFlag: form.noParts,
      parts: form.noParts
        ? []
        : form.parts.map((part) => ({
            partName: part.partName.trim(),
            quantity: part.quantity,
            unit: part.unit || undefined,
            unitPrice: part.unitPrice ?? undefined
          }))
    })
    ElMessage.success('维修记录已保存')
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.part-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
