<template>
  <main class="auth-root">
    <section class="auth-panel">
      <div class="logo-wrap">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2"
            />
          </svg>
        </div>
        <div>
          <div class="brand-name">DataSync</div>
          <div class="brand-sub">客户端控制台</div>
        </div>
      </div>

      <h1>注册账号</h1>
      <p class="subtitle">创建账号后进入 DataSync 控制台。</p>

      <form class="auth-form" @submit.prevent="handleRegister">
        <label class="field-group">
          <span>用户名</span>
          <input
            v-model.trim="username"
            type="text"
            placeholder="请输入用户名"
            :disabled="isLoading"
            autocomplete="name"
          />
        </label>

        <label class="field-group">
          <span>邮箱</span>
          <input
            v-model.trim="email"
            type="email"
            placeholder="请输入邮箱"
            :disabled="isLoading"
            autocomplete="email"
          />
        </label>

        <label class="field-group">
          <span>密码</span>
          <input
            v-model="password"
            type="password"
            placeholder="至少 6 位字符"
            :disabled="isLoading"
            autocomplete="new-password"
          />
        </label>

        <div v-if="errorMessage" class="error-bar">{{ errorMessage }}</div>
        <div v-if="successMessage" class="success-bar">{{ successMessage }}</div>

        <button :disabled="isLoading" class="ds-btn-primary" type="submit">
          {{ isLoading ? '注册中...' : '注册' }}
        </button>
      </form>

      <div class="divider"><span>已有账号？</span></div>
      <button class="ds-btn-ghost" type="button" @click="$router.push('/')">返回登录</button>
    </section>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager from '../utils/request'

const router = useRouter()
const username = ref('')
const email = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const handleRegister = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  if (!username.value || !email.value || !password.value) {
    errorMessage.value = '请填写用户名、邮箱和密码。'
    return
  }
  if (password.value.length < 6) {
    errorMessage.value = '密码至少需要 6 位字符。'
    return
  }

  try {
    isLoading.value = true
    const res = await HttpManager.postNoAuth('/client/user/signup', {
      username: username.value,
      email: email.value,
      password: password.value
    })
    const data = res?.data ?? res
    const token = data?.token || data?.refreshToken
    if (!token) {
      throw new Error('注册响应异常。')
    }
    successMessage.value = '注册成功，正在进入控制台...'
    localStorage.setItem('authToken', token)
    localStorage.setItem(
      'userInfo',
      JSON.stringify({
        id: data.id,
        username: data.username,
        email: data.email,
        avatar: data.avatar
      })
    )
    setTimeout(() => router.push('/dashboard'), 500)
  } catch (err) {
    errorMessage.value = err.data?.message || err.message || '注册失败。'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.auth-root {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background:
    linear-gradient(rgba(26, 115, 232, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(26, 115, 232, 0.06) 1px, transparent 1px), #f8fafd;
  background-size: 40px 40px;
  font-family:
    Inter,
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
}

.auth-panel {
  width: min(100%, 420px);
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  padding: 38px;
  box-shadow:
    0 20px 50px rgba(60, 64, 67, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 30px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: #e8f0fe;
  border-radius: 10px;
  display: grid;
  place-items: center;
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
}

.brand-sub {
  font-size: 12px;
  color: #1a73e8;
  margin-top: 2px;
}

h1 {
  font-size: 24px;
  color: #202124;
  margin: 0 0 6px;
}

.subtitle {
  margin: 0 0 26px;
  font-size: 14px;
  color: #5f6368;
}

.auth-form {
  display: grid;
  gap: 16px;
}

.field-group {
  display: grid;
  gap: 8px;
}

.field-group span {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  color: #5f6368;
}

input {
  width: 100%;
  min-width: 0;
  height: 46px;
  padding: 0 13px;
  background: linear-gradient(#ffffff, #fbfdff);
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #202124;
  font-size: 14px;
  outline: none;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.05);
  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    background 0.15s;
}

input:focus {
  border-color: #1a73e8;
  background: #fff;
  box-shadow:
    0 0 0 3px rgba(26, 115, 232, 0.12),
    0 8px 22px rgba(60, 64, 67, 0.08);
}

.error-bar,
.success-bar {
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
}

.error-bar {
  background: #fce8e6;
  border: 1px solid rgba(217, 48, 37, 0.22);
  color: #d93025;
}

.success-bar {
  background: #e6f4ea;
  border: 1px solid rgba(24, 128, 56, 0.22);
  color: #188038;
}

.ds-btn-primary,
.ds-btn-ghost {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.ds-btn-primary {
  background: #1a73e8;
  border: 0;
  color: #fff;
}

.ds-btn-primary:hover:not(:disabled) {
  background: #1765cc;
}

.ds-btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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

.ds-btn-ghost {
  background: transparent;
  border: 1px solid #dadce0;
  color: #1a73e8;
}

.ds-btn-ghost:hover {
  background: #e8f0fe;
  border-color: #d2e3fc;
}
</style>
