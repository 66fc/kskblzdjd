import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const ADMIN_META = { requiresAuth: true, requiresAdmin: true }

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true, title: '管理后台登录' }
    },
    {
      path: '/club/login',
      name: 'client-login',
      component: () => import('@/views/client/ClientLogin.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/club',
      component: () => import('@/layout/ClientLayout.vue'),
      children: [
        { path: '', redirect: '/club/home' },
        {
          path: 'home',
          name: 'client-home',
          component: () => import('@/views/client/ClientHome.vue'),
          meta: { public: true, title: '首页' }
        },
        {
          path: 'booking',
          name: 'client-booking',
          component: () => import('@/views/client/ClientBooking.vue'),
          meta: { public: true, title: '场地预约' }
        },
        {
          path: 'bookings',
          name: 'client-bookings',
          component: () => import('@/views/client/ClientBookings.vue'),
          meta: { requiresAuth: true, title: '我的预约' }
        },
        {
          path: 'profile',
          name: 'client-profile',
          component: () => import('@/views/client/ClientProfile.vue'),
          meta: { requiresAuth: true, title: '个人中心' }
        }
      ]
    },
    {
      path: '/portal',
      redirect: '/club/home'
    },
    {
      path: '/',
      component: () => import('@/layout/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/club/home' },
        {
          path: 'dashboard', name: 'dashboard',
          component: () => import('@/views/Dashboard.vue'), meta: { ...ADMIN_META, title: '数据总览' }
        },
        {
          path: 'courts', name: 'courts',
          component: () => import('@/views/Courts.vue'), meta: { ...ADMIN_META, title: '场地管理' }
        },
        {
          path: 'members', name: 'members',
          component: () => import('@/views/Members.vue'), meta: { ...ADMIN_META, title: '用户管理' }
        },
        {
          path: 'levels', name: 'levels',
          component: () => import('@/views/Levels.vue'), meta: { ...ADMIN_META, title: '会员等级' }
        },
        {
          path: 'bookings', name: 'bookings',
          component: () => import('@/views/Bookings.vue'), meta: { ...ADMIN_META, title: '预约管理' }
        },
        {
          path: 'sales', name: 'sales',
          component: () => import('@/views/Sales.vue'), meta: { ...ADMIN_META, title: '销售管理' }
        },
        {
          path: 'daily-stats', name: 'daily-stats',
          component: () => import('@/views/DailyStats.vue'), meta: { ...ADMIN_META, title: '日使用统计' }
        },
        {
          path: 'clock', name: 'clock',
          component: () => import('@/views/Clock.vue'), meta: { ...ADMIN_META, title: '模拟时间' }
        },
        {
          path: 'logs', name: 'logs',
          component: () => import('@/views/Logs.vue'), meta: { ...ADMIN_META, title: '操作日志' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/club/home' }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    // 公开页也恢复登录态：直接刷新页面时 Pinia 只有 token，没有 user
    if (auth.token && !auth.user) {
      try {
        await auth.fetchMe()
      } catch {
        auth.logout()
      }
    }
    return true
  }
  if (!auth.token) {
    const loginPath = to.path.startsWith('/club') ? '/club/login' : '/login'
    return { path: loginPath, query: { redirect: to.fullPath } }
  }
  if (!auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      const loginPath = to.path.startsWith('/club') ? '/club/login' : '/login'
      return { path: loginPath, query: { redirect: to.fullPath } }
    }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { path: '/club/home' }
  }
  return true
})

router.afterEach((to) => {
  const isClient = to.path.startsWith('/club')
  document.documentElement.classList.toggle('client-theme', isClient)
  document.title = isClient
    ? `${to.meta.title || '羽球幻想馆'} · 羽球幻想馆`
    : `${to.meta.title || '星羽'} · 羽毛球场馆管理系统`
})

export default router
