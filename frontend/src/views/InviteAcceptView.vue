<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Lock, Message, User } from '@element-plus/icons-vue';
import { acceptInvitation } from '@/services/authService';

const route = useRoute();
const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const tokenValid = ref(true);
const form = reactive({
  token: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

onMounted(() => {
  const token = route.params.token;
  if (typeof token !== 'string' || !token) {
    tokenValid.value = false;
    return;
  }
  form.token = token;
});

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  loading.value = true;
  try {
    await acceptInvitation({
      token: form.token,
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
    });
    ElMessage.success('注册成功，请登录');
    await router.replace('/login');
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败');
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
        <p class="ac-auth__subtitle">接受邀请，完成注册</p>
      </div>
      <el-alert
        v-if="!tokenValid"
        class="ac-auth__hint"
        title="邀请链接无效"
        type="error"
        :closable="false"
        show-icon
      />
      <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" :prefix-icon="Message" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
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
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <el-button class="ac-auth__button" type="primary" size="large" :loading="loading" @click="submit">
          完成注册
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>
