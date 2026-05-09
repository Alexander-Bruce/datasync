<template>
  <div class="auth-root">
    <!-- Animated background grid -->
    <div class="grid-bg"></div>

    <!-- Floating particles -->
    <div
      class="particle"
      style="left: 15%; top: 20%; animation-delay: 0s; width: 3px; height: 3px"
    ></div>
    <div
      class="particle"
      style="left: 70%; top: 60%; animation-delay: 1.5s; width: 2px; height: 2px"
    ></div>
    <div
      class="particle"
      style="left: 40%; top: 80%; animation-delay: 3s; width: 4px; height: 4px"
    ></div>
    <div
      class="particle"
      style="left: 85%; top: 30%; animation-delay: 2s; width: 2px; height: 2px"
    ></div>

    <div class="auth-panel">
      <!-- Logo -->
      <div class="logo-wrap">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01"
            />
          </svg>
        </div>
        <div>
          <div class="brand-name">DataSync</div>
          <div class="brand-sub">BACKUP SYSTEM v2.0</div>
        </div>
      </div>

      <h2 class="form-title">身份验证</h2>
      <p class="form-subtitle">登录以访问您的备份控制台</p>

      <!-- Email -->
      <div class="field-group">
        <label class="field-label">电子邮件</label>
        <div class="input-wrap">
          <svg
            class="input-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75"
            />
          </svg>
          <input
            v-model="email"
            type="email"
            placeholder="your@email.com"
            :disabled="isLoading"
            class="ds-input"
            @keyup.enter="handleLogin"
          />
        </div>
      </div>

      <!-- Password -->
      <div class="field-group">
        <label class="field-label">密码</label>
        <div class="input-wrap">
          <svg
            class="input-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"
            />
          </svg>
          <input
            v-model="password"
            type="password"
            placeholder="••••••••"
            :disabled="isLoading"
            class="ds-input"
            @keyup.enter="handleLogin"
          />
        </div>
      </div>

      <!-- Error -->
      <div v-if="errorMessage" class="error-bar">
        <svg viewBox="0 0 24 24" fill="currentColor" class="err-icon">
          <path
            fill-rule="evenodd"
            d="M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003zM12 8.25a.75.75 0 01.75.75v3.75a.75.75 0 01-1.5 0V9a.75.75 0 01.75-.75zm0 8.25a.75.75 0 100-1.5.75.75 0 000 1.5z"
            clip-rule="evenodd"
          />
        </svg>
        {{ errorMessage }}
      </div>

      <!-- Submit -->
      <button :disabled="isLoading" class="ds-btn-primary" @click="handleLogin">
        <svg v-if="isLoading" class="spin-icon" viewBox="0 0 24 24" fill="none">
          <circle
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="3"
            class="opacity-25"
          />
          <path
            fill="currentColor"
            class="opacity-75"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
        <span>{{ isLoading ? '验证中...' : '登录控制台' }}</span>
      </button>

      <div class="divider"><span>没有账号?</span></div>

      <button class="ds-btn-ghost" @click="$router.push('/register')">创建新账号</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager from '../utils/request'

const router = useRouter()
const email = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  errorMessage.value = ''
  if (!email.value || !password.value) {
    errorMessage.value = '请填写邮箱和密码'
    return
  }
  try {
    isLoading.value = true
    const res = await HttpManager.postNoAuth('/client/user/login', {
      email: email.value,
      password: password.value
    })
    // Handle both wrapped and unwrapped responses
    const data = res?.data ?? res
    if (!data?.refreshToken) throw new Error('返回数据格式异常')
    localStorage.setItem('authToken', data.refreshToken)
    localStorage.setItem(
      'userInfo',
      JSON.stringify({
        id: data.id,
        username: data.username,
        email: data.email,
        avatar: data.avatar
      })
    )
    router.push('/dashboard')
  } catch (err) {
    if (err.response?.status === 401) errorMessage.value = '账号或密码错误'
    else if (err.response?.status === 404) errorMessage.value = '接口地址不存在 (404)'
    else if (err.request) errorMessage.value = '无法连接到服务器，请确认后端已启动'
    else errorMessage.value = err.message || '发生未知错误'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=IBM+Plex+Mono:wght@400;500&display=swap');

* {
  box-sizing: border-box;
}

.auth-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafd;
  position: relative;
  overflow: hidden;
  font-family: 'Syne', sans-serif;
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(26, 115, 232, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26, 115, 232, 0.06) 1px, transparent 1px);
  background-size: 40px 40px;
}

.particle {
  position: absolute;
  background: #1a73e8;
  border-radius: 50%;
  opacity: 0.18;
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.18;
  }
  50% {
    transform: translateY(-20px);
    opacity: 0.34;
  }
}

.auth-panel {
  position: relative;
  z-index: 10;
  width: 420px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 50px rgba(60, 64, 67, 0.12);
  animation: panelIn 0.5s ease-out;
}

@keyframes panelIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: #e8f0fe;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1a73e8;
  border: 1px solid #d2e3fc;
}
.logo-icon svg {
  width: 22px;
  height: 22px;
}

.brand-name {
  font-size: 18px;
  font-weight: 800;
  color: #202124;
  letter-spacing: 0.05em;
}
.brand-sub {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 10px;
  color: #1a73e8;
  letter-spacing: 0.12em;
  margin-top: 1px;
}

.form-title {
  font-size: 22px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 6px;
}
.form-subtitle {
  font-size: 13px;
  color: #5f6368;
  margin: 0 0 28px;
}

.field-group {
  margin-bottom: 18px;
}
.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.input-wrap {
  position: relative;
}
.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #80868b;
}
.ds-input {
  width: 100%;
  padding: 11px 14px 11px 38px;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  color: #202124;
  font-size: 14px;
  font-family: 'Syne', sans-serif;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
  outline: none;
}
.ds-input::placeholder {
  color: #80868b;
}
.ds-input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.12);
  background: #ffffff;
}
.ds-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fce8e6;
  border: 1px solid rgba(217, 48, 37, 0.22);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: #d93025;
  margin-bottom: 18px;
}
.err-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.ds-btn-primary {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 13px;
  background: #1a73e8;
  border: none;
  border-radius: 9px;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  font-family: 'Syne', sans-serif;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition:
    opacity 0.2s,
    transform 0.15s,
    box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.18);
}
.ds-btn-primary:hover:not(:disabled) {
  background: #1765cc;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.22);
}
.ds-btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spin-icon {
  width: 18px;
  height: 18px;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
  color: #80868b;
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8eaed;
}
.divider span {
  color: #5f6368;
  white-space: nowrap;
}

.ds-btn-ghost {
  width: 100%;
  padding: 11px;
  background: transparent;
  border: 1px solid #dadce0;
  border-radius: 9px;
  color: #1a73e8;
  font-size: 14px;
  font-weight: 600;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s;
}
.ds-btn-ghost:hover {
  background: #e8f0fe;
  border-color: #d2e3fc;
}
</style>
