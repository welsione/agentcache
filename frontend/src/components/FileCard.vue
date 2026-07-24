<script setup lang="ts">
import { computed } from 'vue';
import { Document } from '@element-plus/icons-vue';
import type { FileResponse } from '@/types';

interface Props {
  file: FileResponse;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  open: [file: FileResponse];
}>();

const formattedSize = computed(() => {
  if (props.file.size < 1024) {
    return `${props.file.size} B`;
  }
  if (props.file.size < 1024 * 1024) {
    return `${(props.file.size / 1024).toFixed(1)} KB`;
  }
  return `${(props.file.size / (1024 * 1024)).toFixed(1)} MB`;
});
</script>

<template>
  <el-card class="file-card" shadow="hover" @click="emit('open', file)">
    <div class="file-card__icon">
      <el-icon :size="24">
        <Document />
      </el-icon>
    </div>
    <div class="file-card__content">
      <strong class="file-card__name">{{ file.originalName }}</strong>
      <span class="file-card__meta">{{ formattedSize }}</span>
      <el-tag :type="file.visibility === 'PUBLIC' ? 'success' : 'info'" size="small">
        {{ file.visibility }}
      </el-tag>
    </div>
  </el-card>
</template>

<style scoped>
.file-card {
  cursor: pointer;
  transition: transform var(--ac-dur) var(--ac-ease-spring-soft),
    box-shadow var(--ac-dur) var(--ac-ease-out);
}
.file-card:active {
  transform: scale(0.98);
}

.file-card :deep(.el-card__body) {
  display: flex;
  gap: 14px;
  align-items: center;
}

.file-card__icon {
  display: grid;
  width: 44px;
  height: 44px;
  color: var(--ac-primary);
  background: var(--ac-primary-softer);
  border-radius: var(--ac-radius-md);
  place-items: center;
  transition: background-color var(--ac-dur-fast) var(--ac-ease-out);
}
.file-card:hover .file-card__icon {
  background: var(--ac-primary-soft);
}

.file-card__content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.file-card__name {
  overflow: hidden;
  max-width: 100%;
  color: var(--ac-text-primary);
  font-size: var(--ac-text-base);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-card__meta {
  color: var(--ac-text-tertiary);
  font-size: var(--ac-text-xs);
}
</style>
