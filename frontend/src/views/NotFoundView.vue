<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<template>
  <main class="not-found-view">
    <div class="not-found-view__inner">
      <p class="not-found-view__code">404</p>
      <h1 class="not-found-view__title">页面未找到</h1>
      <p class="not-found-view__desc">抱歉，您访问的页面不存在或已被移除。</p>
      <el-button type="primary" size="large" @click="router.push('/dashboard')">返回控制台</el-button>
    </div>
  </main>
</template>

<style scoped>
.not-found-view {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  background: var(--ac-bg);
  place-items: center;
}

.not-found-view__inner {
  display: grid;
  gap: 12px;
  justify-items: center;
  text-align: center;
  animation: ac-auth-in var(--ac-dur-slower) var(--ac-ease-spring-soft) both;
}

.not-found-view__code {
  margin: 0;
  color: var(--ac-primary);
  font-size: 96px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1;
}

.not-found-view__title {
  margin: 0;
  font-size: var(--ac-text-xl);
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--ac-text-primary);
}

.not-found-view__desc {
  margin: 0 0 12px;
  color: var(--ac-text-secondary);
  font-size: var(--ac-text-sm);
}
</style>
