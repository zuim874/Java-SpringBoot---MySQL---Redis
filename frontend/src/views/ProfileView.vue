<template>
  <div class="profile-page">
    <!-- 背景装饰 -->
    <div class="bg-shapes">
      <div class="bg-circle bg-circle--1"></div>
      <div class="bg-circle bg-circle--2"></div>
      <div class="bg-circle bg-circle--3"></div>
    </div>

    <!-- 导航条 -->
    <nav class="nav">
      <div class="nav-logo">ZuiMShop</div>
      <div class="nav-actions">
        <button class="nav-btn" @click="goBackToShop">
          <span class="nav-btn-icon">🏪</span>
          <span class="nav-btn-text">返回商城</span>
        </button>
        <button class="nav-btn nav-btn--outline" @click="handleLogout">
          <span class="nav-btn-icon">🚪</span>
          <span class="nav-btn-text">退出登录</span>
        </button>
      </div>
    </nav>

    <!-- 用户信息卡片 -->
    <div class="profile-wrapper">
      <div class="profile-card">
        <div class="profile-header">
          <div class="profile-avatar" :class="{ 'profile-avatar--editable': !uploading }" @click="triggerFileSelect" title="点击更换头像">
            <img v-if="avatar" :src="avatar" class="avatar-img" alt="头像">
            <span v-else class="avatar-text">{{ userInitial }}</span>
            <span class="avatar-edit-hint" v-if="!uploading">✏️</span>
            <span class="avatar-loading" v-if="uploading"></span>
          </div>
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden-file-input" @change="handleFileChange">
          <h2 class="profile-title">个人中心</h2>
          <p class="profile-desc">管理您的账号信息</p>
        </div>

        <div class="profile-info">
          <div class="info-row">
            <div class="info-label">用户名</div>
            <div class="info-value">{{ username }}</div>
          </div>
          <div class="info-divider"></div>
          <div class="info-row">
            <div class="info-label">昵称</div>
            <div class="info-value">{{ nickname }}</div>
          </div>
          <div class="info-divider"></div>
          <div class="info-row">
            <div class="info-label">邮箱</div>
            <div class="info-value">{{ email || '未绑定' }}</div>
          </div>
        </div>

        <!-- 账号操作区 -->
        <div class="profile-actions">
          <h3 class="actions-title">账号操作</h3>
          <button class="delete-btn" @click="confirmDelete">
            <span class="delete-btn-icon">⚠️</span>
            <span>注销账号</span>
          </button>
          <p class="actions-desc">注销后账号将被停用，可通过恢复功能重新激活</p>
        </div>

        <p v-if="message" :class="['msg', success ? 'msg--success' : 'msg--error']">
          {{ message }}
        </p>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <Teleport to="body">
      <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
        <div class="modal-card">
          <div class="modal-icon">⚠️</div>
          <h3 class="modal-title">确认注销账号</h3>
          <p class="modal-desc">此操作将停用您的账号，确定要注销吗？</p>
          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showConfirm = false">取消</button>
            <button class="modal-btn modal-btn--confirm" @click="handleDelete" :disabled="deleting">
              <span v-if="deleting" class="btn-loading"></span>
              <span v-else>确认注销</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 底部版权 -->
    <div class="profile-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'

const router = useRouter()

const nickname = ref(localStorage.getItem('nickname') || '')
const username = ref(localStorage.getItem('username') || '')
const email = ref(localStorage.getItem('email') || '')
const avatar = ref('')
const message = ref('')
const success = ref(false)
const showConfirm = ref(false)
const deleting = ref(false)
const uploading = ref(false)
const fileInput = ref(null)

const userInitial = computed(() => {
  return nickname.value ? nickname.value.charAt(0).toUpperCase() : '?'
})

// 页面加载时从后端拉取最新用户信息（含头像、邮箱）
onMounted(async () => {
  try {
    const data = await request('/user/me')
    if (data.code === 200) {
      const u = data.data
      if (u.username) username.value = u.username
      if (u.nickname) nickname.value = u.nickname
      email.value = u.email || ''
      avatar.value = u.avatar || ''
    }
  } catch (err) {
    // 拉取失败时保留 localStorage 中的缓存信息
  }
})

function goBackToShop() {
  router.push('/home')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}

// 点击头像触发文件选择
function triggerFileSelect() {
  if (!uploading.value) {
    fileInput.value?.click()
  }
}

// 选择文件后上传头像
async function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  // 前端预校验：类型
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    message.value = '仅支持 jpg/png/gif/webp 格式的图片'
    success.value = false
    event.target.value = ''
    return
  }
  // 前端预校验：大小（与后端 2MB 限制保持一致）
  if (file.size > 2 * 1024 * 1024) {
    message.value = '图片大小不能超过 2MB'
    success.value = false
    event.target.value = ''
    return
  }

  uploading.value = true
  message.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)

    const data = await request('/user/avatar', {
      method: 'POST',
      body: formData
    })
    if (data.code === 200) {
      avatar.value = data.data
      success.value = true
      message.value = '头像更新成功'
    } else {
      success.value = false
      message.value = data.mes || '头像上传失败'
    }
  } catch (err) {
    success.value = false
    message.value = '头像上传失败，请检查后端服务是否启动'
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

function confirmDelete() {
  showConfirm.value = true
}

async function handleDelete() {
  deleting.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('email', email.value || '')
    params.append('code', '000000')

    const data = await request('/user/delete_user', {
      method: 'DELETE',
      body: params
    })
    if (data.code === 200) {
      success.value = true
      message.value = data.mes || '账号注销成功'
      localStorage.removeItem('token')
      localStorage.removeItem('nickname')
      setTimeout(() => { router.push('/login') }, 1500)
    } else {
      success.value = false
      message.value = data.mes || '注销失败'
    }
  } catch (err) {
    success.value = false
    message.value = '网络错误，请检查后端服务是否启动'
  } finally {
    deleting.value = false
    showConfirm.value = false
  }
}
</script>

<style scoped>
/* ===== 全局 ===== */
.profile-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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
  align-items: center;
  gap: 12px;
}
.nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.nav-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(43,108,176,0.25);
}
.nav-btn--outline {
  background: transparent;
  color: #495057;
  border: 1px solid #dee2e6;
}
.nav-btn--outline:hover {
  background: rgba(201,42,42,0.06);
  border-color: #c92a2a;
  color: #c92a2a;
  box-shadow: none;
}
.nav-btn-icon {
  font-size: 0.9rem;
}
.nav-btn-text {
  line-height: 1;
}

/* ===== 个人中心卡片 ===== */
.profile-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 100px 24px 60px;
  position: relative;
  z-index: 1;
}
.profile-card {
  width: 420px;
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
.profile-header {
  text-align: center;
  margin-bottom: 32px;
}
.profile-avatar {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s ease-in-out infinite;
}
.avatar-text {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.8rem;
  font-weight: 700;
  color: white;
}
/* ===== 头像上传 ===== */
.profile-avatar {
  position: relative;
  cursor: pointer;
  overflow: visible;
}
.profile-avatar--editable:hover {
  transform: scale(1.05);
  transition: transform 0.3s ease;
}
.avatar-img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}
.avatar-edit-hint {
  position: absolute;
  right: -6px;
  bottom: -6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
  color: white;
  font-size: 0.7rem;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(43,108,176,0.35);
  border: 2px solid white;
}
.avatar-loading {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 26px;
  height: 26px;
  border: 3px solid rgba(255,255,255,0.6);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
.hidden-file-input {
  display: none;
}
@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.05); opacity: 1; }
}
.profile-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.8rem;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}
.profile-desc {
  font-size: 0.9rem;
  color: #868e96;
}

/* ===== 用户信息 ===== */
.profile-info {
  margin-bottom: 32px;
  background: rgba(255,255,255,0.40);
  border-radius: 16px;
  padding: 8px 0;
  border: 1px solid rgba(255,255,255,0.30);
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
}
.info-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #868e96;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}
.info-value {
  font-size: 0.9rem;
  font-weight: 500;
  color: #212529;
  font-family: 'DM Sans', sans-serif;
}
.info-divider {
  height: 1px;
  background: rgba(0,0,0,0.04);
  margin: 0 20px;
}

/* ===== 账号操作区 ===== */
.profile-actions {
  text-align: center;
  padding-top: 8px;
}
.actions-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1rem;
  font-weight: 700;
  color: #212529;
  margin-bottom: 16px;
}
.delete-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border: 1px solid rgba(201,42,42,0.20);
  border-radius: 12px;
  background: rgba(201,42,42,0.04);
  color: #c92a2a;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.delete-btn:hover {
  background: rgba(201,42,42,0.10);
  border-color: rgba(201,42,42,0.35);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(201,42,42,0.12);
}
.delete-btn-icon {
  font-size: 1rem;
}
.actions-desc {
  margin-top: 12px;
  font-size: 0.75rem;
  color: #adb5bd;
  line-height: 1.5;
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

/* ===== 确认弹窗 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.35);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  animation: overlayIn 0.2s ease;
}
@keyframes overlayIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.modal-card {
  width: 360px;
  max-width: 90%;
  padding: 36px 32px;
  background: rgba(255,255,255,0.90);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-radius: 20px;
  border: 1px solid rgba(255,255,255,0.50);
  box-shadow: 0 20px 60px rgba(0,0,0,0.12);
  text-align: center;
  animation: modalIn 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
@keyframes modalIn {
  from { opacity: 0; transform: translateY(16px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.modal-icon {
  font-size: 2.4rem;
  margin-bottom: 12px;
}
.modal-title {
  font-family: 'Playfair Display', Georgia, serif;
  font-size: 1.3rem;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}
.modal-desc {
  font-size: 0.85rem;
  color: #868e96;
  margin-bottom: 24px;
  line-height: 1.5;
}
.modal-actions {
  display: flex;
  gap: 12px;
}
.modal-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 10px;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.modal-btn--cancel {
  background: #f1f3f5;
  color: #495057;
}
.modal-btn--cancel:hover {
  background: #e9ecef;
}
.modal-btn--confirm {
  background: linear-gradient(135deg, #c92a2a, #e03131);
  color: white;
}
.modal-btn--confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(201,42,42,0.25);
}
.modal-btn--confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-loading {
  display: inline-block;
  width: 18px; height: 18px;
  border: 2px solid rgba(255,255,255,0.30);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  vertical-align: middle;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== 底部 ===== */
.profile-footer {
  position: relative;
  z-index: 1;
  padding: 24px 56px;
  text-align: center;
}
.profile-footer span {
  font-size: 0.7rem;
  color: #adb5bd;
  letter-spacing: 0.02em;
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .nav { padding: 0 24px; }
  .nav-btn-text { display: none; }
  .nav-btn { padding: 8px 12px; }
  .profile-card { padding: 36px 24px; }
}
</style>