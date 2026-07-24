<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { listAccessLogs } from '@/services/fileService';
import type { FileAccessLogResponse } from '@/types';

interface Props {
  spaceId: number;
}

const props = defineProps<Props>();
const logs = ref<FileAccessLogResponse[]>([]);
const total = ref(0);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = 10;

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'medium',
    timeZone: 'Asia/Shanghai',
  }).format(new Date(value));
}

function actionLabel(action: string): string {
  const map: Record<string, string> = {
    VIEW: '查看',
    DOWNLOAD: '下载',
    UPLOAD: '上传',
    DELETE: '删除',
    VISIBILITY_CHANGE: '修改可见性',
  };
  return map[action] || action;
}

function actorLabel(row: FileAccessLogResponse): string {
  if (row.actorName) return row.actorName;
  if (row.actorId) return String(row.actorId);
  if (row.actorType === 'ANONYMOUS') return '匿名';
  return row.actorType;
}

async function fetchLogs(page = currentPage.value - 1): Promise<void> {
  loading.value = true;
  try {
    const result = await listAccessLogs(props.spaceId, page, pageSize);
    logs.value = result.content;
    total.value = result.total;
  } catch {
    // 忽略加载错误
  } finally {
    loading.value = false;
  }
}

watch(() => props.spaceId, () => { currentPage.value = 1; void fetchLogs(0); });
onMounted(() => void fetchLogs(0));
</script>

<template>
  <section class="access-log-list">
    <el-table v-loading="loading" :data="logs" empty-text="暂无访问记录">
      <el-table-column label="时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="row.action === 'DELETE' ? 'danger' : row.action === 'UPLOAD' ? 'success' : 'info'">
            {{ actionLabel(row.action) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作者" min-width="120">
        <template #default="{ row }">{{ actorLabel(row) }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="details" label="详情" min-width="200" show-overflow-tooltip />
    </el-table>
    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      class="access-log-list__pagination"
      background
      layout="prev, pager, next, total"
      :page-size="pageSize"
      :total="total"
      @current-change="(page: number) => fetchLogs(page - 1)"
    />
  </section>
</template>

<style scoped>
.access-log-list {
  display: grid;
  gap: var(--ac-space-4);
}

.access-log-list__pagination {
  justify-self: end;
}
</style>
