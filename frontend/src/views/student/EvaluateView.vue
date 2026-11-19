<template>
  <div class="page-container">
    <PageHeader title="评价维修" :subtitle="detail ? `工单 ${detail.orderNo} · ${detail.buildingName} ${detail.roomNo}` : ''">
      <template #actions>
        <el-button @click="router.back()">返回</el-button>
      </template>
    </PageHeader>

    <Panel title="维修评价" body-padding="24px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 720px">
        <el-form-item label="维修评分" prop="score">
          <StarRate v-model="form.score" />
        </el-form-item>
        <el-form-item label="评价标签">
          <div class="tag-list">
            <el-check-tag
              v-for="tag in TAG_OPTIONS"
              :key="tag"
              :checked="form.tags.includes(tag)"
              class="tag-item"
              @change="toggleTag(tag)"
            >
              {{ tag }}
            </el-check-tag>
          </div>
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说本次维修体验（选填）"
          />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="form.anonymous" />
          <span class="form-tip" style="margin-left: 10px">匿名后维修工与管理端不显示您的姓名</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评价</el-button>
          <el-button @click="router.replace(`/student/orders/${orderId}`)">稍后再说</el-button>
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
import StarRate from '@/components/business/StarRate.vue'
import { evaluateOrder, getOrderDetail } from '@/api/order'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const detail = ref(null)

const TAG_OPTIONS = ['响应及时', '服务态度好', '维修专业', '现场整洁', '收费合理', '需要再修']

const form = reactive({
  score: 0,
  tags: [],
  content: '',
  anonymous: false
})

const rules = {
  score: [
    {
      validator: (rule, value, callback) =>
        value > 0 ? callback() : callback(new Error('请选择维修评分')),
      trigger: 'change'
    }
  ]
}

const orderId = computed(() => route.params.id)

function toggleTag(tag) {
  const index = form.tags.indexOf(tag)
  if (index >= 0) {
    form.tags.splice(index, 1)
  } else {
    form.tags.push(tag)
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await evaluateOrder(orderId.value, {
      score: form.score,
      tags: form.tags.join(',').slice(0, 100) || undefined,
      content: form.content || undefined,
      anonymous: form.anonymous
    })
    ElMessage.success('评价成功，感谢您的反馈')
    router.replace('/student/orders')
  } catch (error) {
    if (error?.code === 40902) {
      ElMessage.warning('该工单已评价')
      router.replace(`/student/orders/${orderId.value}`)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  detail.value = await getOrderDetail(orderId.value)
  if (detail.value?.status !== 'CONFIRMED') {
    await ElMessageBox.alert('当前工单不是待评价状态，无法评价', '提示', { type: 'warning' })
    router.replace(`/student/orders/${orderId.value}`)
  }
})
</script>

<style scoped>
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  font-size: 13px;
}
</style>
