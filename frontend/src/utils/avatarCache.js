/**
 * 头像缓存工具
 * 1. 在 localStorage 中缓存头像 URL，避免每次加载时请求后端
 * 2. 缓存版本号机制：后端 avatar 字段变更时自动更新
 * 3. 头像加载失败时优雅降级为首字头像，避免 @error 循环
 */
const AVATAR_CACHE_KEY = 'avatar_cache'
const DEFAULT_AVATAR = '/uploads/avatars/defaultAvatar.png'

/**
 * 获取缓存的头像 URL
 * @param {string} serverAvatar - 后端返回的头像路径
 * @returns {string} 有效的头像 URL
 */
export function getCachedAvatar(serverAvatar) {
  if (!serverAvatar) return DEFAULT_AVATAR

  try {
    const cache = JSON.parse(localStorage.getItem(AVATAR_CACHE_KEY) || '{}')
    const cached = cache[serverAvatar]

    // 缓存命中且未过期（版本号一致）
    if (cached && cached.url === serverAvatar) {
      return cached.url
    }

    // 更新缓存
    cache[serverAvatar] = {
      url: serverAvatar,
      timestamp: Date.now()
    }
    // 限制缓存数量，防止 localStorage 膨胀
    const keys = Object.keys(cache)
    if (keys.length > 50) {
      const oldest = keys.slice(0, keys.length - 50)
      oldest.forEach(k => delete cache[k])
    }
    localStorage.setItem(AVATAR_CACHE_KEY, JSON.stringify(cache))

    return serverAvatar
  } catch {
    return serverAvatar
  }
}

/**
 * 清除所有头像缓存（用户更新头像后调用）
 */
export function clearAvatarCache() {
  localStorage.removeItem(AVATAR_CACHE_KEY)
}

/**
 * 头像加载失败时的降级处理
 * 生成首字头像需要的背景色（基于 nickname 的哈希值）
 * @param {string} nickname - 用户昵称
 * @returns {string} 背景色 hex
 */
export function getAvatarFallbackColor(nickname) {
  let hash = 0
  const str = nickname || '用'
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash)
  }
  const colors = [
    '#1e3a5f', '#0d9488', '#2563eb', '#7c3aed',
    '#0891b2', '#4f46e5', '#0e7490', '#1d4ed8'
  ]
  return colors[Math.abs(hash) % colors.length]
}

export { DEFAULT_AVATAR }