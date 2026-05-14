<template>
  <div class="explorer-root" @click="closeContextMenu">
    <!-- ─── HEADER ─── -->
    <header class="exp-header">
      <div class="header-left">
        <div class="nav-buttons">
          <button
            class="nav-btn"
            :disabled="pathStack.length === 0"
            title="返回上一级"
            @click="goBack"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M15.75 19.5L8.25 12l7.5-7.5"
              />
            </svg>
          </button>
          <button class="nav-btn" title="返回主界面" @click="router.push('/dashboard')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25"
              />
            </svg>
          </button>
        </div>

        <!-- Breadcrumb -->
        <nav class="breadcrumb">
          <button class="crumb crumb-link" @click="router.push('/dashboard')">主界面</button>
          <span class="crumb-wrap">
            <svg
              class="crumb-sep"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
            <button class="crumb crumb-link" @click="router.push('/groups')">群组</button>
          </span>
          <span class="crumb-wrap">
            <svg
              class="crumb-sep"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
            <button v-if="pathStack.length > 0" class="crumb crumb-link" @click="navigateToRoot">
              {{ scopeDisplayName }}
            </button>
            <span v-else class="crumb crumb-current">{{ scopeDisplayName }}</span>
          </span>
          <span v-for="(seg, i) in pathStack" :key="i" class="crumb-wrap">
            <svg
              class="crumb-sep"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
            <button
              v-if="i < pathStack.length - 1"
              class="crumb crumb-link"
              @click="navigateToBreadcrumb(i)"
            >
              {{ seg }}
            </button>
            <span v-else class="crumb crumb-current">{{ seg }}</span>
          </span>
        </nav>
      </div>

      <div class="header-right">
        <div class="view-toggle">
          <button
            :class="{ active: viewMode === 'grid' }"
            title="网格视图"
            @click="viewMode = 'grid'"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M3.75 6A2.25 2.25 0 016 3.75h2.25A2.25 2.25 0 0110.5 6v2.25a2.25 2.25 0 01-2.25 2.25H6a2.25 2.25 0 01-2.25-2.25V6zM3.75 15.75A2.25 2.25 0 016 13.5h2.25a2.25 2.25 0 012.25 2.25V18a2.25 2.25 0 01-2.25 2.25H6A2.25 2.25 0 013.75 18v-2.25zM13.5 6a2.25 2.25 0 012.25-2.25H18A2.25 2.25 0 0120.25 6v2.25A2.25 2.25 0 0118 10.5h-2.25a2.25 2.25 0 01-2.25-2.25V6zM13.5 15.75a2.25 2.25 0 012.25-2.25H18a2.25 2.25 0 012.25 2.25V18A2.25 2.25 0 0118 20.25h-2.25A2.25 2.25 0 0113.5 18v-2.25z"
              />
            </svg>
          </button>
          <button
            :class="{ active: viewMode === 'list' }"
            title="列表视图"
            @click="viewMode = 'list'"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z"
              />
            </svg>
          </button>
        </div>
        <button
          class="refresh-btn"
          :class="{ spinning: isLoading }"
          title="刷新"
          @click="loadFiles"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
            />
          </svg>
        </button>
      </div>
    </header>

    <!-- ─── TOOLBAR ─── -->
    <div class="exp-toolbar">
      <div class="folder-info">
        <div class="group-badge">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"
            />
          </svg>
        </div>
        <div>
          <p class="folder-name">
            {{ scopeDisplayName }} <span class="group-name-chip">{{ groupName }}</span>
          </p>
          <p class="folder-path">/{{ currentPath || '' }}</p>
        </div>
      </div>
      <div class="toolbar-actions">
        <span class="stat-chip">{{ visibleFiles.length }} 项</span>
        <button
          class="download-btn"
          :class="{ loading: isDownloading }"
          title="下行同步当前文件夹到本地"
          @click="downloadCurrent()"
        >
          <svg v-if="isDownloading" class="spin-icon" viewBox="0 0 24 24" fill="none">
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
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"
            />
          </svg>
          {{ isDownloading ? '同步中...' : '下行同步' }}
        </button>
      </div>
    </div>

    <!-- ─── CONTENT ─── -->
    <div class="exp-content" @contextmenu.prevent>
      <div v-if="isLoading" class="center-state">
        <div class="loading-spinner"></div>
        <p>正在获取文件列表...</p>
      </div>

      <div v-else-if="visibleFiles.length === 0" class="center-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
          />
        </svg>
        <p>此文件夹为空</p>
      </div>

      <!-- GRID VIEW -->
      <div v-else-if="viewMode === 'grid'" class="file-grid">
        <div
          v-for="file in visibleFiles"
          :key="file.relativePath"
          class="file-tile"
          :class="{ selected: selectedPath === file.relativePath }"
          @click.stop="selectedPath = file.relativePath"
          @dblclick="openFile(file)"
          @contextmenu.stop.prevent="showContext(file, $event)"
        >
          <div class="tile-icon" :class="file.dir ? 'tile-dir' : 'tile-file'">
            <svg v-if="file.dir" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M19.5 21a3 3 0 003-3v-4.5a3 3 0 00-3-3h-15a3 3 0 00-3 3V18a3 3 0 003 3h15zM1.5 10.146V6a3 3 0 013-3h5.379a2.25 2.25 0 011.59.659l2.122 2.121c.14.141.331.22.53.22H19.5a3 3 0 013 3v1.146A4.483 4.483 0 0019.5 9h-15a4.483 4.483 0 00-3 1.146z"
              />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M5.625 1.5H9a3.75 3.75 0 013.75 3.75v1.875c0 1.036.84 1.875 1.875 1.875H16.5a3.75 3.75 0 013.75 3.75v7.875c0 1.035-.84 1.875-1.875 1.875H5.625a1.875 1.875 0 01-1.875-1.875V3.375c0-1.036.84-1.875 1.875-1.875zm5.845 17.03a.75.75 0 001.06 0l3-3a.75.75 0 10-1.06-1.06l-1.72 1.72V12a.75.75 0 00-1.5 0v4.19l-1.72-1.72a.75.75 0 00-1.06 1.06l3 3z"
                clip-rule="evenodd"
              />
              <path
                d="M14.25 5.25a5.23 5.23 0 00-1.279-3.434 9.768 9.768 0 016.963 6.963A5.23 5.23 0 0016.5 7.5h-1.875a.375.375 0 01-.375-.375V5.25z"
              />
            </svg>
          </div>
          <!-- Server file badge -->
          <div class="tile-server-dot" title="服务端文件"></div>
          <p class="tile-name" :title="file.name">{{ file.name }}</p>
          <p class="tile-type">{{ file.dir ? '文件夹' : getExt(file.name) }}</p>
        </div>
      </div>

      <!-- LIST VIEW -->
      <div v-else class="file-list">
        <div class="list-header">
          <span class="col-icon"></span>
          <span class="col-name">名称</span>
          <span class="col-status">来源</span>
          <span class="col-path">路径</span>
          <span class="col-type">类型</span>
        </div>
        <div
          v-for="file in visibleFiles"
          :key="file.relativePath"
          class="list-row"
          :class="{ selected: selectedPath === file.relativePath }"
          @click.stop="selectedPath = file.relativePath"
          @dblclick="openFile(file)"
          @contextmenu.stop.prevent="showContext(file, $event)"
        >
          <span class="col-icon">
            <div class="row-icon" :class="file.dir ? 'row-dir' : 'row-file'">
              <svg v-if="file.dir" viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M19.5 21a3 3 0 003-3v-4.5a3 3 0 00-3-3h-15a3 3 0 00-3 3V18a3 3 0 003 3h15zM1.5 10.146V6a3 3 0 013-3h5.379a2.25 2.25 0 011.59.659l2.122 2.121c.14.141.331.22.53.22H19.5a3 3 0 013 3v1.146A4.483 4.483 0 0019.5 9h-15a4.483 4.483 0 00-3 1.146z"
                />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M5.625 1.5H9a3.75 3.75 0 013.75 3.75v1.875c0 1.036.84 1.875 1.875 1.875H16.5a3.75 3.75 0 013.75 3.75v7.875c0 1.035-.84 1.875-1.875 1.875H5.625a1.875 1.875 0 01-1.875-1.875V3.375c0-1.036.84-1.875 1.875-1.875z"
                  clip-rule="evenodd"
                />
              </svg>
            </div>
          </span>
          <span class="col-name">
            <span class="file-name-text">{{ file.name }}</span>
            <span class="file-path-sub">{{ file.relativePath }}</span>
          </span>
          <span class="col-status">
            <span class="status-badge server">
              <span class="status-dot-sm"></span>
              服务端
            </span>
          </span>
          <span class="col-path mono">{{ file.relativePath }}</span>
          <span class="col-type">
            <span class="type-badge" :class="file.dir ? 'dir' : 'file'">
              {{ file.dir ? '文件夹' : getExt(file.name) }}
            </span>
          </span>
        </div>
      </div>
    </div>

    <!-- ─── CONTEXT MENU ─── -->
    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="ctx-menu"
        :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
        @click.stop
      >
        <div class="ctx-file-info">
          <div class="ctx-icon" :class="contextMenu.file?.dir ? 'ctx-dir' : 'ctx-file-ic'">
            <svg v-if="contextMenu.file?.dir" viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M19.5 21a3 3 0 003-3v-4.5a3 3 0 00-3-3h-15a3 3 0 00-3 3V18a3 3 0 003 3h15zM1.5 10.146V6a3 3 0 013-3h5.379a2.25 2.25 0 011.59.659l2.122 2.121c.14.141.331.22.53.22H19.5a3 3 0 013 3v1.146A4.483 4.483 0 0019.5 9h-15a4.483 4.483 0 00-3 1.146z"
              />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M5.625 1.5H9a3.75 3.75 0 013.75 3.75v1.875c0 1.036.84 1.875 1.875 1.875H16.5a3.75 3.75 0 013.75 3.75v7.875c0 1.035-.84 1.875-1.875 1.875H5.625a1.875 1.875 0 01-1.875-1.875V3.375c0-1.036.84-1.875 1.875-1.875z"
                clip-rule="evenodd"
              />
            </svg>
          </div>
          <p class="ctx-filename">{{ contextMenu.file?.name }}</p>
        </div>
        <div class="ctx-sep"></div>
        <button v-if="contextMenu.file?.dir" class="ctx-item" @click="ctxEnter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
          进入文件夹
        </button>
        <button class="ctx-item" @click="ctxDownload">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"
            />
          </svg>
          下行同步到本地
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import HttpManager from '../utils/request'

const router = useRouter()
const route = useRoute()

const groupId = computed(() => route.params.groupId)
const scopeName = computed(() => route.params.scopeName)
const scopeDisplayName = computed(() => {
  const s = scopeName.value || ''
  const idx = s.indexOf('/')
  return idx >= 0 ? s.slice(idx + 1) : s
})
const groupName = computed(() => route.query.groupName || '')

const allFiles = ref([]) // flat list of GroupFileNode from server
const isLoading = ref(false)
const viewMode = ref('grid')
const selectedPath = ref(null)
const isDownloading = ref(false)

// current path within the scope ('' = root)
const pathStack = reactive([]) // segments, e.g. ['subfolder', 'nested']
const currentPath = computed(() => pathStack.join('/'))

// files visible at current level: direct children of currentPath
const visibleFiles = computed(() => {
  const prefix = currentPath.value ? currentPath.value + '/' : ''
  return allFiles.value.filter((f) => {
    if (!f.relativePath.startsWith(prefix)) return false
    const rest = f.relativePath.slice(prefix.length)
    return rest.length > 0 && !rest.includes('/')
  })
})

const loadFiles = async () => {
  isLoading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const res = await HttpManager.post('/client/group/files', { email: userInfo.email })
    const groups = Array.isArray(res?.data ?? res) ? (res?.data ?? res) : []
    const group = groups.find((g) => g.id === groupId.value)
    if (group) {
      const scope = (group.scopes || []).find((s) => s.scopeName === scopeName.value)
      allFiles.value = scope?.files || []
    } else {
      allFiles.value = []
    }
  } catch (err) {
    console.error('加载群组文件失败', err)
    allFiles.value = []
  } finally {
    isLoading.value = false
  }
}

const openFile = (file) => {
  if (file.dir) {
    pathStack.push(file.name)
    selectedPath.value = null
  }
}

const goBack = () => {
  if (pathStack.length > 0) {
    pathStack.pop()
    selectedPath.value = null
  }
}

const navigateToRoot = () => {
  pathStack.splice(0)
  selectedPath.value = null
}

const navigateToBreadcrumb = (idx) => {
  pathStack.splice(idx + 1)
  selectedPath.value = null
}

const downloadCurrent = async (relativePath = currentPath.value) => {
  if (isDownloading.value) return
  isDownloading.value = true

  let localPath = ''
  if (window.electron?.ipcRenderer) {
    localPath = await window.electron.ipcRenderer.invoke('select-folder')
    if (!localPath) {
      isDownloading.value = false
      return
    }
  } else {
    localPath = prompt(`请输入本地保存路径（${scopeName.value}）：`)
    if (!localPath) {
      isDownloading.value = false
      return
    }
  }

  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  try {
    await HttpManager.post('/client/group/download-scope', {
      email: userInfo.email,
      scopeName: scopeName.value,
      localPath,
      relativePath
    })
  } catch (err) {
    console.error('下行同步失败', err)
  } finally {
    isDownloading.value = false
  }
}

// ── Context menu ──────────────────────────────────────────
const contextMenu = reactive({ visible: false, x: 0, y: 0, file: null })

const showContext = (file, event) => {
  selectedPath.value = file.relativePath
  contextMenu.file = file
  const menuW = 200,
    menuH = 120
  contextMenu.x = Math.min(event.clientX, window.innerWidth - menuW - 8)
  contextMenu.y = Math.min(event.clientY, window.innerHeight - menuH - 8)
  contextMenu.visible = true
}
const closeContextMenu = () => {
  contextMenu.visible = false
}

const ctxEnter = () => {
  if (contextMenu.file?.dir) openFile(contextMenu.file)
  closeContextMenu()
}

const ctxDownload = async () => {
  const targetPath = contextMenu.file?.relativePath || currentPath.value
  closeContextMenu()
  await downloadCurrent(targetPath)
}

// ── Utils ─────────────────────────────────────────────────
const getExt = (name) => {
  if (!name) return '文件'
  const dot = name.lastIndexOf('.')
  return dot >= 0 ? name.slice(dot + 1).toUpperCase() : '文件'
}

onMounted(loadFiles)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=IBM+Plex+Mono:wght@400;500&display=swap');

*,
*::before,
*::after {
  box-sizing: border-box;
}

.explorer-root {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8fafd;
  font-family: 'Syne', sans-serif;
  color: #202124;
}

/* ─── HEADER ─── */
.exp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 54px;
  padding: 0 16px;
  background: #ffffff;
  border-bottom: 1px solid #dadce0;
  flex-shrink: 0;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nav-buttons {
  display: flex;
  gap: 4px;
}
.nav-btn {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: #ffffff;
  border: 1px solid #dadce0;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.nav-btn svg {
  width: 14px;
  height: 14px;
}
.nav-btn:hover:not(:disabled) {
  background: #e8f0fe;
  color: #1a73e8;
  border-color: #d2e3fc;
}
.nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: nowrap;
  overflow: hidden;
}
.crumb-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.crumb {
  font-size: 13px;
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
}
.crumb-link {
  background: none;
  border: none;
  color: #5f6368;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}
.crumb-link:hover {
  color: #1a73e8;
  background: #e8f0fe;
}
.crumb-current {
  color: #202124;
  font-weight: 600;
}
.crumb-sep {
  width: 14px;
  height: 14px;
  color: #9aa0a6;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.view-toggle {
  display: flex;
  background: #f1f3f4;
  border: 1px solid #dadce0;
  border-radius: 7px;
  padding: 3px;
  gap: 2px;
}
.view-toggle button {
  width: 28px;
  height: 26px;
  border-radius: 5px;
  border: none;
  background: none;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.view-toggle button svg {
  width: 14px;
  height: 14px;
}
.view-toggle button.active {
  background: #ffffff;
  color: #1a73e8;
  box-shadow: 0 1px 2px rgba(60, 64, 67, 0.2);
}
.refresh-btn {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  background: #ffffff;
  border: 1px solid #dadce0;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.refresh-btn svg {
  width: 15px;
  height: 15px;
}
.refresh-btn:hover {
  background: #e8f0fe;
  color: #1a73e8;
}
.refresh-btn.spinning svg {
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ─── TOOLBAR ─── */
.exp-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}
.folder-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.group-badge {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #e8f0fe;
  color: #1a73e8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.group-badge svg {
  width: 17px;
  height: 17px;
}
.folder-name {
  font-size: 14px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 2px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.group-name-chip {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 99px;
  font-weight: 600;
  background: #e8f0fe;
  color: #1a73e8;
  border: 1px solid #d2e3fc;
}
.folder-path {
  font-size: 11px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
  margin: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stat-chip {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 20px;
  background: #f1f3f4;
  border: 1px solid #dadce0;
  color: #5f6368;
}
.download-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid #d2e3fc;
  background: #e8f0fe;
  color: #1a73e8;
  cursor: pointer;
  font-size: 12px;
  font-family: 'Syne', sans-serif;
  font-weight: 600;
  transition: all 0.15s;
  white-space: nowrap;
}
.download-btn:hover:not(.loading) {
  background: #d2e3fc;
  border-color: #aecbfa;
}
.download-btn.loading {
  opacity: 0.6;
  cursor: not-allowed;
}
.download-btn svg {
  width: 14px;
  height: 14px;
}
.spin-icon {
  animation: spin 0.8s linear infinite;
}

/* ─── CONTENT ─── */
.exp-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8fafd;
}
.center-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  gap: 14px;
  color: #9aa0a6;
}
.center-state svg {
  width: 56px;
  height: 56px;
}
.center-state p {
  font-size: 14px;
  color: #5f6368;
}
.loading-spinner {
  width: 38px;
  height: 38px;
  border: 3px solid #d2e3fc;
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

/* ─── GRID ─── */
.file-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-content: flex-start;
}
.file-tile {
  width: 110px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 8px 10px;
  gap: 8px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  transition:
    background 0.15s,
    border-color 0.15s;
}
.file-tile:hover {
  background: #f1f3f4;
}
.file-tile.selected {
  background: #e8f0fe;
  border-color: #d2e3fc;
}
.tile-icon {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tile-dir {
  background: #e8f0fe;
  color: #1a73e8;
}
.tile-file {
  background: #f3e8fd;
  color: #9334e6;
}
.tile-icon svg {
  width: 28px;
  height: 28px;
}
.tile-server-dot {
  position: absolute;
  top: 10px;
  right: 22px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #1a73e8;
  border: 2px solid #ffffff;
  box-shadow: 0 0 5px rgba(26, 115, 232, 0.35);
}
.tile-name {
  font-size: 12px;
  font-weight: 600;
  color: #202124;
  text-align: center;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
}
.tile-type {
  font-size: 10px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
  margin: 0;
  text-transform: uppercase;
}

/* ─── LIST ─── */
.file-list {
  display: flex;
  flex-direction: column;
}
.list-header {
  display: grid;
  grid-template-columns: 44px 1fr 90px 1fr 70px;
  align-items: center;
  padding: 8px 12px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  font-size: 11px;
  font-weight: 600;
  color: #5f6368;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  margin-bottom: 4px;
}
.list-row {
  display: grid;
  grid-template-columns: 44px 1fr 90px 1fr 70px;
  align-items: center;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition:
    background 0.12s,
    border-color 0.12s;
}
.list-row:hover {
  background: #f1f3f4;
}
.list-row.selected {
  background: #e8f0fe;
  border-color: #d2e3fc;
}
.row-icon {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.row-dir {
  background: #e8f0fe;
  color: #1a73e8;
}
.row-file {
  background: #f3e8fd;
  color: #9334e6;
}
.row-icon svg {
  width: 16px;
  height: 16px;
}
.col-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
  padding-right: 10px;
}
.file-name-text {
  font-size: 13px;
  font-weight: 600;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-path-sub {
  font-size: 10px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 600;
  padding: 3px 7px;
  border-radius: 20px;
}
.status-badge.server {
  background: #e8f0fe;
  color: #1a73e8;
}
.status-dot-sm {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
.mono {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 10px;
  color: #5f6368;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.type-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
}
.type-badge.dir {
  background: #e8f0fe;
  color: #1a73e8;
}
.type-badge.file {
  background: #f3e8fd;
  color: #9334e6;
}

/* ─── CONTEXT MENU ─── */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  min-width: 200px;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 12px;
  padding: 6px;
  box-shadow:
    0 12px 32px rgba(60, 64, 67, 0.28),
    0 1px 3px rgba(60, 64, 67, 0.16);
}
.ctx-file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px 10px;
}
.ctx-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ctx-dir {
  background: #e8f0fe;
  color: #1a73e8;
}
.ctx-file-ic {
  background: #f3e8fd;
  color: #9334e6;
}
.ctx-icon svg {
  width: 15px;
  height: 15px;
}
.ctx-filename {
  font-size: 12px;
  font-weight: 600;
  color: #202124;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 150px;
}
.ctx-sep {
  height: 1px;
  background: #e8eaed;
  margin: 4px 0;
}
.ctx-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: 7px;
  background: none;
  color: #3c4043;
  font-size: 13px;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;
}
.ctx-item svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}
.ctx-item:hover {
  background: #f1f3f4;
  color: #202124;
}
</style>
