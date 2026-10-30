import { createRouter, createWebHistory } from 'vue-router'

export const routes = [
  { path: '/:pathMatch(.*)*', redirect: '/' },
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
    meta: { requiresAuth: true }
  },
  {
    path: '/note-square',
    name: 'NoteSquare',
    component: () => import('@/views/NoteSquare/NoteSquarePage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/note-square/favorites',
    name: 'NoteSquareFavorites',
    component: () => import('@/views/NoteSquare/Favorites.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/note-square/likes',
    name: 'NoteSquareLikes',
    component: () => import('@/views/NoteSquare/Likes.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/note-square/mine',
    name: 'NoteSquareMine',
    component: () => import('@/views/NoteSquare/MyNotes.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/team',
    name: 'Team',
    component: () => import('@/views/Team/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/templates/:id?',
    name: 'TemplateCenter',
    component: () => import('@/views/TemplateCenter/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/shared-kb',
    name: 'SharedKnowledgeBase',
    component: () => import('@/views/SharedKnowledgeBase/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/personal-kb',
    name: 'PersonalKnowledgeBase',
    component: () => import('@/views/PersonalKnowledgeBase/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/recycle-bin',
    name: 'RecycleBin',
    component: () => import('@/views/RecycleBin/index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/member-system',
    name: 'MemberSystem',
    component: () => import('@/views/MemberSystem/index.vue'),
    meta: { requiresAuth: true }
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

router.beforeEach((to) => {
  let token = null
  try {
    token = localStorage.getItem('satoken')
  } catch {}

  if (to.meta.requiresAuth && !token) return { name: 'Auth' }
  if (to.name === 'Auth' && token) return { name: 'Notes' }
  return true
})

export default router
