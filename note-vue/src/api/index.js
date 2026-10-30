// API 请求统一导出
// 导出各模块API
export * from './auth'
export * from './chat'
export * from './notes'
export * from './personalKnowledge'
export * from './settings'
export * from './sharedKnowledge'
export * from './template'
export * from './noteSquare'
export * from './membershipQuota'

// 基础请求配置
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * @typedef {Object} ApiResponse
 * @property {number} code - 响应码
 * @property {string} message - 响应消息
 * @property {*} data - 响应数据
 */

/**
 * 通用请求方法
 * @param {string} url - 请求地址
 * @param {RequestInit} [options] - 请求配置
 * @returns {Promise<ApiResponse>}
 */
export async function request(url, options) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })
  return response.json()
}
