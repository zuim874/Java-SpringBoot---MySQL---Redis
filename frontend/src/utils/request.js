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

    // 如果有 Token，添加到请求头
    // 请求头新增字段Authorization，后端JWT鉴权标准约定头
    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`
    }

    // 合并配置（如有重复，后者覆盖前者）
    // ...对象：将所有键值对展开   {...xxx, ...xxx}:合并对象
    const mergedOptions = { ...defaultOptions, ...options }

    // 如果是表单格式（URLSearchParams），修改 Content-Type
    if (options.body instanceof URLSearchParams) {
        mergedOptions.headers['Content-Type'] = 'application/x-www-form-urlencoded'
    }

    try {
        // 发送请求
        // fetch：浏览器原生发送网络请求的API
        const res = await fetch(BASE_URL + url, mergedOptions)

        // 第一层判断：HTTP 状态码为 400 时直接跳转
        if (res.status === 400) {
            localStorage.removeItem('token')
            localStorage.removeItem('nickname')
            window.location.href = '/login'
            return
        }

        const data = await res.json()

        // 第二层判断：响应体中的 code（兜底）
        if (data.code === 400) {
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