// 封装 HTTP 请求工具，自动带上 Token

// 所有接口都以/api开头，如http://localhost:8080/api/auth/login
const BASE_URL = '/api'  // 后端接口前缀（会被 Vite 代理）

/**
 * 发送 HTTP 请求
 *
 * @param {string} url - 接口路径（如 /auth/login）
 * @param {object} options - 请求配置
 * @returns {Promise} 响应数据
 */
// export：可以将函数导出，其他文件配合 import 使用此函数
// async：标记为异步函数，函数内部可以写 await 等待接口请求完成，函数返回值包装成Promise，外部只能通过.then()或await取数据
//  await：等待一个Promise执行完毕，暂停当前函数代码，直到Promise成功或失败
// 也可以写为： const request = async () => {}
// export const request = async (url, options = {}) => {
export async function request(url, options = {}) {
    // 从 localStorage 获取 Token
    const token = localStorage.getItem('token')

    // 默认配置
    // 调用 request(url, options) 时，如果不传对应配置，就自动用这里的值；手动传入的配置会覆盖默认配置
    const defaultOptions = {
        method: 'GET',  //请求方式默认为GET
        headers: {  //请求头（附加信息）
            'Content-Type': 'application/json', //请求体数据为JSON格式
        },
    }

    // 合并 headers：先复制默认 headers，再叠加自定义 headers，避免自定义 headers 覆盖掉 Authorization
    const headers = { ...defaultOptions.headers, ...(options.headers || {}) }

    // 如果有 Token，添加到请求头
    // 请求头新增字段Authorization，后端JWT鉴权标准约定头
    if (token) {
        headers['Authorization'] = `Bearer ${token}`
    }

    // 如果是 FormData（文件上传），删除 Content-Type，让浏览器自动设置 multipart boundary
    if (options.body instanceof FormData) {
        delete headers['Content-Type']
    }
    // 如果是表单格式（URLSearchParams），修改 Content-Type
    else if (options.body instanceof URLSearchParams) {
        headers['Content-Type'] = 'application/x-www-form-urlencoded'
    }

    const mergedOptions = { ...defaultOptions, ...options, headers }

    try {
        // 发送请求
        // fetch：浏览器原生发送网络请求的API
        const res = await fetch(BASE_URL + url, mergedOptions)

        // 第一层判断：HTTP 状态码为 401 时跳转登录页（Token 过期/未登录）
        // 恢复账号相关接口例外：已删除账号的用户没有有效 token，401 属正常业务状态，不应跳转登录页
        const isPublicRequest = url.includes('/user/recover_user') || url.includes('/user/send-recovercode')
        if (res.status === 401 && !isPublicRequest) {
            localStorage.removeItem('token')
            localStorage.removeItem('nickname')
            window.location.href = '/login'
            // 返回明确的失败对象（而不是 undefined），避免调用方访问 data.code 时抛 TypeError
            return { code: 401, mes: '未登录或登录已过期，请重新登录' }
        }

        const data = await res.json()

        // 第二层判断：响应体中的 code 为 401（兜底），同样排除恢复账号接口
        if (data.code === 401 && !isPublicRequest) {
            localStorage.removeItem('token')
            localStorage.removeItem('nickname')
            window.location.href = '/login'
        }

        return data
    } catch (error) {
        console.error('请求失败:', error)
        throw error
    }
}