<template>
  <el-container class="layout-container">
    <!-- Header -->
    <el-header class="layout-header">
      <div class="header-left">
        <div class="brand">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-title">竞赛组队平台</span>
            <span class="brand-subtitle">Competition Team Platform</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <el-tooltip content="通知消息" placement="bottom">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notif-badge">
            <el-button class="header-btn" @click="showNotifications = true">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
        </el-tooltip>
        <el-dropdown trigger="click" class="user-dropdown">
          <div class="user-info">
            <el-avatar :size="34" :src="userStore.userInfo?.avatar" class="user-avatar">{{ (userStore.username || 'U').charAt(0).toUpperCase() }}</el-avatar>
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
      <!-- Sidebar -->
      <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
        <div class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <el-icon :size="18"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
        </div>
        <el-menu
          :default-active="route.path"
          :collapse="isCollapsed"
          :collapse-transition="false"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <template #title>工作台</template>
          </el-menu-item>
          <el-menu-item index="/recruit">
            <el-icon><Search /></el-icon>
            <template #title>招募广场</template>
          </el-menu-item>
          <el-menu-item index="/teams">
            <el-icon><UserFilled /></el-icon>
            <template #title>我的团队</template>
          </el-menu-item>
          <el-menu-item index="/projects">
            <el-icon><FolderOpened /></el-icon>
            <template #title>项目管理</template>
          </el-menu-item>
          <el-menu-item index="/tasks">
            <el-icon><List /></el-icon>
            <template #title>任务中心</template>
          </el-menu-item>
          <el-menu-item index="/competitions">
            <el-icon><Trophy /></el-icon>
            <template #title>竞赛中心</template>
          </el-menu-item>
          <el-menu-item index="/teachers">
            <el-icon><Reading /></el-icon>
            <template #title>导师库</template>
          </el-menu-item>
          <el-menu-item index="/achievements">
            <el-icon><Collection /></el-icon>
            <template #title>成果展厅</template>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <template #title>个人中心</template>
          </el-menu-item>
          <el-menu-item index="/admin" v-if="userStore.isAdmin">
            <el-icon><Setting /></el-icon>
            <template #title>管理后台</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- Main Content -->
      <el-main class="layout-main">
        <div class="page-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="page-fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>

    <!-- Notification Drawer -->
    <el-drawer v-model="showNotifications" title="通知消息" size="400px" @open="loadNotifications">
      <template #header>
        <div class="drawer-title">
          <el-icon :size="20"><Bell /></el-icon>
          <span>通知消息</span>
          <el-tag v-if="unreadCount > 0" type="danger" size="small">{{ unreadCount }} 条未读</el-tag>
        </div>
      </template>
      <div v-if="notifications.length === 0" class="notif-empty">
        <el-icon :size="48" color="#d0d5dd"><Bell /></el-icon>
        <p>暂无通知消息</p>
      </div>
      <div v-for="n in notifications" :key="n.id" class="notif-item" :class="{ unread: !n.isRead }" @click="handleRead(n)">
        <div class="notif-dot" v-if="!n.isRead" />
        <div class="notif-content">
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-body">{{ n.content }}</div>
          <div class="notif-time">{{ n.createdAt }}</div>
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

const isCollapsed = ref(false)
const showNotifications = ref(false)
const notifications = ref<any[]>([])
const unreadCount = ref(0)

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
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ===== Header ===== */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-header-bg);
  height: 64px !important;
  padding: 0 24px;
  border-bottom: 1px solid var(--color-header-border);
  box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  z-index: 100;
  flex-shrink: 0;
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
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  border-radius: 8px;
  color: #fff;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.2;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-btn {
  border: none !important;
  background: var(--color-bg) !important;
  width: 38px;
  height: 38px;
  border-radius: 8px !important;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 !important;
  transition: all var(--transition-fast);
  color: var(--color-text-secondary) !important;
}
.header-btn:hover {
  background: var(--color-bg-hover) !important;
  color: var(--color-primary) !important;
}

.notif-badge :deep(.el-badge__content) {
  top: 6px;
  right: 6px;
}

/* ===== User Dropdown ===== */
.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  background: var(--color-bg);
  transition: all var(--transition-fast);
}
.user-info:hover {
  background: var(--color-bg-hover);
}

.user-avatar {
  background: var(--color-primary) !important;
  font-size: 14px;
  font-weight: 600;
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
  font-size: 14px;
  color: var(--color-text-muted);
  transition: transform 0.2s;
}

/* ===== Sidebar ===== */
.main-container {
  flex: 1;
  overflow: hidden;
}

.layout-aside {
  background: var(--color-sidebar-bg);
  border-right: 1px solid var(--color-border-light);
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  transition: width var(--transition-normal);
  flex-shrink: 0;
}

.collapse-btn {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-muted);
  border-bottom: 1px solid var(--color-border-light);
  transition: all var(--transition-fast);
}
.collapse-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.sidebar-menu {
  border-right: none;
  padding-top: 4px;
}

.sidebar-menu :deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: 6px;
  height: 42px;
  line-height: 42px;
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--color-sidebar-active);
  color: var(--color-primary);
  font-weight: 600;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: var(--color-sidebar-hover);
  color: var(--color-text-primary);
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  font-size: 18px;
}

/* ===== Main Content ===== */
.layout-main {
  background: var(--color-bg-page);
  height: 100%;
  overflow-y: auto;
  padding: 24px;
}

.page-wrapper {
  max-width: 1400px;
  margin: 0 auto;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .layout-aside {
    position: fixed;
    left: 0;
    top: 64px;
    bottom: 0;
    z-index: 99;
    box-shadow: var(--shadow-lg);
  }
  .layout-aside:not([style*="width: 220px"]) {
    transform: translateX(-100%);
  }
  .layout-main {
    padding: 16px;
  }
  .layout-header {
    padding: 0 16px;
  }
  .brand-subtitle {
    display: none;
  }
  .user-text {
    display: none;
  }
}

/* ===== Page Transition ===== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ===== Notification Drawer ===== */
.drawer-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.notif-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--color-text-placeholder);
  gap: 12px;
}
.notif-empty p {
  font-size: 14px;
  margin: 0;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}
.notif-item:hover {
  background: var(--color-bg);
}
.notif-item.unread {
  background: var(--color-primary-light);
}

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  flex-shrink: 0;
  margin-top: 6px;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.notif-body {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.4;
  margin-bottom: 6px;
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
</style>
