// 封装 HTTP 请求工具，自动带上 Token

const BASE_URL = '/api'  // 后端接口前缀（会被 Vite 代理）

/**
 * 发送 HTTP 请求
 *
 * @param {string} url - 接口路径（如 /auth/login）
 * @param {object} options - 请求配置
 * @returns {Promise} 响应数据
 */
export async function request(url, options = {}) {
    // 从 localStorage 获取 Token
    const token = localStorage.getItem('token')

    // 默认配置
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    }

    // 如果有 Token，添加到请求头
    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`
    }

    // 合并配置
    const mergedOptions = { ...defaultOptions, ...options }

    // 如果是表单格式（URLSearchParams），修改 Content-Type
    if (options.body instanceof URLSearchParams) {
        mergedOptions.headers['Content-Type'] = 'application/x-www-form-urlencoded'
    }

    try {
        // 发送请求
        const res = await fetch(BASE_URL + url, mergedOptions)
        const data = await res.json()

        // 如果返回 401（未授权），可能是 Token 过期或无效
        if (data.code === 401) {
            // 清除 Token
            localStorage.removeItem('token')
            localStorage.removeItem('nickname')

            // 跳转到登录页
            window.location.href = '/login'
        }

        return data
    } catch (error) {
        console.error('请求失败:', error)
        throw error
    }
}