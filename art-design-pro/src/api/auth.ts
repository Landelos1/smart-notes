import request from '@/utils/http'

function mapRoleTypeToRoles(roleType?: number): string[] {
  if (roleType === 2) return ['R_ADMIN']
  return ['R_USER']
}

function mapRoleTypeToButtons(roleType?: number): string[] {
  if (roleType === 2) {
    return ['add', 'edit', 'delete', 'export', 'view', 'publish', 'config', 'manage']
  }

  return ['view']
}

/**
 * 登录
 * @param params 登录参数
 * @returns 登录响应
 */
export function fetchLogin(params: Api.Auth.LoginParams) {
  return request
    .post<string>({
      url: '/api/common/login',
      params
    })
    .then((token) => ({ token }))
}

export function fetchRegister(params: Api.Auth.RegisterParams) {
  return request.post<void>({
    url: '/api/common/register',
    params,
    showSuccessMessage: true
  })
}

export function fetchGetCaptcha() {
  return request.get<Api.Auth.CaptchaVO>({
    url: '/api/common/captcha'
  })
}

export function fetchLogout() {
  return request.post<void>({
    url: '/api/common/logout'
  })
}

/**
 * 获取用户信息
 * @returns 用户信息
 */
export function fetchGetUserInfo() {
  return request
    .get<Api.Auth.BackendUser>({
      url: '/api/common/user/info',
      showErrorMessage: false
    })
    .then((user) => {
      return {
        userId: user.id,
        userName: user.username,
        phone: user.phone,
        roleType: user.roleType,
        avatar: user.avatarUrl,
        email: undefined,
        roles: mapRoleTypeToRoles(user.roleType),
        buttons: mapRoleTypeToButtons(user.roleType)
      } satisfies Api.Auth.UserInfo
    })
}
