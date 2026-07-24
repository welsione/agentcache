<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Lock } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import { changePassword } from '@/services/authService';

const authStore = useAuthStore();
const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  loading.value = true;
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword });
    authStore.setMustChangePassword(false);
    ElMessage.success('密码修改成功');
    await router.replace('/dashboard');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '密码修改失败');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="ac-auth">
    <el-card class="ac-auth__card" shadow="never">
      <div class="ac-auth__heading">
        <h1 class="ac-auth__title">修改密码</h1>
        <p v-if="authStore.mustChangePassword" class="ac-auth__subtitle">
          首次登录请先修改默认密码
        </p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="至少 6 位"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <el-button
          class="ac-auth__button"
          type="primary"
          size="large"
          :loading="loading"
          @click="submit"
        >
          确认修改
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>
