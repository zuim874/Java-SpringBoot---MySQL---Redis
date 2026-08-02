// src/utils/token.js

/**
 * 解析 JWT Token
 */
export function parseJWT(token) {
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
        console.error('解析 Token 失败:', error)
        return null
    }
}

/**
 * 检查 Token 是否过期
 */
export function isTokenExpired(token) {
    if (!token) return true

    const payload = parseJWT(token)
    if (!payload || !payload.exp) return true

    // exp 是秒，当前时间转为秒
    const currentTime = Math.floor(Date.now() / 1000)
    return payload.exp < currentTime
}