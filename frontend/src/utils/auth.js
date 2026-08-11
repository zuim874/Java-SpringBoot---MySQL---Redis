/**
 * 权限检查工具模块
 * 从后端 API 获取用户角色信息，并缓存角色信息
 * 关键：必须从后端获取角色，而不是仅依赖前端本地存储
 */
import { getUserInfo } from '../api/index.js'

// 角色枚举
export const ROLES = {
  ADMIN: 'ROLE_ADMIN',
  SELLER: 'ROLE_SELLER',
  USER: 'ROLE_USER'
}

/**
 * 从后端获取当前用户的完整信息（含角色）
 * 这是最权威的角色获取方式，每次调用都会向后端请求
 * @returns {Promise<object|null>} 用户信息对象，包含 roles 字段
 */
export async function fetchUserInfo() {
  try {
    const res = await getUserInfo()
    if (res && res.code === 200 && res.data) {
      return res.data
    }
    return null
  } catch (e) {
    console.error('获取用户信息失败:', e)
    return null
  }
}

/**
 * 从后端获取当前用户的角色列表
 * @returns {Promise<string[]>} 角色数组，如 ['ROLE_USER', 'ROLE_SELLER']
 */
export async function fetchUserRoles() {
  const userInfo = await fetchUserInfo()
  if (userInfo && userInfo.roles) {
    // 后端可能返回 roles 数组或单个 role 字符串
    if (Array.isArray(userInfo.roles)) {
      return userInfo.roles
    }
    return [userInfo.roles]
  }
  // 降级：尝试从 JWT 中解析角色
  const token = localStorage.getItem('token')
  if (token) {
    try {
      const payload = parseJWT(token)
      if (payload && payload.roles) {
        return Array.isArray(payload.roles) ? payload.roles : [payload.roles]
      }
      if (payload && payload.role) {
        return [payload.role]
      }
    } catch (e) {
      // 忽略解析错误
    }
  }
  return []
}

/**
 * 解析 JWT Token
 * @param {string} token
 * @returns {object|null}
 */
function parseJWT(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (error) {
    return null
  }
}

/**
 * 检查当前用户是否拥有指定角色
 * 从后端获取最新角色信息
 * @param {string} role - 角色名，如 'ROLE_ADMIN'
 * @returns {Promise<boolean>}
 */
export async function hasRole(role) {
  const roles = await fetchUserRoles()
  return roles.includes(role)
}

/**
 * 检查当前用户是否为管理员
 * @returns {Promise<boolean>}
 */
export async function isAdmin() {
  return hasRole(ROLES.ADMIN)
}

/**
 * 检查当前用户是否为商家
 * @returns {Promise<boolean>}
 */
export async function isSeller() {
  return hasRole(ROLES.SELLER)
}

/**
 * 检查当前用户是否为商家或管理员（有商家管理权限）
 * @returns {Promise<boolean>}
 */
export async function isSellerOrAdmin() {
  const roles = await fetchUserRoles()
  return roles.includes(ROLES.SELLER) || roles.includes(ROLES.ADMIN)
}

/**
 * 缓存角色信息到 localStorage（用于快速前端判断）
 * 注意：仅用于 UI 展示优化，真正的权限校验应在路由守卫中调用 fetchUserRoles
 */
export function cacheRoles(roles) {
  if (Array.isArray(roles)) {
    localStorage.setItem('roles', JSON.stringify(roles))
  } else {
    localStorage.setItem('roles', JSON.stringify([roles]))
  }
}

/**
 * 从 localStorage 获取缓存的角色（快速、非权威）
 * @returns {string[]}
 */
export function getCachedRoles() {
  try {
    const cached = localStorage.getItem('roles')
    return cached ? JSON.parse(cached) : []
  } catch {
    return []
  }
}

/**
 * 清除缓存的角色信息
 */
export function clearRoles() {
  localStorage.removeItem('roles')
}