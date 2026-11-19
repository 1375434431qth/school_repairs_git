<template>
  <div class="image-grid">
    <div v-for="(item, index) in modelValue" :key="item.fileId || item.uid || index" class="upload-item">
      <el-image
        v-if="item.url"
        :src="item.url"
        :preview-src-list="previewList"
        :initial-index="index"
        fit="cover"
      />
      <div v-if="item.status === 'uploading'" class="upload-mask">上传中…</div>
      <div v-else-if="item.status === 'failed'" class="upload-mask failed" @click="retry(item)">
        失败，点击重试
      </div>
      <span class="upload-remove" title="删除" @click="remove(index)">×</span>
    </div>
    <el-upload
      v-if="modelValue.length < max"
      class="uploader"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="onRequest"
      accept="image/jpeg,image/png,image/webp"
      multiple
    >
      <div class="upload-btn">
        <span class="plus">+</span>
        <span class="tip">{{ modelValue.length }}/{{ max }}</span>
      </div>
    </el-upload>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUpload } from '@/composables/useUpload'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  bizType: { type: String, default: 'BEFORE' },
  max: { type: Number, default: 6 }
})

const emit = defineEmits(['update:modelValue'])

const model = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const previewList = computed(() => model.value.filter((item) => item.url).map((item) => item.url))

const { beforeUpload, doUpload, retry, remove } = useUpload(props.bizType, model)

const onRequest = (options) => {
  doUpload(options.file)
}
</script>

<style scoped>
.upload-item {
  position: relative;
  width: 96px;
  height: 96px;
  border: 1px solid var(--border);
  border-radius: 4px;
  overflow: hidden;
  background: #fafcfe;
}

.upload-item .el-image {
  width: 100%;
  height: 100%;
}

.upload-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
}

.upload-mask.failed {
  background: rgba(245, 34, 45, 0.75);
  cursor: pointer;
}

.upload-remove {
  position: absolute;
  top: 2px;
  right: 4px;
  width: 18px;
  height: 18px;
  line-height: 16px;
  text-align: center;
  font-size: 14px;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  cursor: pointer;
}

.uploader :deep(.el-upload) {
  width: 96px;
  height: 96px;
}

.upload-btn {
  width: 96px;
  height: 96px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px dashed var(--brand-border);
  border-radius: 4px;
  color: var(--text-sub);
  background: #fbfdff;
  transition: border-color 0.2s;
}

.upload-btn:hover {
  border-color: var(--brand);
  color: var(--brand);
}

.upload-btn .plus {
  font-size: 24px;
  line-height: 1;
}

.upload-btn .tip {
  font-size: 12px;
}
</style>
