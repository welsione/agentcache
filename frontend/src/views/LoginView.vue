<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import type { LoginRequest } from '@/types';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive<LoginRequest>({
  username: '',
  password: '',
});
const rules: FormRules<LoginRequest> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  loading.value = true;
  try {
    await authStore.login(form.username.trim(), form.password);
    ElMessage.success('登录成功');
    if (authStore.mustChangePassword) {
      await router.replace('/change-password');
    } else {
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard';
      await router.replace(redirect);
    }
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请检查用户名和密码');
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="ac-auth">
    <el-card class="ac-auth__card" shadow="never">
      <div class="ac-auth__heading">
        <h1 class="ac-auth__title">AgentCache</h1>
        <p class="ac-auth__subtitle">登录文件中转与交接平台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" :prefix-icon="User" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            type="password"
            show-password
            placeholder="密码"
            size="large"
          />
        </el-form-item>
        <el-button class="ac-auth__button" type="primary" size="large" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <el-alert
        class="ac-auth__hint"
        title="默认账号：admin / admin@123"
        type="info"
        :closable="false"
        show-icon
      />
    </el-card>
  </main>
</template>
