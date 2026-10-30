import { AppRouteRecord } from '@/types/router'

export const orderRoutes: AppRouteRecord = {
  path: '/orders',
  name: 'OrderManagement',
  component: '/payment/orders',
  meta: {
    title: 'menus.payment.orders',
    icon: 'ri:bill-line',
    keepAlive: true,
    roles: ['R_SUPER', 'R_ADMIN']
  }
}
