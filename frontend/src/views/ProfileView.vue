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
            <div class="info-value">
              {{ nickname }}
              <button class="info-edit-btn" @click="openEditNickname" title="编辑昵称">✏️</button>
            </div>
          </div>
          <div class="info-divider"></div>
          <div class="info-row">
            <div class="info-label">邮箱</div>
            <div class="info-value">
              {{ email || '未绑定' }}
              <button class="info-edit-btn" @click="openChangeEmail" title="换绑邮箱">✏️</button>
            </div>
          </div>
        </div>

        <!-- 账号操作区 -->
        <div class="profile-actions">
          <h3 class="actions-title">账号操作</h3>
          <div class="action-btns">
            <button class="edit-profile-btn" @click="openEditNickname">
              <span class="edit-profile-btn-icon">✏️</span>
              <span>编辑昵称</span>
            </button>
            <button class="edit-profile-btn" @click="openChangeEmail">
              <span class="edit-profile-btn-icon">📧</span>
              <span>换绑邮箱</span>
            </button>
            <button class="edit-profile-btn" @click="openChangePassword">
              <span class="edit-profile-btn-icon">🔒</span>
              <span>修改密码</span>
            </button>
          </div>
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

    <!-- 确认注销弹窗 -->
    <Teleport to="body">
      <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
        <div class="modal-card">
          <div class="modal-icon">⚠️</div>
          <h3 class="modal-title">确认注销账号</h3>
          <p class="modal-desc">将向您的绑定邮箱发送验证码，输入验证码后确认注销</p>
          <div class="modal-field">
            <input v-model="deleteCode" type="text" placeholder="请输入邮箱验证码" maxlength="6" />
            <button class="modal-send-btn" :disabled="deleteCodeSending || deleteCountdown > 0" @click="sendDeleteCode">
              <span v-if="deleteCodeSending" class="btn-loading"></span>
              <span v-else-if="deleteCountdown > 0">{{ deleteCountdown }}s</span>
              <span v-else>发送验证码</span>
            </button>
          </div>
          <!-- 发送验证码/注销结果提示（显示在弹窗内） -->
          <p v-if="message" :class="['modal-msg', success ? 'modal-msg--success' : 'modal-msg--error']">{{ message }}</p>
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

    <!-- 编辑昵称弹窗 -->
    <Teleport to="body">
      <div v-if="showEditNickname" class="modal-overlay" @click.self="showEditNickname = false">
        <div class="modal-card">
          <div class="modal-icon">✏️</div>
          <h3 class="modal-title">编辑昵称</h3>
          <p class="modal-desc">修改后将在个人中心展示新昵称</p>
          <div class="modal-field">
            <input v-model="editNickname" type="text" placeholder="请输入新昵称（1-20个字符）" maxlength="20" />
          </div>
          <!-- 保存昵称结果提示 -->
          <p v-if="message" :class="['modal-msg', success ? 'modal-msg--success' : 'modal-msg--error']">{{ message }}</p>
          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showEditNickname = false">取消</button>
            <button class="modal-btn modal-btn--confirm modal-btn--blue" @click="submitEditNickname" :disabled="editingNickname">
              <span v-if="editingNickname" class="btn-loading"></span>
              <span v-else>保存</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 换绑邮箱弹窗 -->
    <Teleport to="body">
      <div v-if="showChangeEmail" class="modal-overlay" @click.self="showChangeEmail = false">
        <div class="modal-card modal-card--wide">
          <div class="modal-icon">📧</div>
          <h3 class="modal-title">换绑邮箱</h3>
          <p class="modal-desc">需分别验证旧邮箱与新邮箱，请先发送验证码</p>

          <div class="modal-form">
            <div class="modal-field-row">
              <label>旧邮箱</label>
              <div class="modal-field">
                <input v-model="oldEmail" type="email" placeholder="当前绑定邮箱" disabled />
              </div>
            </div>
            <div class="modal-field-row">
              <label>旧邮箱验证码</label>
              <div class="modal-field">
                <input v-model="oldEmailCode" type="text" placeholder="6位验证码" maxlength="6" />
                <button class="modal-send-btn" :disabled="oldCodeSending || oldCodeCountdown > 0" @click="sendOldEmailCode">
                  <span v-if="oldCodeSending" class="btn-loading"></span>
                  <span v-else-if="oldCodeCountdown > 0">{{ oldCodeCountdown }}s</span>
                  <span v-else>发送</span>
                </button>
              </div>
            </div>
            <div class="modal-field-row">
              <label>新邮箱</label>
              <div class="modal-field">
                <input v-model="newEmail" type="email" placeholder="请输入新邮箱" />
              </div>
            </div>
            <div class="modal-field-row">
              <label>新邮箱验证码</label>
              <div class="modal-field">
                <input v-model="newEmailCode" type="text" placeholder="6位验证码" maxlength="6" />
                <button class="modal-send-btn" :disabled="newCodeSending || newCodeCountdown > 0 || !newEmailValid" @click="sendNewEmailCode">
                  <span v-if="newCodeSending" class="btn-loading"></span>
                  <span v-else-if="newCodeCountdown > 0">{{ newCodeCountdown }}s</span>
                  <span v-else>发送</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 发送验证码/换绑结果提示（显示在弹窗内） -->
          <p v-if="message" :class="['modal-msg', success ? 'modal-msg--success' : 'modal-msg--error']">{{ message }}</p>

          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showChangeEmail = false">取消</button>
            <button class="modal-btn modal-btn--confirm modal-btn--blue" @click="submitChangeEmail" :disabled="changingEmail">
              <span v-if="changingEmail" class="btn-loading"></span>
              <span v-else>确认换绑</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 修改密码弹窗 -->
    <Teleport to="body">
      <div v-if="showChangePassword" class="modal-overlay" @click.self="showChangePassword = false">
        <div class="modal-card">
          <div class="modal-icon">🔒</div>
          <h3 class="modal-title">修改密码</h3>
          <p class="modal-desc">修改后请使用新密码重新登录</p>

          <div class="modal-form">
            <div class="modal-field-row">
              <label>旧密码</label>
              <div class="modal-field">
                <input v-model="oldPassword" :type="showOldPwd ? 'text' : 'password'" placeholder="请输入当前密码" />
                <span class="toggle-pwd" @click.stop="showOldPwd = !showOldPwd">{{ showOldPwd ? '🙈' : '👁️' }}</span>
              </div>
            </div>
            <div class="modal-field-row">
              <label>新密码</label>
              <div class="modal-field">
                <input v-model="newPassword" :type="showNewPwd ? 'text' : 'password'" placeholder="至少8位，含大小写字母/数字/符号" />
                <span class="toggle-pwd" @click.stop="showNewPwd = !showNewPwd">{{ showNewPwd ? '🙈' : '👁️' }}</span>
              </div>
            </div>
            <div class="modal-field-row">
              <label>确认新密码</label>
              <div class="modal-field">
                <input v-model="newPasswordCheck" :type="showNewPwdCheck ? 'text' : 'password'" placeholder="再次输入新密码" />
                <span class="toggle-pwd" @click.stop="showNewPwdCheck = !showNewPwdCheck">{{ showNewPwdCheck ? '🙈' : '👁️' }}</span>
              </div>
            </div>
          </div>

          <!-- 修改密码结果提示（显示在弹窗内） -->
          <p v-if="message" :class="['modal-msg', success ? 'modal-msg--success' : 'modal-msg--error']">{{ message }}</p>

          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showChangePassword = false">取消</button>
            <button class="modal-btn modal-btn--confirm modal-btn--blue" @click="submitChangePassword" :disabled="changingPassword">
              <span v-if="changingPassword" class="btn-loading"></span>
              <span v-else>确认修改</span>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
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

// 编辑昵称
const showEditNickname = ref(false)
const editNickname = ref('')
const editingNickname = ref(false)

// 换绑邮箱
const showChangeEmail = ref(false)
const oldEmail = ref('')
const oldEmailCode = ref('')
const newEmail = ref('')
const newEmailCode = ref('')
const oldCodeSending = ref(false)
const oldCodeCountdown = ref(0)
const newCodeSending = ref(false)
const newCodeCountdown = ref(0)
const changingEmail = ref(false)

// 修改密码
const showChangePassword = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const newPasswordCheck = ref('')
const showOldPwd = ref(false)
const showNewPwd = ref(false)
const showNewPwdCheck = ref(false)
const changingPassword = ref(false)

// 注销验证码
const deleteCode = ref('')
const deleteCodeSending = ref(false)
const deleteCountdown = ref(0)

// 倒计时定时器（统一清理）
let countdownTimer = null

const userInitial = computed(() => {
  return nickname.value ? nickname.value.charAt(0).toUpperCase() : '?'
})

// 新邮箱格式校验
const newEmailValid = computed(() => {
  const re = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  return re.test(newEmail.value)
})

// 页面加载时从后端拉取最新用户信息（含头像、邮箱）
onMounted(async () => {
  try {
    const data = await request('/user/me')
    if (data && data.code === 200) {
      const u = data.data
      if (u.username) username.value = u.username
      if (u.nickname) nickname.value = u.nickname
      if (u.avatar) avatar.value = u.avatar
      email.value = u.email || ''
      avatar.value = u.avatar || ''
      avatar.value = u.avatar || ''
    }
  } catch (err) {
    // 拉取失败时保留 localStorage 中的缓存信息
  }
})

// 组件卸载时清理倒计时定时器
onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
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

// ==================== 编辑昵称 ====================
function openEditNickname() {
  editNickname.value = nickname.value
  showEditNickname.value = true
  message.value = ''
}

async function submitEditNickname() {
  if (!editNickname.value || !editNickname.value.trim()) {
    message.value = '昵称不能为空'
    success.value = false
    return
  }
  const realNickname = editNickname.value.trim()
  if (realNickname.length > 20) {
    message.value = '昵称长度不能超过 20 个字符'
    success.value = false
    return
  }

  editingNickname.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('nickname', realNickname)
    const data = await request('/user/update_profile', {
      method: 'PUT',
      body: params
    })
    if (data && data.code === 200) {
      nickname.value = realNickname
      localStorage.setItem('nickname', realNickname)
      success.value = true
      message.value = '昵称更新成功'
      showEditNickname.value = false
    } else {
      success.value = false
      message.value = data.mes || '昵称更新失败'
    }
  } catch (err) {
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    editingNickname.value = false
  }
}

// ==================== 换绑邮箱 ====================
function openChangeEmail() {
  oldEmail.value = email.value || ''
  oldEmailCode.value = ''
  newEmail.value = ''
  newEmailCode.value = ''
  oldCodeCountdown.value = 0
  newCodeCountdown.value = 0
  showChangeEmail.value = true
  message.value = ''
}

function startCountdown(targetRef, seconds = 60) {
  targetRef.value = seconds
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    let done = true
    if (oldCodeCountdown.value > 0) oldCodeCountdown.value--
    if (newCodeCountdown.value > 0) newCodeCountdown.value--
    if (deleteCountdown.value > 0) deleteCountdown.value--
    if (oldCodeCountdown.value > 0 || newCodeCountdown.value > 0 || deleteCountdown.value > 0) done = false
    if (done) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// 发送旧邮箱验证码（type=3）
async function sendOldEmailCode() {
  if (!oldEmail.value) {
    message.value = '旧邮箱为空，无法发送验证码'
    success.value = false
    return
  }
  oldCodeSending.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('email', oldEmail.value)
    params.append('type', '3')
    const data = await request('/user/send-changeEmail', {
      method: 'POST',
      body: params
    })
    if (data && data.code === 200) {
      success.value = true
      message.value = data.mes || '旧邮箱验证码已发送'
      startCountdown(oldCodeCountdown)
    } else {
      success.value = false
      message.value = data.mes || '发送失败'
    }
  } catch (err) {
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    oldCodeSending.value = false
  }
}

// 发送新邮箱验证码（type=4）
async function sendNewEmailCode() {
  if (!newEmailValid.value) {
    message.value = '新邮箱格式不正确'
    success.value = false
    return
  }
  newCodeSending.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('email', newEmail.value)
    params.append('type', '4')
    const data = await request('/user/send-changeEmail', {
      method: 'POST',
      body: params
    })
    if (data && data.code === 200) {
      success.value = true
      message.value = data.mes || '新邮箱验证码已发送'
      startCountdown(newCodeCountdown)
    } else {
      success.value = false
      message.value = data.mes || '发送失败'
    }
  } catch (err) {
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    newCodeSending.value = false
  }
}

// 提交换绑邮箱
async function submitChangeEmail() {
  if (!newEmailValid.value) {
    message.value = '新邮箱格式不正确'
    success.value = false
    return
  }
  if (!oldEmailCode.value || !newEmailCode.value) {
    message.value = '请填写新旧邮箱验证码'
    success.value = false
    return
  }

  changingEmail.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('oldEmail', oldEmail.value)
    params.append('oldEmail_code', oldEmailCode.value)
    params.append('newEmail', newEmail.value)
    params.append('newEmail_code', newEmailCode.value)
    const data = await request('/user/change_email', {
      method: 'PUT',
      body: params
    })
    if (data && data.code === 200) {
      email.value = newEmail.value
      localStorage.setItem('email', newEmail.value)
      success.value = true
      message.value = '邮箱更换成功'
      showChangeEmail.value = false
    } else {
      success.value = false
      message.value = data.mes || '换绑失败'
    }
  } catch (err) {
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    changingEmail.value = false
  }
}

// ==================== 修改密码 ====================
function openChangePassword() {
  oldPassword.value = ''
  newPassword.value = ''
  newPasswordCheck.value = ''
  showOldPwd.value = false
  showNewPwd.value = false
  showNewPwdCheck.value = false
  showChangePassword.value = true
  message.value = ''
}

async function submitChangePassword() {
  if (!oldPassword.value) {
    message.value = '请输入旧密码'
    success.value = false
    return
  }
  if (!newPassword.value || newPassword.value.length < 8) {
    message.value = '新密码长度至少 8 位'
    success.value = false
    return
  }
  if (newPassword.value !== newPasswordCheck.value) {
    message.value = '两次输入的新密码不一致'
    success.value = false
    return
  }

  changingPassword.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('oldPassword', oldPassword.value)
    params.append('newPassword', newPassword.value)
    params.append('newPassword_check', newPasswordCheck.value)
    const data = await request('/user/change_password', {
      method: 'POST',
      body: params
    })
    if (data && data.code === 200) {
      success.value = true
      message.value = '密码修改成功，请重新登录'
      showChangePassword.value = false
      setTimeout(() => { handleLogout() }, 1200)
    } else {
      success.value = false
      message.value = data.mes || '密码修改失败'
    }
  } catch (err) {
    console.error('请求异常详情:', err)
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    changingPassword.value = false
  }
}

// ==================== 头像上传 ====================
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
    if (data && data.code === 200) {
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

// ==================== 注销账号 ====================
function confirmDelete() {
  deleteCode.value = ''
  showConfirm.value = true
  message.value = ''
}

// 发送注销验证码（type=2）
async function sendDeleteCode() {
  if (!email.value) {
    message.value = '邮箱为空，无法发送验证码'
    success.value = false
    return
  }
  deleteCodeSending.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('email', email.value)
    const data = await request('/user/send-deletecode', {
      method: 'POST',
      body: params
    })
    if (data && data.code === 200) {
      success.value = true
      message.value = data.mes || '注销验证码已发送'
      startCountdown(deleteCountdown)
    } else {
      success.value = false
      message.value = data.mes || '发送验证码失败'
    }
  } catch (err) {
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    deleteCodeSending.value = false
  }
}

async function handleDelete() {
  if (!deleteCode.value) {
    message.value = '请先获取并输入邮箱验证码'
    success.value = false
    return
  }
  deleting.value = true
  message.value = ''
  try {
    const params = new URLSearchParams()
    params.append('email', email.value || '')
    params.append('code', deleteCode.value)

    const data = await request('/user/delete_user', {
      method: 'DELETE',
      body: params
    })
    if (data && data.code === 200) {
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '网络错误，请检查后端服务是否启动'
  } finally {
    deleting.value = false
    // 仅注销成功时关闭弹窗；失败保留弹窗，让错误消息显示在弹窗内
    if (success.value) {
      showConfirm.value = false
    }
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
  display: flex;
  align-items: center;
  gap: 8px;
}
.info-edit-btn {
  border: none;
  background: transparent;
  font-size: 0.85rem;
  cursor: pointer;
  opacity: 0.45;
  transition: all 0.3s;
  padding: 2px;
  line-height: 1;
}
.info-edit-btn:hover {
  opacity: 1;
  transform: scale(1.15);
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
.action-btns {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.edit-profile-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid rgba(43,108,176,0.20);
  border-radius: 12px;
  background: rgba(43,108,176,0.04);
  color: #2b6cb0;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.edit-profile-btn:hover {
  background: rgba(43,108,176,0.10);
  border-color: rgba(43,108,176,0.35);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(43,108,176,0.12);
}
.edit-profile-btn-icon {
  font-size: 0.9rem;
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

/* ===== 弹窗内消息提示 ===== */
.modal-msg {
  margin: 0 0 16px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 0.8rem;
  text-align: center;
  animation: fadeIn 0.3s ease;
}
.modal-msg--success {
  color: #2b8a3e;
  background: rgba(43,138,62,0.08);
  border: 1px solid rgba(43,138,62,0.12);
}
.modal-msg--error {
  color: #c92a2a;
  background: rgba(201,42,42,0.08);
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
.modal-card--wide {
  width: 440px;
}
/* ===== 弹窗表单 ===== */
.modal-form {
  text-align: left;
  margin-bottom: 20px;
}
.modal-field-row {
  margin-bottom: 14px;
}
.modal-field-row label {
  display: block;
  font-size: 0.78rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}
.modal-field {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1px solid #dee2e6;
  border-radius: 10px;
  background: rgba(255,255,255,0.60);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.modal-field:focus-within {
  border-color: #4a9eff;
  box-shadow: 0 0 0 3px rgba(74,158,255,0.12);
  background: white;
}
.modal-field input {
  flex: 1;
  border: none;
  background: transparent;
  padding: 12px 0;
  font-size: 0.88rem;
  font-family: 'DM Sans', sans-serif;
  color: #212529;
  outline: none;
  min-width: 0;
}
.modal-field input::placeholder {
  color: #adb5bd;
  font-weight: 400;
}
.modal-field input:disabled {
  background: rgba(0,0,0,0.02);
  color: #868e96;
}
.modal-send-btn {
  flex-shrink: 0;
  height: 34px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
  color: white;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
}
.modal-send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(43,108,176,0.30);
}
.modal-send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.toggle-pwd {
  flex-shrink: 0;
  font-size: 0.9rem;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.3s;
  padding: 4px;
}
.toggle-pwd:hover {
  opacity: 1;
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
.modal-btn--blue {
  background: linear-gradient(135deg, #2b6cb0, #4a9eff);
  color: white;
}
.modal-btn--blue:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(43,108,176,0.25);
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