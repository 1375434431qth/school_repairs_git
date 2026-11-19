<template>
  <div class="home">
    <!-- 轮播主视觉 -->
    <section class="home-hero">
      <el-carousel
        v-if="slides.length"
        :interval="5000"
        :autoplay="true"
        arrow="always"
        height="100%"
        class="hero-carousel"
      >
        <el-carousel-item v-for="slide in slides" :key="slide.id || slide.imageUrl">
          <div class="hero-slide" :style="{ backgroundImage: `url(${slide.imageUrl})` }">
            <div class="hero-mask"></div>
            <div class="hero-inner">
              <div class="hero-brand">
                <img src="/images/logo.png" alt="重庆城市科技学院" class="hero-logo" />
                <div>
                  <div class="hero-school">重庆城市科技学院</div>
                  <div class="hero-school-en">重庆城市科技学院 · 公寓报修服务平台</div>
                </div>
              </div>
              <h1 class="hero-title">{{ slide.title || '校园宿舍报修与维修工单派单系统' }}</h1>
              <p class="hero-subtitle">学生报修 · 维修派单 · 工单跟踪 · 评价闭环</p>
              <p class="hero-en">校园宿舍报修与维修工单派单平台</p>
              <el-button type="primary" size="large" class="hero-btn" @click="goPrimary">
                {{ primaryAction.label }}
                <span class="hero-arrow">→</span>
              </el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

      <el-button
        v-if="userStore.role === 'MANAGER'"
        class="hero-manage"
        plain
        @click="router.push('/admin/carousel')"
      >
        管理轮播
      </el-button>
    </section>

    <!-- 四步流程 -->
    <section class="home-steps">
      <div v-for="step in steps" :key="step.title" class="step-item">
        <span class="step-icon"><AppIcon :name="step.icon" :size="26" /></span>
        <div>
          <div class="step-title">{{ step.title }}</div>
          <div class="step-desc">{{ step.desc }}</div>
        </div>
      </div>
    </section>

    <!-- 内容模块 -->
    <div class="home-content">
      <div class="content-row">
        <Panel title="关于我们" class="about-panel">
          <template #extra>
            <span>宿舍报修</span>
          </template>
          <div class="about-body">
            <el-image :src="campusImages[0]" fit="cover" class="about-image" :preview-src-list="campusImages" />
            <div class="about-text">
              <p>
                校园宿舍报修与维修工单派单系统面向学生、维修人员与宿舍管理员三类角色，将“报修—派单—上门—完工—评价”全流程线上化。
              </p>
              <p>
                学生可上传现场照片提交报修并实时跟踪进度；维修工可抢单、登记到场与服务过程；管理员可按楼栋派单、跟踪看板、标记超时并查看统计报表。
              </p>
              <ul class="about-list">
                <li>8 个工单状态全流程留痕，责任可追溯</li>
                <li>按楼栋派单 + 维修工抢单两种调度模式</li>
                <li>超时标记、返修跟踪与三项核心指标可视化</li>
              </ul>
            </div>
          </div>
        </Panel>
      </div>

      <div class="content-row split">
        <Panel title="校园环境">
          <template #extra>
            <span>学生公寓实景</span>
          </template>
          <div class="campus-grid">
            <el-image
              v-for="(image, index) in campusImages"
              :key="image"
              :src="image"
              :preview-src-list="campusImages"
              :initial-index="index"
              fit="cover"
              class="campus-image"
            />
          </div>
          <div class="campus-caption muted">干净整洁的宿舍环境，是安心学习与生活的基础。</div>
        </Panel>

        <Panel title="服务公告">
          <template #extra>
            <router-link to="/service-notices" class="more-link">更多</router-link>
          </template>
          <ul v-if="notices.length" class="notice-list">
            <li v-for="item in notices" :key="item.id" class="notice-row">
              <span class="notice-date">{{ formatDate(item.createTime) }}</span>
              <span class="notice-text">{{ item.title }}</span>
            </li>
          </ul>
          <ul v-else class="notice-list">
            <li v-for="tip in fallbackTips" :key="tip" class="notice-row">
              <span class="notice-date">须知</span>
              <span class="notice-text">{{ tip }}</span>
            </li>
          </ul>
          <div class="notice-foot muted">报修后请保持电话畅通，便于维修师傅联系上门。</div>
        </Panel>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Panel from '@/components/base/Panel.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import { useUserStore } from '@/stores/user'
import { useCarouselStore } from '@/stores/carousel'
import { pageNotices } from '@/api/notice'
import { formatDate } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const carouselStore = useCarouselStore()

const FALLBACK_SLIDES = [
  { id: 'f1', title: '学生公寓外景', imageUrl: '/images/campus-1.jpg' },
  { id: 'f2', title: '标准化宿舍内景', imageUrl: '/images/campus-2.jpg' },
  { id: 'f3', title: '公寓楼近景', imageUrl: '/images/campus-3.jpg' }
]
const campusImages = ['/images/campus-1.jpg', '/images/campus-2.jpg', '/images/campus-3.jpg']
const fallbackTips = [
  '报修时请选择正确的楼栋与房间，并上传 1~6 张现场照片',
  '维修工到场后请确认维修内容，完工后可评价本次服务',
  '未修好可在工单详情页反馈，系统将自动生成返修单'
]

const slides = ref([])
const notices = ref([])

const steps = computed(() => {
  const role = userStore.role
  const link = role === 'REPAIRER' ? '/repairer/pool' : role === 'MANAGER' ? '/admin/dispatch' : '/student/orders/create'
  return [
    { icon: 'clipboard', title: '第一步：提交报修', desc: '选择楼栋房间并上传现场照片', link: '/student/orders/create' },
    { icon: 'users', title: '第二步：等待派单', desc: '管理员按楼栋指派维修工', link },
    { icon: 'wrench', title: '第三步：上门维修', desc: '维修工接单、到场并登记过程', link },
    { icon: 'star', title: '第四步：确认评价', desc: '确认完工并对服务作出评价', link: '/student/orders' }
  ]
})

const primaryAction = computed(() => {
  const role = userStore.role
  if (role === 'REPAIRER') return { label: '进入抢单池', path: '/repairer/pool' }
  if (role === 'MANAGER') return { label: '进入派单看板', path: '/admin/board' }
  return { label: '立即报修', path: '/student/orders/create' }
})

function goPrimary() {
  router.push(primaryAction.value.path)
}

onMounted(async () => {
  try {
    const list = await carouselStore.load()
    slides.value = list.length ? list : FALLBACK_SLIDES
  } catch (e) {
    slides.value = FALLBACK_SLIDES
  }
  try {
    const data = await pageNotices({ pageNum: 1, pageSize: 3 })
    notices.value = data?.records || []
  } catch (e) {
    notices.value = []
  }
})
</script>

<style scoped>
.home {
  background: var(--bg);
}

/* ---------- 轮播 ---------- */
.home-hero {
  position: relative;
  width: 100%;
  height: 380px;
  background: #0f6f9c;
  overflow: hidden;
}

.hero-carousel {
  height: 100%;
}

.hero-carousel :deep(.el-carousel__container) {
  height: 100%;
}

.hero-slide {
  position: relative;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
}

.hero-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(8, 41, 60, 0.78) 0%, rgba(8, 41, 60, 0.45) 46%, rgba(8, 41, 60, 0.12) 100%);
}

.hero-inner {
  position: relative;
  width: 1200px;
  max-width: calc(100% - 48px);
  height: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.hero-logo {
  width: 52px;
  height: 52px;
  object-fit: contain;
  background: #fff;
  border-radius: 50%;
  padding: 3px;
}

.hero-school {
  font-size: 16px;
  letter-spacing: 2px;
}

.hero-school-en {
  margin-top: 2px;
  font-size: 10px;
  letter-spacing: 0.6px;
  opacity: 0.75;
}

.hero-title {
  font-size: 38px;
  line-height: 1.25;
  letter-spacing: 3px;
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.25);
}

.hero-subtitle {
  margin-top: 12px;
  font-size: 17px;
  letter-spacing: 3px;
  opacity: 0.92;
}

.hero-en {
  margin-top: 6px;
  font-size: 11px;
  letter-spacing: 1.4px;
  opacity: 0.62;
}

.hero-btn {
  width: 150px;
  margin-top: 22px;
  letter-spacing: 2px;
}

.hero-arrow {
  margin-left: 6px;
}

.hero-indicators {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 16px;
  display: flex;
  justify-content: center;
  gap: 8px;
  z-index: 3;
}

.hero-dot {
  width: 26px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.38);
  cursor: pointer;
  transition: background 0.2s ease;
}

.hero-dot.active {
  background: #fff;
}

/* 内置指示器样式：白色短条，与门户效果图一致 */
.hero-carousel :deep(.el-carousel__indicators--horizontal) {
  bottom: 16px;
}

.hero-carousel :deep(.el-carousel__indicator .el-carousel__button) {
  width: 26px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.38);
  opacity: 1;
}

.hero-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  background: #fff;
}

.hero-manage {
  position: absolute;
  top: 18px;
  right: 24px;
  z-index: 4;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.14);
}

/* ---------- 四步流程 ---------- */
.home-steps {
  width: 1200px;
  max-width: calc(100% - 32px);
  margin: -30px auto 0;
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  background: #fff;
  box-shadow: var(--shadow);
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  border-right: 1px solid var(--border);
}

.step-item:last-child {
  border-right: none;
}

.step-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: var(--brand);
  background: var(--brand-light);
  flex: none;
}

.step-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.step-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.6;
}

/* ---------- 内容区 ---------- */
.home-content {
  width: 1200px;
  max-width: calc(100% - 32px);
  margin: 20px auto 0;
}

.content-row + .content-row {
  margin-top: 16px;
}

.content-row.split {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: 16px;
}

.content-row.split :deep(.card + .card) {
  margin-top: 0;
}

.about-body {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
}

.about-image {
  width: 100%;
  height: 200px;
  border-radius: 4px;
}

.about-text p {
  font-size: 14px;
  line-height: 1.9;
  color: var(--text-regular);
}

.about-text p + p {
  margin-top: 10px;
}

.about-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 2;
}

.campus-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.campus-image {
  width: 100%;
  height: 130px;
  border-radius: 4px;
}

.campus-caption {
  margin-top: 10px;
  font-size: 12px;
}

.notice-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-row {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 13px;
}

.notice-row:last-child {
  border-bottom: none;
}

.notice-date {
  flex: none;
  color: var(--brand);
  font-family: Consolas, monospace;
}

.notice-text {
  color: var(--text-regular);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-foot {
  margin-top: 10px;
  font-size: 12px;
}

.more-link {
  font-size: 12px;
  color: var(--text-sub);
}

.more-link:hover {
  color: var(--brand);
}

/* ---------- 响应式 ---------- */
@media (max-width: 1080px) {
  .home-hero {
    height: 320px;
  }

  .hero-title {
    font-size: 30px;
  }

  .home-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-top: 16px;
  }

  .step-item:nth-child(2) {
    border-right: none;
  }

  .step-item:nth-child(-n + 2) {
    border-bottom: 1px solid var(--border);
  }

  .content-row.split {
    grid-template-columns: 1fr;
  }

  .about-body {
    grid-template-columns: 240px minmax(0, 1fr);
  }
}

@media (max-width: 720px) {
  .home-hero {
    height: 220px;
  }

  .hero-inner {
    max-width: calc(100% - 32px);
  }

  .hero-brand {
    margin-bottom: 10px;
  }

  .hero-logo {
    width: 38px;
    height: 38px;
  }

  .hero-school-en {
    display: none;
  }

  .hero-title {
    font-size: 21px;
    letter-spacing: 1px;
  }

  .hero-subtitle {
    margin-top: 6px;
    font-size: 12px;
    letter-spacing: 1px;
  }

  .hero-en {
    display: none;
  }

  .hero-btn {
    width: auto;
    margin-top: 12px;
    padding: 8px 14px;
    font-size: 12px;
  }

  .home-steps {
    grid-template-columns: 1fr;
  }

  .step-item {
    border-right: none;
    border-bottom: 1px solid var(--border);
    padding: 14px 16px;
  }

  .step-item:nth-child(2) {
    border-right: none;
  }

  .about-body {
    grid-template-columns: 1fr;
  }

  .about-image {
    height: 160px;
  }

  .campus-grid {
    grid-template-columns: 1fr;
  }

  .campus-image {
    height: 160px;
  }
}
</style>
