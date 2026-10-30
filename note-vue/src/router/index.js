import { createRouter, createWebHistory } from 'vue-router'

export const routes = [
  {
    path: '/login',
    name: 'Auth',
    component: () => import('@/views/Auth/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Notes',
    component: () => import('@/views/Notes/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-chat',
    name: 'AIChat',
    component: () => import('@/views/AIChat/index.vue'),
  },
  {
    path: '/templates/:id?',
    name: 'TemplateCenter',
    component: () => import('@/views/TemplateCenter/index.vue'),
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings/index.vue'),
  },
  {
    path: '/shared-kb',
    name: 'SharedKnowledgeBase',
    component: () => import('@/views/SharedKnowledgeBase/index.vue'),
  },
  {
    path: '/personal-kb',
    name: 'PersonalKnowledgeBase',
    component: () => import('@/views/PersonalKnowledgeBase/index.vue'),
  },
  {
    path: '/recycle-bin',
    name: 'RecycleBin',
    component: () => import('@/views/RecycleBin/index.vue'),
  },
  {
    path: '/member-system',
    name: 'MemberSystem',
    component: () => import('@/views/MemberSystem/index.vue'),
  },
  {
    path: '/payment/result',
    name: 'PaymentResult',
    component: () => import('@/views/PaymentResult/index.vue'),
    meta: { requiresAuth: false }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const requiresAuth = to.meta && to.meta.requiresAuth
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('satoken') : null
  if (requiresAuth && !token) {
    if (to.path !== '/login') {
      next({ path: '/login' })
      return
    }
  }
  next()
})

export default router
