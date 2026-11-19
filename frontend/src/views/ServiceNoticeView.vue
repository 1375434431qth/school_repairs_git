<template>
  <div class="page-container">
    <PageHeader title="服务公告" :subtitle="subtitle" />

    <template v-if="isStudent">
      <Panel title="维修工评分榜">
        <template #extra>
          <span class="muted">平均星级由该维修工全部评价计算</span>
          <el-button size="small" :loading="ratingLoading" @click="loadRatings">刷新</el-button>
        </template>
        <el-table
          v-loading="ratingLoading"
          :data="ratings"
          highlight-current-row
          @row-click="selectRepairer"
        >
          <el-table-column label="排名" type="index" width="70" />
          <el-table-column prop="repairerName" label="维修工" min-width="120" />
          <el-table-column label="平均星级" min-width="220">
            <template #default="{ row }">
              <template v-if="row.avgScore !== null && row.avgScore !== undefined">
                <el-rate :model-value="row.avgScore" disabled allow-half show-score score-template="{value}" />
              </template>
              <span v-else class="muted">暂无评价</span>
            </template>
          </el-table-column>
          <el-table-column prop="ratingCount" label="评价数" width="90" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="selectRepairer(row)">查看评价</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState description="暂无维修工数据" />
          </template>
        </el-table>
      </Panel>

      <Panel :title="evalPanelTitle">
        <div class="toolbar">
          <el-select v-model="evalFilters.repairerId" placeholder="全部维修工" clearable style="width: 180px">
            <el-option
              v-for="item in ratings"
              :key="item.repairerId"
              :label="item.repairerName"
              :value="item.repairerId"
            />
          </el-select>
          <el-select v-model="evalFilters.score" placeholder="全部星级" clearable style="width: 140px">
            <el-option v-for="star in [5, 4, 3, 2, 1]" :key="star" :label="`${star} 星`" :value="star" />
          </el-select>
          <el-button type="primary" @click="handleEvalSearch">查询</el-button>
          <el-button @click="handleEvalReset">重置</el-button>
          <span class="spacer"></span>
          <span class="muted">共 {{ evalPagination.total }} 条</span>
        </div>
        <el-table v-loading="evalLoading" :data="evaluations">
          <el-table-column label="评分" width="200">
            <template #default="{ row }">
              <el-rate :model-value="row.score" disabled />
            </template>
          </el-table-column>
          <el-table-column prop="repairerName" label="维修工" width="120" />
          <el-table-column prop="tags" label="标签" min-width="140" />
          <el-table-column prop="content" label="评价内容" min-width="240">
            <template #default="{ row }">{{ row.content || '（无文字评价）' }}</template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <template #empty>
            <EmptyState :description="selectedRepairer ? '该维修工暂无评价' : '暂无评价数据'" />
          </template>
        </el-table>
        <div class="pagination">
          <el-pagination
            v-model:current-page="evalPagination.pageNum"
            v-model:page-size="evalPagination.pageSize"
            :total="evalPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadEvaluations"
            @size-change="handleEvalSizeChange"
          />
        </div>
      </Panel>
    </template>

    <Panel v-else-if="isRepairer" title="我的服务通知">
      <div class="toolbar">
        <el-select v-model="noticeFilters.readFlag" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-button type="primary" @click="handleNoticeSearch">查询</el-button>
        <el-button @click="handleNoticeReset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ noticePagination.total }} 条</span>
      </div>
      <el-table v-loading="noticeLoading" :data="myNotices">
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.readFlag === 0" type="warning" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="170" />
        <el-table-column prop="content" label="内容" min-width="280" />
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>
          <EmptyState description="暂无服务通知" />
        </template>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="noticePagination.pageNum"
          v-model:page-size="noticePagination.pageSize"
          :total="noticePagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadMyNotices"
          @size-change="handleNoticeSizeChange"
        />
      </div>
    </Panel>

    <Panel v-else title="全部通知">
      <div class="toolbar">
        <el-select v-model="noticeFilters.readFlag" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <el-select v-model="noticeFilters.type" placeholder="全部类型" clearable style="width: 170px">
          <el-option v-for="item in NOTICE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="noticeFilters.keyword"
          placeholder="接收人姓名 / 账号"
          clearable
          style="width: 200px"
          @keyup.enter="handleNoticeSearch"
        />
        <el-button type="primary" @click="handleNoticeSearch">查询</el-button>
        <el-button @click="handleNoticeReset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted">共 {{ noticePagination.total }} 条</span>
      </div>
      <el-table v-loading="noticeLoading" :data="allNotices">
        <el-table-column prop="receiverName" label="接收人" width="120" />
        <el-table-column prop="typeName" label="类型" width="130" />
        <el-table-column prop="title" label="标题" min-width="170" />
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.readFlag === 0" type="warning" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>
          <EmptyState description="暂无通知" />
        </template>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="noticePagination.pageNum"
          v-model:page-size="noticePagination.pageSize"
          :total="noticePagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadAllNotices"
          @size-change="handleNoticeSizeChange"
        />
      </div>
    </Panel>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { usePagination } from '@/composables/usePagination'
import { listRepairerRatings, pageEvaluations as pageRatingEvaluations } from '@/api/rating'
import { pageAllNotices, pageNotices } from '@/api/notice'
import { formatDateTime } from '@/utils/format'

const userStore = useUserStore()

const isStudent = computed(() => userStore.role === 'STUDENT')
const isRepairer = computed(() => userStore.role === 'REPAIRER')

const subtitle = computed(() => {
  if (isStudent.value) return '查看维修工服务评分与匿名评价'
  if (isRepairer.value) return '仅展示与您维修工单相关的通知'
  return '查看全站站内通知'
})

// 学生：评分榜 + 匿名评价
const ratingLoading = ref(false)
const ratings = ref([])
const evalLoading = ref(false)
const evaluations = ref([])
const evalFilters = reactive({ repairerId: null, score: null })
const { pagination: evalPagination, apply: applyEvalPage } = usePagination()

const evalPanelTitle = computed(() => {
  const matched = ratings.value.find((item) => item.repairerId === evalFilters.repairerId)
  return matched ? `${matched.repairerName} 的匿名评价` : '全部匿名评价'
})

async function loadRatings() {
  ratingLoading.value = true
  try {
    ratings.value = (await listRepairerRatings()) || []
  } finally {
    ratingLoading.value = false
  }
}

async function loadEvaluations() {
  evalLoading.value = true
  try {
    const data = await pageRatingEvaluations({
      repairerId: evalFilters.repairerId || undefined,
      score: evalFilters.score || undefined,
      pageNum: evalPagination.pageNum,
      pageSize: evalPagination.pageSize
    })
    evaluations.value = data?.records || []
    applyEvalPage(data)
  } finally {
    evalLoading.value = false
  }
}

function selectRepairer(row) {
  evalFilters.repairerId = row.repairerId
  evalPagination.pageNum = 1
  loadEvaluations()
}

function handleEvalSearch() {
  evalPagination.pageNum = 1
  loadEvaluations()
}

function handleEvalReset() {
  evalFilters.repairerId = null
  evalFilters.score = null
  handleEvalSearch()
}

function handleEvalSizeChange() {
  evalPagination.pageNum = 1
  loadEvaluations()
}

// 维修工 / 管理员：通知
const noticeLoading = ref(false)
const myNotices = ref([])
const allNotices = ref([])
const noticeFilters = reactive({ readFlag: '', type: '', keyword: '' })
const { pagination: noticePagination, apply: applyNoticePage } = usePagination()

const NOTICE_TYPES = [
  { value: 'ORDER_ASSIGNED', label: '派单/抢单成功' },
  { value: 'ORDER_REJECTED', label: '维修工拒单' },
  { value: 'ORDER_ACCEPTED', label: '维修工接单' },
  { value: 'ORDER_ARRIVED', label: '维修工到场' },
  { value: 'ORDER_FINISHED', label: '维修完成待确认' },
  { value: 'ORDER_EVALUATED', label: '收到评价' },
  { value: 'ORDER_TIMEOUT', label: '工单超时' },
  { value: 'ORDER_URGED', label: '管理员催单' },
  { value: 'ORDER_REWORK', label: '生成返修单' }
]

async function loadMyNotices() {
  noticeLoading.value = true
  try {
    const data = await pageNotices({
      readFlag: noticeFilters.readFlag === '' ? undefined : noticeFilters.readFlag,
      pageNum: noticePagination.pageNum,
      pageSize: noticePagination.pageSize
    })
    myNotices.value = data?.records || []
    applyNoticePage(data)
  } finally {
    noticeLoading.value = false
  }
}

async function loadAllNotices() {
  noticeLoading.value = true
  try {
    const data = await pageAllNotices({
      readFlag: noticeFilters.readFlag === '' ? undefined : noticeFilters.readFlag,
      type: noticeFilters.type || undefined,
      keyword: noticeFilters.keyword.trim() || undefined,
      pageNum: noticePagination.pageNum,
      pageSize: noticePagination.pageSize
    })
    allNotices.value = data?.records || []
    applyNoticePage(data)
  } finally {
    noticeLoading.value = false
  }
}

function handleNoticeSearch() {
  noticePagination.pageNum = 1
  if (isRepairer.value) {
    loadMyNotices()
  } else {
    loadAllNotices()
  }
}

function handleNoticeReset() {
  noticeFilters.readFlag = ''
  noticeFilters.type = ''
  noticeFilters.keyword = ''
  handleNoticeSearch()
}

function handleNoticeSizeChange() {
  noticePagination.pageNum = 1
  if (isRepairer.value) {
    loadMyNotices()
  } else {
    loadAllNotices()
  }
}

onMounted(() => {
  if (isStudent.value) {
    loadRatings()
    loadEvaluations()
  } else if (isRepairer.value) {
    loadMyNotices()
  } else {
    loadAllNotices()
  }
})
</script>
