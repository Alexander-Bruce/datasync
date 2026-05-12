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
          <div class="brand-subtitle">Client activation</div>
        </div>
      </div>

      <h1>Connect your server</h1>
      <p class="intro">Set the API endpoint and sync socket before signing in.</p>

      <form class="setup-form" @submit.prevent="saveConfig">
        <label class="field">
          <span>Server API URL</span>
          <input
            v-model.trim="form.serverBaseUrl"
            type="text"
            placeholder="http://119.91.105.168:8090"
            autocomplete="off"
          />
        </label>

        <div class="field-grid">
          <label class="field">
            <span>Sync host</span>
            <input
              v-model.trim="form.syncHost"
              type="text"
              placeholder="119.91.105.168"
              autocomplete="off"
            />
          </label>

          <label class="field">
            <span>Sync port</span>
            <input v-model.number="form.syncPort" type="number" min="1" max="65535" />
          </label>
        </div>

        <div v-if="message" :class="['status-bar', statusType]">
          {{ message }}
        </div>

        <div class="actions">
          <button type="button" class="btn-secondary" :disabled="loading" @click="testConfig">
            Test
          </button>
          <button type="submit" class="btn-primary" :disabled="loading">
            {{ loading ? 'Saving...' : 'Save and continue' }}
          </button>
        </div>
      </form>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager, { LOCAL_API_BASE_URL } from '../utils/request'
import { clearConfigStateCache } from '../router'

const router = useRouter()
const loading = ref(false)
const message = ref('')
const statusType = ref('info')

const form = reactive({
  serverBaseUrl: '',
  syncHost: '',
  syncPort: 8443
})

onMounted(loadConfig)

async function loadConfig() {
  try {
    const res = await HttpManager.getNoAuth('/client/config')
    const data = res?.data ?? res
    form.serverBaseUrl = data?.serverBaseUrl || ''
    form.syncHost = data?.syncHost || ''
    form.syncPort = data?.syncPort || 8443
  } catch {
    statusType.value = 'error'
    message.value = `Local client service is not responding at ${LOCAL_API_BASE_URL}.`
  }
}

async function testConfig() {
  await submit('/client/config/test', 'Server connection OK.')
}

async function saveConfig() {
  const saved = await submit('/client/config', 'Configuration saved.')
  if (!saved) return

  clearConfigStateCache()
  const token = localStorage.getItem('authToken')
  router.push(token ? '/dashboard' : '/')
}

async function submit(url, successText) {
  message.value = ''
  try {
    loading.value = true
    const res = await HttpManager.postNoAuth(url, {
      serverBaseUrl: form.serverBaseUrl,
      syncHost: form.syncHost,
      syncPort: Number(form.syncPort || 8443)
    })
    const data = res?.data ?? res
    form.serverBaseUrl = data?.serverBaseUrl || form.serverBaseUrl
    form.syncHost = data?.syncHost || form.syncHost
    form.syncPort = data?.syncPort || form.syncPort
    statusType.value = 'success'
    message.value = successText
    return true
  } catch (err) {
    statusType.value = 'error'
    message.value = err.data?.message || err.message || 'Configuration failed.'
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
  border-radius: 12px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.12);
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
  height: 42px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  font-size: 14px;
  color: #111827;
  background: #fff;
}

input:focus {
  border-color: #0f62fe;
  box-shadow: 0 0 0 3px rgba(15, 98, 254, 0.13);
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
