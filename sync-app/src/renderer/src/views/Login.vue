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
          <div class="brand-sub">Client Console</div>
        </div>
      </div>

      <h1>Sign in</h1>
      <p class="subtitle">Use your DataSync account to continue.</p>

      <form class="auth-form" @submit.prevent="handleLogin">
        <label class="field-group">
          <span>Email</span>
          <input
            v-model.trim="email"
            type="email"
            placeholder="you@example.com"
            :disabled="isLoading"
            autocomplete="email"
          />
        </label>

        <label class="field-group">
          <span>Password</span>
          <input
            v-model="password"
            type="password"
            placeholder="Password"
            :disabled="isLoading"
            autocomplete="current-password"
          />
        </label>

        <div v-if="errorMessage" class="error-bar">{{ errorMessage }}</div>

        <button :disabled="isLoading" class="ds-btn-primary" type="submit">
          {{ isLoading ? 'Signing in...' : 'Sign in' }}
        </button>
      </form>

      <div class="divider"><span>No account yet?</span></div>

      <button class="ds-btn-ghost" type="button" @click="$router.push('/register')">
        Create account
      </button>
      <button class="setup-link" type="button" @click="$router.push('/setup')">
        Server settings
      </button>
    </section>
  </main>
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
    errorMessage.value = 'Email and password are required.'
    return
  }

  try {
    isLoading.value = true
    const res = await HttpManager.postNoAuth('/client/user/login', {
      email: email.value,
      password: password.value
    })
    const data = res?.data ?? res
    if (!data?.refreshToken) {
      throw new Error('Unexpected login response.')
    }
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
    errorMessage.value = err.data?.message || err.message || 'Sign in failed.'
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
  border-radius: 12px;
  padding: 38px;
  box-shadow: 0 20px 50px rgba(60, 64, 67, 0.12);
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
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #5f6368;
}

input {
  width: 100%;
  min-width: 0;
  height: 42px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  color: #202124;
  font-size: 14px;
  outline: none;
}

input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.12);
}

input:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.error-bar {
  background: #fce8e6;
  border: 1px solid rgba(217, 48, 37, 0.22);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #d93025;
}

.ds-btn-primary,
.ds-btn-ghost,
.setup-link {
  width: 100%;
  height: 42px;
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

.setup-link {
  margin-top: 10px;
  background: transparent;
  border: 0;
  color: #5f6368;
  font-weight: 600;
}

.setup-link:hover {
  color: #1a73e8;
}
</style>
