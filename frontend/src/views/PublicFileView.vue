<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Download } from '@element-plus/icons-vue';
import { getPublicFile, publicDownloadUrl } from '@/services/fileService';
import type { FileResponse } from '@/types';

const route = useRoute();
const file = ref<FileResponse>();
const loading = ref(false);
const fileId = computed(() => Number(route.params.fileId));

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(new Date(value));
}

async function loadFile(): Promise<void> {
  loading.value = true;
  try {
    file.value = await getPublicFile(fileId.value);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '公开文件加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => void loadFile());
</script>

<template>
  <main class="public-file-view">
    <el-card v-loading="loading" class="public-file-card" shadow="never">
      <template #header>
        <div class="public-file-card__header">
          <strong>AgentCache 公开文件</strong>
          <el-tag type="success">PUBLIC</el-tag>
        </div>
      </template>

      <template v-if="file">
        <h1 class="public-file-card__name">{{ file.originalName }}</h1>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件 ID">{{ file.id }}</el-descriptions-item>
          <el-descriptions-item label="内容类型">{{ file.contentType || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ file.size.toLocaleString('zh-CN') }} B</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(file.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <el-button
          class="public-file-card__download"
          type="primary"
          size="large"
          :icon="Download"
          tag="a"
          :href="`/public/files/${file.id}/content`"
        >
          下载文件
        </el-button>
      </template>
      <el-empty v-else-if="!loading" description="文件不存在或不是公开文件" />
    </el-card>
  </main>
</template>

<style scoped>
.public-file-view {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(1000px 500px at 50% -10%, rgba(0, 113, 227, 0.06), transparent 60%),
    var(--ac-bg);
  place-items: center;
}

.public-file-card {
  width: min(680px, 100%);
}

.public-file-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.public-file-card__name {
  overflow-wrap: anywhere;
  margin: 4px 0 24px;
  font-size: var(--ac-text-2xl);
  font-weight: 600;
  letter-spacing: -0.02em;
}

.public-file-card__download {
  margin-top: 24px;
  width: 100%;
}

.public-file-card :deep(.el-descriptions__label) {
  color: var(--ac-text-secondary);
  font-weight: 500;
  background: var(--ac-bg-subtle);
}
</style>
