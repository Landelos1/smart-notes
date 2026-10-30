/**
 * 路由权限验证模块
 *
 * 提供路由权限验证和路径检查功能
 *
 * ## 主要功能
 *
 * - 验证路径是否在用户菜单权限中
 * - 构建菜单路径集合（扁平化处理）
 * - 支持动态路由参数匹配
 * - 路径前缀匹配
 *
 * ## 使用场景
 *
 * - 路由守卫中验证用户权限
 * - 动态路由注册后的权限检查
 * - 防止用户访问无权限的页面
 *
 * @module router/core/RoutePermissionValidator
 * @author Art Design Pro Team
 */

import type { AppRouteRecord } from '@/types/router'

/**
 * 路由权限验证器
 */
export class RoutePermissionValidator {
  /**
   * 验证路径是否在用户菜单权限中
   * @param targetPath 目标路径
   * @param menuList 菜单列表
   * @returns 是否有权限访问
   */
  static hasPermission(targetPath: string, menuList: AppRouteRecord[]): boolean {
    // 根路径始终允许访问
    if (targetPath === '/') {
      return true
    }

    // 构建路径集合
    const pathSet = this.buildMenuPathSet(menuList)

    return pathSet.has(targetPath) || this.checkPathPattern(targetPath, pathSet)
  }

  /**
   * 构建菜单路径集合（扁平化处理）
   * @param menuList 菜单列表
   * @param pathSet 路径集合
   * @returns 路径集合
   */
  static buildMenuPathSet(
    menuList: AppRouteRecord[],
    pathSet: Set<string> = new Set()
  ): Set<string> {
    if (!Array.isArray(menuList) || menuList.length === 0) {
      return pathSet
    }

    for (const menuItem of menuList) {
      // 隐藏仅影响菜单展示。此列表已按角色过滤，详情和编辑页仍需允许访问。
      if (!menuItem.path) {
        continue
      }

      // 标准化路径并添加到集合
      const menuPath = menuItem.path.startsWith('/') ? menuItem.path : `/${menuItem.path}`
      pathSet.add(menuPath)

      // 递归处理子菜单
      if (menuItem.children?.length) {
        this.buildMenuPathSet(menuItem.children, pathSet)
      }
    }

    return pathSet
  }

  /**
   * 按完整路径匹配动态参数，避免父目录权限放行未授权的子页面。
   * @param targetPath 目标路径
   * @param pathSet 路径集合
   * @returns 是否匹配
   */
  static checkPathPattern(targetPath: string, pathSet: Set<string>): boolean {
    for (const menuPath of pathSet) {
      const pattern = menuPath
        .split('/')
        .map((segment) =>
          segment.startsWith(':') ? '[^/]+' : segment.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
        )
        .join('/')
      if (new RegExp(`^${pattern}/?$`).test(targetPath)) {
        return true
      }
    }
    return false
  }

  /**
   * 验证并返回有效的路径
   * 如果目标路径无权限，返回首页路径
   * @param targetPath 目标路径
   * @param menuList 菜单列表
   * @param homePath 首页路径
   * @returns 验证后的路径
   */
  static validatePath(
    targetPath: string,
    menuList: AppRouteRecord[],
    homePath: string = '/'
  ): { path: string; hasPermission: boolean } {
    const hasPermission = this.hasPermission(targetPath, menuList)

    if (hasPermission) {
      return { path: targetPath, hasPermission: true }
    }

    return { path: homePath, hasPermission: false }
  }
}
