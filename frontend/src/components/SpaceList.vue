<script setup lang="ts">
import { useRouter } from 'vue-router';
import { FolderOpened } from '@element-plus/icons-vue';
import type { SpaceResponse } from '@/types';

interface Props {
  spaces: SpaceResponse[];
  compact?: boolean;
}

withDefaults(defineProps<Props>(), {
  compact: false,
});

const router = useRouter();

function openSpace(id: number): void {
  void router.push(`/spaces/${id}`);
}
</script>

<template>
  <el-empty v-if="spaces.length === 0" description="暂无空间" />
  <div v-else :class="['space-list', { 'space-list--compact': compact }]">
    <el-card
      v-for="space in spaces"
      :key="space.id"
      class="space-card"
      shadow="hover"
      @click="openSpace(space.id)"
    >
      <div class="space-card__title">
        <el-icon><FolderOpened /></el-icon>
        <strong>{{ space.name }}</strong>
      </div>
      <p v-if="!compact" class="space-card__description">
        {{ space.description || '暂无描述' }}
      </p>
      <span v-else class="space-card__id">#{{ space.id }}</span>
    </el-card>
  </div>
</template>

<style scoped>
.space-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: var(--ac-space-4);
}

.space-list--compact {
  display: flex;
  flex-direction: column;
  gap: var(--ac-space-2);
}

.space-card {
  cursor: pointer;
  transition: transform var(--ac-dur) var(--ac-ease-spring-soft),
    box-shadow var(--ac-dur) var(--ac-ease-out);
}
.space-card:active {
  transform: scale(0.98);
}

.space-list--compact .space-card :deep(.el-card__body) {
  padding: 12px;
}

.space-card__title {
  display: flex;
  gap: var(--ac-space-2);
  align-items: center;
  color: var(--ac-text-primary);
  font-weight: 500;
}

.space-card__title .el-icon {
  color: var(--ac-primary);
}

.space-card__description {
  min-height: 42px;
  margin: var(--ac-space-3) 0 0;
  color: var(--ac-text-secondary);
  font-size: var(--ac-text-sm);
  line-height: 1.5;
}

.space-card__id {
  display: block;
  margin-top: var(--ac-space-1);
  color: var(--ac-text-tertiary);
  font-size: var(--ac-text-xs);
}
</style>
