<template>
  <el-container class="layout-container">
    <!-- ===== Glass Header ===== -->
    <el-header class="layout-header">
      <div class="header-left">
        <div class="brand">
          <div class="brand-icon">
            <img src="/icon.png" width="22" height="22" alt="青创协同" />
          </div>
          <div class="brand-text">
            <span class="brand-title">青创协同</span>
            <span class="brand-subtitle">QingChuang XieTong</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <!-- Notification -->
        <el-tooltip content="通知消息" placement="bottom">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0">
            <button class="header-icon-btn" @click="showNotifications = true">
              <el-icon :size="20"><Bell /></el-icon>
            </button>
          </el-badge>
        </el-tooltip>
        <!-- User Dropdown -->
        <el-dropdown trigger="click" class="user-dropdown">
          <div class="user-info">
            <el-avatar :size="36" :src="userStore.userInfo?.avatar" class="user-avatar">
              {{ (userStore.username || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="user-text">
              <span class="user-name">{{ userStore.username || '用户' }}</span>
              <span class="user-role">{{ roleLabel }}</span>
            </div>
            <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item @click="router.push('/admin')" v-if="userStore.isAdmin">
                <el-icon><Setting /></el-icon>管理后台
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- ===== Icon Sidebar ===== -->
      <el-aside
        :width="isExpanded ? '220px' : '64px'"
        class="layout-aside"
        @mouseenter="handleSidebarHover(true)"
        @mouseleave="handleSidebarHover(false)"
      >
        <div class="sidebar-trigger" />
        <el-menu
          :default-active="route.path"
          :collapse="!isExpanded"
          :collapse-transition="false"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <template #title>
              <span class="menu-title-text">工作台</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/recruit">
            <el-icon><Search /></el-icon>
            <template #title>
              <span class="menu-title-text">招募广场</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/teams">
            <el-icon><UserFilled /></el-icon>
            <template #title>
              <span class="menu-title-text">我的团队</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/projects">
            <el-icon><FolderOpened /></el-icon>
            <template #title>
              <span class="menu-title-text">项目管理</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/tasks">
            <el-icon><List /></el-icon>
            <template #title>
              <span class="menu-title-text">任务中心</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/competitions">
            <el-icon><Trophy /></el-icon>
            <template #title>
              <span class="menu-title-text">竞赛中心</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/teachers">
            <el-icon><Reading /></el-icon>
            <template #title>
              <span class="menu-title-text">导师库</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/achievements">
            <el-icon><Collection /></el-icon>
            <template #title>
              <span class="menu-title-text">成果展厅</span>
            </template>
          </el-menu-item>
          <el-menu-item index="/ai-chat">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>
              <span class="menu-title-text">AI 助手</span>
            </template>
          </el-menu-item>
          <div class="sidebar-divider" />
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <template #title>
              <span class="menu-title-text">个人中心</span>
            </template>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/admin">
            <el-icon><Setting /></el-icon>
            <template #title>
              <span class="menu-title-text">管理后台</span>
            </template>
          </el-menu-item>
        </el-menu>

        <!-- Collapse tooltips when icon-only -->
        <div v-show="!isExpanded" class="sidebar-tooltips">
          <div v-for="item in menuItems" :key="item.index" class="tooltip-item" @click="router.push(item.index)">
            <el-tooltip :content="item.title" placement="right" :show-after="300">
              <div class="tooltip-trigger" />
            </el-tooltip>
          </div>
        </div>
      </el-aside>

      <!-- ===== Main Content ===== -->
      <el-main class="layout-main">
        <div class="page-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>

    <!-- ===== Notification Drawer ===== -->
    <el-drawer v-model="showNotifications" :size="420" @open="loadNotifications">
      <template #header>
        <div class="drawer-header">
          <div class="drawer-title">
            <el-icon :size="22"><Bell /></el-icon>
            <span>通知消息</span>
          </div>
          <el-tag v-if="unreadCount > 0" type="danger" size="small" round>
            {{ unreadCount }} 条未读
          </el-tag>
        </div>
      </template>
      <div v-if="notifications.length === 0" class="notif-empty">
        <div class="empty-icon">
          <el-icon :size="52"><Bell /></el-icon>
        </div>
        <p class="empty-text">暂无通知消息</p>
        <p class="empty-hint">当有新消息时，会在这里显示</p>
      </div>
      <div v-else class="notif-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notif-item"
          :class="{ unread: !n.isRead }"
          @click="handleRead(n)"
        >
          <div class="notif-indicator" />
          <div class="notif-content">
            <div class="notif-title">{{ n.title }}</div>
            <div class="notif-body">{{ n.content }}</div>
            <div class="notif-time">{{ n.createdAt }}</div>
          </div>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { listNotifications, getUnreadCount, markAsRead } from '@/api/notification'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isExpanded = ref(false)
const showNotifications = ref(false)
const notifications = ref<any[]>([])
const unreadCount = ref(0)
let hoverTimer: ReturnType<typeof setTimeout> | null = null

const menuItems = [
  { index: '/dashboard', title: '工作台' },
  { index: '/recruit', title: '招募广场' },
  { index: '/teams', title: '我的团队' },
  { index: '/projects', title: '项目管理' },
  { index: '/tasks', title: '任务中心' },
  { index: '/competitions', title: '竞赛中心' },
  { index: '/teachers', title: '导师库' },
  { index: '/achievements', title: '成果展厅' },
  { index: '/ai-chat', title: 'AI 助手' },
  { index: '/profile', title: '个人中心' },
]

const roleLabel = computed(() => {
  const role = userStore.userInfo?.role
  const map: Record<string, string> = { ADMIN: '管理员', TEACHER: '导师', REVIEWER: '审核员' }
  return map[role] || '学生'
})

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data || 0
    } catch (_) {}
  }
})

function handleSidebarHover(enter: boolean) {
  if (hoverTimer) clearTimeout(hoverTimer)
  if (enter) {
    hoverTimer = setTimeout(() => { isExpanded.value = true }, 150)
  } else {
    hoverTimer = setTimeout(() => { isExpanded.value = false }, 200)
  }
}

async function loadNotifications() {
  try {
    const res = await listNotifications()
    notifications.value = res.data || []
  } catch (_) {}
}

async function handleRead(n: any) {
  if (!n.isRead) {
    await markAsRead(n.id)
    n.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== Glass Header ===== */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--glass-bg) !important;
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  height: 64px !important;
  padding: 0 24px !important;
  border-bottom: 1px solid var(--color-header-border);
  z-index: 100;
  flex-shrink: 0;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  border-radius: var(--radius-sm);
  color: #fff;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.brand-subtitle {
  font-size: 10px;
  font-weight: 500;
  color: var(--color-text-muted);
  line-height: 1.2;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--color-text-muted);
  transition: all var(--transition-fast);
  position: relative;
}
.header-icon-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

/* User Dropdown */
.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 14px 4px 4px;
  border-radius: 28px;
  background: var(--color-bg);
  transition: all var(--transition-fast);
  border: 1px solid transparent;
}
.user-info:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-light);
}

.user-avatar {
  background: var(--gradient-primary) !important;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.user-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.user-role {
  font-size: 11px;
  color: var(--color-text-muted);
}

.dropdown-arrow {
  font-size: 13px;
  color: var(--color-text-muted);
  transition: transform 0.2s;
}

/* ===== Icon Sidebar ===== */
.main-container {
  flex: 1;
  overflow: hidden;
}

.layout-aside {
  background: var(--color-sidebar-bg);
  border-right: 1px solid var(--color-border-light);
  height: 100%;
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  position: relative;
}

.sidebar-trigger {
  position: absolute;
  top: 0;
  right: -8px;
  width: 8px;
  height: 100%;
  z-index: 10;
}

.sidebar-menu {
  border-right: none !important;
  padding: 8px;
  height: 100%;
}

.sidebar-menu :deep(.el-menu-item) {
  margin: 2px 0;
  border-radius: var(--radius-sm);
  height: 42px;
  line-height: 42px;
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  position: relative;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--color-sidebar-active);
  color: var(--color-primary);
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: var(--color-border);
  border-radius: 0 2px 2px 0;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--color-sidebar-hover);
  color: var(--color-text-primary);
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 19px;
}

.menu-title-text {
  font-size: 14px;
  font-weight: 500;
}

.sidebar-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 8px 12px;
}

.sidebar-tooltips {
  display: none;
}

/* ===== Main Content ===== */
.layout-main {
  background: var(--color-bg-page);
  height: 100%;
  overflow-y: auto;
  padding: 28px 32px !important;
}

.page-wrapper {
  max-width: 1440px;
  margin: 0 auto;
}

/* ===== Page Transition ===== */
.page-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ===== Notification Drawer ===== */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.drawer-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.notif-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-placeholder);
  margin-bottom: 20px;
}

.empty-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 0 0 6px 0;
}

.empty-hint {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.notif-list {
  display: flex;
  flex-direction: column;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  margin: 0 4px;
}
.notif-item:hover {
  background: var(--color-bg-hover);
}
.notif-item.unread {
  background: var(--color-primary-light);
}

.notif-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
  margin-top: 6px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}
.notif-item.unread .notif-indicator {
  opacity: 1;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.notif-body {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notif-time {
  font-size: 11px;
  color: var(--color-text-placeholder);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .layout-aside {
    position: fixed;
    left: 0;
    top: 64px;
    bottom: 0;
    z-index: 99;
    box-shadow: var(--shadow-xl);
    width: 64px !important;
  }
  .layout-aside:hover {
    width: 220px !important;
  }
  .layout-main {
    padding: 16px !important;
  }
  .layout-header {
    padding: 0 16px !important;
  }
  .brand-subtitle,
  .user-text {
    display: none;
  }
  .brand-title {
    font-size: 14px;
  }
}
</style>
