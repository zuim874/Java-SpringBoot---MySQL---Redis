/**
 * 全局 Toast 提示工具
 * 提供统一的、美观的通知提示（成功/错误/信息）
 * 配合 components/GlobalToast.vue 使用
 */
import { reactive } from 'vue'

// 全局 Toast 状态（单例）
export const toastState = reactive({
  toasts: []
})

let seq = 0

/**
 * 弹出提示
 * @param {string} message 提示内容
 * @param {'success'|'error'|'info'} type 类型（默认 info）
 * @param {number} duration 显示时长（毫秒，默认 2600）
 */
export function showToast(message, type = 'info', duration = 2600) {
  const id = ++seq
  toastState.toasts.push({ id, message, type })
  // 自动消失
  setTimeout(() => {
    const idx = toastState.toasts.findIndex(t => t.id === id)
    if (idx !== -1) {
      toastState.toasts.splice(idx, 1)
    }
  }, duration)
}

/** 成功提示 */
export function toastSuccess(message) {
  showToast(message, 'success')
}

/** 错误提示 */
export function toastError(message) {
  showToast(message, 'error', 3200)
}

/** 普通提示 */
export function toastInfo(message) {
  showToast(message, 'info')
}

export default { showToast, toastSuccess, toastError, toastInfo }
