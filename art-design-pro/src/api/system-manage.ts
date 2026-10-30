import request from '@/utils/http'
import { AppRouteRecord } from '@/types/router'

function mapRoleTypeToRoles(roleType?: number): string[] {
  if (roleType === 2) return ['R_ADMIN']
  return ['R_USER']
}

function mapGenderToText(gender?: number): string {
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  return ''
}

function parseNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') return undefined
  const num = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(num) ? num : undefined
}

// 获取用户列表
export function fetchGetUserList(params: Api.SystemManage.UserSearchParams) {
  const page = params.current ?? 1
  const size = params.size ?? 10

  const keyword =
    (typeof params.userName === 'string' && params.userName.trim()) ||
    (typeof params.userPhone === 'string' && params.userPhone.trim()) ||
    (typeof params.userEmail === 'string' && params.userEmail.trim()) ||
    undefined

  const roleType = parseNumber((params as any).roleType)
  const status = parseNumber(params.status)
  const gender = parseNumber((params as any).userGender)

  return request
    .get<{ total?: number; data?: Api.Auth.BackendUser[] }>({
      url: '/api/common/users',
      params: {
        page,
        size,
        keyword,
        roleType,
        status,
        gender
      }
    })
    .then((res) => {
      const list = Array.isArray(res?.data) ? res.data : []
      const records: Api.SystemManage.UserListItem[] = list.map((user) => ({
        id: user.id,
        avatar: user.avatarUrl || '',
        status: String(user.status ?? ''),
        userName: user.username,
        userGender: mapGenderToText(user.gender),
        nickName: user.realName || '',
        userPhone: user.phone,
        userEmail: '',
        userRoles: mapRoleTypeToRoles(user.roleType),
        createBy: '',
        createTime: user.createTime || '',
        updateBy: '',
        updateTime: user.updateTime || '',
        roleType: user.roleType,
        gender: user.gender,
        statusCode: user.status,
        avatarUrl: user.avatarUrl,
        realName: user.realName,
        memberActive: user.isMember,
        memberExpireTime: user.memberExpireTime,
        memberLevelName: user.memberLevelName
      }))

      return {
        records,
        total: res?.total ?? records.length,
        current: page,
        size
      }
    })
}

export function fetchCreateUserByAdmin(params: Api.SystemManage.AdminUserCreateParams) {
  return request.post<Api.Auth.BackendUser>({
    url: '/api/common/users',
    params,
    showSuccessMessage: true
  })
}

export function fetchUpdateUserByAdmin(userId: number, params: Api.SystemManage.AdminUserUpdateParams) {
  return request.put<void>({
    url: `/api/common/users/${userId}`,
    params,
    showSuccessMessage: true
  })
}

export function fetchDisableUserByAdmin(userId: number) {
  return request.del<void>({
    url: `/api/common/users/${userId}`,
    showSuccessMessage: true
  })
}

export function fetchEnableUserByAdmin(userId: number) {
  return request.post<void>({
    url: `/api/common/users/${userId}/enable`,
    showSuccessMessage: true
  })
}

export function fetchHardDeleteUserByAdmin(userId: number) {
  return request.post<void>({
    url: `/api/common/users/${userId}/hard-delete`,
    showSuccessMessage: true
  })
}

// 获取角色列表
export function fetchGetRoleList(params: Api.SystemManage.RoleSearchParams) {
  return request.get<Api.SystemManage.RoleList>({
    url: '/api/role/list',
    params
  })
}

// 获取菜单列表
export function fetchGetMenuList() {
  return request.get<AppRouteRecord[]>({
    url: '/api/v3/system/menus'
  })
}
