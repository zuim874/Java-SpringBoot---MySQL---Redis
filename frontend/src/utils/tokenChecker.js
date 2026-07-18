// src/utils/tokenChecker.js
import { isTokenExpired } from "./token.js"

let checkInterval = null
let countdownTimer = null

/**
 * 启动定时检查
 * @param interval 检查间隔，默认30s
 * @param onExpired 过期回调
 */
export function startTokenCheck(interval = 30000, onExpired) {
    //清除旧的定时器
    stopTokenCheck()

    //执行一次检查
    checkToken(onExpired)

    //定时检查
    checkInterval = setInterval(() => {
        checkToken(onExpired)
    },interval)
}

/**
 * 停止检查
 */
export function stopTokenCheck() {
    if (checkInterval) {
        clearInterval(checkInterval)
        checkInterval = null
    }
    if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
    }
}

/**
 * 检查ToKen状态
 */
function checkToken(onExpired) {
    const token = localStorage.getItem('token')

    if (!token) {
        if (onExpired) onExpired('未登录')
        return
    }

    if (isTokenExpired(token)) {
        clearUserData()
        if (onExpired) onExpired('Token已过期')
        return
    }
}

export function clearUserData() {
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
}