<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { createApiKey, listApiKeys } from '@/services/spaceService';
import type {
  ApiKeyInfo,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  SpaceMemberRole,
} from '@/types';

interface Props {
  spaceId: number;
}

const props = defineProps<Props>();
const apiKeys = ref<ApiKeyInfo[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const createdKey = ref<CreateApiKeyResponse>();
const form = reactive<CreateApiKeyRequest>({
  name: '',
  role: 'MEMBER',
});
const rules: FormRules<CreateApiKeyRequest> = {
  name: [{
    validator: (_rule, value, callback) => {
      if (!value || !value.trim()) {
        callback(new Error('请输入名称'));
      } else {
        callback();
      }
    },
    trigger: 'blur',
  }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
};
const roles: SpaceMemberRole[] = ['MANAGER', 'MEMBER', 'READER'];

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

async function fetchApiKeys(): Promise<void> {
  loading.value = true;
  try {
    apiKeys.value = await listApiKeys(props.spaceId);
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : 'API Key 列表加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog(): void {
  form.name = '';
  form.role = 'MEMBER';
  dialogVisible.value = true;
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  try {
    createdKey.value = await createApiKey(props.spaceId, {
      name: form.name.trim(),
      role: form.role,
    });
    dialogVisible.value = false;
    await fetchApiKeys();
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : 'API Key 创建失败');
  }
}

async function copyKey(): Promise<void> {
  if (!createdKey.value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(createdKey.value.apiKey);
    ElMessage.success('API Key 已复制');
  } catch {
    ElMessage.error('复制失败，请手动复制');
  }
}

watch(
  () => props.spaceId,
  () => void fetchApiKeys(),
);

onMounted(() => void fetchApiKeys());
</script>

<template>
  <section class="api-key-manager">
    <div class="api-key-manager__header">
      <h3>API Key</h3>
      <el-button type="primary" @click="openCreateDialog">创建 API Key</el-button>
    </div>

    <el-table v-loading="loading" :data="apiKeys" empty-text="暂无 API Key">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column label="创建时间" min-width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="创建 API Key" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="例如：build-agent" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" class="api-key-manager__select">
            <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog
      :model-value="Boolean(createdKey)"
      title="API Key 创建成功"
      width="560px"
      @close="createdKey = undefined"
    >
      <el-alert
        title="请立即保存，关闭后无法再次查看明文 API Key。"
        type="warning"
        show-icon
        :closable="false"
      />
      <div v-if="createdKey" class="api-key-manager__secret">
        <code>{{ createdKey.apiKey }}</code>
        <el-button type="primary" plain @click="copyKey">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="createdKey = undefined">我已保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.api-key-manager {
  display: grid;
  gap: var(--ac-space-4);
}

.api-key-manager__header {
  display: flex;
  gap: var(--ac-space-4);
  align-items: center;
  justify-content: space-between;
}

.api-key-manager__header h3 {
  margin: 0;
  font-size: var(--ac-text-lg);
  font-weight: 600;
  letter-spacing: -0.01em;
}

.api-key-manager__select {
  width: 100%;
}

.api-key-manager__secret {
  display: flex;
  gap: var(--ac-space-3);
  align-items: center;
  margin-top: var(--ac-space-5);
}

.api-key-manager__secret code {
  overflow-x: auto;
  padding: 10px 14px;
  flex: 1;
  color: var(--ac-text-primary);
  background: var(--ac-bg);
  border: 1px solid var(--ac-border);
  border-radius: var(--ac-radius-md);
  font-family: var(--ac-font-mono);
  font-size: var(--ac-text-sm);
  white-space: nowrap;
}
</style>
