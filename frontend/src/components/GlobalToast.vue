<template>
  <Teleport to="body">
    <div class="global-toast-container">
      <transition-group name="toast">
        <div v-for="t in toastState.toasts" :key="t.id"
             class="global-toast" :class="'global-toast--' + t.type">
          <span class="toast-icon" v-html="iconFor(t.type)"></span>
          <span class="toast-message">{{ t.message }}</span>
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<script setup>
import { toastState } from '../utils/toast.js'

function iconFor(type) {
  if (type === 'success') return '&#10003;'
  if (type === 'error') return '&#10005;'
  return '&#8505;'
}
</script>

<style scoped>
/* ===== 全局 Toast 容器（固定右上角） ===== */
.global-toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
  max-width: 360px;
}

/* ===== 单条 Toast（毛玻璃 + 渐变描边，NebulaTech 风格） ===== */
.global-toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px) saturate(1.6);
  -webkit-backdrop-filter: blur(20px) saturate(1.6);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.7);
  font-family: 'DM Sans', -apple-system, sans-serif;
  font-size: 0.88rem;
  font-weight: 500;
  color: #212529;
  pointer-events: auto;
  animation: toastIn 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.toast-icon {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
}

.toast-message {
  line-height: 1.4;
  word-break: break-word;
}

/* ===== 类型配色 ===== */
.global-toast--success .toast-icon {
  background: linear-gradient(135deg, #2b8a3e, #51cf66);
}
.global-toast--error .toast-icon {
  background: linear-gradient(135deg, #e03131, #ff6b6b);
}
.global-toast--info .toast-icon {
  background: linear-gradient(135deg, #2b6cb0, #7c3aed);
}
.global-toast--success {
  border-left: 4px solid #2b8a3e;
}
.global-toast--error {
  border-left: 4px solid #e03131;
}
.global-toast--info {
  border-left: 4px solid #4a9eff;
}

/* ===== 动画 ===== */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

@keyframes toastIn {
  from { opacity: 0; transform: translateX(40px) scale(0.96); }
  to { opacity: 1; transform: translateX(0) scale(1); }
}

@media (max-width: 480px) {
  .global-toast-container {
    top: 16px;
    right: 16px;
    left: 16px;
    max-width: none;
  }
}
</style>
