 import { AppRouteRecord } from '@/types/router'

export const articleRoutes: AppRouteRecord[] = [
  {
    path: '/note/list',
    name: 'ArticleList',
    component: '/article/list',
    meta: {
      title: 'menus.article.articleList',
      icon: 'ri:sticky-note-line',
      keepAlive: true,
      roles: ['R_SUPER', 'R_ADMIN'],
      authList: [
        { title: '新增', authMark: 'add' },
        { title: '编辑', authMark: 'edit' }
      ]
    }
  },
  {
    path: '/notebook/list',
    name: 'NotebookManage',
    component: '/notebook/list',
    meta: {
      title: 'menus.article.title',
      icon: 'ri:book-open-line',
      keepAlive: true,
      roles: ['R_SUPER', 'R_ADMIN']
    }
  },
  {
    path: '/note/publish',
    name: 'ArticlePublish',
    component: '/article/publish',
    meta: {
      title: 'menus.article.articlePublish',
      icon: 'ri:edit-box-line',
      keepAlive: true,
      isHide: true,
      roles: ['R_SUPER', 'R_ADMIN'],
      authList: [{ title: '发布', authMark: 'add' }],
      activePath: '/note/list'
    }
  },
  {
    path: '/note-template/manage',
    name: 'NoteTemplateManage',
    component: '/article/template-manage',
    meta: {
      title: 'menus.article.templateManage',
      icon: 'ri:file-copy-2-line',
      keepAlive: true,
      roles: ['R_SUPER', 'R_ADMIN'],
      authList: [{ title: '发布', authMark: 'add' }]
    }
  },
  {
    path: '/note-comment/manage',
    name: 'ArticleComment',
    component: '/article/comment',
    meta: {
      title: 'menus.article.comment',
      icon: 'ri:chat-3-line',
      keepAlive: true,
      roles: ['R_SUPER', 'R_ADMIN']
    }
  },
  {
    path: '/note-detail/:id',
    name: 'ArticleDetail',
    component: '/article/detail',
    meta: {
      title: 'menus.article.articleDetail',
      isHide: true,
      keepAlive: true,
      roles: ['R_SUPER', 'R_ADMIN'],
      activePath: '/note/list'
    }
  }
]
