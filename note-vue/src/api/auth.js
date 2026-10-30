import { apiFetch } from './apiClient'

/**
 * 获取图形验证码
 * @returns {Promise<{code: number, message: string, data: {captchaId: string, imageBase64: string}}>}
 */
export async function getCaptcha() {
  try {
    const r = await apiFetch('/api/common/captcha', { method: 'GET' })
    console.log('[Auth] 获取验证码成功')
    return r
  } catch (error) {
    console.error('[Auth] 获取验证码失败:', error)
    throw error
  }
}

/**
 * 用户登录
 * @param {Object} payload - 登录参数
 * @param {string} payload.phone - 手机号
 * @param {string} payload.password - 密码
 * @param {string} payload.captcha - 验证码
 * @param {string} payload.captchaId - 验证码ID
 * @param {number} payload.roleType - 角色类型 1-求职者 2-招聘者 3-管理员
 * @returns {Promise<{code: number, message: string, data: string}>} - data为token
 */
export async function login(payload) {
  try {
    const r = await apiFetch('/api/common/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    if (r && r.code === 0) {
      console.log('[Auth] 登录成功')
    } else {
      console.warn('[Auth] 登录失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Auth] 登录请求异常:', error)
    throw error
  }
}

/**
 * 用户注册
 * @param {Object} payload - 注册参数
 * @param {string} payload.phone - 手机号
 * @param {string} payload.password - 密码
 * @param {string} payload.captcha - 验证码
 * @param {string} payload.captchaId - 验证码ID
 * @param {number} payload.roleType - 角色类型
 * @param {string} [payload.username] - 用户名
 * @param {string} [payload.avatarUrl] - 头像URL
 * @returns {Promise<{code: number, message: string, data: null}>}
 */
export async function register(payload) {
  try {
    const r = await apiFetch('/api/common/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
    if (r && r.code === 0) {
      console.log('[Auth] 注册成功')
    } else {
      console.warn('[Auth] 注册失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Auth] 注册请求异常:', error)
    throw error
  }
}

/**
 * 退出登录
 * @returns {Promise<{code: number, message: string, data: null}>}
 */
export async function logout() {
  try {
    const r = await apiFetch('/api/common/logout', { method: 'POST' })
    // 清除本地存储的token
    try {
      localStorage.removeItem('satoken')
    } catch (e) {
      console.warn('[Auth] 清除本地token失败:', e)
    }
    console.log('[Auth] 退出登录成功')
    return r
  } catch (error) {
    console.error('[Auth] 退出登录请求异常:', error)
    throw error
  }
}

/**
 * 获取当前登录用户信息
 * @returns {Promise<import('../types/api').ApiResponse<import('../types/api').UserInfo>>}
 */
export async function getUserInfo() {
  try {
    const r = await apiFetch('/api/common/user/info', { method: 'GET' })
    if (r && r.code === 0) {
      console.log('[Auth] 获取用户信息成功')
    }
    return r
  } catch (error) {
    console.error('[Auth] 获取用户信息失败:', error)
    throw error
  }
}

/**
 * 根据用户ID获取用户信息
 * @param {number} userId - 用户ID
 * @returns {Promise<{code: number, message: string, data: Object}>}
 */
export async function getUserInfoById(userId) {
  try {
    const r = await apiFetch(`/api/common/user/info/${userId}`, { method: 'GET' })
    return r
  } catch (error) {
    console.error('[Auth] 获取用户信息失败:', error)
    throw error
  }
}

/**
 * 更新用户信息
 * @param {Object} userInfo - 用户信息
 * @returns {Promise<{code: number, message: string, data: null}>}
 */
export async function updateUserInfo(userInfo) {
  try {
    const r = await apiFetch('/api/common/user/update', {
      method: 'PUT',
      body: JSON.stringify(userInfo),
    })
    if (r && r.code === 0) {
      console.log('[Auth] 更新用户信息成功')
    } else {
      console.warn('[Auth] 更新用户信息失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Auth] 更新用户信息请求异常:', error)
    throw error
  }
}

/**
 * 检查登录状态
 * @returns {boolean}
 */
export function isLoggedIn() {
  try {
    const token = localStorage.getItem('satoken')
    return !!token
  } catch {
    return false
  }
}

/**
 * 获取本地存储的token
 * @returns {string|null}
 */
export function getToken() {
  try {
    return localStorage.getItem('satoken')
  } catch {
    return null
  }
}

