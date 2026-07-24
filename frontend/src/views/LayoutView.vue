<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { House, SwitchButton, User, Key, Setting } from '@element-plus/icons-vue';
import SpaceList from '@/components/SpaceList.vue';
import { useAuthStore } from '@/stores/auth';
import { useSpacesStore } from '@/stores/spaces';

const authStore = useAuthStore();
const spacesStore = useSpacesStore();
const route = useRoute();
const router = useRouter();
const activePath = computed(() => route.path);

function logout(): void {
  authStore.clear();
  void router.replace('/login');
}

function goToChangePassword(): void {
  void router.push('/change-password');
}

function goToUsers(): void {
  void router.push('/users');
}

onMounted(() => {
  authStore.initFromStorage();
  void spacesStore.fetchSpaces().catch((error: unknown) => {
    ElMessage.error(error instanceof Error ? error.message : '空间列表加载失败');
  });
});
</script>

<template>
  <el-container class="layout-view">
    <el-aside class="layout-view__aside" width="260px">
      <button class="layout-view__brand" type="button" @click="router.push('/dashboard')">
        AgentCache
      </button>
      <el-menu :default-active="activePath" router class="layout-view__menu">
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>控制台</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/users">
          <el-icon><Setting /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
      <div class="layout-view__spaces">
        <span class="layout-view__label">空间</span>
        <SpaceList :spaces="spacesStore.spaces" compact />
      </div>
    </el-aside>

    <el-container class="layout-view__right">
      <el-header class="layout-view__header ac-glass">
        <el-dropdown>
          <span class="layout-view__user">
            <span class="layout-view__avatar">{{ (authStore.username || '管')[0] }}</span>
            {{ authStore.username || '管理员' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="Key" @click="goToChangePassword">修改密码</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" :icon="Setting" @click="goToUsers">
                用户管理
              </el-dropdown-item>
              <el-dropdown-item :icon="SwitchButton" divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="layout-view__main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-view {
  min-height: 100vh;
}

/* ===== 侧栏:去硬边框,柔和背景层级 ===== */
.layout-view__aside {
  overflow-y: auto;
  padding: 20px 14px;
  background: var(--ac-bg-elevated);
  border-right: 1px solid var(--ac-divider);
}

.layout-view__brand {
  padding: 4px 8px 22px;
  color: var(--ac-primary);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.03em;
  transition: opacity var(--ac-dur-fast) var(--ac-ease-out);
}
.layout-view__brand:hover {
  opacity: 0.8;
}
.layout-view__brand:active {
  opacity: 0.6;
}

.layout-view__menu {
  border-right: 0;
}

.layout-view__spaces {
  margin-top: 28px;
}

.layout-view__label {
  display: block;
  margin: 0 8px 10px;
  color: var(--ac-text-tertiary);
  font-size: var(--ac-text-xs);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

/* ===== 顶栏:毛玻璃 + 滚动边缘渐隐 ===== */
.layout-view__header {
  display: flex;
  height: 52px;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  border-bottom: 1px solid var(--ac-divider);
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: var(--ac-z-sticky);
}

/* ===== 用户区:圆头像 + 胶囊 hover ===== */
.layout-view__user {
  display: flex;
  gap: 10px;
  align-items: center;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: var(--ac-radius-pill);
  color: var(--ac-text-primary);
  font-size: var(--ac-text-sm);
  font-weight: 500;
  transition: background-color var(--ac-dur-fast) var(--ac-ease-out);
}
.layout-view__user:hover {
  background: var(--ac-bg-hover);
}

.layout-view__avatar {
  display: grid;
  width: 28px;
  height: 28px;
  background: var(--ac-primary-soft);
  color: var(--ac-primary);
  border-radius: 50%;
  font-size: var(--ac-text-xs);
  font-weight: 600;
  place-items: center;
}

.layout-view__main {
  min-width: 0;
  padding: 28px;
}

/* ===== 响应式 ===== */
@media (max-width: 760px) {
  .layout-view__aside {
    display: none;
  }
  .layout-view__main {
    padding: 18px;
  }
}
</style>
