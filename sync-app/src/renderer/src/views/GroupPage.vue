<template>
  <div class="app-root">
    <!-- HEADER -->
    <header class="app-header">
      <div class="header-left">
        <button class="back-btn" @click="router.push('/dashboard')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18"
            />
          </svg>
        </button>
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"
            />
          </svg>
        </div>
        <span class="logo-text">群组管理</span>
      </div>
      <div class="header-right">
        <span class="user-label">{{ currentUser.email }}</span>
      </div>
    </header>

    <!-- MAIN -->
    <main class="app-main">
      <div class="section-header">
        <div>
          <h1 class="section-title">我的群组</h1>
          <p class="section-sub">管理文件共享群组 · 所有者和管理员可管理成员与共享文件夹</p>
        </div>
        <div class="header-actions">
          <button class="refresh-btn" :class="{ spinning: isLoading }" @click="loadGroups">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
              />
            </svg>
            刷新
          </button>
          <button class="create-btn" @click="openCreateModal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            新建群组
          </button>
        </div>
      </div>

      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>正在加载群组...</p>
      </div>

      <div v-else-if="loadError" class="error-state">
        <p>{{ loadError }}</p>
        <button @click="loadGroups">重试</button>
      </div>

      <div v-else class="group-grid">
        <div v-if="groups.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"
            />
          </svg>
          <p>暂无群组，点击「新建群组」开始吧</p>
        </div>

        <div v-for="group in groups" :key="group.id" class="group-card">
          <!-- Card header -->
          <div class="group-card-header">
            <div class="group-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"
                />
              </svg>
            </div>
            <div class="group-title-area">
              <h3 class="group-name">{{ group.name }}</h3>
              <span v-if="group.ownerEmail === currentUser.email" class="role-badge owner"
                >所有者</span
              >
              <span
                v-else-if="(group.admins || []).includes(currentUser.email)"
                class="role-badge admin"
                >管理员</span
              >
              <span v-else class="role-badge member">成员</span>
            </div>
            <button
              v-if="group.ownerEmail === currentUser.email"
              class="delete-group-btn"
              title="删除群组"
              @click="confirmDeleteGroup(group)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"
                />
              </svg>
            </button>
          </div>

          <p class="group-owner-email">所有者: {{ group.ownerEmail }}</p>

          <!-- ── MEMBERS ── -->
          <div class="section-label">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"
              />
            </svg>
            成员 ({{ totalMemberCount(group) }})
          </div>

          <!-- Admin chips -->
          <div v-if="(group.admins || []).length > 0" class="sub-label">管理员</div>
          <div v-if="(group.admins || []).length > 0" class="members-list">
            <div
              v-for="a in group.admins || []"
              :key="'admin-' + a"
              class="member-chip admin-chip"
              :class="{ removing: opLoading[`${group.id}_rmAdmin_${a}`] }"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                class="chip-icon"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"
                />
              </svg>
              <span>{{ a }}</span>
              <button
                v-if="group.ownerEmail === currentUser.email"
                class="chip-remove"
                title="撤销管理员"
                :disabled="opLoading[`${group.id}_rmAdmin_${a}`]"
                @click="removeAdmin(group, a)"
              >
                ×
              </button>
            </div>
          </div>

          <!-- Regular member chips -->
          <div v-if="(group.members || []).length > 0" class="sub-label">普通成员</div>
          <div class="members-list">
            <div
              v-if="(group.members || []).length === 0 && (group.admins || []).length === 0"
              class="empty-hint"
            >
              暂无成员
            </div>
            <div
              v-for="m in filteredMembers(group)"
              :key="m"
              class="member-chip"
              :class="{ removing: opLoading[`${group.id}_rm_${m}`] }"
            >
              <img
                :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${m}`"
                class="chip-avatar"
                alt=""
              />
              <span>{{ m }}</span>
              <button
                v-if="canManage(group)"
                class="chip-remove"
                title="移除成员"
                :disabled="opLoading[`${group.id}_rm_${m}`]"
                @click="removeMember(group, m)"
              >
                ×
              </button>
            </div>
            <div
              v-if="memberFilter[group.id] && filteredMembers(group).length === 0"
              class="empty-hint"
            >
              无匹配成员
            </div>
          </div>

          <!-- Member management (admin+) -->
          <div v-if="canManage(group)" class="manage-area">
            <!-- Filter bar -->
            <div v-if="(group.members || []).length > 3" class="filter-row">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                class="filter-icon"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M21 21l-4.35-4.35M10.5 18a7.5 7.5 0 100-15 7.5 7.5 0 000 15z"
                />
              </svg>
              <input
                v-model="memberFilter[group.id]"
                type="text"
                class="ds-input sm filter-input"
                placeholder="筛选成员..."
              />
            </div>

            <!-- Search add -->
            <div class="add-section-label">添加成员</div>
            <div class="search-add-row">
              <div class="search-wrap">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                  class="search-icon"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M21 21l-4.35-4.35M10.5 18a7.5 7.5 0 100-15 7.5 7.5 0 000 15z"
                  />
                </svg>
                <input
                  v-model="searchInputs[group.id]"
                  type="text"
                  class="ds-input sm search-input"
                  placeholder="搜索用户 email / 用户名..."
                  :disabled="opLoading[`${group.id}_addMember`]"
                  @input="onSearchInput(group.id)"
                  @keyup.enter="addFromSearch(group)"
                  @blur="hideDropdownDelayed(group.id)"
                  @focus="showDropdown[group.id] = true"
                />
                <!-- Search dropdown -->
                <div
                  v-if="showDropdown[group.id] && searchResults[group.id]?.length"
                  class="search-dropdown"
                >
                  <div
                    v-for="u in searchResults[group.id]"
                    :key="u.email"
                    class="search-option"
                    :class="{ 'option-disabled': isInGroup(group, u.email) }"
                    @mousedown.prevent="!isInGroup(group, u.email) && selectUser(group, u)"
                  >
                    <img
                      :src="
                        u.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${u.email}`
                      "
                      class="option-avatar"
                      alt=""
                    />
                    <div class="option-info">
                      <span class="option-name">{{ u.username || u.email }}</span>
                      <span class="option-email">{{ u.email }}</span>
                    </div>
                    <span v-if="isInGroup(group, u.email)" class="option-badge">已在群组</span>
                  </div>
                </div>
                <div
                  v-else-if="showDropdown[group.id] && (searchInputs[group.id] || '').trim()"
                  class="search-dropdown empty-dropdown"
                >
                  <div class="search-empty">未找到匹配用户</div>
                </div>
              </div>
              <button
                class="add-btn"
                :disabled="
                  !(selectedSearchUsers[group.id] || (searchInputs[group.id] || '').trim()) ||
                  opLoading[`${group.id}_addMember`]
                "
                @click="addFromSearch(group)"
              >
                <svg
                  v-if="opLoading[`${group.id}_addMember`]"
                  class="spin-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
                  />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
                {{ opLoading[`${group.id}_addMember`] ? '添加中...' : '添加' }}
              </button>
            </div>

            <!-- Batch import -->
            <div class="batch-row">
              <button class="batch-btn" @click="triggerBatchFile(group.id, 'add')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                  />
                </svg>
                批量导入（.txt / .csv）
              </button>
              <button
                v-if="(group.members || []).length > 0"
                class="batch-btn remove-batch"
                @click="triggerBatchFile(group.id, 'remove')"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m6.75 12H9m1.5-12H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
                  />
                </svg>
                批量移除
              </button>
              <input
                :ref="(el) => (batchFileRefs[group.id] = el)"
                type="file"
                accept=".txt,.csv"
                class="hidden-file"
                @change="handleBatchFile($event, group)"
              />
            </div>

            <!-- Promote to admin (owner only) -->
            <div
              v-if="group.ownerEmail === currentUser.email && (group.members || []).length > 0"
              class="admin-promote-row"
            >
              <div class="add-section-label">设为管理员</div>
              <div class="promote-wrap">
                <div class="promote-select-wrap">
                  <button
                    type="button"
                    class="promote-trigger ds-input sm"
                    @click="togglePromoteDropdown(group.id, $event)"
                  >
                    <span>{{ promoteTarget[group.id] || '-- 选择普通成员 --' }}</span>
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      class="promote-arrow"
                    >
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        d="M19.5 8.25l-7.5 7.5-7.5-7.5"
                      />
                    </svg>
                  </button>
                  <div
                    v-if="showPromoteDropdown[group.id]"
                    class="search-dropdown promote-dropdown"
                  >
                    <!-- Existing admins shown as disabled -->
                    <template v-if="(group.admins || []).length > 0">
                      <div class="dropdown-section-label">已是管理员</div>
                      <div
                        v-for="a in group.admins || []"
                        :key="'admin-' + a"
                        class="search-option option-disabled"
                      >
                        <img
                          :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${a}`"
                          class="option-avatar"
                          alt=""
                        />
                        <div class="option-info">
                          <span class="option-email">{{ a }}</span>
                        </div>
                        <span class="option-badge admin-badge">管理员</span>
                      </div>
                    </template>
                    <!-- Regular members — selectable -->
                    <div v-if="(group.members || []).length > 0" class="dropdown-section-label">
                      普通成员
                    </div>
                    <div
                      v-for="m in group.members || []"
                      :key="m"
                      class="search-option"
                      @mousedown.prevent="selectPromoteTarget(group.id, m)"
                    >
                      <img
                        :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${m}`"
                        class="option-avatar"
                        alt=""
                      />
                      <div class="option-info">
                        <span class="option-email">{{ m }}</span>
                      </div>
                    </div>
                    <div
                      v-if="!(group.members || []).length && !(group.admins || []).length"
                      class="search-empty"
                    >
                      暂无成员
                    </div>
                  </div>
                </div>
                <button
                  class="add-btn admin-promote-btn"
                  :disabled="!promoteTarget[group.id] || opLoading[`${group.id}_addAdmin`]"
                  @click="addAdmin(group)"
                >
                  <svg
                    v-if="opLoading[`${group.id}_addAdmin`]"
                    class="spin-icon"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
                    />
                  </svg>
                  <svg
                    v-else
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.5"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z"
                    />
                  </svg>
                  {{ opLoading[`${group.id}_addAdmin`] ? '设置中...' : '设为管理员' }}
                </button>
              </div>
            </div>
          </div>

          <!-- ── SCOPES ── -->
          <div class="section-label mt">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
              />
            </svg>
            共享文件夹 ({{ (group.scopes || []).length }})
          </div>

          <div class="scopes-list">
            <div
              v-for="scope in group.scopes || []"
              :key="scope"
              class="scope-chip"
              :class="{ removing: opLoading[`${group.id}_rmScope_${scope}`] }"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
                />
              </svg>
              <span>{{ scopeDisplayName(scope) }}</span>
              <button
                v-if="canManage(group)"
                class="chip-remove"
                :disabled="opLoading[`${group.id}_rmScope_${scope}`]"
                @click="removeScope(group, scope)"
              >
                ×
              </button>
            </div>
            <div v-if="(group.scopes || []).length === 0" class="empty-hint">暂无共享文件夹</div>
          </div>

          <div v-if="canManage(group)" class="add-member-row">
            <div class="search-wrap">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
                class="search-icon"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
                />
              </svg>
              <input
                v-model="addScopeInputs[group.id]"
                type="text"
                class="ds-input sm search-input"
                placeholder="输入或选择同步任务文件夹..."
                :disabled="opLoading[`${group.id}_addScope`]"
                @keyup.enter="addScope(group)"
                @input="onScopeInput(group.id)"
                @focus="showScopeDropdown[group.id] = true"
                @blur="hideScopeDropdownDelayed(group.id)"
              />
              <div
                v-if="showScopeDropdown[group.id] && scopeFilteredSuggestions(group).length"
                class="search-dropdown"
              >
                <div
                  v-for="task in scopeFilteredSuggestions(group)"
                  :key="task.alias || task.id"
                  class="search-option"
                  @mousedown.prevent="selectScope(group, task)"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.5"
                    class="option-scope-icon"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-8.69-6.44l-2.12-2.12a1.5 1.5 0 00-1.061-.44H4.5A2.25 2.25 0 002.25 6v12a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9a2.25 2.25 0 00-2.25-2.25h-5.379a1.5 1.5 0 01-1.06-.44z"
                    />
                  </svg>
                  <div class="option-info">
                    <span class="option-name">{{ task.alias }}</span>
                    <span class="option-email">{{ task.path }}</span>
                  </div>
                </div>
              </div>
            </div>
            <button
              class="add-btn"
              :disabled="!addScopeInputs[group.id] || opLoading[`${group.id}_addScope`]"
              @click="addScope(group)"
            >
              <svg
                v-if="opLoading[`${group.id}_addScope`]"
                class="spin-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
                />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              {{ opLoading[`${group.id}_addScope`] ? '添加中...' : '共享' }}
            </button>
          </div>

          <!-- Per-card error -->
          <div v-if="opErrors[group.id]" class="card-error">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
              />
            </svg>
            {{ opErrors[group.id] }}
          </div>
        </div>
      </div>
    </main>

    <!-- Batch preview modal -->
    <Teleport to="body">
      <div v-if="batchPreview.show" class="modal-overlay" @click.self="batchPreview.show = false">
        <div class="modal-panel">
          <div class="modal-hd">
            <h3>{{ batchPreview.mode === 'add' ? '批量添加成员' : '批量移除成员' }}</h3>
            <button class="modal-close" @click="batchPreview.show = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <p class="batch-hint">从文件中解析到 {{ batchPreview.emails.length }} 个邮箱：</p>
            <div class="batch-email-list">
              <div v-for="e in batchPreview.emails" :key="e" class="batch-email-item">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                  class="batch-email-icon"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75"
                  />
                </svg>
                {{ e }}
              </div>
            </div>
            <div v-if="batchPreview.emails.length === 0" class="empty-hint">未找到有效邮箱</div>
          </div>
          <div class="modal-ft">
            <button class="btn-cancel" @click="batchPreview.show = false">取消</button>
            <button
              class="btn-primary"
              :class="{ 'btn-danger-soft': batchPreview.mode === 'remove' }"
              :disabled="batchPreview.emails.length === 0 || batchPreview.loading"
              @click="confirmBatch"
            >
              <svg
                v-if="batchPreview.loading"
                class="spin-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
                />
              </svg>
              {{
                batchPreview.loading
                  ? '处理中...'
                  : batchPreview.mode === 'add'
                    ? '确认添加'
                    : '确认移除'
              }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Create group modal -->
    <Teleport to="body">
      <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
        <div class="modal-panel">
          <div class="modal-hd">
            <h3>新建群组</h3>
            <button class="modal-close" @click="showCreateModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field-group">
              <label class="field-label">群组名称</label>
              <input
                v-model="newGroupName"
                type="text"
                class="ds-input"
                placeholder="输入群组名称..."
              />
            </div>
            <div v-if="createError" class="modal-error">{{ createError }}</div>
          </div>
          <div class="modal-ft">
            <button class="btn-cancel" @click="showCreateModal = false">取消</button>
            <button class="btn-primary" :disabled="isCreating" @click="doCreateGroup">
              {{ isCreating ? '创建中...' : '立即创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Delete confirm -->
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
          <h3>确认删除群组？</h3>
          <p>
            即将删除群组 <strong>{{ deleteTarget?.name }}</strong
            >，此操作不可撤销。
          </p>
          <div class="confirm-actions">
            <button class="btn-cancel" @click="showDeleteConfirm = false">取消</button>
            <button class="btn-danger" :disabled="isDeleting" @click="doDeleteGroup">
              {{ isDeleting ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import HttpManager from '../utils/request'

const router = useRouter()
const currentUser = reactive({ id: null, username: '', email: '', avatar: '' })

const groups = ref([])
const isLoading = ref(false)
const loadError = ref('')

const opLoading = reactive({})
const opErrors = reactive({})

const searchInputs = reactive({})
const searchResults = reactive({})
const showDropdown = reactive({})
const selectedSearchUsers = reactive({})
const memberFilter = reactive({})
const addScopeInputs = reactive({})
const selectedScopeTasks = reactive({})
const promoteTarget = reactive({})
const batchFileRefs = reactive({})
const batchMode = reactive({})
const userTasks = ref([])
const showScopeDropdown = reactive({})
const showPromoteDropdown = reactive({})

const batchPreview = reactive({
  show: false,
  mode: 'add',
  emails: [],
  groupId: null,
  group: null,
  loading: false
})

const showCreateModal = ref(false)
const newGroupName = ref('')
const createError = ref('')
const isCreating = ref(false)

const showDeleteConfirm = ref(false)
const deleteTarget = ref(null)
const isDeleting = ref(false)

let searchTimers = {}

// ── scope helpers ─────────────────────────────────────────
const getScopeFolderName = (task) =>
  (task.path || '').replace(/\\/g, '/').split('/').filter(Boolean).pop() || task.alias || ''

const getScopeKey = (task) => [currentUser.email, task.alias, getScopeFolderName(task)].join('/')

const scopeDisplayName = (scope) => {
  const idx = scope.indexOf('/')
  return idx >= 0 ? scope.slice(idx + 1) : scope
}

// ── scope dropdown helpers ────────────────────────────────
const scopeFilteredSuggestions = (group) => {
  const input = (addScopeInputs[group.id] || '').trim().toLowerCase()
  const existing = new Set(group.scopes || [])
  return userTasks.value.filter((t) => {
    if (!t.alias) return false
    if (existing.has(getScopeKey(t))) return false
    return (
      !input ||
      t.alias.toLowerCase().includes(input) ||
      getScopeFolderName(t).toLowerCase().includes(input)
    )
  })
}

const hideScopeDropdownDelayed = (groupId) => {
  setTimeout(() => {
    showScopeDropdown[groupId] = false
  }, 200)
}

const onScopeInput = (groupId) => {
  showScopeDropdown[groupId] = true
  selectedScopeTasks[groupId] = null
}

const selectScope = (group, task) => {
  addScopeInputs[group.id] = task.alias
  selectedScopeTasks[group.id] = task
  showScopeDropdown[group.id] = false
}

// ── promote dropdown helpers ──────────────────────────────
const closePromoteDropdowns = () => {
  Object.keys(showPromoteDropdown).forEach((k) => {
    showPromoteDropdown[k] = false
  })
}

const togglePromoteDropdown = (groupId, event) => {
  event.stopPropagation()
  const next = !showPromoteDropdown[groupId]
  closePromoteDropdowns()
  showPromoteDropdown[groupId] = next
}

const selectPromoteTarget = (groupId, email) => {
  promoteTarget[groupId] = email
  showPromoteDropdown[groupId] = false
}

// ── helpers ──────────────────────────────────────────────
const isInGroup = (group, email) =>
  email === group.ownerEmail ||
  (group.admins || []).includes(email) ||
  (group.members || []).includes(email)

const canManage = (group) =>
  group.ownerEmail === currentUser.email || (group.admins || []).includes(currentUser.email)

const totalMemberCount = (group) => (group.admins || []).length + (group.members || []).length

const filteredMembers = (group) => {
  const q = (memberFilter[group.id] || '').toLowerCase()
  if (!q) return group.members || []
  return (group.members || []).filter((m) => m.toLowerCase().includes(q))
}

const patchGroup = (updated) => {
  if (!updated?.id) return
  const idx = groups.value.findIndex((g) => g.id === updated.id)
  if (idx !== -1) groups.value[idx] = updated
}

const setCardError = (groupId, msg) => {
  opErrors[groupId] = msg
  setTimeout(() => {
    opErrors[groupId] = ''
  }, 4000)
}

// ── load ─────────────────────────────────────────────────
const loadGroups = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const res = await HttpManager.post('/client/group/list', { email: currentUser.email })
    const data = res?.data ?? res
    groups.value = Array.isArray(data) ? data : []
  } catch (err) {
    loadError.value = err.request ? '无法连接服务器' : err.message || '加载失败'
  } finally {
    isLoading.value = false
  }
}

// ── create / delete ───────────────────────────────────────
const openCreateModal = () => {
  newGroupName.value = ''
  createError.value = ''
  showCreateModal.value = true
}

const doCreateGroup = async () => {
  if (!newGroupName.value.trim()) {
    createError.value = '请输入群组名称'
    return
  }
  isCreating.value = true
  try {
    await HttpManager.post('/client/group/create', {
      email: currentUser.email,
      name: newGroupName.value.trim()
    })
    showCreateModal.value = false
    await loadGroups()
  } catch (err) {
    createError.value = err.message || '创建失败'
  } finally {
    isCreating.value = false
  }
}

const confirmDeleteGroup = (group) => {
  deleteTarget.value = group
  showDeleteConfirm.value = true
}

const doDeleteGroup = async () => {
  if (!deleteTarget.value) return
  isDeleting.value = true
  try {
    await HttpManager.post('/client/group/delete', {
      email: currentUser.email,
      groupId: deleteTarget.value.id
    })
    groups.value = groups.value.filter((g) => g.id !== deleteTarget.value.id)
    showDeleteConfirm.value = false
  } catch (err) {
    console.error('删除群组失败', err)
  } finally {
    isDeleting.value = false
  }
}

// ── search & add member ───────────────────────────────────
const onSearchInput = (groupId) => {
  clearTimeout(searchTimers[groupId])
  selectedSearchUsers[groupId] = null
  const q = (searchInputs[groupId] || '').trim()
  if (!q) {
    searchResults[groupId] = []
    return
  }
  searchTimers[groupId] = setTimeout(async () => {
    try {
      const res = await HttpManager.post('/client/user/search', { q })
      const data = res?.data ?? res
      searchResults[groupId] = Array.isArray(data) ? data : []
      showDropdown[groupId] = true
    } catch {
      searchResults[groupId] = []
    }
  }, 280)
}

const hideDropdownDelayed = (groupId) => {
  setTimeout(() => {
    showDropdown[groupId] = false
  }, 200)
}

const selectUser = (group, user) => {
  searchInputs[group.id] = user.email
  selectedSearchUsers[group.id] = user
  showDropdown[group.id] = false
}

const addFromSearch = async (group) => {
  let selected = selectedSearchUsers[group.id]
  if (!selected && searchResults[group.id]?.length) {
    selected = searchResults[group.id][0]
    selectUser(group, selected)
  }
  // Allow direct email input when search returns no results
  const inputText = (searchInputs[group.id] || '').trim()
  const email = selected?.email || (inputText.includes('@') ? inputText : '')
  if (!email) {
    setCardError(group.id, '请输入完整的用户邮箱地址后添加')
    return
  }
  opLoading[`${group.id}_addMember`] = true
  try {
    const res = await HttpManager.post('/client/group/add-member', {
      email: currentUser.email,
      groupId: group.id,
      memberEmail: email
    })
    patchGroup(res?.data ?? res)
    searchInputs[group.id] = ''
    searchResults[group.id] = []
    selectedSearchUsers[group.id] = null
  } catch (err) {
    setCardError(group.id, err.message || '添加成员失败')
  } finally {
    opLoading[`${group.id}_addMember`] = false
  }
}

// ── remove member ────────────────────────────────────────
const removeMember = async (group, memberEmail) => {
  opLoading[`${group.id}_rm_${memberEmail}`] = true
  try {
    const res = await HttpManager.post('/client/group/remove-member', {
      email: currentUser.email,
      groupId: group.id,
      memberEmail
    })
    patchGroup(res?.data ?? res)
  } catch (err) {
    setCardError(group.id, err.message || '移除成员失败')
  } finally {
    opLoading[`${group.id}_rm_${memberEmail}`] = false
  }
}

// ── admin ─────────────────────────────────────────────────
const addAdmin = async (group) => {
  const adminEmail = promoteTarget[group.id]
  if (!adminEmail) return
  opLoading[`${group.id}_addAdmin`] = true
  try {
    await HttpManager.post('/client/group/add-admin', {
      email: currentUser.email,
      groupId: group.id,
      adminEmail
    })
    promoteTarget[group.id] = ''
    await loadGroups()
  } catch (err) {
    setCardError(group.id, err.message || '设置管理员失败')
  } finally {
    opLoading[`${group.id}_addAdmin`] = false
  }
}

const removeAdmin = async (group, adminEmail) => {
  opLoading[`${group.id}_rmAdmin_${adminEmail}`] = true
  try {
    await HttpManager.post('/client/group/remove-admin', {
      email: currentUser.email,
      groupId: group.id,
      adminEmail
    })
    await loadGroups()
  } catch (err) {
    setCardError(group.id, err.message || '撤销管理员失败')
  } finally {
    opLoading[`${group.id}_rmAdmin_${adminEmail}`] = false
  }
}

// ── batch file ────────────────────────────────────────────
const triggerBatchFile = (groupId, mode) => {
  batchMode[groupId] = mode
  batchFileRefs[groupId]?.click()
}

const handleBatchFile = (event, group) => {
  const file = event.target.files?.[0]
  if (!file) return
  const mode = batchMode[group.id] || 'add'
  const reader = new FileReader()
  reader.onload = (e) => {
    const text = e.target.result || ''
    const emails = text
      .split(/[\r\n,;]+/)
      .map((s) => s.trim())
      .filter((s) => s.includes('@'))
    batchPreview.show = true
    batchPreview.mode = mode
    batchPreview.emails = emails
    batchPreview.group = group
    batchPreview.groupId = group.id
  }
  reader.readAsText(file)
  event.target.value = ''
}

const confirmBatch = async () => {
  const { mode, emails, group } = batchPreview
  if (!emails.length) return
  batchPreview.loading = true
  const endpoint = mode === 'add' ? '/client/group/add-members' : '/client/group/remove-members'
  try {
    const res = await HttpManager.post(endpoint, {
      email: currentUser.email,
      groupId: group.id,
      memberEmails: emails
    })
    patchGroup(res?.data ?? res)
    batchPreview.show = false
  } catch (err) {
    setCardError(group.id, err.message || (mode === 'add' ? '批量添加失败' : '批量移除失败'))
    batchPreview.show = false
  } finally {
    batchPreview.loading = false
  }
}

// ── scopes ────────────────────────────────────────────────
const addScope = async (group) => {
  const input = (addScopeInputs[group.id] || '').trim()
  if (!input) return
  // 新布局下 scope 名形如 email/alias/rootName，必须从任务中拿。若用户没从下拉里选，按 alias 在本地任务列表里精确匹配一次，
  // 兜不到就直接报错——不再回退到旧的 email/X 双段格式，避免在桶里生成永远不会有数据进来的孤儿 scope。
  let task = selectedScopeTasks[group.id]
  if (!task) {
    task = userTasks.value.find((t) => t.alias && t.alias.trim() === input)
  }
  if (!task) {
    setCardError(group.id, '找不到对应的同步任务，请从下拉中选择已存在的任务')
    return
  }
  const scopeName = getScopeKey(task)
  opLoading[`${group.id}_addScope`] = true
  try {
    const res = await HttpManager.post('/client/group/add-scope', {
      email: currentUser.email,
      groupId: group.id,
      scopeName
    })
    patchGroup(res?.data ?? res)
    addScopeInputs[group.id] = ''
    selectedScopeTasks[group.id] = null
  } catch (err) {
    setCardError(group.id, err.message || '添加文件夹失败')
  } finally {
    opLoading[`${group.id}_addScope`] = false
  }
}

const removeScope = async (group, scopeName) => {
  opLoading[`${group.id}_rmScope_${scopeName}`] = true
  try {
    const res = await HttpManager.post('/client/group/remove-scope', {
      email: currentUser.email,
      groupId: group.id,
      scopeName
    })
    patchGroup(res?.data ?? res)
  } catch (err) {
    setCardError(group.id, err.message || '移除文件夹失败')
  } finally {
    opLoading[`${group.id}_rmScope_${scopeName}`] = false
  }
}

onMounted(async () => {
  const saved = localStorage.getItem('userInfo')
  if (saved) Object.assign(currentUser, JSON.parse(saved))
  loadGroups()
  try {
    const email = JSON.parse(localStorage.getItem('userInfo') || '{}').email
    if (email) {
      const res = await HttpManager.post('/client/file/brief-list', { email })
      const data = res?.data ?? res
      userTasks.value = Array.isArray(data) ? data : []
    }
  } catch {
    userTasks.value = []
  }
  document.addEventListener('click', closePromoteDropdowns)
})

onUnmounted(() => {
  document.removeEventListener('click', closePromoteDropdowns)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=IBM+Plex+Mono:wght@400;500&display=swap');

*,
*::before,
*::after {
  box-sizing: border-box;
}

.app-root {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f8fafd;
  font-family: 'Syne', sans-serif;
  color: #202124;
}

.app-header {
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
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #dadce0;
  background: #fff;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-btn:hover {
  background: #f1f3f4;
  color: #1a73e8;
  border-color: #c9d7f8;
}
.back-btn svg {
  width: 16px;
  height: 16px;
}
.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-icon svg {
  width: 18px;
  height: 18px;
  color: #1a73e8;
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #202124;
}
.user-label {
  font-size: 13px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
}

.app-main {
  flex: 1;
  padding: 24px;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}
.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #202124;
  margin: 0 0 4px;
}
.section-sub {
  font-size: 13px;
  color: #5f6368;
  margin: 0;
}
.header-actions {
  display: flex;
  gap: 10px;
}

.refresh-btn,
.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #dadce0;
  background: #fff;
  color: #5f6368;
  cursor: pointer;
  font-family: 'Syne', sans-serif;
  font-size: 13px;
  transition: all 0.2s;
}
.refresh-btn svg,
.create-btn svg {
  width: 15px;
  height: 15px;
}
.refresh-btn:hover {
  background: #f8fafd;
  color: #1a73e8;
  border-color: #c9d7f8;
}
.refresh-btn.spinning svg {
  animation: spin 0.8s linear infinite;
}
.create-btn {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
  font-weight: 600;
}
.create-btn:hover {
  background: #1765cc;
}

.loading-state {
  text-align: center;
  padding: 60px;
  color: #5f6368;
}
.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e8f0fe;
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-state {
  text-align: center;
  padding: 40px;
  color: #d93025;
}
.error-state button {
  margin-top: 12px;
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: #1a73e8;
  color: #fff;
  cursor: pointer;
}

.empty-state {
  text-align: center;
  padding: 60px;
  color: #80868b;
}
.empty-state svg {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  display: block;
}
.empty-state p {
  font-size: 14px;
}

.group-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-card {
  background: #fff;
  border: 1px solid #e0e3e7;
  border-radius: 12px;
  padding: 20px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}
.group-card:hover {
  border-color: #c9d7f8;
  box-shadow: 0 2px 8px rgba(60, 64, 67, 0.1);
}

.group-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.group-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.group-icon svg {
  width: 20px;
  height: 20px;
  color: #1a73e8;
}
.group-title-area {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.group-name {
  font-size: 16px;
  font-weight: 700;
  color: #202124;
  margin: 0;
}

.role-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 99px;
  font-weight: 600;
}
.role-badge.owner {
  background: #e8f0fe;
  color: #1a73e8;
  border: 1px solid #c9d7f8;
}
.role-badge.admin {
  background: #fef3c7;
  color: #d97706;
  border: 1px solid #fde68a;
}
.role-badge.member {
  background: #f1f3f4;
  color: #5f6368;
  border: 1px solid #dadce0;
}

.delete-group-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #fad2cf;
  background: #fff;
  color: #d93025;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.delete-group-btn:hover {
  background: #fce8e6;
}
.delete-group-btn svg {
  width: 14px;
  height: 14px;
}

.group-owner-email {
  font-size: 12px;
  color: #5f6368;
  margin: 0 0 14px;
  font-family: 'IBM Plex Mono', monospace;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.section-label.mt {
  margin-top: 16px;
}
.section-label svg {
  width: 13px;
  height: 13px;
}

.sub-label {
  font-size: 11px;
  font-weight: 600;
  color: #9aa0a6;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin: 8px 0 4px;
}

.members-list,
.scopes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
  margin-bottom: 6px;
}

.member-chip,
.scope-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 99px;
  background: #f8fafd;
  border: 1px solid #e0e3e7;
  font-size: 12px;
  color: #3c4043;
}
.admin-chip {
  background: #fffbeb;
  border-color: #fde68a;
}
.chip-avatar {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  flex-shrink: 0;
}
.chip-icon {
  width: 12px;
  height: 12px;
  color: #d97706;
  flex-shrink: 0;
}
.scope-chip svg {
  width: 12px;
  height: 12px;
  color: #1a73e8;
}

.chip-remove {
  background: none;
  border: none;
  color: #80868b;
  cursor: pointer;
  padding: 0 0 0 2px;
  font-size: 14px;
  line-height: 1;
  transition: color 0.15s;
}
.chip-remove:hover:not(:disabled) {
  color: #d93025;
}
.chip-remove:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.member-chip.removing,
.scope-chip.removing {
  opacity: 0.35;
  pointer-events: none;
  transition: opacity 0.2s;
}

.empty-hint {
  font-size: 12px;
  color: #80868b;
  padding: 4px 0;
}

.manage-area {
  margin-top: 10px;
  padding: 12px;
  background: #f8fafd;
  border-radius: 8px;
  border: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.add-section-label {
  font-size: 11px;
  font-weight: 600;
  color: #9aa0a6;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.filter-icon {
  width: 14px;
  height: 14px;
  color: #9aa0a6;
  flex-shrink: 0;
}
.filter-input {
  flex: 1;
}

.search-add-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.search-wrap {
  flex: 1;
  position: relative;
}
.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 14px;
  height: 14px;
  color: #9aa0a6;
  pointer-events: none;
}
.search-input {
  padding-left: 30px !important;
  width: 100%;
}

.search-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e0e3e7;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(60, 64, 67, 0.15);
  z-index: 50;
  max-height: 200px;
  overflow-y: auto;
}
.search-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.1s;
}
.search-option:hover {
  background: #f1f3f4;
}
.search-empty {
  padding: 10px 12px;
  color: #80868b;
  font-size: 12px;
}
.option-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  flex-shrink: 0;
}
.option-info {
  display: flex;
  flex-direction: column;
}
.option-name {
  font-size: 13px;
  font-weight: 600;
  color: #202124;
}
.option-email {
  font-size: 11px;
  color: #5f6368;
  font-family: 'IBM Plex Mono', monospace;
}

.batch-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.batch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #dadce0;
  background: #fff;
  color: #5f6368;
  cursor: pointer;
  font-size: 12px;
  font-family: 'Syne', sans-serif;
  font-weight: 600;
  transition: all 0.15s;
}
.batch-btn svg {
  width: 13px;
  height: 13px;
}
.batch-btn:hover {
  background: #f1f3f4;
  border-color: #bdc1c6;
}
.batch-btn.remove-batch {
  color: #d93025;
  border-color: #fad2cf;
}
.batch-btn.remove-batch:hover {
  background: #fce8e6;
}
.hidden-file {
  display: none;
}

.admin-promote-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 6px;
  border-top: 1px solid #e8eaed;
}
.promote-wrap {
  display: flex;
  gap: 8px;
  align-items: center;
}
.ds-select {
  flex: 1;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 6px 10px;
  color: #202124;
  font-family: 'Syne', sans-serif;
  font-size: 12px;
  outline: none;
}
.ds-select:focus {
  border-color: #1a73e8;
}
.admin-promote-btn {
  background: #fffbeb;
  border-color: #fde68a;
  color: #d97706;
}
.admin-promote-btn:hover:not(:disabled) {
  background: #fef3c7;
}

.add-member-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}
.ds-input {
  flex: 1;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 8px;
  padding: 8px 12px;
  color: #202124;
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  outline: none;
}
.ds-input.sm {
  padding: 6px 10px;
  font-size: 12px;
}
.ds-input::placeholder {
  color: #9aa0a6;
}
.ds-input:focus {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}
.ds-input:disabled {
  background: #f8fafd;
  color: #9aa0a6;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid #c9d7f8;
  background: #e8f0fe;
  color: #1a73e8;
  cursor: pointer;
  font-size: 12px;
  font-family: 'Syne', sans-serif;
  font-weight: 600;
  white-space: nowrap;
  transition: background 0.15s;
}
.add-btn:hover:not(:disabled) {
  background: #d2e3fc;
}
.add-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.add-btn svg {
  width: 13px;
  height: 13px;
}

.spin-icon {
  width: 13px;
  height: 13px;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}

.card-error {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #fce8e6;
  border: 1px solid rgba(217, 48, 37, 0.2);
  border-radius: 8px;
  color: #d93025;
  font-size: 12px;
  animation: slideDown 0.2s ease;
}
.card-error svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Batch preview modal */
.batch-hint {
  font-size: 13px;
  color: #5f6368;
  margin: 0 0 10px;
}
.batch-email-list {
  max-height: 200px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.batch-email-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #3c4043;
  font-family: 'IBM Plex Mono', monospace;
  padding: 4px 8px;
  background: #f8fafd;
  border-radius: 6px;
}
.batch-email-icon {
  width: 13px;
  height: 13px;
  color: #1a73e8;
  flex-shrink: 0;
}

/* Modals */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(32, 33, 36, 0.36);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}
.modal-panel {
  background: #fff;
  border: 1px solid #e0e3e7;
  border-radius: 12px;
  padding: 28px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 8px 24px rgba(60, 64, 67, 0.18);
}
.modal-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.modal-hd h3 {
  font-size: 17px;
  font-weight: 700;
  color: #202124;
  margin: 0;
}
.modal-close {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #dadce0;
  background: #fff;
  color: #5f6368;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-close:hover {
  color: #202124;
  background: #f1f3f4;
}
.modal-close svg {
  width: 14px;
  height: 14px;
}
.modal-body {
  margin-bottom: 20px;
}
.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #5f6368;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.modal-error {
  color: #d93025;
  font-size: 13px;
  margin-top: 10px;
}
.modal-ft {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.btn-cancel {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #dadce0;
  background: #fff;
  color: #5f6368;
  cursor: pointer;
  font-family: 'Syne', sans-serif;
}
.btn-cancel:hover {
  color: #1a73e8;
  background: #f8fafd;
  border-color: #c9d7f8;
}
.btn-primary {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: #1a73e8;
  color: #fff;
  cursor: pointer;
  font-family: 'Syne', sans-serif;
  font-weight: 600;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-danger-soft {
  background: #ef4444;
}

/* Confirm */
.confirm-panel {
  background: #fff;
  border: 1px solid #fad2cf;
  border-radius: 12px;
  padding: 32px;
  width: 360px;
  max-width: 90vw;
  text-align: center;
  box-shadow: 0 8px 24px rgba(60, 64, 67, 0.18);
}
.confirm-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fce8e6;
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
  margin: 0 0 8px;
}
.confirm-panel p {
  font-size: 13px;
  color: #5f6368;
  margin: 0 0 24px;
}
.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}
.btn-danger {
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  background: #ef4444;
  color: #fff;
  cursor: pointer;
  font-family: 'Syne', sans-serif;
  font-weight: 600;
}
.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.promote-select-wrap {
  flex: 1;
  position: relative;
}
.promote-trigger {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  text-align: left;
  font-family: 'IBM Plex Mono', monospace;
  background: #fff;
  border: 1px solid #dadce0;
}
.promote-trigger:hover {
  border-color: #1a73e8;
}
.promote-arrow {
  width: 14px;
  height: 14px;
  color: #9aa0a6;
  flex-shrink: 0;
  transition: transform 0.15s;
}
.promote-dropdown {
  position: absolute;
  z-index: 51;
}
.option-scope-icon {
  width: 16px;
  height: 16px;
  color: #1a73e8;
  flex-shrink: 0;
}

.option-disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f8fafd !important;
}
.option-badge {
  margin-left: auto;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 99px;
  background: #e8f0fe;
  color: #1a73e8;
  border: 1px solid #c9d7f8;
  white-space: nowrap;
  flex-shrink: 0;
}
.admin-badge {
  background: #fffbeb;
  color: #d97706;
  border-color: #fde68a;
}
.dropdown-section-label {
  font-size: 10px;
  font-weight: 600;
  color: #9aa0a6;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 6px 12px 2px;
}
</style>
