import request from '@/utils/http'

function normalizeOrderList(
  response: {
    total?: number
    data?: Api.Payment.OrderVO[]
    records?: Api.Payment.OrderVO[]
  } | Api.Payment.OrderVO[] | null | undefined,
  current: number,
  size: number
) {
  if (Array.isArray(response)) {
    return {
      records: response,
      total: response.length,
      current,
      size
    }
  }

  const records = Array.isArray(response?.records)
    ? response.records
    : Array.isArray(response?.data)
      ? response.data
      : []

  return {
    records,
    total: typeof response?.total === 'number' ? response.total : records.length,
    current,
    size
  }
}

export function fetchOrderList(params: Api.Payment.OrderListParams) {
  const current = params.current ?? 1
  const size = params.size ?? 10

  return request
    .get<{ total?: number; data?: Api.Payment.OrderVO[]; records?: Api.Payment.OrderVO[] } | Api.Payment.OrderVO[]>({
      url: '/api/payment/admin/orders',
      params: {
        ...params,
        current,
        size
      }
    })
    .then((res) => normalizeOrderList(res, current, size))
}

export function fetchOrderStatus(orderNo: string) {
  return request.get<Api.Payment.OrderVO>({
    url: `/api/payment/order/${orderNo}`
  })
}

export function fetchCancelOrder(orderNo: string) {
  return request.post<void>({
    url: `/api/payment/admin/order/${orderNo}/cancel`
  })
}

export function fetchDeleteOrder(orderNo: string) {
  return request.del<void>({
    url: `/api/payment/admin/order/${orderNo}`
  })
}
