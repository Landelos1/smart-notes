import request from '@/utils/http'

export function fetchDashboardOverview() {
  return request.get<Api.Dashboard.DashboardOverviewVO>({
    url: '/api/dashboard/overview'
  })
}

export function fetchUserGrowth() {
  return request.get<Api.Dashboard.UserGrowth>({
    url: '/api/dashboard/user-growth'
  })
}

export function fetchNoteSquareStats() {
  return request.get<Api.Dashboard.NoteSquareStats>({
    url: '/api/dashboard/note-square-stats'
  })
}

export function fetchDashboardDynamics() {
  return request.get<Api.Dashboard.Dynamics>({
    url: '/api/dashboard/dynamics'
  })
}

export function fetchOrderStats() {
  return request.get<Api.Dashboard.OrderStats>({
    url: '/api/dashboard/order-stats'
  })
}
