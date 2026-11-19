<template>
  <div class="empty-wrap">
    <el-empty :description="description" :image-size="90">
      <slot></slot>
    </el-empty>
  </div>
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' }
})
</script>

<style scoped>
.empty-wrap {
  padding: 24px 0;
}
</style>
