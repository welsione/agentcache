<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { useFilesStore } from '@/stores/files';
import type { FileResponse } from '@/types';

interface Props {
  spaceId: number;
}

const props = defineProps<Props>();
const filesStore = useFilesStore();
const router = useRouter();
const query = ref('');
const currentPage = ref(1);
const pageSize = 10;

const files = computed(() => filesStore.files);
const total = computed(() => filesStore.total);
const loading = computed(() => filesStore.loading);

function formatSize(size: number): string {
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Asia/Shanghai',
  }).format(new Date(value));
}

function formatExpiry(expiresAt: string | null): string {
  if (!expiresAt) return '永久';
  return formatDate(expiresAt);
}

async function fetchFiles(page = currentPage.value - 1): Promise<void> {
  try {
    await filesStore.fetchFiles(props.spaceId, query.value.trim(), page);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '文件列表加载失败');
  }
}

async function search(): Promise<void> {
  currentPage.value = 1;
  await fetchFiles(0);
}

function openFile(file: FileResponse): void {
  void router.push(`/files/${props.spaceId}/${file.id}`);
}

async function removeFile(file: FileResponse): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除"${file.originalName}"吗？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await filesStore.remove(props.spaceId, file.id);
    ElMessage.success('文件已删除');
  } catch (error: unknown) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败');
    }
  }
}

watch(
  () => props.spaceId,
  () => {
    query.value = '';
    currentPage.value = 1;
    void fetchFiles(0);
  },
);

onMounted(() => {
  void fetchFiles(0);
});
</script>

<template>
  <section class="file-list">
    <div class="file-list__toolbar">
      <el-input
        v-model="query"
        clearable
        placeholder="按文件名搜索"
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button :icon="Search" aria-label="搜索" @click="search" />
        </template>
      </el-input>
    </div>

    <el-table v-loading="loading" :data="files" empty-text="暂无文件">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="originalName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column label="说明" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="大小" width="100">
        <template #default="scope">
          {{ formatSize(scope.row.size) }}
        </template>
      </el-table-column>
      <el-table-column label="可见性" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.visibility === 'PUBLIC' ? 'success' : 'info'" size="small">
            {{ scope.row.visibility }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="120">
        <template #default="{ row }">
          <span :class="{ 'file-list__expired': row.expiresAt && new Date(row.expiresAt) < new Date() }">
            {{ formatExpiry(row.expiresAt) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="160">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openFile(scope.row)">查看</el-button>
          <el-button link type="danger" @click="removeFile(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      class="file-list__pagination"
      background
      layout="prev, pager, next, total"
      :page-size="pageSize"
      :total="total"
      @current-change="(page: number) => fetchFiles(page - 1)"
    />
  </section>
</template>

<style scoped>
.file-list {
  display: grid;
  gap: var(--ac-space-4);
}

.file-list__toolbar {
  max-width: 420px;
}

.file-list__pagination {
  justify-self: end;
}

.file-list__expired {
  color: var(--el-color-danger);
  font-size: var(--ac-text-xs);
}
</style>
