<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, CopyDocument, Delete, Download, Edit } from '@element-plus/icons-vue';
import { downloadFile, getFile, updateFileDescription, updateFileExpiry } from '@/services/fileService';
import { useFilesStore } from '@/stores/files';
import type { FileResponse, FileVisibility } from '@/types';

const route = useRoute();
const router = useRouter();
const filesStore = useFilesStore();
const file = ref<FileResponse>();
const loading = ref(false);
const downloading = ref(false);
const spaceId = computed(() => Number(route.params.spaceId));
const fileId = computed(() => Number(route.params.fileId));

const expiryOptions = [
  { label: '永久', value: null as number | null },
  { label: '1 小时', value: 1 },
  { label: '24 小时', value: 24 },
  { label: '7 天', value: 168 },
  { label: '30 天', value: 720 },
];

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'long',
    timeStyle: 'medium',
    timeZone: 'Asia/Shanghai',
  }).format(new Date(value));
}

function formatSize(size: number): string {
  return new Intl.NumberFormat('zh-CN').format(size) + ' B';
}

async function loadFile(): Promise<void> {
  loading.value = true;
  try {
    file.value = await getFile(fileId.value, spaceId.value);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '文件加载失败');
  } finally {
    loading.value = false;
  }
}

async function copyAccessUrl(): Promise<void> {
  if (!file.value) {
    return;
  }
  const path = file.value.visibility === 'PUBLIC'
    ? `/public/files/${file.value.id}/content`
    : `/files/${spaceId.value}/${file.value.id}`;
  try {
    await navigator.clipboard.writeText(new URL(path, window.location.origin).toString());
    if (file.value.visibility === 'PRIVATE') {
      ElMessage.success('访问地址已复制（需登录后查看）');
    } else {
      ElMessage.success('访问地址已复制');
    }
  } catch {
    ElMessage.error('复制失败，请手动复制');
  }
}

async function download(): Promise<void> {
  if (!file.value) {
    return;
  }
  downloading.value = true;
  try {
    const blob = await downloadFile(file.value.id, spaceId.value);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.value.originalName;
    document.body.appendChild(link);
    setTimeout(() => link.click(), 0);
    setTimeout(() => {
      URL.revokeObjectURL(url);
      document.body.removeChild(link);
    }, 1000);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败');
  } finally {
    downloading.value = false;
  }
}

async function changeVisibility(visibility: FileVisibility): Promise<void> {
  if (!file.value || file.value.visibility === visibility) {
    return;
  }
  try {
    file.value = await filesStore.setVisibility(spaceId.value, file.value.id, visibility);
    ElMessage.success('可见性已更新');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '可见性更新失败');
  }
}

async function editDescription(): Promise<void> {
  if (!file.value) return;
  try {
    const { value } = await ElMessageBox.prompt('请输入文件说明', '修改文件说明', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: file.value.description || '',
    });
    const updated = await updateFileDescription(file.value.id, spaceId.value, value);
    file.value = updated;
    ElMessage.success('说明已更新');
  } catch {
    // 取消
  }
}

async function changeExpiry(hours: number | null): Promise<void> {
  if (!file.value) return;
  try {
    const updated = await updateFileExpiry(file.value.id, spaceId.value, hours);
    file.value = updated;
    ElMessage.success('有效期已更新');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '有效期更新失败');
  }
}

async function remove(): Promise<void> {
  if (!file.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认删除"${file.value.originalName}"吗？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
    await filesStore.remove(spaceId.value, file.value.id);
    ElMessage.success('文件已删除');
    await router.replace(`/spaces/${spaceId.value}`);
  } catch (error: unknown) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败');
    }
  }
}

onMounted(() => void loadFile());
</script>

<template>
  <section v-loading="loading" class="file-detail-view">
    <button class="file-detail-view__back" type="button" @click="router.push(`/spaces/${spaceId}`)">
      <el-icon><ArrowLeft /></el-icon>
      返回空间
    </button>
    <h1 class="file-detail-view__title">{{ file?.originalName || '文件详情' }}</h1>

    <el-card v-if="file" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件 ID">{{ file.id }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ file.originalName }}</el-descriptions-item>
        <el-descriptions-item label="内容类型">{{ file.contentType || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="大小">{{ formatSize(file.size) }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(file.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(file.updatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ file.createdBy ?? 'API Key' }}</el-descriptions-item>
        <el-descriptions-item label="可见性">
          <el-radio-group
            :model-value="file.visibility"
            @change="(value: string | number | boolean | undefined) => changeVisibility(value as FileVisibility)"
          >
            <el-radio-button value="PRIVATE">PRIVATE</el-radio-button>
            <el-radio-button value="PUBLIC">PUBLIC</el-radio-button>
          </el-radio-group>
        </el-descriptions-item>
        <el-descriptions-item label="说明">
          <div class="file-detail-view__description">
            <span>{{ file.description || '无' }}</span>
            <el-button link type="primary" :icon="Edit" @click="editDescription">编辑</el-button>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="有效期">
          <div class="file-detail-view__expiry">
            <el-select
              :model-value="file.expiresAt ? 'custom' : null"
              placeholder="永久"
              @change="(val: string | number | boolean | undefined) => changeExpiry(val as number | null)"
            >
              <el-option
                v-for="opt in expiryOptions"
                :key="String(opt.value)"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span v-if="file.expiresAt" class="file-detail-view__expiry-date">
              {{ formatDate(file.expiresAt) }}
            </span>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <div class="file-detail-view__actions">
        <el-button :icon="Download" :loading="downloading" @click="download">下载</el-button>
        <el-button :icon="CopyDocument" @click="copyAccessUrl">复制访问地址</el-button>
        <el-button type="danger" :icon="Delete" @click="remove">删除文件</el-button>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.file-detail-view {
  display: grid;
  gap: var(--ac-space-5);
  min-height: 300px;
}

.file-detail-view__back {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  width: fit-content;
  padding: 4px 0;
  background: transparent;
  border: 0;
  cursor: pointer;
  color: var(--ac-primary);
  font-size: var(--ac-text-sm);
  font-weight: 500;
  transition: opacity var(--ac-dur-fast) var(--ac-ease-out);
}
.file-detail-view__back:hover {
  opacity: 0.7;
}

.file-detail-view__title {
  margin: 0;
  font-size: var(--ac-text-2xl);
  font-weight: 600;
  letter-spacing: -0.02em;
}

.file-detail-view__description {
  display: flex;
  gap: 8px;
  align-items: center;
}

.file-detail-view__expiry {
  display: flex;
  gap: 8px;
  align-items: center;
}

.file-detail-view__expiry-date {
  color: var(--ac-text-secondary);
  font-size: var(--ac-text-sm);
}

.file-detail-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ac-space-3);
  margin-top: var(--ac-space-6);
}

.file-detail-view__actions .el-button + .el-button {
  margin-left: 0;
}

.file-detail-view :deep(.el-descriptions__label) {
  color: var(--ac-text-secondary);
  font-weight: 500;
  background: var(--ac-bg-subtle);
}

@media (max-width: 760px) {
  .file-detail-view :deep(.el-descriptions__body) {
    overflow-x: auto;
  }
}
</style>
