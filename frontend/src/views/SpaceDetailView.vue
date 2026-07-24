<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, type FormInstance } from 'element-plus';
import { Plus, Setting } from '@element-plus/icons-vue';
import ApiKeyManager from '@/components/ApiKeyManager.vue';
import FileList from '@/components/FileList.vue';
import FileUploader from '@/components/FileUploader.vue';
import AccessLogList from '@/components/AccessLogList.vue';
import { getSpace, updateSpace } from '@/services/spaceService';
import { useSpacesStore } from '@/stores/spaces';
import type { SpaceResponse, StorageType, FileVisibility } from '@/types';

const route = useRoute();
const spacesStore = useSpacesStore();
const space = ref<SpaceResponse>();
const loading = ref(false);
const spaceId = computed(() => Number(route.params.id));
const activeTab = ref('files');

// 上传对话框
const uploadDialogVisible = ref(false);

// 空间设置对话框
const settingsDialogVisible = ref(false);
const settingsLoading = ref(false);
const settingsForm = ref({
  name: '',
  description: '',
  storageType: 'LOCAL' as StorageType,
  defaultVisibility: 'PRIVATE' as FileVisibility,
});

async function loadSpace(): Promise<void> {
  if (!Number.isInteger(spaceId.value) || spaceId.value <= 0) {
    ElMessage.error('无效的空间 ID');
    return;
  }
  loading.value = true;
  try {
    space.value = await getSpace(spaceId.value);
    spacesStore.setCurrentSpace(spaceId.value);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '空间加载失败');
  } finally {
    loading.value = false;
  }
}

function openSettings(): void {
  if (space.value) {
    settingsForm.value = {
      name: space.value.name,
      description: space.value.description || '',
      storageType: space.value.storageType,
      defaultVisibility: space.value.defaultVisibility,
    };
  }
  settingsDialogVisible.value = true;
}

async function handleUpdateSpace(): Promise<void> {
  settingsLoading.value = true;
  try {
    space.value = await updateSpace(spaceId.value, {
      name: settingsForm.value.name,
      description: settingsForm.value.description,
      storageType: settingsForm.value.storageType,
      defaultVisibility: settingsForm.value.defaultVisibility,
    });
    settingsDialogVisible.value = false;
    ElMessage.success('空间设置已更新');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败');
  } finally {
    settingsLoading.value = false;
  }
}

function handleUploadSuccess(): void {
  uploadDialogVisible.value = false;
}

watch(spaceId, () => void loadSpace());
onMounted(() => void loadSpace());
</script>

<template>
  <section v-loading="loading" class="space-detail-view">
    <header class="space-detail-view__header">
      <div>
        <h1>{{ space?.name || '空间' }}</h1>
        <p>{{ space?.description || '管理空间内的文件与 API Key。' }}</p>
      </div>
      <div class="space-detail-view__header-actions">
        <el-tag v-if="space" size="large">空间 #{{ space.id }}</el-tag>
        <el-button :icon="Setting" @click="openSettings">设置</el-button>
        <el-button type="primary" :icon="Plus" @click="uploadDialogVisible = true">上传文件</el-button>
      </div>
    </header>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="文件列表" name="files">
        <FileList :space-id="spaceId" />
      </el-tab-pane>
      <el-tab-pane label="访问日志" name="logs">
        <AccessLogList :space-id="spaceId" />
      </el-tab-pane>
      <el-tab-pane label="API Key" name="api-keys">
        <ApiKeyManager :space-id="spaceId" />
      </el-tab-pane>
    </el-tabs>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="600px" :close-on-click-modal="false">
      <FileUploader :space-id="spaceId" :default-visibility="space?.defaultVisibility" @upload-success="handleUploadSuccess" />
    </el-dialog>

    <!-- 空间设置对话框 -->
    <el-dialog v-model="settingsDialogVisible" title="空间设置" width="480px" :close-on-click-modal="false">
      <el-form :model="settingsForm" label-width="100px">
        <el-form-item label="空间名称">
          <el-input v-model="settingsForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="settingsForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="存储方案">
          <el-select v-model="settingsForm.storageType">
            <el-option label="本地存储 (LOCAL)" value="LOCAL" />
            <el-option label="腾讯云 COS" value="COS" disabled />
          </el-select>
        </el-form-item>
        <el-form-item label="默认可见性">
          <el-radio-group v-model="settingsForm.defaultVisibility">
            <el-radio-button value="PRIVATE">PRIVATE</el-radio-button>
            <el-radio-button value="PUBLIC">PUBLIC</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingsLoading" @click="handleUpdateSpace">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.space-detail-view {
  display: grid;
  gap: var(--ac-space-6);
  min-height: 300px;
}

.space-detail-view__header {
  display: flex;
  gap: var(--ac-space-5);
  align-items: flex-start;
  justify-content: space-between;
}

.space-detail-view__header h1 {
  margin: 0 0 8px;
  font-size: var(--ac-text-2xl);
  font-weight: 600;
  letter-spacing: -0.02em;
}

.space-detail-view__header p {
  margin: 0;
  color: var(--ac-text-secondary);
  font-size: var(--ac-text-sm);
}

.space-detail-view__header-actions {
  display: flex;
  gap: var(--ac-space-3);
  align-items: center;
  flex-shrink: 0;
}
</style>
