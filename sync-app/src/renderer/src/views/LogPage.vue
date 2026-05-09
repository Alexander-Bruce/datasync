<template>
  <div class="log-root">
    <header class="log-header">
      <div class="header-left">
        <button class="back-btn" @click="router.push('/dashboard')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          返回
        </button>
        <span class="logo-text">DataSync</span>
        <div class="header-badge">SYNC LOGS</div>
      </div>
      <div class="header-right">
        <input v-model="filter" class="filter-input" placeholder="过滤日志..." />
        <button class="refresh-btn" :class="{ spinning: isLoading }" @click="loadLogs">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
            />
          </svg>
          刷新
        </button>
      </div>
    </header>

    <main class="log-main">
      <div class="log-toolbar">
        <div class="toolbar-left">
          <h1 class="page-title">同步日志</h1>
          <span class="log-count">共 {{ filteredLogs.length }} 条</span>
        </div>
        <div class="level-filters">
          <button
            v-for="lv in levels"
            :key="lv.key"
            class="level-btn"
            :class="[lv.cls, { active: activeLevel === lv.key }]"
            @click="activeLevel = activeLevel === lv.key ? '' : lv.key"
          >
            {{ lv.label }}
          </button>
        </div>
      </div>

      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="loadError" class="error-state">
        <p>{{ loadError }}</p>
        <button @click="loadLogs">重试</button>
      </div>

      <div v-else ref="logContainer" class="log-container">
        <div v-for="(line, i) in filteredLogs" :key="i" class="log-line" :class="lineClass(line)">
          {{ line }}
        </div>
        <div v-if="filteredLogs.length === 0" class="empty-state">暂无匹配日志</div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager from '../utils/request'

const router = useRouter()
const logs = ref([])
const filter = ref('')
const activeLevel = ref('')
const isLoading = ref(false)
const loadError = ref('')
const logContainer = ref(null)

const levels = [
  { key: 'ERROR', label: 'ERROR', cls: 'lv-error' },
  { key: 'WARN', label: 'WARN', cls: 'lv-warn' },
  { key: 'INFO', label: 'INFO', cls: 'lv-info' }
]

const lineClass = (line) => {
  if (line.includes(' ERROR ')) return 'line-error'
  if (line.includes(' WARN ')) return 'line-warn'
  return 'line-info'
}

const filteredLogs = computed(() => {
  return logs.value.filter((line) => {
    if (activeLevel.value && !line.includes(` ${activeLevel.value} `)) return false
    if (filter.value && !line.toLowerCase().includes(filter.value.toLowerCase())) return false
    return true
  })
})

const loadLogs = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await HttpManager.get('/client/log/list?lines=300')
    const data = res?.data ?? res
    logs.value = Array.isArray(data) ? data : []
    await nextTick()
    if (logContainer.value) logContainer.value.scrollTop = logContainer.value.scrollHeight
  } catch (err) {
    loadError.value = err.request ? '无法连接服务器' : err.message || '加载失败'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=IBM+Plex+Mono:wght@400;500&display=swap');

*,
*::before,
*::after {
  box-sizing: border-box;
}

.log-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8fafd;
  font-family: 'Syne', sans-serif;
  color: #202124;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e0e3e7;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  color: #5f6368;
  font-size: 13px;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  transition: all 0.15s;
}
.back-btn svg {
  width: 15px;
  height: 15px;
}
.back-btn:hover {
  background: #f8fafd;
  color: #1a73e8;
  border-color: #c9d7f8;
}

.logo-text {
  font-size: 17px;
  font-weight: 800;
  color: #202124;
  letter-spacing: 0.05em;
}
.header-badge {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 9px;
  color: #1a73e8;
  letter-spacing: 0.1em;
  background: #e8f0fe;
  border: 1px solid #c9d7f8;
  border-radius: 4px;
  padding: 2px 7px;
}

.filter-input {
  padding: 7px 12px;
  background: #f1f3f4;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #202124;
  font-size: 12px;
  font-family: 'IBM Plex Mono', monospace;
  outline: none;
  width: 200px;
}
.filter-input::placeholder {
  color: #80868b;
}
.filter-input:focus {
  background: #fff;
  border-color: #1a73e8;
  box-shadow: 0 1px 4px rgba(60, 64, 67, 0.14);
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 7px 14px;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  color: #5f6368;
  font-size: 13px;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  transition: all 0.15s;
}
.refresh-btn svg {
  width: 14px;
  height: 14px;
}
.refresh-btn:hover {
  background: #f8fafd;
  color: #1a73e8;
  border-color: #c9d7f8;
}
.refresh-btn.spinning svg {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.log-main {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 24px 28px 20px;
  gap: 16px;
}

.log-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.toolbar-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #202124;
  margin: 0;
}
.log-count {
  font-size: 12px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
}

.level-filters {
  display: flex;
  gap: 8px;
}
.level-btn {
  padding: 5px 12px;
  border-radius: 6px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 600;
  font-family: 'IBM Plex Mono', monospace;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.15s;
}
.level-btn.active,
.level-btn:hover {
  opacity: 1;
}
.lv-error {
  background: #fce8e6;
  border-color: #fad2cf;
  color: #d93025;
}
.lv-error.active {
  background: #fad2cf;
}
.lv-warn {
  background: #fef7e0;
  border-color: #feefc3;
  color: #b06000;
}
.lv-warn.active {
  background: #feefc3;
}
.lv-info {
  background: #e8f0fe;
  border-color: #c9d7f8;
  color: #1a73e8;
}
.lv-info.active {
  background: #d2e3fc;
}

.loading-state,
.error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #5f6368;
}
.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e8f0fe;
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}
.error-state p {
  color: #d93025;
  font-size: 14px;
}
.error-state button {
  padding: 7px 18px;
  background: #fce8e6;
  border: 1px solid #fad2cf;
  border-radius: 8px;
  color: #d93025;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
}

.log-container {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e0e3e7;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(60, 64, 67, 0.08);
}
.log-container::-webkit-scrollbar {
  width: 6px;
}
.log-container::-webkit-scrollbar-track {
  background: transparent;
}
.log-container::-webkit-scrollbar-thumb {
  background: #c9d7f8;
  border-radius: 3px;
}

.log-line {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11.5px;
  line-height: 1.7;
  padding: 1px 4px;
  border-radius: 3px;
  word-break: break-all;
}
.log-line:hover {
  background: #f8fafd;
}
.line-error {
  color: #d93025;
}
.line-warn {
  color: #b06000;
}
.line-info {
  color: #3c4043;
}

.empty-state {
  text-align: center;
  color: #80868b;
  font-size: 13px;
  padding: 40px 0;
}
</style>
