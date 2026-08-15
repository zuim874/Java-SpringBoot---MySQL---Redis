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
      <div class="avatar-wrapper" @click.stop>
        <span class="nav-balance" v-if="userBalance !== null">余额 ¥{{ Number(userBalance).toFixed(2) }}</span>
        <span class="nav-nickname">{{ nickname }}</span>
        <button class="avatar-btn" @click="profileMenuOpen = !profileMenuOpen" aria-label="用户菜单">
          <span class="avatar-circle">
            <img :src="getCachedAvatar(avatarUrl)" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
          </span>
          <span class="avatar-caret" :class="{ open: profileMenuOpen }">▾</span>
        </button>
        <transition name="profile">
          <div class="profile-menu" v-if="profileMenuOpen">
            <div class="profile-menu-header">
              <span class="profile-menu-avatar">
                <img :src="getCachedAvatar(avatarUrl)" alt="头像" @error="avatarUrl = DEFAULT_AVATAR">
              </span>
              <div class="profile-menu-id">
                <p class="profile-menu-name">{{ nickname }}</p>
                <p class="profile-menu-role">{{ roleLabel }}</p>
              </div>
            </div>
            <button class="profile-menu-item" @click="profileMenuOpen = false; goBackToShop()">返回商城</button>
            <button class="profile-menu-item" @click="profileMenuOpen = false; goOrders()">我的订单</button>
            <button class="profile-menu-item" @click="profileMenuOpen = false; goProfile()">个人中心</button>
            <button class="profile-menu-item" @click="profileMenuOpen = false; goCoupons()">我的优惠券</button>
            <button class="profile-menu-item" v-if="isAdminUser" @click="profileMenuOpen = false; goAdmin()">管理后台</button>
            <button class="profile-menu-item" v-if="isSellerUser" @click="profileMenuOpen = false; goSeller()">商家管理</button>
            <div class="profile-menu-divider"></div>
            <button class="profile-menu-item profile-menu-item--logout" @click="handleLogout">退出登录</button>
          </div>
        </transition>
      </div>
    </nav>

    <!-- 主内容 -->
    <main class="profile-main">
      <!-- 会员横幅 -->
      <section class="member-banner">
        <div class="member-banner__deco member-banner__deco--1"></div>
        <div class="member-banner__deco member-banner__deco--2"></div>
        <div class="member-avatar" :class="{ 'member-avatar--editable': !uploading }" @click="triggerFileSelect" title="点击更换头像">
          <img v-if="avatar" :src="getCachedAvatar(avatar)" class="member-avatar__img" alt="头像">
          <span v-else class="member-avatar__text">{{ userInitial }}</span>
          <span class="member-avatar__edit" v-if="!uploading">✏️</span>
          <span class="member-avatar__loading" v-if="uploading"></span>
        </div>
        <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden-file-input" @change="handleFileChange">
        <div class="member-info">
          <h1 class="member-nickname">
            {{ nickname }}
            <span v-if="isVipUser" class="vip-chip">★ 会员</span>
            <span v-else class="role-tag">{{ roleLabel }}</span>
          </h1>
          <p class="member-username">@{{ username }}</p>
          <p class="member-email">{{ email || '未绑定邮箱' }}</p>
        </div>
        <div class="member-balance">
          <span class="member-balance__label">账户余额</span>
          <span class="member-balance__amount">¥{{ (balance || 0).toFixed(2) }}</span>
          <button class="member-balance__btn" @click="openRecharge">申请充值</button>
        </div>
      </section>

      <!-- 订单中心 -->
      <section class="stats-card card">
        <div class="stats-card__head">
          <h2 class="section-title">订单中心</h2>
          <button class="section-link" @click="router.push('/orders')">全部订单 ›</button>
        </div>
        <div class="stats-grid">
          <button class="stat-item" @click="router.push({ path: '/orders', query: { status: '0' } })">
            <span class="stat-item__icon">💰</span>
            <span class="stat-item__label">待付款</span>
          </button>
          <button class="stat-item" @click="router.push({ path: '/orders', query: { status: '1' } })">
            <span class="stat-item__icon">📦</span>
            <span class="stat-item__label">待发货</span>
          </button>
          <button class="stat-item" @click="router.push({ path: '/orders', query: { status: '2' } })">
            <span class="stat-item__icon">🚚</span>
            <span class="stat-item__label">待收货</span>
          </button>
          <button class="stat-item" @click="router.push('/orders')">
            <span class="stat-item__icon">🧾</span>
            <span class="stat-item__label">全部订单</span>
          </button>
        </div>
      </section>

      <!-- 常用功能宫格 -->
      <section class="menu-card card">
        <div class="section-head">
          <h2 class="section-title">常用功能</h2>
        </div>
        <div class="menu-grid">
          <button class="menu-item" @click="router.push('/orders')">
            <span class="menu-item__icon">📦</span>
            <span class="menu-item__text">我的订单</span>
          </button>
          <button class="menu-item" @click="goCoupons">
            <span class="menu-item__icon">🎟️</span>
            <span class="menu-item__text">我的优惠券</span>
          </button>
          <button class="menu-item" @click="openAddressManager">
            <span class="menu-item__icon">📍</span>
            <span class="menu-item__text">收货地址</span>
          </button>
          <button class="menu-item" @click="openRecharge">
            <span class="menu-item__icon">💰</span>
            <span class="menu-item__text">申请充值</span>
          </button>
          <button class="menu-item" @click="openEditNickname">
            <span class="menu-item__icon">✏️</span>
            <span class="menu-item__text">修改昵称</span>
          </button>
          <button class="menu-item" @click="openChangeEmail">
            <span class="menu-item__icon">📧</span>
            <span class="menu-item__text">换绑邮箱</span>
          </button>
          <button class="menu-item" @click="openChangePassword">
            <span class="menu-item__icon">🔒</span>
            <span class="menu-item__text">修改密码</span>
          </button>
          <button class="menu-item" @click="goBackToShop">
            <span class="menu-item__icon">🏪</span>
            <span class="menu-item__text">返回商城</span>
          </button>
        </div>
      </section>

      <!-- 余额充值 -->
      <section class="recharge-card card">
        <div class="section-head">
          <h2 class="section-title">余额充值</h2>
          <span class="section-sub">提交申请后由管理员审核到账</span>
        </div>
        <div class="recharge-panel">
          <div class="recharge-input">
            <span class="recharge-input__prefix">¥</span>
            <input v-model="rechargeAmount" type="number" min="0.01" step="0.01" placeholder="请输入充值金额，最低 0.01 元" />
          </div>
          <button class="btn btn--primary recharge-btn" @click="handleSubmitRecharge" :disabled="recharging">
            <span v-if="recharging" class="btn-loading"></span>
            <span v-else>申请充值</span>
          </button>
        </div>
        <p v-if="message" :class="['msg', success ? 'msg--success' : 'msg--error']">{{ message }}</p>

        <div class="recharge-records" v-if="rechargeRecords.length > 0">
          <h3 class="records-title">充值记录</h3>
          <div class="records-list">
            <div v-for="record in rechargeRecords" :key="record.id" class="record-item">
              <div class="record-info">
                <span class="record-amount">¥{{ Number(record.amount || 0).toFixed(2) }}</span>
                <span :class="['record-status', 'record-status--' + record.status]">
                  {{ record.status === 0 ? '待审核' : record.status === 1 ? '已通过' : '已拒绝' }}
                </span>
              </div>
              <div class="record-time">
                <span>{{ record.createTime }}</span>
                <span v-if="record.remark" class="record-remark">{{ record.remark }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 账号安全 -->
      <section class="security-card card">
        <div class="section-head">
          <h2 class="section-title">账号安全</h2>
        </div>
        <div class="security-list">
          <div class="security-row">
            <span class="security-row__label">用户名</span>
            <span class="security-row__value">{{ username }}</span>
          </div>
          <div class="security-row">
            <span class="security-row__label">昵称</span>
            <span class="security-row__value">
              {{ nickname }}
              <button class="link-btn" @click="openEditNickname">修改</button>
            </span>
          </div>
          <div class="security-row">
            <span class="security-row__label">邮箱</span>
            <span class="security-row__value">
              {{ email || '未绑定' }}
              <button class="link-btn" @click="openChangeEmail">换绑</button>
            </span>
          </div>
          <div class="security-row">
            <span class="security-row__label">登录密码</span>
            <span class="security-row__value">
              ••••••••
              <button class="link-btn" @click="openChangePassword">修改</button>
            </span>
          </div>
        </div>
        <div class="security-actions">
          <button class="btn btn--ghost" @click="handleLogout">
            <span class="btn-text-icon">🚪</span>退出登录
          </button>
          <button class="btn btn--ghost btn--danger-ghost" @click="confirmDelete">
            <span class="btn-text-icon">⚠️</span>注销账号
          </button>
        </div>
        <p class="actions-desc">注销后账号将被停用，可通过恢复功能重新激活</p>
      </section>
    </main>

    <!-- 底部版权 -->
    <footer class="profile-footer">
      <span>© 2026 ZuiMShop. All rights reserved.</span>
    </footer>

    <!-- 注销账号弹窗 -->
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

    <!-- 申请充值弹窗 -->
    <Teleport to="body">
      <div v-if="showRecharge" class="modal-overlay" @click.self="showRecharge = false">
        <div class="modal-card">
          <div class="modal-icon">💰</div>
          <h3 class="modal-title">申请充值</h3>
          <p class="modal-desc">充值金额将直接添加到您的账户余额</p>
          <div class="modal-form">
            <div class="modal-field-row">
              <label>当前余额</label>
              <div class="modal-field">
                <input type="text" :value="'¥' + (balance || 0).toFixed(2)" disabled />
              </div>
            </div>
            <div class="modal-field-row">
              <label>充值金额（元）</label>
              <div class="modal-field">
                <input v-model="rechargeAmount" type="number" min="0.01" step="0.01" placeholder="请输入充值金额，最低 0.01 元" />
              </div>
            </div>
          </div>
          <p v-if="message" :class="['modal-msg', success ? 'modal-msg--success' : 'modal-msg--error']">{{ message }}</p>
          <div class="modal-actions">
            <button class="modal-btn modal-btn--cancel" @click="showRecharge = false">取消</button>
            <button class="modal-btn modal-btn--confirm modal-btn--blue" @click="handleSubmitRecharge" :disabled="recharging">
              <span v-if="recharging" class="btn-loading"></span>
              <span v-else>确认充值</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 地址管理弹窗 -->
    <Teleport to="body">
      <div v-if="showAddressManager" class="modal-overlay" @click.self="closeAddressManager">
        <div class="address-manager-modal">
          <div class="modal-header">
            <div class="modal-icon">📍</div>
            <h3 class="modal-title">收货地址管理</h3>
            <button class="modal-close" @click="closeAddressManager">✕</button>
          </div>

          <!-- 地址列表 -->
          <div class="address-list" v-if="addresses.length > 0">
            <div v-for="addr in addresses" :key="addr.id" class="address-card"
                 :class="{ 'address-card--default': addr.isDefault === 1 }">
              <div class="address-card-header">
                <div class="address-card-info">
                  <span class="address-card-name">{{ addr.receiverName }}</span>
                  <span class="address-card-phone">{{ addr.receiverPhone }}</span>
                  <span class="address-card-tag" v-if="addr.isDefault === 1">默认</span>
                </div>
                <div class="address-card-actions">
                  <button class="addr-btn addr-btn--edit" @click.stop="editAddress(addr)">编辑</button>
                  <button class="addr-btn addr-btn--default" @click.stop="setAddrDefault(addr.id)"
                          :disabled="addr.isDefault === 1">设为默认</button>
                  <button class="addr-btn addr-btn--delete" @click.stop="deleteAddr(addr.id)">删除</button>
                </div>
              </div>
              <p class="address-card-detail">{{ addr.receiverAddress }}</p>
            </div>
          </div>

          <div class="empty-state" v-if="addresses.length === 0 && !loadingAddresses">
            <span>暂无保存的地址</span>
          </div>

          <!-- 添加/编辑地址 -->
          <div class="address-form-section">
            <h4 class="section-title">{{ editingAddress ? '编辑地址' : '新增地址' }}</h4>
            <div class="address-form-fields">
              <div class="form-field">
                <label>收货人姓名</label>
                <input v-model="formAddr.receiverName" placeholder="请输入收货人姓名" />
              </div>
              <div class="form-field">
                <label>收货人电话</label>
                <input v-model="formAddr.receiverPhone" placeholder="请输入联系电话" />
              </div>
              <div class="form-field form-field--full">
                <label>详细地址</label>
                <input v-model="formAddr.receiverAddress" placeholder="如：XX省XX市XX区XX路XX号" />
              </div>
              <label class="default-check">
                <input type="checkbox" v-model="formAddr.isDefault" />
                <span>设为默认收货地址</span>
              </label>
            </div>
            <p v-if="addressMsg" :class="['msg', addressMsgSuccess ? 'msg--success' : 'msg--error']">{{ addressMsg }}</p>
            <div class="form-actions">
              <button class="form-btn form-btn--cancel" @click="clearForm">清空</button>
              <button class="form-btn form-btn--confirm" @click="saveAddress" :disabled="saving">
                <span v-if="saving" class="btn-loading"></span>
                <span v-else>{{ editingAddress ? '保存修改' : '添加地址' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../utils/request'
import { getUserInfo, submitRecharge, getRechargeRecords, getUserAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../api/index.js'
import { getCachedAvatar, DEFAULT_AVATAR } from '../utils/avatarCache.js'

const router = useRouter()

const nickname = ref(localStorage.getItem('nickname') || '')
const username = ref(localStorage.getItem('username') || '')
const email = ref(localStorage.getItem('email') || '')
const avatar = ref('')
const balance = ref(0)
const isVipUser = ref(false)
const message = ref('')
const success = ref(false)
const showConfirm = ref(false)
const deleting = ref(false)
const uploading = ref(false)
const fileInput = ref(null)

// 导航栏头像下拉菜单
const profileMenuOpen = ref(false)
const avatarUrl = ref(DEFAULT_AVATAR)
const userBalance = ref(null)

function getCachedRoles() {
  const roles = localStorage.getItem('roles')
  return roles ? JSON.parse(roles) : []
}
const cachedRoles = getCachedRoles()
const isAdminUser = computed(() => cachedRoles.includes('ROLE_ADMIN'))
const isSellerUser = computed(() => cachedRoles.includes('ROLE_SELLER'))
const roleLabel = computed(() => {
  if (isAdminUser.value) return '管理员'
  if (isSellerUser.value) return '商家'
  return '普通用户'
})

async function fetchUserProfile() {
  try {
    const res = await getUserInfo()
    if (res && res.code === 200 && res.data) {
      const u = res.data
      if (u.avatar) avatarUrl.value = u.avatar
      if (u.nickname) nickname.value = u.nickname
      if (u.balance !== undefined && u.balance !== null) {
        userBalance.value = Number(u.balance)
      }
    }
  } catch (e) {
    console.error('获取用户资料失败:', e)
  }
}

function closePopups() {
  profileMenuOpen.value = false
}

function goProfile() {
  router.push('/profile')
}

function goHome() { router.push('/home') }
function goOrders() { router.push('/orders') }
function goAdmin() { router.push('/admin/dashboard') }
function goSeller() { router.push('/seller/dashboard') }

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

// 地址管理
const showAddressManager = ref(false)
const addresses = ref([])
const loadingAddresses = ref(false)
const editingAddress = ref(null)
const saving = ref(false)
const addressMsg = ref('')
const addressMsgSuccess = ref(false)
const formAddr = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  isDefault: 0
})

// 申请充值
const showRecharge = ref(false)
const rechargeAmount = ref('')
const recharging = ref(false)
const rechargeRecords = ref([])

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
  document.addEventListener('click', closePopups)
  try {
    const data = await request('/user/me')
    if (data && data.code === 200) {
      const u = data.data
      if (u.username) username.value = u.username
      if (u.nickname) nickname.value = u.nickname
      if (u.avatar) avatar.value = u.avatar
      email.value = u.email || ''
      balance.value = u.balance || 0
      avatar.value = u.avatar || ''
      // 会员买家标识
      const role = u.userRole || ''
      isVipUser.value = role.includes('ROLE_VIP_USER')
    }
  } catch (err) {
    // 拉取失败时保留 localStorage 中的缓存信息
  }
  // 加载充值记录
  try {
    const records = await getRechargeRecords()
    if (records && records.code === 200) {
      rechargeRecords.value = records.data || []
    }
  } catch {
    // 充值记录加载失败不影响页面
  }
  fetchUserProfile()
})

// 组件卸载时清理倒计时定时器
onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})

onUnmounted(() => {
  document.removeEventListener('click', closePopups)
})

function goBackToShop() {
  router.push('/home')
}
function goCoupons() {
  router.push('/coupons')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}

// ==================== 申请充值 ====================
function openRecharge() {
  rechargeAmount.value = ''
  showRecharge.value = true
  message.value = ''
}

async function handleSubmitRecharge() {
  const amount = parseFloat(rechargeAmount.value)
  if (!amount || amount <= 0) {
    message.value = '请输入有效的充值金额'
    success.value = false
    return
  }
  if (amount > 100000) {
    message.value = '单次充值金额不能超过 100,000 元'
    success.value = false
    return
  }

  recharging.value = true
  message.value = ''
  try {
    const data = await submitRecharge(amount)
    if (data && data.code === 200) {
      success.value = true
      message.value = '充值申请已提交，请等待管理员审核'
      // 刷新充值记录
      try {
        const records = await getRechargeRecords()
        if (records && records.code === 200) {
          rechargeRecords.value = records.data || []
        }
      } catch {
        // ignore
      }
      setTimeout(() => { showRecharge.value = false }, 1500)
    } else {
      success.value = false
      message.value = data && data.mes ? data.mes : '提交失败，请稍后重试'
    }
  } catch (err) {
    success.value = false
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
  } finally {
    recharging.value = false
  }
}

// ==================== 地址管理 ====================
async function openAddressManager() {
  showAddressManager.value = true
  addressMsg.value = ''
  clearForm()
  loadingAddresses.value = true
  try {
    const res = await getUserAddresses()
    if (res && res.code === 200) {
      addresses.value = res.data || []
    }
  } catch {
    addresses.value = []
  } finally {
    loadingAddresses.value = false
  }
}

function closeAddressManager() {
  showAddressManager.value = false
}

function clearForm() {
  editingAddress.value = null
  formAddr.value = { receiverName: '', receiverPhone: '', receiverAddress: '', isDefault: 0 }
  addressMsg.value = ''
}

function editAddress(addr) {
  editingAddress.value = addr
  formAddr.value = {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    receiverAddress: addr.receiverAddress,
    isDefault: addr.isDefault === 1
  }
  addressMsg.value = ''
}

async function saveAddress() {
  const { receiverName, receiverPhone, receiverAddress, isDefault } = formAddr.value
  if (!receiverName || !receiverName.trim()) {
    addressMsg.value = '请输入收货人姓名'
    addressMsgSuccess.value = false
    return
  }
  if (!receiverPhone || !receiverPhone.trim()) {
    addressMsg.value = '请输入收货人电话'
    addressMsgSuccess.value = false
    return
  }
  if (!receiverAddress || !receiverAddress.trim()) {
    addressMsg.value = '请输入收货地址'
    addressMsgSuccess.value = false
    return
  }

  saving.value = true
  addressMsg.value = ''
  try {
    const payload = {
      receiverName: receiverName.trim(),
      receiverPhone: receiverPhone.trim(),
      receiverAddress: receiverAddress.trim(),
      isDefault: isDefault ? 1 : 0
    }

    let res
    if (editingAddress.value) {
      payload.id = editingAddress.value.id
      res = await updateAddress(payload)
    } else {
      res = await addAddress(payload)
    }

    if (res && res.code === 200) {
      addressMsg.value = editingAddress.value ? '地址更新成功' : '地址添加成功'
      addressMsgSuccess.value = true
      clearForm()
      // 刷新列表
      const refresh = await getUserAddresses()
      if (refresh && refresh.code === 200) {
        addresses.value = refresh.data || []
      }
    } else {
      addressMsg.value = (res && res.mes) || '操作失败'
      addressMsgSuccess.value = false
    }
  } catch (err) {
    addressMsg.value = (err && err.message) || '网络错误'
    addressMsgSuccess.value = false
  } finally {
    saving.value = false
  }
}

async function setAddrDefault(id) {
  try {
    const res = await setDefaultAddress(id)
    if (res && res.code === 200) {
      const refresh = await getUserAddresses()
      if (refresh && refresh.code === 200) {
        addresses.value = refresh.data || []
      }
    }
  } catch {
    // ignore
  }
}

async function deleteAddr(id) {
  if (!confirm('确定要删除该地址吗？')) return
  try {
    const res = await deleteAddress(id)
    if (res && res.code === 200) {
      addresses.value = addresses.value.filter(a => a.id !== id)
    }
  } catch {
    // ignore
  }
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
    message.value = (err && err.message) ? ('请求异常：' + err.message) : '服务连接失败，请稍后重试'
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
  background: var(--bg);
  font-family: var(--font-sans);
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
  filter: blur(90px);
  opacity: 0.18;
}
.bg-circle--1 {
  width: 560px; height: 560px;
  top: -180px; right: -160px;
  background: radial-gradient(circle, var(--primary), transparent 70%);
}
.bg-circle--2 {
  width: 480px; height: 480px;
  bottom: -160px; left: -160px;
  background: radial-gradient(circle, var(--accent), transparent 70%);
}
.bg-circle--3 {
  width: 320px; height: 320px;
  top: 42%; left: 12%;
  background: radial-gradient(circle, var(--primary), transparent 70%);
}

/* ===== 导航 ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: rgba(255,255,255,0.82);
  backdrop-filter: blur(18px) saturate(1.6);
  -webkit-backdrop-filter: blur(18px) saturate(1.6);
  border-bottom: 1px solid var(--border);
  z-index: 100;
}
.nav-logo {
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: 700;
  background: linear-gradient(120deg, var(--primary-2), var(--primary) 55%, var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}
.nav-nickname { font-size: 0.85rem; font-weight: 600; color: var(--text-2); padding: 0 4px; }

/* ===== 用户头像与下拉菜单 ===== */
.avatar-wrapper { position: relative; display: flex; align-items: center; gap: 6px; }
.nav-balance {
  padding: 5px 12px; border-radius: var(--radius-pill);
  background: var(--primary-soft); border: 1px solid rgba(47,84,235,0.18);
  color: var(--primary); font-size: 0.75rem; font-weight: 600;
  white-space: nowrap;
}
.avatar-btn { display: flex; align-items: center; gap: 4px; background: none; border: none; cursor: pointer; padding: 2px; transition: transform 0.3s; }
.avatar-btn:hover { transform: scale(1.05); }
.avatar-circle {
  width: 38px; height: 38px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-2), var(--primary), var(--accent));
  color: #fff; font-size: 1rem; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm); overflow: hidden;
}
.avatar-circle img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-caret { font-size: 0.7rem; color: var(--text-4); transition: transform 0.3s; }
.avatar-caret.open { transform: rotate(180deg); }
.profile-menu {
  position: absolute; top: calc(100% + 12px); right: 0; min-width: 184px; padding: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
}
.profile-menu-header { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border-bottom: 1px solid var(--border); margin-bottom: 6px; }
.profile-menu-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-2), var(--primary), var(--accent));
  color: #fff; font-size: 1.05rem; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0; overflow: hidden;
}
.profile-menu-avatar img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.profile-menu-id { min-width: 0; }
.profile-menu-name { font-size: 0.9rem; font-weight: 600; color: var(--text); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-menu-role { font-size: 0.72rem; color: var(--text-3); margin: 2px 0 0; }
.profile-menu-item {
  display: block; width: 100%; text-align: left; padding: 10px 14px; border: none;
  border-radius: var(--radius-sm); background: transparent; cursor: pointer;
  font-family: var(--font-sans); font-size: 0.85rem; color: var(--text-2); transition: all 0.2s;
}
.profile-menu-item:hover { background: var(--primary-soft); color: var(--primary); }
.profile-menu-item--logout:hover { background: var(--danger-soft); color: var(--danger); }
.profile-menu-divider { height: 1px; background: var(--border); margin: 6px 0; }
.profile-enter-active, .profile-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.profile-enter-from, .profile-leave-to { opacity: 0; transform: translateY(-6px); }

/* ===== 主内容 ===== */
.profile-main {
  width: 100%;
  max-width: 860px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 96px 20px 40px;
  position: relative;
  z-index: 1;
}

/* ===== 会员横幅 ===== */
.member-banner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 28px 32px;
  border-radius: var(--radius-lg);
  background: linear-gradient(120deg, var(--primary-2) 0%, var(--primary) 55%, #7c5cff 100%);
  box-shadow: var(--shadow-primary);
  color: #fff;
  overflow: hidden;
  animation: cardIn 0.5s ease both;
}
.member-banner__deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255,255,255,0.12);
}
.member-banner__deco--1 { width: 220px; height: 220px; top: -80px; right: -40px; }
.member-banner__deco--2 { width: 120px; height: 120px; bottom: -50px; left: 28%; }
.member-avatar {
  position: relative;
  width: 76px; height: 76px;
  flex-shrink: 0;
  border-radius: 50%;
  background: rgba(255,255,255,0.22);
  border: 3px solid rgba(255,255,255,0.75);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  overflow: visible;
  transition: transform 0.3s ease;
}
.member-avatar--editable:hover { transform: scale(1.06); }
.member-avatar__img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; display: block; }
.member-avatar__text {
  font-family: var(--font-display);
  font-size: 2rem; font-weight: 700; color: #fff;
}
.member-avatar__edit {
  position: absolute; right: -4px; bottom: -4px;
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--surface);
  color: var(--primary);
  font-size: 0.7rem;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
}
.member-avatar__loading {
  position: absolute; inset: 0; margin: auto;
  width: 26px; height: 26px;
  border: 3px solid rgba(255,255,255,0.6);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
.hidden-file-input { display: none; }
.member-info { flex: 1; min-width: 0; }
.member-nickname {
  font-family: var(--font-display);
  font-size: 1.5rem; font-weight: 700; color: #fff;
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  margin-bottom: 6px;
}
.vip-chip {
  padding: 3px 12px; border-radius: var(--radius-pill);
  background: linear-gradient(120deg, #ffd76a, #ff9f43);
  color: #7a3b00; font-size: 0.72rem; font-weight: 700;
  font-family: var(--font-sans);
}
.role-tag {
  padding: 3px 12px; border-radius: var(--radius-pill);
  background: rgba(255,255,255,0.2); color: #fff;
  font-size: 0.72rem; font-weight: 600; font-family: var(--font-sans);
}
.member-username { font-size: 0.85rem; color: rgba(255,255,255,0.85); margin: 0 0 2px; }
.member-email { font-size: 0.78rem; color: rgba(255,255,255,0.7); margin: 0; }
.member-balance {
  flex-shrink: 0;
  text-align: right;
  display: flex; flex-direction: column; align-items: flex-end; gap: 4px;
}
.member-balance__label { font-size: 0.72rem; color: rgba(255,255,255,0.8); letter-spacing: 0.02em; }
.member-balance__amount { font-size: 1.7rem; font-weight: 700; font-family: var(--font-sans); line-height: 1.1; }
.member-balance__btn {
  margin-top: 6px;
  padding: 7px 18px;
  border: 1px solid rgba(255,255,255,0.65);
  border-radius: var(--radius-pill);
  background: rgba(255,255,255,0.16);
  color: #fff;
  font-family: var(--font-sans); font-size: 0.78rem; font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}
.member-balance__btn:hover { background: #fff; color: var(--primary); transform: translateY(-1px); }

/* ===== 通用卡片区块 ===== */
.section-title {
  font-family: var(--font-display);
  font-size: 1.1rem; font-weight: 700; color: var(--text);
  margin: 0;
}
.section-head {
  display: flex; align-items: baseline; justify-content: space-between; gap: 12px;
  margin-bottom: 18px;
}
.section-sub { font-size: 0.78rem; color: var(--text-3); }
.stats-card, .menu-card, .recharge-card, .security-card { padding: 24px 26px; animation: cardIn 0.5s ease both; }
.stats-card__head {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  margin-bottom: 18px;
}
.section-link {
  border: none; background: none; padding: 0;
  font-family: var(--font-sans); font-size: 0.8rem; font-weight: 600; color: var(--link);
  cursor: pointer; transition: color 0.2s;
}
.section-link:hover { color: var(--primary-2); }

/* ===== 订单中心 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.stat-item {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 18px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-2);
  cursor: pointer;
  transition: all 0.25s ease;
}
.stat-item:hover {
  border-color: var(--primary);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}
.stat-item__icon { font-size: 1.5rem; line-height: 1; }
.stat-item__label { font-size: 0.8rem; font-weight: 600; color: var(--text-2); }

/* ===== 常用功能宫格 ===== */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.menu-item {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 18px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface-2);
  cursor: pointer;
  font-family: var(--font-sans);
  transition: all 0.25s ease;
}
.menu-item:hover {
  border-color: var(--primary);
  background: var(--surface);
  box-shadow: var(--shadow-sm);
  transform: translateY(-2px);
}
.menu-item__icon { font-size: 1.5rem; line-height: 1; }
.menu-item__text { font-size: 0.8rem; font-weight: 600; color: var(--text-2); }

/* ===== 余额充值 ===== */
.recharge-panel { display: flex; gap: 12px; margin-bottom: 14px; }
.recharge-input {
  flex: 1;
  display: flex; align-items: center; gap: 8px;
  padding: 0 16px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  transition: all 0.25s;
}
.recharge-input:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(47,84,235,0.12);
}
.recharge-input__prefix { font-size: 1.1rem; font-weight: 700; color: var(--primary); }
.recharge-input input {
  flex: 1; min-width: 0;
  border: none; background: transparent; outline: none;
  padding: 13px 0; font-size: 1rem; font-family: var(--font-sans); color: var(--text);
}
.recharge-input input::placeholder { color: var(--text-4); font-weight: 400; }
.recharge-btn {
  flex-shrink: 0;
  min-width: 132px;
  border-radius: var(--radius);
  background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%);
}
.recharge-btn:disabled { opacity: 0.55; cursor: not-allowed; }

/* ===== 消息提示 ===== */
.msg {
  margin: 6px 0 14px;
  text-align: center;
  font-size: 0.82rem;
  padding: 9px 16px;
  border-radius: var(--radius-sm);
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.msg--success { color: var(--success); background: var(--success-soft); border: 1px solid rgba(47,158,68,0.18); }
.msg--error { color: var(--danger); background: var(--danger-soft); border: 1px solid rgba(224,49,49,0.18); }

/* ===== 充值记录 ===== */
.recharge-records {
  margin-top: 6px;
  padding-top: 18px;
  border-top: 1px dashed var(--border);
}
.records-title { font-size: 0.95rem; font-weight: 700; color: var(--text); margin: 0 0 12px; }
.records-list { display: flex; flex-direction: column; gap: 8px; }
.record-item {
  padding: 12px 16px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: border-color 0.2s;
}
.record-item:hover { border-color: var(--border-strong); }
.record-info { display: flex; align-items: center; gap: 12px; margin-bottom: 4px; }
.record-amount { font-size: 1.05rem; font-weight: 700; color: var(--price); }
.record-status { padding: 2px 10px; border-radius: var(--radius-pill); font-size: 0.72rem; font-weight: 600; }
.record-status--0 { background: var(--warning-soft); color: var(--warning); }
.record-status--1 { background: var(--success-soft); color: var(--success); }
.record-status--2 { background: var(--danger-soft); color: var(--danger); }
.record-time { display: flex; align-items: center; gap: 12px; font-size: 0.75rem; color: var(--text-3); }
.record-remark { color: var(--danger); }

/* ===== 账号安全 ===== */
.security-list { display: flex; flex-direction: column; margin-bottom: 18px; }
.security-row {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px solid var(--border);
}
.security-row:last-child { border-bottom: none; }
.security-row__label { font-size: 0.8rem; font-weight: 600; color: var(--text-3); letter-spacing: 0.02em; }
.security-row__value {
  font-size: 0.9rem; font-weight: 500; color: var(--text);
  display: flex; align-items: center; gap: 10px;
}
.link-btn {
  border: none; background: none; padding: 2px 6px;
  font-family: var(--font-sans); font-size: 0.76rem; font-weight: 600; color: var(--link);
  cursor: pointer; border-radius: var(--radius-sm);
  transition: all 0.2s;
}
.link-btn:hover { background: var(--primary-soft); }
.security-actions {
  display: flex; gap: 12px;
  padding-top: 4px;
}
.btn-text-icon { font-size: 0.9rem; line-height: 1; }
.btn--danger-ghost {
  background: var(--surface);
  color: var(--danger);
  border-color: rgba(224,49,49,0.35);
}
.btn--danger-ghost:hover:not(:disabled) {
  background: var(--danger-soft);
  border-color: var(--danger);
  color: var(--danger);
  transform: translateY(-1px);
  box-shadow: none;
}
.actions-desc { margin-top: 14px; font-size: 0.75rem; color: var(--text-4); text-align: center; }

/* ===== 底部 ===== */
.profile-footer { position: relative; z-index: 1; padding: 8px 0 28px; text-align: center; }
.profile-footer span { font-size: 0.72rem; color: var(--text-4); letter-spacing: 0.02em; }

/* ===== 弹窗通用 ===== */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(23,35,61,0.42);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 200;
  padding: 20px;
  animation: overlayIn 0.2s ease;
}
@keyframes overlayIn { from { opacity: 0; } to { opacity: 1; } }
.modal-card {
  width: 400px; max-width: 100%;
  padding: 34px 32px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  text-align: center;
  max-height: 90vh; overflow-y: auto;
  animation: modalIn 0.3s ease;
}
.modal-card--wide { width: 480px; }
@keyframes modalIn { from { opacity: 0; transform: translateY(16px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
.modal-icon { font-size: 2.3rem; margin-bottom: 12px; }
.modal-title { font-family: var(--font-display); font-size: 1.25rem; font-weight: 700; color: var(--text); margin-bottom: 8px; }
.modal-desc { font-size: 0.85rem; color: var(--text-3); margin-bottom: 24px; line-height: 1.5; }

/* ===== 弹窗表单 ===== */
.modal-form { text-align: left; margin-bottom: 20px; }
.modal-field-row { margin-bottom: 14px; }
.modal-field-row label {
  display: block; font-size: 0.78rem; font-weight: 600; color: var(--text-2);
  margin-bottom: 6px; letter-spacing: 0.02em;
}
.modal-field {
  display: flex; align-items: center; gap: 10px;
  padding: 0 14px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--surface);
  transition: all 0.25s;
}
.modal-field:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(47,84,235,0.12); }
.modal-field input {
  flex: 1; min-width: 0;
  border: none; background: transparent; outline: none;
  padding: 12px 0; font-size: 0.88rem; font-family: var(--font-sans); color: var(--text);
}
.modal-field input::placeholder { color: var(--text-4); font-weight: 400; }
.modal-field input:disabled { color: var(--text-4); background: transparent; }
.modal-send-btn {
  flex-shrink: 0; height: 34px; padding: 0 12px;
  border: none; border-radius: var(--radius-sm);
  background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%);
  color: #fff;
  font-family: var(--font-sans); font-size: 0.75rem; font-weight: 600;
  white-space: nowrap; cursor: pointer;
  display: flex; align-items: center; justify-content: center; min-width: 84px;
  transition: all 0.25s;
}
.modal-send-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-primary); }
.modal-send-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.toggle-pwd { flex-shrink: 0; font-size: 0.9rem; cursor: pointer; opacity: 0.6; transition: opacity 0.3s; padding: 4px; }
.toggle-pwd:hover { opacity: 1; }

.modal-msg { margin: 0 0 16px; padding: 8px 12px; border-radius: var(--radius-sm); font-size: 0.8rem; text-align: center; animation: fadeIn 0.3s ease; }
.modal-msg--success { color: var(--success); background: var(--success-soft); border: 1px solid rgba(47,158,68,0.18); }
.modal-msg--error { color: var(--danger); background: var(--danger-soft); border: 1px solid rgba(224,49,49,0.18); }

.modal-actions { display: flex; gap: 12px; }
.modal-btn {
  flex: 1; padding: 12px;
  border: none; border-radius: var(--radius-sm);
  font-family: var(--font-sans); font-size: 0.85rem; font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}
.modal-btn--cancel { background: var(--surface-3); color: var(--text-2); }
.modal-btn--cancel:hover { background: var(--border); }
.modal-btn--confirm {
  background: linear-gradient(120deg, var(--danger) 0%, #f03e3e 100%);
  color: #fff;
}
.modal-btn--confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.modal-btn--confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(224,49,49,0.3); }
.modal-btn--blue {
  background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%);
  color: #fff;
}
.modal-btn--blue:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-primary); }

.btn-loading {
  display: inline-block; width: 16px; height: 16px;
  border: 2px solid rgba(255,255,255,0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  vertical-align: middle;
}
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes cardIn { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: translateY(0); } }

/* ===== 地址管理弹窗 ===== */
.address-manager-modal {
  width: 600px; max-width: 100%; max-height: 82vh; overflow-y: auto;
  background: var(--surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  padding: 0;
  animation: modalIn 0.3s ease;
}
.address-manager-modal .modal-header {
  display: flex; align-items: center; gap: 12px;
  padding: 20px 24px; border-bottom: 1px solid var(--border);
  position: sticky; top: 0; background: var(--surface); z-index: 1;
}
.address-manager-modal .modal-icon { font-size: 1.4rem; margin: 0; }
.address-manager-modal .modal-title { flex: 1; font-size: 1.15rem; margin: 0; text-align: left; }
.address-manager-modal .modal-close {
  background: none; border: none; font-size: 1.2rem; color: var(--text-3);
  cursor: pointer; padding: 4px; transition: color 0.2s;
}
.address-manager-modal .modal-close:hover { color: var(--text); }
.address-list { padding: 16px 24px 8px; }
.address-card {
  padding: 14px 16px; border: 1px solid var(--border); border-radius: var(--radius);
  margin-bottom: 10px; background: var(--surface); transition: all 0.2s;
}
.address-card:hover { border-color: var(--primary); }
.address-card--default { border-color: var(--primary); background: var(--primary-softer); }
.address-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; gap: 12px; }
.address-card-info { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.address-card-name { font-size: 0.9rem; font-weight: 600; color: var(--text); }
.address-card-phone { font-size: 0.8rem; color: var(--text-3); }
.address-card-tag {
  padding: 2px 8px; border-radius: var(--radius-pill);
  background: var(--primary-soft); color: var(--primary);
  font-size: 0.68rem; font-weight: 600;
}
.address-card-detail { font-size: 0.82rem; color: var(--text-2); margin: 0; }
.address-card-actions { display: flex; gap: 6px; flex-shrink: 0; }
.addr-btn {
  padding: 4px 10px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface); font-size: 0.7rem; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.addr-btn:hover { transform: translateY(-1px); }
.addr-btn--edit { color: var(--primary); border-color: rgba(47,84,235,0.25); }
.addr-btn--edit:hover { border-color: var(--primary); background: var(--primary-soft); }
.addr-btn--default { color: #7c5cff; border-color: rgba(124,92,255,0.25); }
.addr-btn--default:hover { border-color: #7c5cff; background: rgba(124,92,255,0.08); }
.addr-btn--default:disabled { opacity: 0.4; cursor: not-allowed; transform: none; }
.addr-btn--delete { color: var(--danger); border-color: rgba(224,49,49,0.25); }
.addr-btn--delete:hover { border-color: var(--danger); background: var(--danger-soft); }
.address-form-section { padding: 16px 24px 24px; border-top: 1px solid var(--border); }
.address-form-section .section-title { font-size: 0.92rem; font-weight: 700; color: var(--text-2); margin: 0 0 12px; }
.address-form-fields { display: flex; flex-direction: column; gap: 10px; }
.address-form-fields .form-field { display: flex; flex-direction: column; gap: 4px; }
.address-form-fields label { font-size: 0.78rem; color: var(--text-3); font-weight: 600; }
.address-form-fields input {
  padding: 10px 12px; border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  font-size: 0.85rem; font-family: var(--font-sans); color: var(--text);
  outline: none; transition: border-color 0.2s;
}
.address-form-fields input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(47,84,235,0.08); }
.default-check {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  font-size: 0.8rem; color: var(--text-2); margin-top: 8px;
}
.default-check input[type="checkbox"] { accent-color: var(--primary); }
.form-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.form-btn {
  padding: 10px 24px; border: none; border-radius: var(--radius-sm);
  font-family: var(--font-sans); font-size: 0.85rem; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
}
.form-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.form-btn--cancel { border: 1px solid var(--border-strong); background: transparent; color: var(--text-2); }
.form-btn--cancel:hover { border-color: var(--primary); color: var(--primary); }
.form-btn--confirm { background: linear-gradient(120deg, var(--primary) 0%, var(--primary-2) 100%); color: #fff; }
.form-btn--confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: var(--shadow-primary); }
.address-manager-modal .empty-state { text-align: center; padding: 32px; color: var(--text-3); font-size: 0.9rem; }

@media (max-width: 720px) {
  .profile-main { padding: 84px 14px 32px; }
  .member-banner { flex-wrap: wrap; padding: 22px 20px; }
  .member-balance { text-align: left; align-items: flex-start; width: 100%; }
  .stats-grid, .menu-grid { grid-template-columns: repeat(2, 1fr); }
  .recharge-panel { flex-direction: column; }
  .recharge-btn { width: 100%; }
  .security-actions { flex-direction: column; }
  .security-actions .btn { width: 100%; }
  .address-card-header { flex-direction: column; align-items: flex-start; }
  .address-card-actions { width: 100%; justify-content: flex-start; flex-wrap: wrap; }
  .form-actions { flex-direction: column; }
  .form-btn { width: 100%; text-align: center; }
  .nav { padding: 0 16px; }
  .nav-nickname { display: none; }
}
</style>
