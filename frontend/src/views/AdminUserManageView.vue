<template>
  <div class="admin-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
    <nav class="nav">
      <div class="nav-logo">XuWenYeShop</div>
      <div class="nav-actions">
        <button class="nav-btn" @click="goHome">返回商城</button>
        <button class="nav-btn nav-btn--logout" @click="handleLogout">退出</button>
      </div>
    </nav>

    <!-- 主内容 -->
    <div class="admin-wrapper">
      <div class="admin-card">
        <div class="admin-header">
          <div class="admin-icon">⚙</div>
          <h2 class="admin-title">用户管理</h2>
          <p class="admin-desc">管理员用户管理与恢复操作</p>
        </div>

        <!-- 管理员权限码 -->
        <div class="section">
          <div class="form-group">
            <label>管理员权限码</label>
            <div class="input-wrap">
              <span class="input-icon">🔑</span>
              <input
                v-model="adminCode"
                type="password"
                placeholder="请输入管理员确认码"
              />
            </div>
          </div>
        </div>

        <!-- 用户列表 -->
        <div class="section">
          <h3 class="section-title">当前用户信息</h3>
          <div class="table-wrap">
            <table class="user-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>昵称</th>
                  <th>邮箱</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{{ currentUser.id || '-' }}</td>
                  <td>{{ currentUser.username || '-' }}</td>
                  <td>{{ currentUser.nickname || '-' }}</td>
                  <td>{{ currentUser.email || '-' }}</td>
                  <td>
                    <span :class="['status-badge', currentUser.status === 1 ? 'status--active' : 'status--disabled']">
                      {{ currentUser.status === 1 ? '正常' : '已删除' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 操作区域 -->
        <div class="section">
          <h3 class="section-title">删除 / 恢复用户</h3>
          <div class="operation-row">
            <div class="form-group operation-input">
              <label>目标用户 ID</label>
              <div class="input-wrap">
                <span class="input-icon">#</span>
                <input
                  v-model="targetUserId"
                  type="number"
                  placeholder="请输入用户ID"
                />
              </div>
            </div>
            <div class="operation-btns">
              <button class="op-btn op-btn--delete" :disabled="loading" @click="handleDeleteUser">删除用户</button>
              <button class="op-btn op-btn--recover" :disabled="loading" @click="handleRecoverUser">恢复用户</button>
            </div>
          </div>
        </div>

        <!-- 消息提示 -->
        <p v-if="message" :class="['msg', msgSuccess ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="admin-footer">
      <span>© 2026 XuWenYeShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'
import { parseJWT } from '../utils/token'

const router = useRouter()

const adminCode = ref('')
const targetUserId = ref('')
const loading = ref(false)
const message = ref('')
const msgSuccess = ref(false)
const currentUser = ref({})

onMounted(() => {
  loadCurrentUser()
})

function loadCurrentUser() {
  const token = localStorage.getItem('token')
  if (token) {
    const payload = parseJWT(token)
    if (payload) {
      currentUser.value = {
        id: payload.id || payload.userId || '',
        username: payload.sub || payload.username || '',
        nickname: localStorage.getItem('nickname') || '',
        email: payload.email || '',
        status: payload.status !== undefined ? payload.status : 1
      }
    }
  }
}

function goHome() {
  router.push('/home')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}

async function handleDeleteUser() {
  if (!validateInput()) return
  loading.value = true
  message.value = ''
  try {
    const data = await request('/user/delete_admin', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: Number(targetUserId.value),
        adminCode: adminCode.value
      })
    })
    if (data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '用户已删除'
    } else {
      msgSuccess.value = false
      message.value = data.mes || '删除失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}

async function handleRecoverUser() {
  if (!validateInput()) return
  loading.value = true
  message.value = ''
  try {
    const data = await request('/user/recover_admin', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: Number(targetUserId.value),
        adminCode: adminCode.value
      })
    })
    if (data.code === 200) {
      msgSuccess.value = true
      message.value = data.mes || '用户已恢复'
    } else {
      msgSuccess.value = false
      message.value = data.mes || '恢复失败'
    }
  } catch (err) {
    msgSuccess.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    loading.value = false
  }
}

function validateInput() {
  if (!adminCode.value.trim()) {
    message.value = '请输入管理员权限码'
    msgSuccess.value = false
    return false
  }
  if (!targetUserId.value) {
    message.value = '请输入目标用户 ID'
    msgSuccess.value = false
    return false
  }
  return true
}
</script>

<style scoped>
/* ===== 全局 ===== */
.admin-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 50%, #f1f3f5 100%);
  font-family: 'DM Sans', -apple-system, sans-serif;
  position: relative;
  overflow: hidden;
  -webkit-font-smoothing: antialiased;
}

/* ===== 背景装饰 ===== */
.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
}
.bg-circle--1 {
  width: 600px; height: 600px;
  top: -200px; right: -200px;
  background: radial-gradient(circle, #4a9eff, #2b6cb0);
}
.bg-circle--2 {
  width: 500px; height: 500px;
  bottom: -150px; left: -150px;
  background: radial-gradient(circle, #7c3aed, #5b21b6);
}
.bg-circle--3 {
  width: 300px; height: 300px;
  top: 40%; left: 10%;
  background: radial-gradient(circle, #4a9eff, transparent);
}

/* ===== 导航 ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 56px;
  background: rgba(255,255,255,0.60);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid rgba(255,255,255,0.30);
  z-index: 100;
}
.nav-logo {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.4rem;
  font-weight: 700;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.03em;
}
.nav-actions {
  display: flex;
  gap: 12px;
}
.nav-btn {
  padding: 8px 20px;
  border: 1px solid rgba(43,108,176,0.20);
  border-radius: 10px;
  background: rgba(255,255,255,0.60);
  font-family: 'DM Sans', sans-serif;
  font-size: 0.8rem;
  font-weight: 600;
  color: #2b6cb0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav-btn:hover {
  background: #2b6cb0;
  color: white;
  border-color: #2b6cb0;
  transform: translateY(-1px);
}
.nav-btn--logout {
  color: #c92a2a;
  border-color: rgba(201,42,42,0.20);
}
.nav-btn--logout:hover {
  background: #c92a2a;
  color: white;
  border-color: #c92a2a;
}

/* ===== 主卡片 ===== */
.admin-wrapper {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 100%;
  padding: 100px 24px 60px;
  position: relative;
  z-index: 1;
}
.admin-card {
  width: 700px;
  max-width: 100%;
  padding: 48px 40px;
  background: rgba(255,255,255,0.75);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 24px;
  border: 1px solid rgba(255,255,255,0.50);
  box-shadow:
    0 4px 24px rgba(0,0,0,0.04),
    0 20px 60px rgba(0,0,0,0.06),
    inset 0 1px 0 rgba(255,255,255,0.60);
  animation: cardIn 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(24px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== 卡片头部 ===== */
.admin-header {
  text-align: center;
  margin-bottom: 36px;
}
.admin-icon {
  font-size: 2rem;
  margin-bottom: 12px;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { transform: scale(1) rotate(0deg); opacity: 0.6; }
  50% { transform: scale(1.1) rotate(15deg); opacity: 1; }
}
.admin-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.8rem;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}
.admin-desc {
  font-size: 0.9rem;
  color: #868e96;
}

/* ===== 分区 ===== */
.section {
  margin-bottom: 28px;
}
.section-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.1rem;
  font-weight: 700;
  color: #343a40;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}

/* ===== 表单 ===== */
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}
.input-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border: 1px solid #dee2e6;
  border-radius: 12px;
  background: rgba(255,255,255,0.60);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.input-wrap:focus-within {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.12);
  background: white;
}
.input-icon {
  font-size: 1rem;
  opacity: 0.5;
}
.input-wrap input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 14px 0;
  font-size: 0.9rem;
  font-family: 'DM Sans', sans-serif;
  color: #212529;
  outline: none;
}
.input-wrap input::placeholder {
  color: #adb5bd;
  font-weight: 400;
}
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
input[type="number"] {
  -moz-appearance: textfield;
}

/* ===== 表格 ===== */
.table-wrap {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,0.05);
}
.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
.user-table th {
  text-align: left;
  padding: 12px 16px;
  background: rgba(43,108,176,0.04);
  color: #495057;
  font-weight: 600;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}
.user-table td {
  padding: 14px 16px;
  color: #212529;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.user-table tr:last-child td {
  border-bottom: none;
}
.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status--active {
  background: rgba(43,138,62,0.08);
  color: #2b8a3e;
}
.status--disabled {
  background: rgba(201,42,42,0.08);
  color: #c92a2a;
}

/* ===== 操作区域 ===== */
.operation-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
}
.operation-input {
  flex: 1;
  min-width: 200px;
  margin-bottom: 0;
}
.operation-btns {
  display: flex;
  gap: 12px;
  padding-bottom: 4px;
}
.op-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 12px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  white-space: nowrap;
}
.op-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.op-btn--delete {
  background: linear-gradient(135deg, #c92a2a, #e03131);
  color: white;
}
.op-btn--delete:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(201,42,42,0.30);
}
.op-btn--recover {
  background: linear-gradient(135deg, #2b8a3e, #40c057);
  color: white;
}
.op-btn--recover:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(43,138,62,0.30);
}

/* ===== 消息提示 ===== */
.msg {
  margin-top: 16px;
  text-align: center;
  font-size: 0.85rem;
  padding: 10px 16px;
  border-radius: 10px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success {
  color: #2b8a3e;
  background: rgba(43,138,62,0.06);
  border: 1px solid rgba(43,138,62,0.12);
}
.msg--error {
  color: #c92a2a;
  background: rgba(201,42,42,0.06);
  border: 1px solid rgba(201,42,42,0.12);
}

/* ===== 底部 ===== */
.admin-footer {
  position: relative;
  z-index: 1;
  padding: 24px 56px;
  text-align: center;
}
.admin-footer span {
  font-size: 0.7rem;
  color: #adb5bd;
  letter-spacing: 0.02em;
}

/* ===== 响应式 ===== */
@media (max-width: 640px) {
  .nav { padding: 0 24px; }
  .nav-actions { gap: 8px; }
  .nav-btn { padding: 6px 14px; font-size: 0.75rem; }
  .admin-card { padding: 36px 24px; }
  .operation-row { flex-direction: column; }
  .operation-input { min-width: 100%; }
  .operation-btns { width: 100%; }
  .op-btn { flex: 1; text-align: center; }
}
</style>