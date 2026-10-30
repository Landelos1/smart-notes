import request from '@/utils/http'

export function fetchTokenStats() {
  return request.get<Api.Chat.TokenStatsVO>({
    url: '/api/chat/token-stats'
  })
}

export function fetchTodayRequestCount() {
  return request.get<number>({
    url: '/api/chat/request-count/today'
  })
}
