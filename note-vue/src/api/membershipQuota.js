/**
 * 会员配额相关 API
 */
import apiClient from './apiClient';

/**
 * 获取当前用户的会员配额信息
 * @param {number} userId - 用户ID
 * @returns {Promise} 配额信息
 */
export const getUserMembershipQuota = (userId) => {
  return apiClient.get(`/api/common/users/${userId}/quota`);
};

/**
 * 刷新配额信息（可用于强制刷新缓存）
 * @param {number} userId - 用户ID
 * @returns {Promise} 配额信息
 */
export const refreshUserQuota = (userId) => {
  return apiClient.get(`/api/common/users/${userId}/quota?refresh=true`);
};
