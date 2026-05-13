<template>
  <main class="setup-root">
    <section class="setup-panel">
      <div class="brand-row">
        <div class="brand-mark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M4 7h16M4 12h16M4 17h16M7 7v10M17 7v10"
            />
          </svg>
        </div>
        <div>
          <div class="brand-name">DataSync</div>
          <div class="brand-subtitle">客户端激活</div>
        </div>
      </div>

      <h1>连接服务器</h1>
      <p class="intro">首次使用前配置服务端地址，保存后会写入本机。</p>

      <form class="setup-form" @submit.prevent="saveConfig">
        <label class="field">
          <span>服务端 API 地址</span>
          <input
            v-model.trim="form.serverBaseUrl"
            type="text"
            placeholder="https://ccrystal-my-webapp.hf.space"
            autocomplete="off"
          />
        </label>

        <div class="field-grid">
          <label class="field">
            <span>同步主机</span>
            <input
              v-model.trim="form.syncHost"
              type="text"
              placeholder="119.91.105.168"
              autocomplete="off"
            />
          </label>

          <label class="field">
            <span>同步端口</span>
            <input v-model.number="form.syncPort" type="number" min="1" max="65535" />
          </label>
        </div>

        <div v-if="message" :class="['status-bar', statusType]">
          {{ message }}
        </div>

        <div class="actions">
          <button type="button" class="btn-secondary" :disabled="loading" @click="testConfig">
            测试连接
          </button>
          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? '保存中...' : '保存并继续' }}
          </button>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager, {
  LOCAL_API_BASE_URL,
  getCachedClientConfig,
  setCachedClientConfig
} from '../utils/request'
import { clearConfigStateCache } from '../router'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const statusType = ref('info')

const form = reactive({
  serverBaseUrl: '',
  syncHost: '',
  syncPort: 8080
})

onMounted(() => {
  fillFromConfig(getCachedClientConfig())
  loadConfig()
})

function fillFromConfig(config) {
  if (!config) return
  form.serverBaseUrl = config.serverBaseUrl || form.serverBaseUrl
  form.syncHost = config.syncHost || form.syncHost
  form.syncPort = config.syncPort || form.syncPort || 8080
}

async function loadConfig() {
  try {
    const res = await HttpManager.getNoAuth('/client/config')
    const data = res?.data ?? res
    fillFromConfig(data)
    if (data?.configured) setCachedClientConfig(data)
  } catch {
    statusType.value = 'error'
    message.value = `本地客户端服务暂未响应：${LOCAL_API_BASE_URL}`
  }
}

async function testConfig() {
  await submit('/client/config/test', '服务器连接正常。')
}

async function saveConfig() {
  const saved = await submit('/client/config', '配置已保存到本机。', true)
  if (!saved) return

  clearConfigStateCache()
  const token = localStorage.getItem('authToken')
  router.push(token ? '/dashboard' : '/')
}

async function submit(url, successText, persist = false) {
  message.value = ''
  try {
    loading.value = true
    const res = await HttpManager.postNoAuth(url, {
      serverBaseUrl: form.serverBaseUrl,
      syncHost: form.syncHost,
      syncPort: Number(form.syncPort || 8080)
    })
    const data = res?.data ?? res
    fillFromConfig(data)
    if (persist && data?.configured) setCachedClientConfig(data)
    statusType.value = 'success'
    message.value = successText
    return true
  } catch (err) {
    statusType.value = 'error'
    message.value = err.data?.message || err.message || '配置失败。'
    return false
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.setup-root {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background:
    linear-gradient(rgba(28, 100, 242, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(28, 100, 242, 0.05) 1px, transparent 1px), #f7f9fc;
  background-size: 38px 38px;
  font-family:
    Inter,
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif;
}

.setup-panel {
  width: min(100%, 460px);
  padding: 36px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow:
    0 24px 70px rgba(15, 23, 42, 0.12),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  color: #0f62fe;
  background: #eef4ff;
  border: 1px solid #cfe0ff;
  border-radius: 10px;
}

.brand-mark svg {
  width: 23px;
  height: 23px;
}

.brand-name {
  font-weight: 800;
  color: #111827;
}

.brand-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.2;
  color: #111827;
}

.intro {
  margin: 8px 0 28px;
  color: #64748b;
  font-size: 14px;
}

.setup-form,
.field {
  display: grid;
  gap: 16px;
}

.field {
  gap: 8px;
}

.field span {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #475569;
}

input {
  width: 100%;
  min-width: 0;
  height: 46px;
  padding: 0 13px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  color: #111827;
  background: linear-gradient(#ffffff, #fbfdff);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition:
    border-color 0.15s,
    box-shadow 0.15s,
    background 0.15s;
}

input:focus {
  border-color: #0f62fe;
  background: #fff;
  box-shadow:
    0 0 0 3px rgba(15, 98, 254, 0.13),
    0 8px 22px rgba(15, 23, 42, 0.08);
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
}

.status-bar {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.status-bar.success {
  color: #166534;
  background: #dcfce7;
  border: 1px solid #bbf7d0;
}

.status-bar.error {
  color: #b91c1c;
  background: #fee2e2;
  border: 1px solid #fecaca;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

button {
  height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.btn-primary {
  color: #fff;
  background: #0f62fe;
}

.btn-primary:hover:not(:disabled) {
  background: #0b55df;
}

.btn-secondary {
  color: #0f62fe;
  background: #fff;
  border-color: #cfe0ff;
}

.btn-secondary:hover:not(:disabled) {
  background: #eef4ff;
}

@media (max-width: 540px) {
  .setup-panel {
    padding: 28px 22px;
  }

  .field-grid,
  .actions {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
