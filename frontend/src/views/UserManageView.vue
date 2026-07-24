<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, CopyDocument, Link } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth';
import {
  listUsers,
  changeUserRole,
  changeUserStatus,
  resetUserPassword,
  deleteUser,
} from '@/services/userService';
import { createInvitation } from '@/services/invitationService';
import type { UserResponse, UserRole, UserStatus } from '@/types';

const authStore = useAuthStore();
const users = ref<UserResponse[]>([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);

// 邀请对话框状态
const inviteDialogVisible = ref(false);
const inviteLoading = ref(false);
const inviteHours = ref(72);
const generatedLink = ref('');
const generatedLinkVisible = ref(false);

async function copyToClipboard(text: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success('链接已复制到剪贴板');
  } catch {
    ElMessage.warning('复制失败,请手动选择文本复制');
  }
}

async function handleCreateInvitation(): Promise<void> {
  inviteLoading.value = true;
  try {
    const result = await createInvitation(
      inviteHours.value ? { expiresInHours: inviteHours.value } : {},
    );
    // inviteUrl 形如 /invite/<token>,拼接为完整可访问链接
    const origin = window.location.origin;
    generatedLink.value = `${origin}${result.inviteUrl}`;
    generatedLinkVisible.value = true;
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '创建邀请失败');
  } finally {
    inviteLoading.value = false;
  }
}

function openInviteDialog(): void {
  inviteHours.value = 72;
  generatedLink.value = '';
  generatedLinkVisible.value = false;
  inviteDialogVisible.value = true;
}

async function fetchUsers(): Promise<void> {
  loading.value = true;
  try {
    const result = await listUsers(page.value - 1, 20);
    users.value = result.content;
    total.value = result.total;
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载用户列表失败');
  } finally {
    loading.value = false;
  }
}

async function handleRoleChange(user: UserResponse): Promise<void> {
  const newRole: UserRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
  try {
    await ElMessageBox.confirm(
      `确定将用户 ${user.username} 的角色修改为 ${newRole}？`,
      '修改角色',
      { type: 'warning' },
    );
    await changeUserRole(user.id, { role: newRole });
    ElMessage.success('角色修改成功');
    await fetchUsers();
  } catch {
    // 取消操作
  }
}

async function handleStatusToggle(user: UserResponse): Promise<void> {
  const newStatus: UserStatus = user.status === 'ACTIVE' ? 'DELETED' : 'ACTIVE';
  const action = newStatus === 'DELETED' ? '禁用' : '启用';
  try {
    await ElMessageBox.confirm(`确定${action}用户 ${user.username}？`, `${action}用户`, {
      type: 'warning',
    });
    await changeUserStatus(user.id, { status: newStatus });
    ElMessage.success(`${action}成功`);
    await fetchUsers();
  } catch {
    // 取消操作
  }
}

async function handleResetPassword(user: UserResponse): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入新密码（至少 6 位）',
      `重置 ${user.username} 的密码`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.{6,}/,
        inputErrorMessage: '密码至少 6 位',
      },
    );
    await resetUserPassword(user.id, { newPassword: value });
    ElMessage.success('密码重置成功，用户下次登录需修改密码');
  } catch {
    // 取消操作
  }
}

async function handleDelete(user: UserResponse): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${user.username}？此操作不可恢复。`, '删除用户', {
      type: 'warning',
    });
    await deleteUser(user.id);
    ElMessage.success('用户已删除');
    await fetchUsers();
  } catch {
    // 取消操作
  }
}

onMounted(() => {
  void fetchUsers();
});
</script>

<template>
  <div class="user-manage-view">
    <div class="user-manage-view__header">
      <h2>用户管理</h2>
      <el-button type="primary" :icon="Plus" @click="openInviteDialog">邀请用户</el-button>
    </div>
    <el-card shadow="never">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="需改密" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.mustChangePassword" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.id === authStore.userId"
              @click="handleRoleChange(row)"
            >
              切换角色
            </el-button>
            <el-button
              link
              type="primary"
              :disabled="row.id === authStore.userId"
              @click="handleStatusToggle(row)"
            >
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="primary" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.id === authStore.userId"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-pagination
      class="user-manage-view__pagination"
      v-model:current-page="page"
      :total="total"
      :page-size="20"
      layout="total, prev, pager, next"
      @current-change="fetchUsers"
    />
    <!-- 邀请用户对话框 -->
    <el-dialog v-model="inviteDialogVisible" title="邀请用户" width="480px" :close-on-click-modal="false">
      <el-form label-width="100px" v-if="!generatedLinkVisible">
        <el-form-item label="有效期(小时)">
          <el-input-number v-model="inviteHours" :min="1" :max="720" :step="24" />
        </el-form-item>
      </el-form>
      <div v-else class="user-manage-view__invite-result">
        <el-alert type="success" :closable="false" show-icon>
          <template #title>邀请链接已生成</template>
          <template #default>
            <div class="user-manage-view__invite-link">
              <el-input :model-value="generatedLink" readonly>
                <template #append>
                  <el-button :icon="CopyDocument" @click="copyToClipboard(generatedLink)" />
                </template>
              </el-input>
            </div>
            <p class="user-manage-view__invite-hint">
              请将此链接发送给被邀请者，对方打开后可自行设置用户名和密码完成注册。
            </p>
          </template>
        </el-alert>
      </div>
      <template #footer>
        <template v-if="!generatedLinkVisible">
          <el-button @click="inviteDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="inviteLoading" @click="handleCreateInvitation">
            生成邀请链接
          </el-button>
        </template>
        <template v-else>
          <el-button @click="inviteDialogVisible = false">关闭</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage-view {
  display: grid;
  gap: var(--ac-space-5);
}

.user-manage-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-manage-view__header h2 {
  margin: 0;
  font-size: var(--ac-text-2xl);
  font-weight: 600;
  letter-spacing: -0.02em;
}

.user-manage-view__pagination {
  justify-content: flex-end;
}

.user-manage-view__invite-link {
  margin-top: 12px;
}

.user-manage-view__invite-hint {
  margin: 12px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
