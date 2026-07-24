<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import SpaceList from '@/components/SpaceList.vue';
import { useSpacesStore } from '@/stores/spaces';
import type { CreateSpaceRequest } from '@/types';

const spacesStore = useSpacesStore();
const dialogVisible = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<CreateSpaceRequest>({
  name: '',
  description: '',
});
const rules: FormRules<CreateSpaceRequest> = {
  name: [{
    validator: (_rule, value, callback) => {
      if (!value || !value.trim()) {
        callback(new Error('请输入空间名称'));
      } else {
        callback();
      }
    },
    trigger: 'blur',
  }],
};

function openDialog(): void {
  form.name = '';
  form.description = '';
  dialogVisible.value = true;
}

async function createSpace(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    await spacesStore.createSpace(form.name.trim(), form.description.trim());
    dialogVisible.value = false;
    ElMessage.success('空间创建成功');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '空间创建失败');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section class="dashboard-view">
    <header class="dashboard-view__hero">
      <div>
        <p class="dashboard-view__eyebrow">文件交接中心</p>
        <h1>欢迎使用 AgentCache</h1>
        <p>创建空间，与团队成员和 Agent 安全地中转文件。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog">创建空间</el-button>
    </header>

    <el-card shadow="never">
      <template #header>
        <strong>我的空间</strong>
      </template>
      <SpaceList :spaces="spacesStore.spaces" />
    </el-card>

    <el-dialog v-model="dialogVisible" title="创建空间" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="输入空间名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="createSpace">创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.dashboard-view {
  display: grid;
  gap: var(--ac-space-6);
}

.dashboard-view__hero {
  display: flex;
  gap: var(--ac-space-6);
  align-items: center;
  justify-content: space-between;
  padding: var(--ac-space-7);
  background: var(--ac-bg-elevated);
  border: 1px solid var(--ac-divider);
  border-radius: var(--ac-radius-lg);
  box-shadow: var(--ac-shadow-sm);
}

.dashboard-view__eyebrow {
  margin: 0 0 6px;
  color: var(--ac-primary);
  font-size: var(--ac-text-xs);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.dashboard-view__hero h1 {
  margin: 0 0 8px;
  color: var(--ac-text-primary);
  font-size: var(--ac-text-3xl);
  font-weight: 600;
  letter-spacing: -0.02em;
}

.dashboard-view__hero p {
  margin: 0;
  color: var(--ac-text-secondary);
  font-size: var(--ac-text-md);
}

.dashboard-view__hero .el-button {
  flex: none;
}

@media (max-width: 640px) {
  .dashboard-view__hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
