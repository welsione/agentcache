import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router';
import DashboardView from '@/views/DashboardView.vue';
import ChangePasswordView from '@/views/ChangePasswordView.vue';
import FileDetailView from '@/views/FileDetailView.vue';
import InviteAcceptView from '@/views/InviteAcceptView.vue';
import LayoutView from '@/views/LayoutView.vue';
import LoginView from '@/views/LoginView.vue';
import NotFoundView from '@/views/NotFoundView.vue';
import PublicFileView from '@/views/PublicFileView.vue';
import SpaceDetailView from '@/views/SpaceDetailView.vue';
import UserManageView from '@/views/UserManageView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true },
    },
    {
      path: '/invite/:token',
      name: 'invite-accept',
      component: InviteAcceptView,
      meta: { public: true },
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: ChangePasswordView,
      meta: { allowDuringMustChange: true },
    },
    {
      path: '/',
      component: LayoutView,
      children: [
        {
          path: '',
          redirect: '/dashboard',
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
        },
        {
          path: 'spaces/:id',
          name: 'space-detail',
          component: SpaceDetailView,
        },
        {
          path: 'files/:spaceId/:fileId',
          name: 'file-detail',
          component: FileDetailView,
        },
        {
          path: 'users',
          name: 'users',
          component: UserManageView,
          meta: { requiresAdmin: true },
        },
      ],
    },
    {
      path: '/public/files/:fileId/view',
      name: 'public-file',
      component: PublicFileView,
      meta: { public: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
    },
  ],
});

function isPublicRoute(to: RouteLocationNormalized): boolean {
  return to.matched.some((route) => route.meta.public === true) || to.path.startsWith('/public/');
}

router.beforeEach((to) => {
  const token = localStorage.getItem('agentcache-token');
  const mustChangePassword = localStorage.getItem('agentcache-must-change-password') === 'true';
  const role = localStorage.getItem('agentcache-role');

  // 未认证访问受保护路由 → 登录页
  if (!isPublicRoute(to) && !token) {
    return {
      name: 'login',
      query: to.fullPath === '/' ? undefined : { redirect: to.fullPath },
    };
  }

  // 已认证访问登录页 → 跳转
  if (to.name === 'login' && token) {
    return mustChangePassword ? { name: 'change-password' } : { name: 'dashboard' };
  }

  // 必须改密时,只允许访问改密页
  if (
    token &&
    mustChangePassword &&
    to.name !== 'change-password' &&
    !isPublicRoute(to) &&
    !to.matched.some((route) => route.meta.allowDuringMustChange === true)
  ) {
    return { name: 'change-password' };
  }

  // 非 ADMIN 访问管理页面 → 跳控制台
  if (to.matched.some((route) => route.meta.requiresAdmin === true) && role !== 'ADMIN') {
    return { name: 'dashboard' };
  }

  return true;
});

export default router;
