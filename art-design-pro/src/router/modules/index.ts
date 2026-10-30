import { AppRouteRecord } from '@/types/router'
import { dashboardRoutes } from './dashboard'
import { systemRoutes, userRoutes } from './system'
import { articleRoutes } from './article'
import { helpRoutes } from './help'
import { orderRoutes } from './order'

/**
 * 导出所有模块化路由
 */
export const routeModules: AppRouteRecord[] = [
  dashboardRoutes,
  orderRoutes,
  userRoutes,
  systemRoutes,
  ...articleRoutes,
  ...helpRoutes
]
