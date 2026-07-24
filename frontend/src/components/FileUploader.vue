<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage, type UploadFile, type UploadFiles, type UploadInstance } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';
import { useFilesStore } from '@/stores/files';
import type { FileVisibility } from '@/types';

interface Props {
  spaceId: number;
  defaultVisibility?: FileVisibility;
}

const props = defineProps<Props>();

const filesStore = useFilesStore();
const uploadRef = ref<UploadInstance>();
const uploading = ref(false);
const selectedFile = ref<File>();
const description = ref('');
const visibility = ref<FileVisibility>(props.defaultVisibility || 'PRIVATE');
const expiresInHours = ref<number | undefined>(undefined);

const expiryOptions = [
  { label: '永久', value: undefined as number | undefined },
  { label: '1 小时', value: 1 },
  { label: '24 小时', value: 24 },
  { label: '7 天', value: 168 },
  { label: '30 天', value: 720 },
];

function handleChange(uploadFile: UploadFile, uploadFiles: UploadFiles): void {
  selectedFile.value = uploadFile.raw;
  if (uploadFiles.length > 1) {
    uploadFiles.splice(0, uploadFiles.length - 1);
  }
}

function handleRemove(): void {
  selectedFile.value = undefined;
}

async function submitUpload(): Promise<void> {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件');
    return;
  }
  uploading.value = true;
  try {
    await filesStore.upload(props.spaceId, selectedFile.value, {
      description: description.value || undefined,
      visibility: visibility.value,
      expiresInHours: expiresInHours.value,
    });
    uploadRef.value?.clearFiles();
    selectedFile.value = undefined;
    description.value = '';
    visibility.value = props.defaultVisibility || 'PRIVATE';
    expiresInHours.value = undefined;
    ElMessage.success('上传成功');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败');
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <div class="uploader">
    <el-upload
      ref="uploadRef"
      drag
      action="#"
      :auto-upload="false"
      :limit="1"
      :disabled="uploading"
      :on-change="handleChange"
      :on-remove="handleRemove"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或<em>点击选择</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">每次上传一个文件</div>
      </template>
    </el-upload>
    <el-form label-width="80px" class="uploader__form">
      <el-form-item label="文件说明">
        <el-input
          v-model="description"
          type="textarea"
          :rows="2"
          placeholder="描述文件用途、内容大纲等（可选）"
        />
      </el-form-item>
      <el-form-item label="可见性">
        <el-radio-group v-model="visibility">
          <el-radio-button value="PRIVATE">PRIVATE</el-radio-button>
          <el-radio-button value="PUBLIC">PUBLIC</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="有效期">
        <el-select v-model="expiresInHours" placeholder="永久" clearable>
          <el-option
            v-for="opt in expiryOptions"
            :key="String(opt.value)"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <el-button
      type="primary"
      :loading="uploading"
      :disabled="!selectedFile"
      @click="submitUpload"
    >
      上传文件
    </el-button>
  </div>
</template>

<style scoped>
.uploader {
  display: grid;
  gap: var(--ac-space-3);
}

.uploader .el-button {
  justify-self: start;
}

.uploader__form {
  margin-top: var(--ac-space-2);
}

.uploader :deep(.el-upload) {
  width: 100%;
}

.uploader :deep(.el-upload-dragger) {
  width: 100%;
  border-radius: var(--ac-radius-lg);
  border: 2px dashed var(--ac-border-strong);
  background: var(--ac-bg-subtle);
  transition: border-color var(--ac-dur-fast) var(--ac-ease-out),
    background-color var(--ac-dur-fast) var(--ac-ease-out);
}
.uploader :deep(.el-upload-dragger:hover) {
  border-color: var(--ac-primary);
  background: var(--ac-primary-softer);
}
.uploader :deep(.el-upload-dragger.is-dragover) {
  border-color: var(--ac-primary);
  background: var(--ac-primary-soft);
}

.uploader :deep(.el-icon--upload) {
  color: var(--ac-text-tertiary);
  font-size: 40px;
  margin: 8px 0 4px;
}

.uploader :deep(.el-upload__text) {
  color: var(--ac-text-secondary);
  font-size: var(--ac-text-sm);
}
.uploader :deep(.el-upload__text em) {
  color: var(--ac-primary);
  font-style: normal;
  font-weight: 500;
}

.uploader :deep(.el-upload__tip) {
  color: var(--ac-text-tertiary);
  font-size: var(--ac-text-xs);
}
</style>
