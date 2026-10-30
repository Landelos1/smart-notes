import { AppRouteRecord } from '@/types/router'

export const dashboardRoutes: AppRouteRecord = {
  name: 'Dashboard',
  path: '/dashboard',
  component: '/dashboard/console',
  meta: {
    title: 'menus.dashboard.title',
    icon: 'ri:dashboard-2-line',
    roles: ['R_SUPER', 'R_ADMIN'],
    keepAlive: false,
    fixedTab: false,
    isHome: true
  }
}
