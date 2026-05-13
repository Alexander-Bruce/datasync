<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-panel">
        <div class="modal-header">
          <div class="modal-title-row">
            <svg
              class="modal-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M17.982 18.725A7.488 7.488 0 0012 15.75a7.488 7.488 0 00-5.982 2.975m11.963 0a9 9 0 10-11.963 0m11.963 0A8.966 8.966 0 0112 21a8.966 8.966 0 01-5.982-2.275M15 9.75a3 3 0 11-6 0 3 3 0 016 0z"
              />
            </svg>
            <h3>账号设置</h3>
          </div>
          <button class="modal-close" @click="$emit('close')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Avatar preview -->
        <div class="avatar-section">
          <div class="avatar-preview">
            <img
              :src="
                form.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${form.username}`
              "
              alt="avatar"
            />
          </div>
          <div class="avatar-info">
            <p class="avatar-name">{{ form.username || '用户' }}</p>
            <p class="avatar-email">{{ form.email }}</p>
          </div>
        </div>

        <div class="divider-line"></div>

        <!-- Form -->
        <div class="section-title">
          <h4>账号资料</h4>
          <span>用于客户端显示和服务端账号信息</span>
        </div>
        <div class="form-grid">
          <div class="field-group">
            <label class="field-label">用户名</label>
            <input v-model="form.username" type="text" class="ds-input" placeholder="输入用户名" />
          </div>
          <div class="field-group">
            <label class="field-label">电子邮件</label>
            <input
              v-model="form.email"
              type="email"
              class="ds-input"
              placeholder="邮箱地址"
              readonly
            />
          </div>
          <div class="field-group full-width">
            <label class="field-label">头像 URL <span class="optional">(可选)</span></label>
            <input
              v-model="form.avatar"
              type="text"
              class="ds-input"
              placeholder="https://example.com/avatar.png"
            />
          </div>
          <div class="field-group full-width">
            <label class="field-label"
              >本地头像图片 <span class="optional">(PNG/JPG/WebP/GIF)</span></label
            >
            <div class="avatar-upload-control">
              <label class="avatar-upload-btn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                  />
                </svg>
                <span>选择图片</span>
                <input
                  type="file"
                  class="avatar-file-input"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  @change="onAvatarFileChange"
                />
              </label>
              <span class="avatar-file-name">{{ avatarFileName || '未选择文件' }}</span>
            </div>
            <p class="avatar-upload-hint">支持 PNG、JPG、WebP、GIF，最大 2MB。</p>
          </div>
        </div>

        <div class="divider-line"></div>

        <div class="section-title">
          <h4>服务器配置</h4>
          <span>修改后会保存到本机，下次启动继续使用</span>
        </div>
        <div class="form-grid config-grid">
          <div class="field-group full-width">
            <label class="field-label">服务端 API 地址</label>
            <input
              v-model.trim="clientConfig.serverBaseUrl"
              type="text"
              class="ds-input"
              placeholder="https://ccrystal-my-webapp.hf.space"
            />
          </div>
          <div class="field-group">
            <label class="field-label">同步主机</label>
            <input
              v-model.trim="clientConfig.syncHost"
              type="text"
              class="ds-input"
              placeholder="119.91.105.168"
            />
          </div>
          <div class="field-group">
            <label class="field-label">同步端口</label>
            <input
              v-model.number="clientConfig.syncPort"
              type="number"
              min="1"
              max="65535"
              class="ds-input"
            />
          </div>
        </div>

        <div v-if="configMessage.text" :class="['msg-bar', configMessage.type]">
          {{ configMessage.text }}
        </div>

        <div class="config-actions">
          <button class="btn-cancel" :disabled="configBusy" @click="testServerConfig">
            {{ configTesting ? '测试中...' : '测试连接' }}
          </button>
          <button class="btn-save" :disabled="configBusy" @click="saveServerConfig">
            {{ configSaving ? '保存中...' : '保存服务器配置' }}
          </button>
        </div>

        <!-- Error/Success -->
        <div v-if="message.text" :class="['msg-bar', message.type]">
          {{ message.text }}
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-save" :disabled="isSaving" @click="handleSave">
            <svg v-if="isSaving" class="spin-icon" viewBox="0 0 24 24" fill="none">
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
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                class="opacity-75"
              />
            </svg>
            {{ isSaving ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, ref, reactive, watch } from 'vue'
import HttpManager, { getCachedClientConfig, setCachedClientConfig } from '../utils/request'
import { clearConfigStateCache } from '../router'

const props = defineProps({
  visible: Boolean,
  user: Object
})
const emit = defineEmits(['close', 'updated'])

const isSaving = ref(false)
const configSaving = ref(false)
const configTesting = ref(false)
const message = reactive({ text: '', type: '' })
const configMessage = reactive({ text: '', type: '' })
const form = reactive({ username: '', email: '', avatar: '' })
const clientConfig = reactive({ serverBaseUrl: '', syncHost: '', syncPort: 8080 })
const avatarFileName = ref('')
const configBusy = computed(() => configSaving.value || configTesting.value)

const fillForm = (u) => {
  if (!u) return
  form.username = u.username || ''
  form.email = u.email || ''
  form.avatar = u.avatar || ''
  avatarFileName.value = ''
}

const fillConfig = (config) => {
  if (!config) return
  clientConfig.serverBaseUrl = config.serverBaseUrl || clientConfig.serverBaseUrl
  clientConfig.syncHost = config.syncHost || clientConfig.syncHost
  clientConfig.syncPort = config.syncPort || clientConfig.syncPort || 8080
}

const loadClientConfig = async () => {
  fillConfig(getCachedClientConfig())
  try {
    const res = await HttpManager.getNoAuth('/client/config')
    const data = res?.data ?? res
    fillConfig(data)
    if (data?.configured) setCachedClientConfig(data)
  } catch {
    configMessage.text = '本地客户端服务暂未响应，稍后可重试。'
    configMessage.type = 'error'
  }
}

watch(
  () => props.user,
  (u) => {
    fillForm(u)
  },
  { immediate: true }
)

watch(
  () => props.visible,
  (v) => {
    if (v) {
      message.text = ''
      message.type = ''
      configMessage.text = ''
      configMessage.type = ''
      const saved = JSON.parse(localStorage.getItem('userInfo') || '{}')
      fillForm({ ...saved, ...(props.user || {}) })
      loadClientConfig()
    }
  }
)

const submitServerConfig = async (url, successText, persist = false) => {
  configMessage.text = ''
  const payload = {
    serverBaseUrl: clientConfig.serverBaseUrl,
    syncHost: clientConfig.syncHost,
    syncPort: Number(clientConfig.syncPort || 8080)
  }
  try {
    const res = await HttpManager.postNoAuth(url, payload)
    const data = res?.data ?? res
    fillConfig(data)
    if (persist && data?.configured) {
      setCachedClientConfig(data)
      clearConfigStateCache()
    }
    configMessage.text = successText
    configMessage.type = 'success'
    return true
  } catch (err) {
    configMessage.text = err.data?.message || err.message || '服务器配置失败'
    configMessage.type = 'error'
    return false
  }
}

const testServerConfig = async () => {
  configTesting.value = true
  try {
    await submitServerConfig('/client/config/test', '服务器连接正常。')
  } finally {
    configTesting.value = false
  }
}

const saveServerConfig = async () => {
  configSaving.value = true
  try {
    await submitServerConfig('/client/config', '服务器配置已保存到本机。', true)
  } finally {
    configSaving.value = false
  }
}

const onAvatarFileChange = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/webp', 'image/gif'].includes(file.type)) {
    message.text = '不支持的头像图片格式'
    message.type = 'error'
    event.target.value = ''
    avatarFileName.value = ''
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    message.text = '头像图片不能超过 2MB'
    message.type = 'error'
    event.target.value = ''
    avatarFileName.value = ''
    return
  }
  avatarFileName.value = file.name
  const reader = new FileReader()
  reader.onload = () => {
    form.avatar = reader.result || ''
  }
  reader.onerror = () => {
    message.text = '读取头像图片失败'
    message.type = 'error'
  }
  reader.readAsDataURL(file)
}

const handleSave = async () => {
  if (!form.username || !form.email) {
    message.text = '用户名和邮箱不能为空'
    message.type = 'error'
    return
  }
  try {
    isSaving.value = true
    message.text = ''
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const payload = {
      id: userInfo.id,
      username: form.username,
      email: userInfo.email || form.email,
      avatar: form.avatar,
      userAgent: navigator.userAgent,
      token: localStorage.getItem('authToken')
    }
    const res = await HttpManager.post('/client/user/update', payload)
    const data = res?.data ?? res
    const updated = {
      id: data?.id ?? userInfo.id,
      username: data?.username ?? form.username,
      email: data?.email ?? userInfo.email ?? form.email,
      avatar: data?.avatar ?? form.avatar
    }
    localStorage.setItem('userInfo', JSON.stringify(updated))
    if (data?.token) localStorage.setItem('authToken', data.token)
    message.text = '修改已保存!'
    message.type = 'success'
    emit('updated', updated)
    setTimeout(() => emit('close'), 1000)
  } catch (err) {
    message.text = err.request ? '无法连接到服务器' : err.message || '保存失败'
    message.type = 'error'
  } finally {
    isSaving.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700&family=IBM+Plex+Mono&display=swap');

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(60, 64, 67, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.15s ease;
}
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-panel {
  width: min(560px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  overflow: auto;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  padding: 28px;
  box-shadow:
    0 24px 60px rgba(60, 64, 67, 0.18),
    0 1px 0 rgba(255, 255, 255, 0.8) inset;
  font-family: 'Syne', sans-serif;
  animation: slideUp 0.2s ease;
}
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.modal-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.modal-icon {
  width: 20px;
  height: 20px;
  color: #1a73e8;
}
.modal-title-row h3 {
  font-size: 17px;
  font-weight: 700;
  color: #202124;
  margin: 0;
}
.modal-close {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.15s,
    color 0.15s;
}
.modal-close:hover {
  background: #fce8e6;
  color: #d93025;
  border-color: rgba(217, 48, 37, 0.22);
}
.modal-close svg {
  width: 14px;
  height: 14px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafd;
  border-radius: 10px;
  border: 1px solid #e8eaed;
  margin-bottom: 20px;
}
.avatar-preview {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #d2e3fc;
  flex-shrink: 0;
}
.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-name {
  font-size: 15px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 3px;
}
.avatar-email {
  font-size: 12px;
  color: #5f6368;
  margin: 0;
  font-family: 'IBM Plex Mono', monospace;
}

.divider-line {
  height: 1px;
  background: #e8eaed;
  margin-bottom: 20px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 16px;
}
.config-grid {
  margin-bottom: 12px;
}
.section-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title h4 {
  margin: 0;
  color: #202124;
  font-size: 15px;
  font-weight: 700;
}
.section-title span {
  color: #80868b;
  font-size: 12px;
  text-align: right;
}
.field-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.full-width {
  grid-column: 1 / -1;
}
.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
  letter-spacing: 0;
}
.optional {
  color: #80868b;
  text-transform: none;
  letter-spacing: 0;
  font-weight: 400;
}
.ds-input {
  height: 42px;
  padding: 0 12px;
  background: linear-gradient(#ffffff, #fbfdff);
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  color: #202124;
  font-size: 13px;
  font-family: 'Syne', sans-serif;
  outline: none;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.05);
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background 0.2s;
}
.ds-input::placeholder {
  color: #80868b;
}
.ds-input:focus {
  border-color: #1a73e8;
  background: #fff;
  box-shadow:
    0 0 0 3px rgba(26, 115, 232, 0.12),
    0 8px 22px rgba(60, 64, 67, 0.08);
}
.ds-input[readonly] {
  background: #f8fafd;
  color: #5f6368;
}
.avatar-upload-control {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-upload-btn {
  height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid #c9d7f8;
  border-radius: 8px;
  background: #e8f0fe;
  color: #1a73e8;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}
.avatar-upload-btn:hover {
  background: #d2e3fc;
  border-color: #a8c7fa;
}
.avatar-upload-btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.avatar-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}
.avatar-file-name {
  min-width: 0;
  color: #5f6368;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.avatar-upload-hint {
  margin: 0;
  color: #80868b;
  font-size: 12px;
}

.msg-bar {
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}
.msg-bar.error {
  background: #fce8e6;
  border: 1px solid rgba(217, 48, 37, 0.22);
  color: #d93025;
}
.msg-bar.success {
  background: #e6f4ea;
  border: 1px solid rgba(24, 128, 56, 0.22);
  color: #188038;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.config-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 16px;
}
.btn-cancel {
  padding: 9px 20px;
  background: transparent;
  border: 1px solid #dadce0;
  border-radius: 8px;
  color: #5f6368;
  font-size: 13px;
  font-weight: 600;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-cancel:hover {
  background: #edf2fa;
  color: #202124;
}
.btn-cancel:disabled,
.btn-save:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.btn-save {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 22px;
  background: #1a73e8;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  transition: opacity 0.15s;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.18);
}
.btn-save:hover:not(:disabled) {
  background: #1765cc;
}
.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.spin-icon {
  width: 15px;
  height: 15px;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
