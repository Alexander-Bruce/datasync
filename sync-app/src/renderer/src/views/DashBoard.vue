<template>
  <div class="app-root">
    <aside class="drive-sidebar">
      <div class="brand-row">
        <div class="brand-mark">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01"
            />
          </svg>
        </div>
        <div class="brand-copy">
          <span class="brand-name">DataSync</span>
          <span class="brand-sub">Desktop</span>
        </div>
      </div>

      <button class="create-sync-btn" @click="openAdd">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        <span>新建同步</span>
      </button>

      <nav class="nav-stack">
        <button class="nav-item active" type="button">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M3.75 6.75A2.25 2.25 0 016 4.5h12a2.25 2.25 0 012.25 2.25v10.5A2.25 2.25 0 0118 19.5H6a2.25 2.25 0 01-2.25-2.25V6.75z"
            />
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M8.25 9.75h7.5M8.25 14.25h4.5"
            />
          </svg>
          <span>同步首页</span>
        </button>
        <button class="nav-item" type="button" @click="router.push('/groups')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zM12 12.75a5.995 5.995 0 00-5.058 2.772A6.062 6.062 0 006 18.719 11.944 11.944 0 0012 21c2.17 0 4.207-.576 5.963-1.584a5.971 5.971 0 00-.904-3.894A5.995 5.995 0 0012 12.75z"
            />
          </svg>
          <span>共享群组</span>
        </button>
        <button class="nav-item" type="button" @click="router.push('/logs')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M9 12h6m-6 3h6m-6-6h6M5.25 4.5h13.5A1.5 1.5 0 0120.25 6v12A1.5 1.5 0 0118.75 19.5H5.25A1.5 1.5 0 013.75 18V6a1.5 1.5 0 011.5-1.5z"
            />
          </svg>
          <span>同步活动</span>
        </button>
      </nav>

      <div class="sidebar-section">
        <p class="sidebar-label">策略</p>
        <button
          v-for="filter in policyFilters"
          :key="filter.key"
          class="policy-filter"
          :class="{ active: activePolicyFilter === filter.key }"
          type="button"
          @click="activePolicyFilter = filter.key"
        >
          <span class="policy-dot" :class="filter.key"></span>
          <span>{{ filter.label }}</span>
          <strong>{{ policyCount(filter.key) }}</strong>
        </button>
      </div>

      <div class="storage-summary">
        <div class="storage-head">
          <span>同步空间</span>
          <strong>{{ syncedCount }}/{{ tasks.length }}</strong>
        </div>
        <div class="storage-bar">
          <span :style="{ width: syncedPercent + '%' }"></span>
        </div>
        <p>{{ pendingCount }} 个任务等待同步</p>
      </div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div class="search-box">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M21 21l-4.35-4.35M10.5 18a7.5 7.5 0 100-15 7.5 7.5 0 000 15z"
            />
          </svg>
          <input v-model="searchQuery" type="search" placeholder="搜索同步任务、路径或群组" />
        </div>

        <div class="topbar-actions">
          <button class="icon-text-btn" type="button" @click="syncPaused = !syncPaused">
            <svg
              v-if="syncPaused"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 010 1.972l-11.54 6.347a1.125 1.125 0 01-1.667-.986V5.653z"
              />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M15.75 5.25v13.5m-7.5-13.5v13.5"
              />
            </svg>
            <span>{{ syncPaused ? '继续同步' : '暂停同步' }}</span>
          </button>
          <button
            class="icon-btn"
            type="button"
            :class="{ spinning: isLoading }"
            title="刷新"
            @click="loadTasks"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M16.023 9.348h4.992M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
              />
            </svg>
          </button>

          <div ref="userMenuRef" class="user-menu">
            <button class="user-trigger" type="button" @click="showUserMenu = !showUserMenu">
              <span class="user-name">{{ currentUser.username || '用户' }}</span>
              <div class="avatar-ring">
                <img
                  :src="
                    currentUser.avatar ||
                    `https://api.dicebear.com/7.x/avataaars/svg?seed=${currentUser.username || currentUser.email}`
                  "
                  alt="avatar"
                />
              </div>
              <svg
                class="chevron"
                :class="{ open: showUserMenu }"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M19.5 8.25l-7.5 7.5-7.5-7.5"
                />
              </svg>
            </button>

            <div v-if="showUserMenu" class="user-dropdown">
              <div class="dropdown-head">
                <p>{{ currentUser.email }}</p>
              </div>
              <button class="dropdown-item" type="button" @click="openProfile">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.893.149c-.425.07-.765.383-.93.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.397.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.273-.806.108-1.204-.165-.397-.505-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.107-1.204l-.527-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z"
                  />
                </svg>
                <span>账号设置</span>
              </button>
              <button class="dropdown-item danger" type="button" @click="handleLogout">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9"
                  />
                </svg>
                <span>退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <main class="workspace-main">
        <section class="content-column">
          <div class="page-title-row">
            <div>
              <h1>同步任务</h1>
              <p>管理本地文件夹、同步策略与共享空间。</p>
            </div>
            <button class="primary-btn" type="button" @click="openAdd">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              <span>新建任务</span>
            </button>
          </div>

          <div class="metric-strip">
            <div class="metric-item">
              <span>总任务</span>
              <strong>{{ tasks.length }}</strong>
            </div>
            <div class="metric-item success">
              <span>已同步</span>
              <strong>{{ syncedCount }}</strong>
            </div>
            <div class="metric-item warning">
              <span>待同步</span>
              <strong>{{ pendingCount }}</strong>
            </div>
            <div class="metric-item blue">
              <span>运行中</span>
              <strong>{{ activeSyncCount }}</strong>
            </div>
          </div>

          <div v-if="isLoading" class="center-state">
            <div class="loading-spinner"></div>
            <p>正在加载任务列表...</p>
          </div>

          <div v-else-if="loadError" class="center-state error">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 9v3.75m0 3.75h.007M2.697 16.126L10.05 3.378c.866-1.5 3.032-1.5 3.898 0l7.355 12.748c.866 1.5-.217 3.374-1.948 3.374H4.645c-1.73 0-2.813-1.874-1.948-3.374z"
              />
            </svg>
            <p>{{ loadError }}</p>
            <button type="button" @click="loadTasks">重试</button>
          </div>

          <div v-else class="task-panel">
            <div class="task-panel-head">
              <div>
                <h2>我的同步文件夹</h2>
                <p>当前显示 {{ filteredTasks.length }} 个任务</p>
              </div>
              <span class="sync-state" :class="{ paused: syncPaused }">
                {{ syncPaused ? '已暂停' : '同步可用' }}
              </span>
            </div>

            <div v-if="filteredTasks.length === 0" class="empty-list">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75M4.5 9.75V6A2.25 2.25 0 016.75 3.75h4.379a1.5 1.5 0 011.06.44l2.122 2.12a1.5 1.5 0 001.06.44h1.879A2.25 2.25 0 0119.5 9v.75"
                />
              </svg>
              <p>没有匹配的同步任务</p>
              <button type="button" class="restore-btn" @click="openRestoreModal">
                从服务器恢复任务
              </button>
            </div>

            <div v-else class="task-list">
              <div class="list-header">
                <span>名称</span>
                <span>策略</span>
                <span>状态</span>
                <span>操作</span>
              </div>

              <article
                v-for="task in filteredTasks"
                :key="task.id"
                class="task-row"
                :class="{ active: syncStatus[task.id], synced: task.isSync }"
              >
                <button class="task-main" type="button" @click="navigateInto(task)">
                  <span class="task-icon" :class="task.isDir ? 'folder' : 'file'">
                    <svg v-if="task.isDir" viewBox="0 0 24 24" fill="currentColor">
                      <path
                        d="M19.5 21a3 3 0 003-3v-4.5a3 3 0 00-3-3h-15a3 3 0 00-3 3V18a3 3 0 003 3h15zM1.5 10.146V6a3 3 0 013-3h5.379a2.25 2.25 0 011.59.659l2.122 2.121c.14.141.331.22.53.22H19.5a3 3 0 013 3v1.146A4.483 4.483 0 0019.5 9h-15a4.483 4.483 0 00-3 1.146z"
                      />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" fill="currentColor">
                      <path
                        d="M5.625 1.5h6.879c.298 0 .585.119.796.33l5.37 5.37c.211.211.33.498.33.796v12.629A1.875 1.875 0 0117.125 22.5h-11.5A1.875 1.875 0 013.75 20.625V3.375A1.875 1.875 0 015.625 1.5z"
                      />
                    </svg>
                  </span>
                  <span class="task-copy">
                    <strong :title="task.alias">{{ task.alias }}</strong>
                    <small :title="task.path">{{ task.path }}</small>
                  </span>
                </button>

                <div class="task-policy">
                  <span class="strategy-chip" :class="taskPolicyKind(task)">{{
                    taskPolicyLabel(task)
                  }}</span>
                  <small>{{ taskPolicyDetail(task) }}</small>
                </div>

                <div class="task-status">
                  <span class="status-badge" :class="task.isSync ? 'ok' : 'pending'">
                    {{ task.isSync ? '已同步' : '待同步' }}
                  </span>
                  <small>{{ formatTime(task.updateTime) }}</small>
                </div>

                <div class="task-actions">
                  <button
                    class="icon-btn small"
                    type="button"
                    :disabled="syncPaused || syncStatus[task.id] === 'up'"
                    title="上行同步"
                    @click.stop="syncUpload(task)"
                  >
                    <svg
                      v-if="syncStatus[task.id] === 'up'"
                      class="spin-icon"
                      viewBox="0 0 24 24"
                      fill="none"
                    >
                      <circle
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        stroke-width="3"
                        opacity="0.25"
                      />
                      <path
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                        opacity="0.75"
                      />
                    </svg>
                    <svg
                      v-else
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.8"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                      />
                    </svg>
                  </button>
                  <button
                    class="icon-btn small"
                    type="button"
                    title="修改策略"
                    @click.stop="openEdit(task)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z"
                      />
                    </svg>
                  </button>
                  <button
                    class="icon-btn small danger"
                    type="button"
                    title="删除任务"
                    @click.stop="confirmDeleteTask(task)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166M4.772 5.79a48.108 48.108 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0m-3.478.397L5.84 19.673a2.25 2.25 0 002.244 2.077h7.832a2.25 2.25 0 002.244-2.077l1.068-13.883"
                      />
                    </svg>
                  </button>
                </div>

                <div v-if="syncSuccess[task.id]" class="sync-flash">同步成功</div>
              </article>
            </div>
          </div>

          <section v-if="filteredGroupFiles.length > 0" class="shared-section">
            <div class="section-line-head">
              <div>
                <h2>共享空间</h2>
                <p>{{ groupScopeCount }} 个共享文件夹</p>
              </div>
              <button class="text-btn" type="button" @click="router.push('/groups')">
                管理群组
              </button>
            </div>

            <div class="scope-list">
              <article v-for="group in filteredGroupFiles" :key="group.id" class="group-row">
                <div class="group-title">
                  <strong>{{ group.name }}</strong>
                  <span>{{ group.ownerEmail }}</span>
                </div>
                <div class="scope-chips">
                  <button
                    v-for="scope in group.scopes || []"
                    :key="scope.scopeName"
                    class="scope-chip"
                    type="button"
                    @click="navigateToScope(group, scope)"
                  >
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path
                        d="M19.5 21a3 3 0 003-3v-4.5a3 3 0 00-3-3h-15a3 3 0 00-3 3V18a3 3 0 003 3h15zM1.5 10.146V6a3 3 0 013-3h5.379a2.25 2.25 0 011.59.659l2.122 2.121c.14.141.331.22.53.22H19.5a3 3 0 013 3v1.146A4.483 4.483 0 0019.5 9h-15a4.483 4.483 0 00-3 1.146z"
                      />
                    </svg>
                    <span>{{ scopeDisplayName(scope.scopeName) }}</span>
                    <small>{{ (scope.files || []).length }}</small>
                  </button>
                </div>
              </article>
            </div>
          </section>
        </section>

        <aside class="activity-pane">
          <section class="activity-card">
            <div class="activity-head">
              <h2>同步状态</h2>
              <span
                class="status-light"
                :class="{ paused: syncPaused, active: activeSyncCount > 0 }"
              ></span>
            </div>
            <div class="sync-overview">
              <strong>{{
                syncPaused ? '已暂停' : activeSyncCount > 0 ? '正在同步' : '保持最新'
              }}</strong>
              <p>{{ pendingCount === 0 ? '全部任务已同步' : `${pendingCount} 个任务需要处理` }}</p>
            </div>
            <button
              class="wide-btn"
              type="button"
              :disabled="syncPaused || pendingTasks.length === 0"
              @click="syncAllPending"
            >
              同步待处理任务
            </button>
          </section>

          <section class="activity-card">
            <div class="activity-head">
              <h2>最近活动</h2>
              <button class="text-btn compact" type="button" @click="router.push('/logs')">
                日志
              </button>
            </div>
            <div class="activity-list">
              <div v-for="task in recentActivities" :key="task.id" class="activity-item">
                <span class="activity-dot" :class="task.isSync ? 'ok' : 'pending'"></span>
                <div>
                  <strong>{{ task.alias }}</strong>
                  <small
                    >{{ task.isSync ? '最近完成同步' : '等待同步' }} ·
                    {{ formatTime(task.updateTime) }}</small
                  >
                </div>
              </div>
              <p v-if="recentActivities.length === 0" class="muted-empty">暂无活动</p>
            </div>
          </section>

          <section class="activity-card">
            <div class="activity-head">
              <h2>策略概览</h2>
            </div>
            <div class="policy-overview">
              <div v-for="filter in policyFilters.slice(1)" :key="filter.key">
                <span>{{ filter.label }}</span>
                <strong>{{ policyCount(filter.key) }}</strong>
              </div>
            </div>
          </section>
        </aside>
      </main>
    </section>

    <Teleport to="body">
      <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
        <div class="modal-panel">
          <div class="modal-hd">
            <div>
              <h3>{{ isEdit ? '修改同步任务' : '新建同步任务' }}</h3>
              <p>{{ syncStrategyMeta }}</p>
            </div>
            <button class="modal-close" type="button" title="关闭" @click="showModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="form-grid">
              <div class="field-group">
                <label class="field-label">任务别名</label>
                <input
                  v-model="form.alias"
                  type="text"
                  class="ds-input"
                  placeholder="给任务起个名字"
                />
              </div>
              <div class="field-group">
                <label class="field-label">CDC 算法</label>
                <select v-model="form.cdcAlg" class="ds-input">
                  <option value="FlipCDC">FlipCDC</option>
                  <option value="RabinCDC">RabinCDC</option>
                  <option value="FastCDC">FastCDC</option>
                  <option value="QuickCDC">QuickCDC</option>
                </select>
              </div>
            </div>

            <div class="field-group">
              <label class="field-label">本地路径</label>
              <div class="path-row">
                <input
                  v-model="form.path"
                  type="text"
                  class="ds-input"
                  :placeholder="form.isDir ? '选择本地文件夹...' : '选择本地文件...'"
                  readonly
                />
                <button class="browse-btn" type="button" @click="selectLocalPath">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75M4.5 9.75V6A2.25 2.25 0 016.75 3.75h4.379a1.5 1.5 0 011.06.44l2.122 2.12a1.5 1.5 0 001.06.44h1.879A2.25 2.25 0 0119.5 9v.75"
                    />
                  </svg>
                  <span>{{ form.isDir ? '浏览文件夹' : '浏览文件' }}</span>
                </button>
              </div>
            </div>

            <div class="form-grid">
              <div class="field-group">
                <label class="field-label">远程主机</label>
                <input
                  v-model="form.remoteHost"
                  type="text"
                  class="ds-input"
                  placeholder="192.168.1.100:8080"
                />
              </div>
              <div class="field-group">
                <label class="field-label">类型</label>
                <div class="type-toggle">
                  <button type="button" :class="{ active: form.isDir }" @click="setTaskType(true)">
                    文件夹
                  </button>
                  <button
                    type="button"
                    :class="{ active: !form.isDir }"
                    @click="setTaskType(false)"
                  >
                    单文件
                  </button>
                </div>
              </div>
            </div>

            <div class="sync-designer">
              <div class="strategy-list">
                <button
                  v-for="strategy in strategyOptions"
                  :key="strategy.key"
                  class="strategy-option"
                  :class="{ active: syncStrategy === strategy.key }"
                  type="button"
                  @click="setSyncStrategy(strategy.key)"
                >
                  <span class="strategy-dot" :class="strategy.key"></span>
                  <span>
                    <strong>{{ strategy.label }}</strong>
                    <small>{{ strategy.caption }}</small>
                  </span>
                </button>
              </div>

              <div class="strategy-panel">
                <div class="panel-title">
                  <h4>{{ activeStrategy.label }}</h4>
                  <span>scheduled = {{ scheduledPreview || '手动' }}</span>
                </div>

                <div v-if="syncStrategy === 'warm'" class="schedule-grid">
                  <label>
                    <span>同步间隔</span>
                    <input
                      v-model.number="warmAmount"
                      min="1"
                      max="999"
                      type="number"
                      class="ds-input"
                    />
                  </label>
                  <label>
                    <span>单位</span>
                    <select v-model="warmUnit" class="ds-input">
                      <option value="m">分钟</option>
                      <option value="h">小时</option>
                      <option value="d">天</option>
                    </select>
                  </label>
                </div>

                <div v-else class="strategy-note">
                  <strong>{{ activeStrategy.note }}</strong>
                  <span>{{ activeStrategy.valueText }}</span>
                </div>

                <div class="field-group">
                  <label class="field-label">描述</label>
                  <input
                    v-model="form.description"
                    type="text"
                    class="ds-input"
                    placeholder="任务说明（可选）"
                  />
                </div>
              </div>
            </div>
          </div>

          <div v-if="modalError" class="modal-error">{{ modalError }}</div>

          <div class="modal-ft">
            <button class="btn-secondary" type="button" @click="showModal = false">取消</button>
            <button class="btn-primary" type="button" :disabled="isSaving" @click="saveTask()">
              <svg v-if="isSaving" class="spin-icon" viewBox="0 0 24 24" fill="none">
                <circle
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  stroke-width="3"
                  opacity="0.25"
                />
                <path
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                  opacity="0.75"
                />
              </svg>
              <span>{{ isSaving ? '保存中...' : isEdit ? '保存修改' : '立即创建' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <ProfileModal
      :visible="showProfile"
      :user="currentUser"
      @close="showProfile = false"
      @updated="onUserUpdated"
    />

    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="cancelDeleteTask">
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
          <h3>确认删除任务?</h3>
          <p>
            即将删除任务
            <strong>{{ deleteTargetTask?.alias }}</strong> 及其所有子文件记录，此操作不可撤销。
          </p>
          <p v-if="deleteError" class="delete-error-msg">{{ deleteError }}</p>
          <div class="confirm-actions">
            <button class="btn-secondary" type="button" @click="cancelDeleteTask">取消</button>
            <button class="btn-danger" type="button" :disabled="isDeleting" @click="doDeleteTask">
              {{ isDeleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 远端任务恢复弹窗：刚装好客户端、本地数据库还没有 task 时，把 bucket 里历史的 scope 拉出来选择性恢复到本地 -->
    <Teleport to="body">
      <div v-if="showRestoreModal" class="modal-overlay" @click.self="closeRestoreModal">
        <div class="restore-panel">
          <h3>从服务器恢复任务</h3>
          <p class="restore-sub">
            下列任务在远端 bucket
            中已存在。勾选要拉回本地的任务，然后选择一个本地基础目录，每个任务会被恢复到
            <code>本地目录/任务别名/根名</code> 下并立即下行同步。
          </p>

          <div v-if="restoreLoading" class="restore-loading">
            <div class="loading-spinner"></div>
            <p>正在拉取远端任务列表...</p>
          </div>
          <p v-else-if="restoreError" class="delete-error-msg">{{ restoreError }}</p>
          <div v-else-if="remoteScopes.length === 0" class="restore-empty">
            远端没有该账号的任务，直接点上方"新建任务"开始即可。
          </div>
          <div v-else class="restore-list">
            <label
              v-for="scope in remoteScopes"
              :key="scope.scopeName"
              class="restore-row"
              :class="{ active: restoreSelection[scope.scopeName] }"
            >
              <input
                v-model="restoreSelection[scope.scopeName]"
                type="checkbox"
                :disabled="scope.localExists"
              />
              <span class="restore-icon" :class="scope.isDir ? 'folder' : 'file'"></span>
              <div class="restore-info">
                <strong>{{ scope.alias }}</strong>
                <small>{{ scope.rootName }} · {{ scope.isDir ? '文件夹' : '单文件' }}</small>
              </div>
              <small v-if="scope.localExists" class="restore-exists">本地已有</small>
            </label>
          </div>

          <div v-if="remoteScopes.length > 0" class="restore-path-row">
            <label class="field-label">本地基础目录</label>
            <div class="path-row">
              <input v-model="restoreBasePath" type="text" class="ds-input" readonly />
              <button type="button" class="browse-btn" @click="pickRestoreBasePath">
                浏览文件夹
              </button>
            </div>
          </div>

          <div v-if="restoreProgress" class="restore-progress">{{ restoreProgress }}</div>

          <div class="confirm-actions">
            <button class="btn-secondary" type="button" @click="closeRestoreModal">关闭</button>
            <button
              v-if="remoteScopes.length > 0"
              class="btn-primary"
              type="button"
              :disabled="restoreRunning || selectedRestoreCount === 0 || !restoreBasePath"
              @click="doRestore"
            >
              {{ restoreRunning ? '恢复中...' : `恢复 ${selectedRestoreCount} 个任务` }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager from '../utils/request'
import ProfileModal from '../components/Profilemodal.vue'

const router = useRouter()

const currentUser = reactive({ id: null, username: '', email: '', avatar: '' })
const showProfile = ref(false)
const showUserMenu = ref(false)
const userMenuRef = ref(null)

const openProfile = () => {
  showUserMenu.value = false
  showProfile.value = true
}
const onUserUpdated = (u) => Object.assign(currentUser, u)

const handleLogout = () => {
  localStorage.removeItem('authToken')
  localStorage.removeItem('userInfo')
  router.push('/')
}

const onClickOutside = (e) => {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserMenu.value = false
  }
}

const tasks = ref([])
const isLoading = ref(false)
const loadError = ref('')
const syncStatus = reactive({})
const syncSuccess = reactive({})
const syncPaused = ref(false)
const searchQuery = ref('')
const activePolicyFilter = ref('all')

const groupFiles = ref([])

const policyFilters = [
  { key: 'all', label: '全部' },
  { key: 'hot', label: '热同步' },
  { key: 'warm', label: '温同步' },
  { key: 'cold', label: '冷同步' },
  { key: 'disabled', label: '禁止同步' }
]

const strategyOptions = [
  {
    key: 'disabled',
    label: '禁止同步',
    caption: '只保留任务配置',
    note: '任务不会自动触发',
    valueText: '保存为 never'
  },
  {
    key: 'hot',
    label: '热同步',
    caption: '高频周期同步',
    note: '按 1 分钟间隔扫描',
    valueText: '保存为 1m'
  },
  {
    key: 'warm',
    label: '温同步',
    caption: '按间隔定时同步',
    note: '按自定义间隔运行',
    valueText: '保存为 Xm / Xh / Xd'
  },
  {
    key: 'cold',
    label: '冷同步',
    caption: '用户手动触发',
    note: '不写入自动计划',
    valueText: '保存为空值'
  }
]

const syncStrategy = ref('cold')
const warmAmount = ref(1)
const warmUnit = ref('h')

const activeStrategy = computed(
  () => strategyOptions.find((item) => item.key === syncStrategy.value) || strategyOptions[3]
)

const scheduledPreview = computed(() => {
  if (syncStrategy.value === 'disabled') return 'never'
  if (syncStrategy.value === 'hot') return '1m'
  if (syncStrategy.value === 'warm') {
    const amount = Number.isFinite(Number(warmAmount.value))
      ? Math.max(1, Number(warmAmount.value))
      : 1
    return `${amount}${warmUnit.value}`
  }
  return ''
})

const syncStrategyMeta = computed(
  () => `${activeStrategy.value.label} · ${activeStrategy.value.valueText}`
)

const normalizeSchedule = (value) =>
  String(value || '')
    .trim()
    .toLowerCase()

const taskPolicyKind = (task) => {
  const scheduled = normalizeSchedule(task.scheduled)
  if (!scheduled) return 'cold'
  if (scheduled === 'never' || scheduled === 'disabled') return 'disabled'
  if (scheduled === '1m') return 'hot'
  return 'warm'
}

const taskPolicyLabel = (task) => {
  const kind = taskPolicyKind(task)
  if (kind === 'disabled') return '禁止同步'
  if (kind === 'hot') return '热同步'
  if (kind === 'warm') return '温同步'
  return '冷同步'
}

const taskPolicyDetail = (task) => {
  const scheduled = normalizeSchedule(task.scheduled)
  if (!scheduled) return '手动触发'
  if (scheduled === 'never' || scheduled === 'disabled') return '未启用'
  if (scheduled.endsWith('m')) return `每 ${scheduled.slice(0, -1)} 分钟`
  if (scheduled.endsWith('h')) return `每 ${scheduled.slice(0, -1)} 小时`
  if (scheduled.endsWith('d')) return `每 ${scheduled.slice(0, -1)} 天`
  return scheduled
}

const hydrateStrategyFromScheduled = (value) => {
  const scheduled = normalizeSchedule(value)
  if (!scheduled) {
    syncStrategy.value = 'cold'
    warmAmount.value = 1
    warmUnit.value = 'h'
    return
  }
  if (scheduled === 'never' || scheduled === 'disabled') {
    syncStrategy.value = 'disabled'
    return
  }
  if (scheduled === '1m') {
    syncStrategy.value = 'hot'
    return
  }
  const match = scheduled.match(/^(\d+)([mhd])$/)
  if (match) {
    syncStrategy.value = 'warm'
    warmAmount.value = Number(match[1])
    warmUnit.value = match[2]
    return
  }
  syncStrategy.value = 'warm'
}

const setSyncStrategy = (key) => {
  syncStrategy.value = key
}

const syncedCount = computed(() => tasks.value.filter((task) => task.isSync).length)
const pendingTasks = computed(() => tasks.value.filter((task) => !task.isSync))
const pendingCount = computed(() => pendingTasks.value.length)
const activeSyncCount = computed(() => Object.keys(syncStatus).length)
const syncedPercent = computed(() => {
  if (tasks.value.length === 0) return 0
  return Math.round((syncedCount.value / tasks.value.length) * 100)
})

const matchesSearchQuery = (values, query) =>
  values.filter(Boolean).some((value) => String(value).toLowerCase().includes(query))

const filteredGroupFiles = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  const groupsWithScopes = groupFiles.value
    .map((group) => ({ ...group, scopes: group.scopes || [] }))
    .filter((group) => group.scopes.length > 0)
  if (!query) return groupsWithScopes
  return groupsWithScopes
    .map((group) => {
      const groupMatches = matchesSearchQuery(
        [group.name, group.ownerEmail, ...(group.admins || []), ...(group.members || [])],
        query
      )
      if (groupMatches) return group
      const scopes = (group.scopes || []).filter((scope) => {
        if (matchesSearchQuery([scope.scopeName], query)) return true
        return (scope.files || []).some((file) =>
          matchesSearchQuery([file.name, file.relativePath], query)
        )
      })
      return scopes.length ? { ...group, scopes } : null
    })
    .filter(Boolean)
})

const groupScopeCount = computed(() =>
  filteredGroupFiles.value.reduce((sum, group) => sum + (group.scopes || []).length, 0)
)

const filteredTasks = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return tasks.value.filter((task) => {
    const kindMatch =
      activePolicyFilter.value === 'all' || taskPolicyKind(task) === activePolicyFilter.value
    if (!kindMatch) return false
    if (!query) return true
    return [task.alias, task.path, task.remoteHost, task.cdcAlg, task.description]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(query))
  })
})

const recentActivities = computed(() => {
  return [...tasks.value]
    .sort((a, b) => new Date(b.updateTime || 0) - new Date(a.updateTime || 0))
    .slice(0, 6)
})

const policyCount = (key) => {
  if (key === 'all') return tasks.value.length
  return tasks.value.filter((task) => taskPolicyKind(task) === key).length
}

const loadGroupFiles = async () => {
  try {
    const res = await HttpManager.post('/client/group/files', { email: currentUser.email })
    const data = res?.data ?? res
    groupFiles.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.warn('群组文件加载失败', err.message)
    groupFiles.value = []
  }
}

const navigateToScope = (group, scope) => {
  router.push({
    path: `/group-explorer/${group.id}/${encodeURIComponent(scope.scopeName)}`,
    query: { groupName: group.name }
  })
}

const loadTasks = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await HttpManager.post('/client/file/brief-list', { email: currentUser.email })
    const data = res?.data ?? res
    tasks.value = Array.isArray(data) ? data : []
  } catch (err) {
    loadError.value = err.request ? '无法连接服务器，请确认后端已启动' : err.message || '加载失败'
    tasks.value = [
      {
        id: 1,
        alias: '工作文档',
        path: 'D:\\Documents\\Work',
        remoteHost: '192.168.1.100',
        scheduled: '1h',
        cdcAlg: 'FastCDC',
        isDir: true,
        isSync: true,
        description: '',
        updateTime: new Date().toISOString(),
        userId: 1
      },
      {
        id: 2,
        alias: '照片归档',
        path: 'E:\\Photos',
        remoteHost: '192.168.1.100',
        scheduled: '',
        cdcAlg: 'RabinCDC',
        isDir: true,
        isSync: false,
        description: '',
        updateTime: new Date().toISOString(),
        userId: 1
      }
    ]
  } finally {
    isLoading.value = false
  }
  loadGroupFiles()
}

const syncUpload = async (task) => {
  if (syncPaused.value || syncStatus[task.id]) return
  syncStatus[task.id] = 'up'
  try {
    await HttpManager.post(
      '/client/sync/upload',
      {
        fileId: task.id,
        email: currentUser.email,
        path: task.path
      },
      { timeout: 0 }
    )
    syncSuccess[task.id] = true
    task.isSync = true
    task.updateTime = new Date().toISOString()
    setTimeout(() => {
      delete syncSuccess[task.id]
    }, 2500)
  } catch (err) {
    console.error('上行同步失败', err)
  } finally {
    delete syncStatus[task.id]
  }
}

const syncAllPending = async () => {
  for (const task of pendingTasks.value) {
    if (syncPaused.value) return
    await syncUpload(task)
  }
}

const showDeleteConfirm = ref(false)
const deleteTargetTask = ref(null)
const isDeleting = ref(false)
const deleteError = ref('')

const confirmDeleteTask = (task) => {
  deleteTargetTask.value = task
  deleteError.value = ''
  showDeleteConfirm.value = true
}

const cancelDeleteTask = () => {
  showDeleteConfirm.value = false
  deleteError.value = ''
}

const doDeleteTask = async () => {
  if (!deleteTargetTask.value) return
  isDeleting.value = true
  deleteError.value = ''
  try {
    await HttpManager.post('/client/sync/delete', {
      fileId: deleteTargetTask.value.id,
      email: currentUser.email,
      path: deleteTargetTask.value.path
    })
    tasks.value = tasks.value.filter((t) => t.id !== deleteTargetTask.value.id)
    showDeleteConfirm.value = false
    deleteTargetTask.value = null
  } catch (err) {
    deleteError.value = err.message || '删除失败'
  } finally {
    isDeleting.value = false
  }
}

const scopeDisplayName = (scopeName) => {
  if (!scopeName) return scopeName
  const idx = scopeName.indexOf('/')
  return idx >= 0 ? scopeName.slice(idx + 1) : scopeName
}

// ── 远端任务恢复 ──────────────────────────────────────────
// 刚装好客户端时本地数据库是空的，但 bucket 里可能还存着该账号过往的任务文件。
// 这里从 server 拉到 scope 列表，让用户挑选要恢复哪些任务、放到本地哪里，然后逐个创建本地 task 并触发下行同步。
const showRestoreModal = ref(false)
const restoreLoading = ref(false)
const restoreError = ref('')
const restoreRunning = ref(false)
const remoteScopes = ref([])
const restoreSelection = reactive({})
const restoreBasePath = ref('')
const restoreProgress = ref('')

const selectedRestoreCount = computed(() => Object.values(restoreSelection).filter(Boolean).length)

const openRestoreModal = async () => {
  showRestoreModal.value = true
  restoreLoading.value = true
  restoreError.value = ''
  restoreProgress.value = ''
  remoteScopes.value = []
  Object.keys(restoreSelection).forEach((k) => delete restoreSelection[k])
  try {
    const res = await HttpManager.post('/client/file/remote-scopes', {
      email: currentUser.email
    })
    const data = res?.data ?? res
    const list = Array.isArray(data) ? data : []
    // 标记本地是否已有相同 alias 的任务，避免用户重复创建
    const existingAliases = new Set(
      tasks.value.map((t) => normalizeTaskAlias(t.alias)).filter(Boolean)
    )
    remoteScopes.value = list.map((s) => ({
      ...s,
      localExists: existingAliases.has(normalizeTaskAlias(s.alias))
    }))
    // 默认勾选所有本地没有的 scope
    remoteScopes.value.forEach((s) => {
      restoreSelection[s.scopeName] = !s.localExists
    })
  } catch (err) {
    restoreError.value = err.message || '无法获取远端任务列表'
  } finally {
    restoreLoading.value = false
  }
}

const closeRestoreModal = () => {
  if (restoreRunning.value) return
  showRestoreModal.value = false
}

const pickRestoreBasePath = async () => {
  if (!window.electron?.ipcRenderer) {
    alert('非桌面客户端环境，请在桌面端选择路径')
    return
  }
  const result = await window.electron.ipcRenderer.invoke('select-folder')
  if (result) restoreBasePath.value = result
}

const joinLocalPath = (base, ...segments) => {
  const sep = base.includes('\\') ? '\\' : '/'
  const trimmed = base.replace(/[\\/]+$/, '')
  return [trimmed, ...segments].join(sep)
}

const doRestore = async () => {
  if (restoreRunning.value) return
  if (!restoreBasePath.value) {
    restoreError.value = '请选择本地基础目录'
    return
  }
  restoreRunning.value = true
  restoreError.value = ''
  const selected = remoteScopes.value.filter((s) => restoreSelection[s.scopeName] && !s.localExists)
  try {
    for (let i = 0; i < selected.length; i++) {
      const scope = selected[i]
      restoreProgress.value = `(${i + 1}/${selected.length}) 创建任务 ${scope.alias}...`
      // 单文件 task 的本地路径要落到具体文件上，目录 task 则落到目录。
      const localTaskPath = scope.isDir
        ? joinLocalPath(restoreBasePath.value, scope.alias, scope.rootName)
        : joinLocalPath(restoreBasePath.value, scope.alias, scope.rootName)
      const createRes = await HttpManager.post('/client/sync/update', {
        fileId: null,
        email: currentUser.email,
        path: localTaskPath,
        alias: scope.alias,
        remoteHost: '',
        scheduled: '',
        cdcAlg: 'FastCDC',
        description: '从远端恢复',
        isDir: scope.isDir
      })
      const createdTask = createRes?.data ?? createRes
      const createdId = createdTask?.id ?? Date.now()
      tasks.value.push({
        id: createdId,
        alias: scope.alias,
        path: localTaskPath,
        isDir: scope.isDir,
        isSync: false,
        cdcAlg: 'FastCDC',
        description: '从远端恢复',
        updateTime: new Date().toISOString(),
        userId: currentUser.id
      })
      restoreProgress.value = `(${i + 1}/${selected.length}) 下行同步 ${scope.alias}...`
      await HttpManager.post(
        '/client/sync/download',
        {
          fileId: String(createdId),
          email: currentUser.email,
          path: localTaskPath
        },
        { timeout: 0 }
      )
    }
    restoreProgress.value = '全部完成'
    setTimeout(() => {
      showRestoreModal.value = false
      restoreRunning.value = false
      loadTasks()
    }, 800)
  } catch (err) {
    restoreError.value = err.message || '恢复过程出错'
    restoreRunning.value = false
  }
}

const navigateInto = async (task) => {
  router.push({
    path: `/files/${task.id}`,
    query: { alias: task.alias, path: task.path, email: currentUser.email }
  })
}

const showModal = ref(false)
const isEdit = ref(false)
const isSaving = ref(false)
const modalError = ref('')
const editingTaskId = ref(null)
const duplicateTaskAliasMessage =
  '\u4efb\u52a1\u540d\u79f0\u5df2\u5b58\u5728\uff0c\u8bf7\u66f4\u6362\u4e00\u4e2a\u540d\u79f0'

const form = reactive({
  alias: '',
  path: '',
  remoteHost: '',
  scheduled: '',
  cdcAlg: 'FastCDC',
  description: '',
  isDir: true
})

const normalizeTaskAlias = (value) => String(value || '').trim()

const hasDuplicateTaskAlias = (alias) =>
  tasks.value.some(
    (task) =>
      normalizeTaskAlias(task.alias) === alias && Number(task.id) !== Number(editingTaskId.value)
  )

const resetForm = () => {
  form.alias = ''
  form.path = ''
  form.remoteHost = ''
  form.scheduled = ''
  form.cdcAlg = 'FastCDC'
  form.description = ''
  form.isDir = true
  hydrateStrategyFromScheduled('')
  modalError.value = ''
}

const openAdd = () => {
  isEdit.value = false
  editingTaskId.value = null
  resetForm()
  showModal.value = true
}

const openEdit = (task) => {
  isEdit.value = true
  editingTaskId.value = task.id
  form.alias = task.alias
  form.path = task.path
  form.remoteHost = task.remoteHost
  form.scheduled = task.scheduled || ''
  form.cdcAlg = task.cdcAlg || 'FastCDC'
  form.description = task.description || ''
  form.isDir = task.isDir
  hydrateStrategyFromScheduled(form.scheduled)
  modalError.value = ''
  showModal.value = true
}

const setTaskType = (isDir) => {
  if (form.isDir !== isDir) {
    form.path = ''
  }
  form.isDir = isDir
}

const selectLocalPath = async () => {
  if (window.electron?.ipcRenderer) {
    const result = await window.electron.ipcRenderer.invoke(
      form.isDir ? 'select-folder' : 'select-file'
    )
    if (result) form.path = result
  } else {
    alert('非桌面客户端环境，请在桌面端选择路径')
  }
}

async function saveTask() {
  const alias = normalizeTaskAlias(form.alias)
  if (!alias) {
    modalError.value = '请填写任务别名'
    return
  }
  if (hasDuplicateTaskAlias(alias)) {
    modalError.value = duplicateTaskAliasMessage
    return
  }
  if (!form.path) {
    modalError.value = '请选择本地路径'
    return
  }
  try {
    isSaving.value = true
    form.alias = alias
    form.scheduled = scheduledPreview.value
    const payload = {
      fileId: isEdit.value ? editingTaskId.value : null,
      email: currentUser.email,
      path: form.path,
      alias: form.alias,
      remoteHost: form.remoteHost,
      scheduled: form.scheduled,
      cdcAlg: form.cdcAlg,
      description: form.description,
      isDir: form.isDir
    }
    const res = await HttpManager.post('/client/sync/update', payload)
    const data = res?.data ?? res
    if (isEdit.value) {
      const idx = tasks.value.findIndex((t) => t.id === editingTaskId.value)
      if (idx !== -1) {
        tasks.value[idx] = {
          ...tasks.value[idx],
          ...form,
          id: editingTaskId.value,
          updateTime: new Date().toISOString()
        }
      }
    } else {
      tasks.value.push({
        id: data?.id ?? Date.now(),
        ...form,
        isSync: false,
        updateTime: new Date().toISOString(),
        userId: currentUser.id
      })
    }
    showModal.value = false
  } catch (err) {
    modalError.value = err.request ? '无法连接服务器' : err.message || '保存失败'
  } finally {
    isSaving.value = false
  }
}

const formatTime = (t) => {
  if (!t) return '暂无记录'
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

onMounted(() => {
  const saved = localStorage.getItem('userInfo')
  if (saved) Object.assign(currentUser, JSON.parse(saved))
  loadTasks()
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
:global(:root) {
  --ds-bg: #ffffff;
  --ds-page: #f8fafd;
  --ds-surface: #ffffff;
  --ds-line: #dadce0;
  --ds-line-soft: #e8eaed;
  --ds-text: #202124;
  --ds-muted: #5f6368;
  --ds-muted-soft: #80868b;
  --ds-blue: #1a73e8;
  --ds-blue-soft: #e8f0fe;
  --ds-green: #188038;
  --ds-green-soft: #e6f4ea;
  --ds-yellow: #f9ab00;
  --ds-yellow-soft: #fef7e0;
  --ds-red: #d93025;
  --ds-red-soft: #fce8e6;
  --bg: var(--ds-bg);
  --page: var(--ds-page);
  --surface: var(--ds-surface);
  --line: var(--ds-line);
  --line-soft: var(--ds-line-soft);
  --text: var(--ds-text);
  --muted: var(--ds-muted);
  --muted-soft: var(--ds-muted-soft);
  --blue: var(--ds-blue);
  --blue-soft: var(--ds-blue-soft);
  --green: var(--ds-green);
  --green-soft: var(--ds-green-soft);
  --yellow: var(--ds-yellow);
  --yellow-soft: var(--ds-yellow-soft);
  --red: var(--ds-red);
  --red-soft: var(--ds-red-soft);
}

*,
*::before,
*::after {
  box-sizing: border-box;
}

.app-root {
  --bg: var(--ds-bg);
  --page: var(--ds-page);
  --surface: var(--ds-surface);
  --line: var(--ds-line);
  --line-soft: var(--ds-line-soft);
  --text: var(--ds-text);
  --muted: var(--ds-muted);
  --muted-soft: var(--ds-muted-soft);
  --blue: var(--ds-blue);
  --blue-soft: var(--ds-blue-soft);
  --green: var(--ds-green);
  --green-soft: var(--ds-green-soft);
  --yellow: var(--ds-yellow);
  --yellow-soft: var(--ds-yellow-soft);
  --red: var(--ds-red);
  --red-soft: var(--ds-red-soft);
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  background: var(--bg);
  color: var(--text);
  font-family:
    Inter,
    'Segoe UI',
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    sans-serif;
}

button,
input,
select {
  font: inherit;
}

button {
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

svg {
  flex-shrink: 0;
}

.drive-sidebar {
  min-height: 100vh;
  padding: 18px 14px;
  background: var(--page);
  border-right: 1px solid var(--line-soft);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px 4px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--blue);
  background: var(--surface);
  border: 1px solid var(--line-soft);
}

.brand-mark svg {
  width: 20px;
  height: 20px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  line-height: 1.1;
}

.brand-sub {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.3;
}

.create-sync-btn,
.primary-btn,
.wide-btn,
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  background: var(--blue);
  color: #fff;
  border-radius: 8px;
  font-weight: 600;
  transition:
    background 0.15s,
    box-shadow 0.15s;
}

.create-sync-btn {
  height: 44px;
  padding: 0 16px;
  align-self: flex-start;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.18);
}

.create-sync-btn svg,
.primary-btn svg,
.btn-primary svg {
  width: 18px;
  height: 18px;
}

.create-sync-btn:hover,
.primary-btn:hover,
.wide-btn:hover,
.btn-primary:hover:not(:disabled) {
  background: #1765cc;
}

.nav-stack,
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item,
.policy-filter {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--muted);
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 14px;
  text-align: left;
}

.nav-item svg {
  width: 18px;
  height: 18px;
}

.nav-item:hover,
.policy-filter:hover {
  background: #edf2fa;
  color: var(--text);
}

.nav-item.active,
.policy-filter.active {
  background: var(--blue-soft);
  color: var(--blue);
  font-weight: 600;
}

.sidebar-label {
  margin: 8px 10px 4px;
  color: var(--muted-soft);
  font-size: 12px;
  font-weight: 600;
}

.policy-filter {
  justify-content: flex-start;
}

.policy-filter strong {
  margin-left: auto;
  font-size: 12px;
  color: inherit;
}

.policy-dot,
.strategy-dot,
.activity-dot,
.status-light {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--muted-soft);
}

.policy-dot.hot,
.strategy-dot.hot {
  background: var(--blue);
}

.policy-dot.warm,
.strategy-dot.warm {
  background: var(--yellow);
}

.policy-dot.cold,
.strategy-dot.cold {
  background: var(--green);
}

.policy-dot.disabled,
.strategy-dot.disabled {
  background: var(--red);
}

.storage-summary {
  margin-top: auto;
  padding: 12px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface);
}

.storage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--muted);
  font-size: 12px;
}

.storage-head strong {
  color: var(--text);
}

.storage-bar {
  height: 6px;
  margin: 10px 0 8px;
  border-radius: 999px;
  background: #edf0f4;
  overflow: hidden;
}

.storage-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--blue);
}

.storage-summary p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
}

.workspace {
  min-width: 0;
  min-height: 100vh;
  background: var(--bg);
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 64px;
  padding: 12px 22px;
  border-bottom: 1px solid var(--line-soft);
  display: flex;
  align-items: center;
  gap: 18px;
  background: var(--surface);
  position: sticky;
  top: 0;
  z-index: 20;
}

.search-box {
  flex: 1;
  max-width: 720px;
  min-width: 180px;
  height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  background: #f1f3f4;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--muted);
}

.search-box:focus-within {
  background: var(--surface);
  border-color: var(--blue);
  box-shadow: 0 1px 6px rgba(60, 64, 67, 0.18);
}

.search-box svg {
  width: 18px;
  height: 18px;
}

.search-box input {
  width: 100%;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-size: 14px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-btn,
.icon-text-btn,
.text-btn,
.btn-secondary,
.browse-btn {
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--muted);
  border-radius: 8px;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
}

.icon-btn,
.icon-text-btn,
.browse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn {
  width: 38px;
  height: 38px;
}

.icon-btn.small {
  width: 32px;
  height: 32px;
}

.icon-text-btn {
  height: 38px;
  gap: 7px;
  padding: 0 12px;
  white-space: nowrap;
}

.icon-btn svg,
.icon-text-btn svg {
  width: 17px;
  height: 17px;
}

.icon-btn:hover,
.icon-text-btn:hover,
.text-btn:hover,
.browse-btn:hover,
.btn-secondary:hover {
  background: #f8fafd;
  color: var(--blue);
  border-color: #c9d7f8;
}

.icon-btn.danger:hover {
  color: var(--red);
  border-color: #f3b7b3;
  background: var(--red-soft);
}

.spinning svg,
.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.user-menu {
  position: relative;
}

.user-trigger {
  height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text);
  border-radius: 8px;
  padding: 0 6px 0 10px;
}

.user-trigger:hover {
  background: #f8fafd;
  border-color: var(--line-soft);
}

.user-name {
  max-width: 110px;
  font-size: 13px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.avatar-ring {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--line);
}

.avatar-ring img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chevron {
  width: 14px;
  height: 14px;
  color: var(--muted);
  transition: transform 0.2s;
}

.chevron.open {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 210px;
  padding: 6px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: 0 12px 30px rgba(60, 64, 67, 0.18);
  z-index: 50;
}

.dropdown-head {
  padding: 8px 10px 10px;
  border-bottom: 1px solid var(--line-soft);
  margin-bottom: 4px;
}

.dropdown-head p {
  margin: 0;
  color: var(--muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-item {
  width: 100%;
  min-height: 36px;
  display: flex;
  align-items: center;
  gap: 9px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text);
  padding: 8px 10px;
  text-align: left;
}

.dropdown-item svg {
  width: 16px;
  height: 16px;
}

.dropdown-item:hover {
  background: #f1f3f4;
}

.dropdown-item.danger {
  color: var(--red);
}

.workspace-main {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 24px;
  padding: 24px;
  overflow-y: auto;
}

.content-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-title-row,
.task-panel-head,
.section-line-head,
.activity-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title-row h1,
.task-panel-head h2,
.section-line-head h2,
.activity-head h2 {
  margin: 0;
  color: var(--text);
}

.page-title-row h1 {
  font-size: 26px;
  font-weight: 600;
}

.page-title-row p,
.task-panel-head p,
.section-line-head p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.primary-btn {
  height: 38px;
  padding: 0 14px;
  white-space: nowrap;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item,
.task-panel,
.shared-section,
.activity-card {
  background: var(--surface);
  border: 1px solid var(--line-soft);
  border-radius: 8px;
}

.metric-item {
  min-width: 0;
  padding: 14px 16px;
}

.metric-item span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.metric-item strong {
  display: block;
  margin-top: 4px;
  color: var(--text);
  font-size: 24px;
  font-weight: 600;
}

.metric-item.success strong {
  color: var(--green);
}

.metric-item.warning strong {
  color: #b06000;
}

.metric-item.blue strong {
  color: var(--blue);
}

.center-state {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--muted);
  border: 1px solid var(--line-soft);
  border-radius: 8px;
}

.center-state.error {
  color: var(--red);
  background: var(--red-soft);
  border-color: #fad2cf;
}

.center-state svg {
  width: 40px;
  height: 40px;
}

.center-state p {
  margin: 0;
  font-size: 14px;
}

.center-state button {
  border: 1px solid #f3b7b3;
  border-radius: 8px;
  background: var(--surface);
  color: var(--red);
  padding: 8px 16px;
}

.loading-spinner {
  width: 34px;
  height: 34px;
  border: 3px solid var(--blue-soft);
  border-top-color: var(--blue);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

.task-panel,
.shared-section {
  overflow: hidden;
}

.task-panel-head,
.section-line-head {
  padding: 16px 18px;
  border-bottom: 1px solid var(--line-soft);
}

.task-panel-head h2,
.section-line-head h2,
.activity-head h2 {
  font-size: 16px;
  font-weight: 600;
}

.sync-state,
.status-badge,
.strategy-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.sync-state {
  background: var(--green-soft);
  color: var(--green);
}

.sync-state.paused {
  background: var(--red-soft);
  color: var(--red);
}

.empty-list {
  min-height: 220px;
  display: grid;
  place-items: center;
  color: var(--muted);
}

.empty-list svg {
  width: 42px;
  height: 42px;
  margin-bottom: 10px;
}

.empty-list p {
  margin: 0;
}

.restore-btn {
  margin-top: 14px;
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text);
  cursor: pointer;
  font-weight: 500;
  transition:
    background 0.15s,
    border-color 0.15s;
}

.restore-btn:hover {
  background: var(--surface-hover, rgba(0, 0, 0, 0.04));
  border-color: var(--accent, #5366ff);
}

.restore-panel {
  background: var(--surface);
  border-radius: 16px;
  padding: 26px;
  width: min(520px, 92vw);
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
}

.restore-panel h3 {
  margin: 0 0 6px;
}

.restore-sub {
  margin: 0 0 16px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
}

.restore-sub code {
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  padding: 0 4px;
}

.restore-loading,
.restore-empty {
  text-align: center;
  color: var(--muted);
  padding: 24px 0;
}

.restore-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  max-height: 280px;
  overflow-y: auto;
}

.restore-row {
  display: grid;
  grid-template-columns: 18px 28px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  user-select: none;
}

.restore-row.active {
  border-color: var(--accent, #5366ff);
  background: rgba(83, 102, 255, 0.06);
}

.restore-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.05);
}

.restore-icon.folder {
  background: rgba(247, 184, 1, 0.18);
}

.restore-icon.file {
  background: rgba(83, 102, 255, 0.14);
}

.restore-info strong {
  display: block;
  font-size: 14px;
}

.restore-info small {
  color: var(--muted);
  font-size: 12px;
}

.restore-exists {
  color: var(--muted);
  font-size: 12px;
}

.restore-path-row {
  margin-bottom: 12px;
}

.restore-progress {
  margin: 8px 0;
  color: var(--muted);
  font-size: 13px;
}

.task-list {
  display: flex;
  flex-direction: column;
}

.list-header,
.task-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 150px 122px 120px;
  gap: 12px;
  align-items: center;
}

.list-header {
  padding: 9px 18px;
  color: var(--muted);
  font-size: 12px;
  border-bottom: 1px solid var(--line-soft);
}

.task-row {
  position: relative;
  min-height: 76px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--line-soft);
  transition: background 0.15s;
}

.task-row:last-child {
  border-bottom: none;
}

.task-row:hover {
  background: #f8fafd;
}

.task-row.active {
  background: var(--blue-soft);
}

.task-main {
  min-width: 0;
  border: none;
  background: transparent;
  color: inherit;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  text-align: left;
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.task-icon.folder {
  color: #fbbc04;
  background: var(--yellow-soft);
}

.task-icon.file {
  color: #9334e6;
  background: #f3e8fd;
}

.task-icon svg {
  width: 20px;
  height: 20px;
}

.task-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.task-copy strong {
  color: var(--text);
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.task-copy small,
.task-policy small,
.task-status small,
.activity-item small,
.strategy-option small {
  color: var(--muted);
  font-size: 12px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.task-policy,
.task-status {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: flex-start;
}

.strategy-chip.hot {
  background: var(--blue-soft);
  color: var(--blue);
}

.strategy-chip.warm {
  background: var(--yellow-soft);
  color: #8c5000;
}

.strategy-chip.cold {
  background: var(--green-soft);
  color: var(--green);
}

.strategy-chip.disabled {
  background: var(--red-soft);
  color: var(--red);
}

.status-badge.ok {
  background: var(--green-soft);
  color: var(--green);
}

.status-badge.pending {
  background: var(--yellow-soft);
  color: #8c5000;
}

.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
}

.sync-flash {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--green);
  background: rgba(230, 244, 234, 0.92);
  font-weight: 700;
}

.shared-section {
  display: flex;
  flex-direction: column;
}

.text-btn {
  min-height: 32px;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--blue);
}

.text-btn.compact {
  min-height: 28px;
  padding: 0 8px;
}

.scope-list {
  display: flex;
  flex-direction: column;
}

.group-row {
  padding: 14px 18px;
  border-bottom: 1px solid var(--line-soft);
}

.group-row:last-child {
  border-bottom: none;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  min-width: 0;
}

.group-title strong {
  font-size: 14px;
  font-weight: 600;
}

.group-title span {
  color: var(--muted);
  font-size: 12px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.scope-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.scope-chip {
  min-height: 34px;
  max-width: 220px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  padding: 0 10px;
}

.scope-chip:hover {
  background: #f8fafd;
  border-color: #c9d7f8;
}

.scope-chip svg {
  width: 16px;
  height: 16px;
  color: #fbbc04;
}

.scope-chip span {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.scope-chip small {
  color: var(--muted);
}

.activity-pane {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.activity-card {
  padding: 16px;
}

.activity-head {
  margin-bottom: 14px;
}

.status-light {
  background: var(--green);
}

.status-light.paused {
  background: var(--red);
}

.status-light.active {
  background: var(--blue);
  box-shadow: 0 0 0 4px var(--blue-soft);
}

.sync-overview strong {
  display: block;
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
}

.sync-overview p {
  margin: 4px 0 14px;
  color: var(--muted);
  font-size: 13px;
}

.wide-btn {
  width: 100%;
  min-height: 38px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.activity-dot {
  margin-top: 5px;
}

.activity-dot.ok {
  background: var(--green);
}

.activity-dot.pending {
  background: var(--yellow);
}

.activity-item div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-item strong {
  color: var(--text);
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.muted-empty {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.policy-overview {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.policy-overview div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--muted);
}

.policy-overview strong {
  color: var(--text);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(32, 33, 36, 0.38);
}

.modal-panel,
.confirm-panel {
  width: min(760px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  overflow: auto;
  background: var(--surface);
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  box-shadow: 0 24px 80px rgba(60, 64, 67, 0.28);
}

.modal-hd,
.modal-ft {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--line-soft);
}

.modal-ft {
  justify-content: flex-end;
  border-top: 1px solid var(--line-soft);
  border-bottom: none;
}

.modal-hd h3 {
  margin: 0;
  color: var(--text);
  font-size: 18px;
  font-weight: 600;
}

.modal-hd p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--muted);
  display: grid;
  place-items: center;
}

.modal-close svg {
  width: 15px;
  height: 15px;
}

.modal-close:hover {
  color: var(--red);
  border-color: #f3b7b3;
  background: var(--red-soft);
}

.modal-body {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.field-group {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.field-label {
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}

.ds-input {
  width: 100%;
  height: 38px;
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  outline: none;
  padding: 0 11px;
  font-size: 14px;
}

.ds-input:focus {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px var(--blue-soft);
}

.path-row {
  display: flex;
  gap: 8px;
}

.path-row .ds-input {
  flex: 1;
}

.browse-btn {
  height: 38px;
  gap: 7px;
  padding: 0 12px;
  white-space: nowrap;
}

.browse-btn svg {
  width: 16px;
  height: 16px;
}

.type-toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.type-toggle button {
  height: 38px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  color: var(--muted);
}

.type-toggle button.active {
  color: var(--blue);
  background: var(--blue-soft);
  border-color: #c9d7f8;
  font-weight: 600;
}

.sync-designer {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  padding: 12px;
  background: #fbfcff;
}

.strategy-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.strategy-option {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  padding: 10px;
  text-align: left;
}

.strategy-option:hover,
.strategy-option.active {
  border-color: #c9d7f8;
  background: var(--blue-soft);
}

.strategy-option span:last-child {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.strategy-option strong {
  font-size: 14px;
  font-weight: 600;
}

.strategy-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface);
  padding: 14px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title h4 {
  margin: 0;
  color: var(--text);
  font-size: 16px;
  font-weight: 600;
}

.panel-title span {
  color: var(--muted);
  font-size: 12px;
  background: #f1f3f4;
  border-radius: 8px;
  padding: 4px 8px;
  white-space: nowrap;
}

.schedule-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.schedule-grid label {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}

.strategy-note {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  border-radius: 8px;
  background: #f8fafd;
  color: var(--muted);
}

.strategy-note strong {
  color: var(--text);
  font-weight: 600;
}

.modal-error {
  margin: 0 22px 6px;
  padding: 10px 12px;
  border: 1px solid #f3b7b3;
  border-radius: 8px;
  background: var(--red-soft);
  color: var(--red);
  font-size: 13px;
}

.btn-secondary,
.btn-danger,
.btn-primary {
  min-height: 38px;
  padding: 0 16px;
}

.btn-secondary {
  color: var(--muted);
}

.btn-primary {
  min-width: 112px;
}

.btn-danger {
  border: 1px solid #f3b7b3;
  border-radius: 8px;
  background: var(--red-soft);
  color: var(--red);
  font-weight: 600;
}

.btn-danger:hover:not(:disabled) {
  background: #fad2cf;
}

.confirm-panel {
  width: min(390px, calc(100vw - 48px));
  padding: 28px;
  text-align: center;
}

.confirm-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--red);
  background: var(--red-soft);
}

.confirm-icon svg {
  width: 24px;
  height: 24px;
}

.confirm-panel h3 {
  margin: 0 0 8px;
  color: var(--text);
  font-size: 18px;
  font-weight: 600;
}

.confirm-panel p {
  margin: 0 0 22px;
  color: var(--muted);
  line-height: 1.6;
  font-size: 14px;
}

.confirm-panel strong {
  color: var(--text);
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
.delete-error-msg {
  color: #d93025;
  font-size: 13px;
  background: #fce8e6;
  border-radius: 6px;
  padding: 8px 12px;
  margin: 4px 0 0;
  text-align: left;
}

@media (max-width: 1100px) {
  .workspace-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .activity-pane {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .app-root {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .brand-copy,
  .create-sync-btn span,
  .nav-item span,
  .policy-filter span:not(.policy-dot),
  .storage-summary {
    display: none;
  }

  .create-sync-btn {
    width: 44px;
    padding: 0;
  }

  .nav-item,
  .policy-filter {
    justify-content: center;
    padding: 8px;
  }

  .policy-filter strong {
    display: none;
  }
}

@media (max-width: 760px) {
  .topbar {
    height: auto;
    align-items: stretch;
    flex-direction: column;
  }

  .topbar-actions {
    justify-content: space-between;
  }

  .workspace-main {
    padding: 16px;
  }

  .metric-strip,
  .activity-pane,
  .form-grid,
  .sync-designer,
  .schedule-grid {
    grid-template-columns: 1fr;
  }

  .list-header {
    display: none;
  }

  .task-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .task-actions {
    justify-content: flex-start;
  }
}
</style>
