<template>
  <div class="explorer-root" @click="closeContextMenu">
    <!-- ─── HEADER ─── -->
    <header class="exp-header">
      <div class="header-left">
        <div class="nav-buttons">
          <button
            class="nav-btn"
            :disabled="navStack.length <= 1"
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
          <span v-for="(crumb, i) in navStack" :key="crumb.fileId + '-' + i" class="crumb-wrap">
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
              v-if="i < navStack.length - 1"
              class="crumb crumb-link"
              @click="navigateToBreadcrumb(i)"
            >
              {{ crumb.name }}
            </button>
            <span v-else class="crumb crumb-current">{{ crumb.name }}</span>
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
          @click="loadCurrentLevel"
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
        <div class="folder-icon-sm">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
            />
          </svg>
        </div>
        <div>
          <p class="folder-name">{{ currentFolder.name }}</p>
          <p class="folder-path">{{ currentFolder.path || '/' }}</p>
        </div>
      </div>
      <div class="toolbar-stats">
        <span class="stat-chip">{{ files.length }} 项</span>
        <span class="stat-chip">{{ syncedCount }} 已同步</span>
        <span class="stat-chip pending-chip">{{ files.length - syncedCount }} 待同步</span>
      </div>
    </div>

    <!-- ─── CONTENT ─── -->
    <div class="exp-content" @contextmenu.prevent>
      <!-- Loading -->
      <div v-if="isLoading" class="center-state">
        <div class="loading-spinner"></div>
        <p>正在获取文件列表...</p>
      </div>

      <!-- Empty -->
      <div v-else-if="files.length === 0" class="center-state">
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
          v-for="file in files"
          :key="file.id"
          class="file-tile"
          :class="{ selected: selectedId === file.id }"
          @click.stop="selectFile(file)"
          @dblclick="openFile(file)"
          @contextmenu.stop.prevent="showContext(file, $event)"
        >
          <div class="tile-icon" :class="file.isDir ? 'tile-dir' : 'tile-file'">
            <svg v-if="file.isDir" viewBox="0 0 24 24" fill="currentColor">
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
          <div
            class="tile-sync-dot"
            :class="file.isSync ? 'synced' : 'pending'"
            :title="file.isSync ? '已同步' : '待同步'"
          ></div>
          <p class="tile-name" :title="fileName(file.relativePath)">
            {{ fileName(file.relativePath) }}
          </p>
          <p class="tile-time">{{ formatTime(file.updateTime) }}</p>
        </div>
      </div>

      <!-- LIST VIEW -->
      <div v-else class="file-list">
        <div class="list-header">
          <span class="col-icon"></span>
          <span class="col-name">名称</span>
          <span class="col-status">状态</span>
          <span class="col-depth">层级</span>
          <span class="col-time">更新时间</span>
          <span class="col-type">类型</span>
        </div>
        <div
          v-for="file in files"
          :key="file.id"
          class="list-row"
          :class="{ selected: selectedId === file.id }"
          @click.stop="selectFile(file)"
          @dblclick="openFile(file)"
          @contextmenu.stop.prevent="showContext(file, $event)"
        >
          <span class="col-icon">
            <div class="row-icon" :class="file.isDir ? 'row-dir' : 'row-file'">
              <svg v-if="file.isDir" viewBox="0 0 24 24" fill="currentColor">
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
          </span>
          <span class="col-name">
            <span class="file-name-text">{{ fileName(file.relativePath) }}</span>
            <span class="file-path-sub">{{ file.relativePath }}</span>
          </span>
          <span class="col-status">
            <span class="status-badge" :class="file.isSync ? 'ok' : 'pending'">
              <span class="status-dot-sm"></span>
              {{ file.isSync ? '已同步' : '待同步' }}
            </span>
          </span>
          <span class="col-depth mono">L{{ file.depth }}</span>
          <span class="col-time mono">{{ formatTime(file.updateTime) }}</span>
          <span class="col-type">
            <span class="type-badge" :class="file.isDir ? 'dir' : 'file'">
              {{ file.isDir ? '文件夹' : '文件' }}
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
          <div class="ctx-icon" :class="contextMenu.file?.isDir ? 'ctx-dir' : 'ctx-file-ic'">
            <svg v-if="contextMenu.file?.isDir" viewBox="0 0 24 24" fill="currentColor">
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
          <p class="ctx-filename">
            {{ contextMenu.file ? fileName(contextMenu.file.relativePath) : '' }}
          </p>
        </div>

        <div class="ctx-sep"></div>

        <button class="ctx-item" @click="ctxOpen">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M13.5 6H5.25A2.25 2.25 0 003 8.25v10.5A2.25 2.25 0 005.25 21h10.5A2.25 2.25 0 0018 18.75V10.5m-10.5 6L21 3m0 0h-5.25M21 3v5.25"
            />
          </svg>
          使用系统默认工具打开
        </button>

        <button class="ctx-item" @click="ctxUpload">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
            />
          </svg>
          立刻上行同步
        </button>

        <button class="ctx-item" @click="ctxDownload">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"
            />
          </svg>
          立刻下行同步
        </button>

        <div class="ctx-sep"></div>

        <button class="ctx-item danger" @click="ctxDelete">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
            />
          </svg>
          删除
        </button>
      </div>
    </Teleport>

    <!-- ─── DELETE CONFIRM ─── -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
        <div class="confirm-panel">
          <div class="confirm-icon">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path
                fill-rule="evenodd"
                d="M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003zM12 8.25a.75.75 0 01.75.75v3.75a.75.75 0 01-1.5 0V9a.75.75 0 01.75-.75zm0 8.25a.75.75 0 100-1.5.75.75 0 000 1.5z"
                clip-rule="evenodd"
              />
            </svg>
          </div>
          <h3>确认删除?</h3>
          <p>
            即将删除 <strong>{{ deleteTarget ? fileName(deleteTarget.relativePath) : '' }}</strong
            >，此操作不可撤销。
          </p>
          <div class="confirm-actions">
            <button class="btn-cancel" @click="showDeleteConfirm = false">取消</button>
            <button class="btn-danger" @click="confirmDelete">确认删除</button>
          </div>
        </div>
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

// ── State ──────────────────────────────────────────────────
const files = ref([])
const isLoading = ref(false)
const viewMode = ref('grid')
const selectedId = ref(null)

/**
 * navStack 是导航历史栈，每一项记录：
 *   { fileId: number, name: string, path: string }
 *
 * navStack[0] 永远是根节点（由路由 params 传入的 fileId 和 alias）。
 * originalId 始终等于 navStack[0].fileId，用于 detail-list-parent 接口。
 */
const navStack = reactive([])

// ── Computed ───────────────────────────────────────────────
// 当前正在展示的层级条目（栈顶）
const currentFolder = computed(() => navStack[navStack.length - 1] ?? { name: '文件夹', path: '' })

// 根节点 fileId（路由参数，整个会话固定不变）
const originalId = computed(() => navStack[0]?.fileId ?? route.params.fileId)

const syncedCount = computed(() => files.value.filter((f) => f.isSync).length)

const userEmail = computed(() => {
  const info = localStorage.getItem('userInfo')
  return info ? JSON.parse(info).email : ''
})

// ── API ────────────────────────────────────────────────────
/**
 * 加载根目录（首次进入页面）
 * 使用原接口 /client/file/detail-list
 */
const loadRootFiles = async () => {
  isLoading.value = true
  try {
    const res = await HttpManager.post('/client/file/detail-list', {
      fileId: originalId.value
    })
    const data = res?.data ?? res
    files.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('加载根目录失败:', err)
    files.value = mockFiles(originalId.value)
  } finally {
    isLoading.value = false
  }
}

/**
 * 加载子目录
 * 使用新接口 /client/file/detail-list-parent
 * @param {number} fileId  - 当前文件夹的 id
 */
const loadChildFiles = async (fileId) => {
  isLoading.value = true
  try {
    const res = await HttpManager.post('/client/file/detail-list-parent', {
      fileId: String(fileId),
      originalId: String(originalId.value)
    })
    const data = res?.data ?? res
    files.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('加载子目录失败:', err)
    files.value = mockFiles(fileId)
  } finally {
    isLoading.value = false
  }
}

/**
 * 刷新当前层级
 */
const loadCurrentLevel = () => {
  if (navStack.length <= 1) {
    loadRootFiles()
  } else {
    loadChildFiles(currentFolder.value.fileId)
  }
}

// ── Navigation ────────────────────────────────────────────
const selectFile = (file) => {
  selectedId.value = file.id
}

const openFile = async (file) => {
  if (file.isDir) {
    // 文件夹的打开逻辑是正确的，保持不变
    navStack.push({
      fileId: file.id,
      name: fileName(file.relativePath),
      path: file.relativePath
    })
    selectedId.value = null
    await loadChildFiles(file.id)
  } else {
    // 修正文件打开逻辑
    if (window.electron?.ipcRenderer) {
      // 1. 获取同步文件夹的根路径 (绝对路径)
      // 这个路径在页面加载时从路由参数中获取，并存储在导航栈的第一个元素中
      const basePath = navStack[0]?.path

      if (!basePath) {
        console.error('文件根路径未设置，无法构造完整路径来打开文件。')
        // 可以在这里添加一个用户可见的错误提示，例如使用 Toast 通知
        return
      }

      // 2. 将【根路径】和【文件相对路径】作为一个对象发送给主进程
      // 这是最稳妥的做法，让主进程负责拼接，以兼容不同操作系统
      const pathPayload = {
        basePath: basePath,
        relativePath: file.relativePath
      }
      console.log('正在请求打开文件:', pathPayload)
      window.electron.ipcRenderer.send('open-file', pathPayload)
    } else {
      console.warn('非 Electron 环境，无法调用 "open-file"。')
    }
  }
}

const goBack = async () => {
  if (navStack.length <= 1) {
    router.push('/dashboard')
    return
  }
  navStack.pop()
  selectedId.value = null
  loadCurrentLevel()
}

/**
 * 点击面包屑跳转到指定层级
 * @param {number} idx - navStack 中的索引
 */
const navigateToBreadcrumb = async (idx) => {
  navStack.splice(idx + 1) // 保留 0..idx，截断后面的
  selectedId.value = null
  loadCurrentLevel()
}

// ── Context menu ──────────────────────────────────────────
const contextMenu = reactive({ visible: false, x: 0, y: 0, file: null })

const showContext = (file, event) => {
  selectedId.value = file.id
  contextMenu.file = file
  const menuW = 220,
    menuH = 220
  contextMenu.x = Math.min(event.clientX, window.innerWidth - menuW - 8)
  contextMenu.y = Math.min(event.clientY, window.innerHeight - menuH - 8)
  contextMenu.visible = true
}

const closeContextMenu = () => {
  contextMenu.visible = false
}

const ctxOpen = () => {
  if (contextMenu.file) openFile(contextMenu.file)
  closeContextMenu()
}

const ctxUpload = async () => {
  const file = contextMenu.file
  closeContextMenu()
  // 与 ctxDownload 保持一致，使用同步任务根路径
  const rootPath = navStack[0]?.path || file.relativePath
  try {
    await HttpManager.post(
      '/client/sync/upload',
      {
        fileId: originalId.value,
        email: userEmail.value,
        path: rootPath
      },
      { timeout: 0 }
    )
    const f = files.value.find((f) => f.id === file.id)
    if (f) f.isSync = true
  } catch (err) {
    console.error('上行同步失败', err)
  }
}

const ctxDownload = async () => {
  const file = contextMenu.file
  closeContextMenu()
  // 使用同步任务的根路径（navStack[0].path）而非子文件的相对路径，
  // 保证客户端能定位到正确的 File 记录并与服务端范围名匹配。
  const rootPath = navStack[0]?.path || file.relativePath
  try {
    await HttpManager.post(
      '/client/sync/download',
      {
        fileId: originalId.value,
        email: userEmail.value,
        path: rootPath
      },
      { timeout: 0 }
    )
    const f = files.value.find((f) => f.id === file.id)
    if (f) f.isSync = true
  } catch (err) {
    console.error('下行同步失败', err)
  }
}

// ── Delete ────────────────────────────────────────────────
const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)

const ctxDelete = () => {
  deleteTarget.value = contextMenu.file
  closeContextMenu()
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  if (!deleteTarget.value) return
  try {
    await HttpManager.post('/client/file/delete', {
      email: userEmail.value,
      path: deleteTarget.value.relativePath
    })
  } catch (err) {
    console.error('删除请求失败（乐观移除）', err)
  } finally {
    files.value = files.value.filter((f) => f.id !== deleteTarget.value.id)
    showDeleteConfirm.value = false
    deleteTarget.value = null
  }
}

// ── Utils ─────────────────────────────────────────────────
const fileName = (path) => {
  if (!path) return '未知'
  return path.replace(/\\/g, '/').split('/').filter(Boolean).pop() || path
}

const formatTime = (t) => {
  if (!t) return '—'
  try {
    return new Date(t).toLocaleString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return t
  }
}

// Mock 数据（接口异常时兜底）
const mockFiles = (parentId) => [
  {
    id: 101,
    fileId: Number(parentId),
    relativePath: 'reports/Q1_Report.pdf',
    depth: 2,
    isDir: false,
    isSync: true,
    updateTime: new Date().toISOString()
  },
  {
    id: 102,
    fileId: Number(parentId),
    relativePath: 'images',
    depth: 2,
    isDir: true,
    isSync: false,
    updateTime: new Date().toISOString()
  },
  {
    id: 103,
    fileId: Number(parentId),
    relativePath: 'data.xlsx',
    depth: 2,
    isDir: false,
    isSync: true,
    updateTime: new Date().toISOString()
  },
  {
    id: 104,
    fileId: Number(parentId),
    relativePath: 'backup',
    depth: 2,
    isDir: true,
    isSync: true,
    updateTime: new Date().toISOString()
  },
  {
    id: 105,
    fileId: Number(parentId),
    relativePath: 'notes.txt',
    depth: 2,
    isDir: false,
    isSync: false,
    updateTime: new Date().toISOString()
  }
]

// ── Lifecycle ─────────────────────────────────────────────
onMounted(() => {
  const rootFileId = route.params.fileId
  const rootName = route.query.alias || '文件夹'
  const rootPath = route.query.path || ''

  // 初始化栈根节点
  navStack.push({ fileId: rootFileId, name: rootName, path: rootPath })

  loadRootFiles()
})
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
.folder-icon-sm {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  background: #e8f0fe;
  color: #1a73e8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.folder-icon-sm svg {
  width: 16px;
  height: 16px;
}
.folder-name {
  font-size: 14px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 2px;
}
.folder-path {
  font-size: 11px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
  margin: 0;
}

.toolbar-stats {
  display: flex;
  gap: 8px;
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
.stat-chip.pending-chip {
  color: #b06000;
  background: #fef7e0;
  border-color: #fde293;
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

/* ─── GRID VIEW ─── */
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
  animation: tileIn 0.2s ease both;
}
@keyframes tileIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
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
  position: relative;
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

.tile-sync-dot {
  position: absolute;
  top: 10px;
  right: 22px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid #ffffff;
}
.tile-sync-dot.synced {
  background: #188038;
  box-shadow: 0 0 5px rgba(24, 128, 56, 0.35);
}
.tile-sync-dot.pending {
  background: #f9ab00;
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
.tile-time {
  font-size: 10px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
  margin: 0;
}

/* ─── LIST VIEW ─── */
.file-list {
  display: flex;
  flex-direction: column;
}
.list-header {
  display: grid;
  grid-template-columns: 44px 1fr 100px 60px 130px 80px;
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
  grid-template-columns: 44px 1fr 100px 60px 130px 80px;
  align-items: center;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition:
    background 0.12s,
    border-color 0.12s;
  animation: rowIn 0.2s ease both;
}
@keyframes rowIn {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
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
.status-badge.ok {
  background: #e6f4ea;
  color: #188038;
}
.status-badge.pending {
  background: #fef7e0;
  color: #b06000;
}
.status-dot-sm {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.mono {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 11px;
  color: #5f6368;
}
.col-depth.mono {
  font-size: 12px;
  color: #1a73e8;
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
  min-width: 220px;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 12px;
  padding: 6px;
  box-shadow:
    0 12px 32px rgba(60, 64, 67, 0.28),
    0 1px 3px rgba(60, 64, 67, 0.16);
  animation: ctxIn 0.12s ease;
}
@keyframes ctxIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
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
  max-width: 160px;
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
  transition:
    background 0.12s,
    color 0.12s;
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
.ctx-item.danger {
  color: #d93025;
}
.ctx-item.danger:hover {
  background: #fce8e6;
}

/* ─── DELETE CONFIRM ─── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 9998;
  background: rgba(60, 64, 67, 0.35);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.confirm-panel {
  width: 360px;
  background: #ffffff;
  border: 1px solid #fad2cf;
  border-radius: 16px;
  padding: 28px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(60, 64, 67, 0.3);
  animation: slideUp 0.2s ease;
}
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.confirm-icon {
  width: 52px;
  height: 52px;
  background: #fce8e6;
  border: 1px solid #fad2cf;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.confirm-icon svg {
  width: 24px;
  height: 24px;
  color: #d93025;
}
.confirm-panel h3 {
  font-size: 17px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 10px;
}
.confirm-panel p {
  font-size: 13px;
  color: #5f6368;
  margin: 0 0 24px;
  line-height: 1.6;
}
.confirm-panel strong {
  color: #202124;
}
.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}
.btn-cancel {
  padding: 9px 22px;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  color: #5f6368;
  font-size: 13px;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
}
.btn-cancel:hover {
  background: #f1f3f4;
}
.btn-danger {
  padding: 9px 22px;
  background: #fce8e6;
  border: 1px solid #fad2cf;
  border-radius: 8px;
  color: #d93025;
  font-size: 13px;
  font-weight: 700;
  font-family: 'Syne', sans-serif;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-danger:hover {
  background: #fad2cf;
}
</style>
