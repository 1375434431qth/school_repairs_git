<template>
  <div class="page-container">
    <PageHeader title="首页轮播" subtitle="上传图片、拖拽调整展示顺序、删除或停用，保存后首页立即生效">
      <template #actions>
        <el-button :loading="loading" @click="load">
          <AppIcon name="refresh" :size="14" /> 刷新
        </el-button>
        <el-button type="primary" @click="router.push('/')">查看首页</el-button>
      </template>
    </PageHeader>

    <Panel title="上传新图片">
      <template #extra>
        <span>已上传 {{ list.length }}/8</span>
      </template>
      <div class="upload-row">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          :disabled="uploading || list.length >= 8"
          accept="image/jpeg,image/png,image/webp"
        >
          <div class="upload-box" :class="{ disabled: list.length >= 8 }">
            <AppIcon name="plus" :size="22" />
            <span>{{ uploading ? '上传中…' : '上传轮播图' }}</span>
            <small>建议 16:6 横图，jpg/png/webp，≤5MB</small>
          </div>
        </el-upload>
        <div class="form-tip">
          <p>· 上传成功后会立即出现在首页轮播中，可继续拖拽排序。</p>
          <p>· 首页轮播最多 8 张，至少保留 1 张。</p>
          <p>· 删除为不可逆操作，会同时删除已上传的图片文件。</p>
        </div>
      </div>
    </Panel>

    <Panel title="当前轮播">
      <template #extra>
        <span class="muted">按住卡片拖拽可调整顺序</span>
      </template>
      <div v-loading="loading" class="carousel-grid">
        <div
          v-for="(item, index) in list"
          :key="item.id"
          class="carousel-card"
          :class="{ dragging: dragIndex === index }"
          draggable="true"
          @dragstart="onDragStart(index)"
          @dragover.prevent
          @drop="onDrop(index)"
          @dragend="dragIndex = -1"
        >
          <div class="card-index">{{ index + 1 }}</div>
          <el-image :src="item.imageUrl" fit="cover" class="card-image" :preview-src-list="previewList" :initial-index="index" />
          <div class="card-body">
            <el-input v-model="item.title" placeholder="图片标题（选填）" maxlength="60" @blur="saveTitle(item)" />
            <div class="card-meta">
              <el-switch
                :model-value="item.status === 1"
                :loading="statusLoadingId === item.id"
                @change="(value) => toggleStatus(item, value)"
              />
              <span class="muted">{{ item.status === 1 ? '首页展示中' : '已停用' }}</span>
            </div>
            <div class="card-actions">
              <el-button link :disabled="index === 0" @click="move(index, -1)">上移</el-button>
              <el-button link :disabled="index === list.length - 1" @click="move(index, 1)">下移</el-button>
              <el-button link type="danger" @click="openDelete(item)">删除</el-button>
            </div>
          </div>
        </div>
        <EmptyState v-if="!loading && !list.length" description="还没有轮播图，先上传一张吧" />
      </div>
    </Panel>

    <el-dialog v-model="deleteVisible" title="删除轮播图" width="420px">
      <div class="delete-tip">删除后该图片将从首页移除且不可恢复，确认删除？</div>
      <template #footer>
        <el-button @click="deleteVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="confirmDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/utils/feedback'
import PageHeader from '@/components/base/PageHeader.vue'
import Panel from '@/components/base/Panel.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import AppIcon from '@/components/base/AppIcon.vue'
import { uploadFile } from '@/api/file'
import { createCarousel, deleteCarousel, listManagerCarousels, sortCarousels, updateCarousel } from '@/api/carousel'
import { useCarouselStore } from '@/stores/carousel'

const router = useRouter()
const carouselStore = useCarouselStore()
const loading = ref(false)
const uploading = ref(false)
const list = ref([])
const dragIndex = ref(-1)
const statusLoadingId = ref(null)
const deleteVisible = ref(false)
const deleting = ref(false)
const pendingDelete = ref(null)

const previewList = computed(() => list.value.map((item) => item.imageUrl))

async function load() {
  loading.value = true
  try {
    list.value = (await listManagerCarousels()) || []
  } finally {
    loading.value = false
  }
}

function beforeUpload(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 jpg / png / webp 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  if (list.value.length >= 8) {
    ElMessage.warning('轮播图最多 8 张')
    return false
  }
  return true
}

async function handleUpload(options) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    formData.append('bizType', 'CAROUSEL')
    const uploaded = await uploadFile(formData)
    await createCarousel({
      imageFileId: uploaded.fileId,
      imageUrl: uploaded.url,
      title: (options.file.name || '').replace(/\.[^.]+$/, '').slice(0, 60)
    })
    ElMessage.success('轮播图已添加')
    await load()
    await carouselStore.reload()
  } finally {
    uploading.value = false
  }
}

async function saveTitle(item) {
  const title = (item.title || '').trim()
  try {
    await updateCarousel(item.id, { title })
    await carouselStore.reload()
  } catch (e) {
    load()
  }
}

async function toggleStatus(item, enabled) {
  statusLoadingId.value = item.id
  try {
    await updateCarousel(item.id, { status: enabled ? 1 : 0 })
    ElMessage.success(enabled ? '已启用' : '已停用，首页不再展示')
    await load()
    await carouselStore.reload()
  } catch (e) {
    load()
  } finally {
    statusLoadingId.value = null
  }
}

function onDragStart(index) {
  dragIndex.value = index
}

async function onDrop(targetIndex) {
  const from = dragIndex.value
  dragIndex.value = -1
  if (from < 0 || from === targetIndex) return
  const next = [...list.value]
  const [moved] = next.splice(from, 1)
  next.splice(targetIndex, 0, moved)
  list.value = next
  await persistOrder()
}

async function move(index, offset) {
  const target = index + offset
  if (target < 0 || target >= list.value.length) return
  const next = [...list.value]
  const [moved] = next.splice(index, 1)
  next.splice(target, 0, moved)
  list.value = next
  await persistOrder()
}

async function persistOrder() {
  try {
    await sortCarousels(list.value.map((item) => item.id))
    ElMessage.success('顺序已保存')
    await carouselStore.reload()
  } catch (e) {
    load()
  }
}

function openDelete(item) {
  pendingDelete.value = item
  deleteVisible.value = true
}

async function confirmDelete() {
  if (!pendingDelete.value) return
  deleting.value = true
  try {
    await deleteCarousel(pendingDelete.value.id)
    ElMessage.success('已删除')
    deleteVisible.value = false
    pendingDelete.value = null
    await load()
    await carouselStore.reload()
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.upload-row {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  align-items: center;
}

.upload-box {
  width: 220px;
  height: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed var(--brand-border);
  border-radius: 4px;
  color: var(--text-sub);
  background: #fbfdff;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.upload-box:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.upload-box.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.upload-box small {
  font-size: 11px;
}

.carousel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 100px;
}

.carousel-card {
  position: relative;
  border: 1px solid var(--border);
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  cursor: grab;
  transition: box-shadow 0.2s, border-color 0.2s, opacity 0.2s;
}

.carousel-card:hover {
  border-color: var(--brand-border);
  box-shadow: var(--shadow);
}

.carousel-card.dragging {
  opacity: 0.45;
  border-style: dashed;
}

.card-index {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(15, 111, 156, 0.86);
  color: #fff;
  font-size: 12px;
  line-height: 22px;
  text-align: center;
}

.card-image {
  width: 100%;
  height: 150px;
}

.card-body {
  padding: 10px 12px 12px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}

@media (max-width: 1080px) {
  .carousel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .upload-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .carousel-grid {
    grid-template-columns: 1fr;
  }
}

.delete-tip {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-regular);
}
</style>
