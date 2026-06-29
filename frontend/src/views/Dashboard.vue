<template>
  <div class="dashboard">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="banner-content">
        <div class="banner-text">
          <h2>欢迎回来，{{ userStore.username }}</h2>
          <p>今日又是充满活力的一天！查看你的团队和任务进展</p>
        </div>
        <div class="banner-actions">
          <el-button class="banner-btn" size="large" :icon="Plus" @click="router.push('/recruit/create')">
            发布招募
          </el-button>
        </div>
      </div>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="{ '--card-accent': card.accent }">
          <div class="stat-inner">
            <div class="stat-info">
              <span class="stat-label">{{ card.label }}</span>
              <span class="stat-value">{{ card.value }}</span>
            </div>
            <div class="stat-icon-wrap">
              <el-icon :size="24">
                <component :is="card.icon" />
              </el-icon>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Main Content -->
    <el-row :gutter="20" class="main-row">
      <!-- Recent Recruits -->
      <el-col :xs="24" :md="16">
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <div class="header-dot" />
                <span>最新招募</span>
              </div>
              <el-button text type="primary" @click="router.push('/recruit')">查看全部</el-button>
            </div>
          </template>
          <div v-if="recentRecruits.length === 0" class="empty-state">
            <el-empty description="暂无招募活动" :image-size="80" />
          </div>
          <div v-else class="recruit-list">
            <div
              v-for="recruit in recentRecruits"
              :key="recruit.id"
              class="recruit-item"
              @click="router.push(`/recruit/${recruit.id}`)"
            >
              <div class="recruit-left">
                <span class="recruit-dot" :class="recruit.status === 'OPEN' ? 'open' : 'closed'" />
                <div class="recruit-info">
                  <div class="recruit-title">{{ recruit.title }}</div>
                  <div class="recruit-meta">
                    <el-icon :size="13"><User /></el-icon>
                    <span>{{ recruit.publisherName || '未知用户' }}</span>
                    <span class="meta-divider">·</span>
                    <span>{{ formatDate(recruit.createdAt) }}</span>
                  </div>
                </div>
              </div>
              <el-tag
                size="small"
                :type="recruit.status === 'OPEN' ? 'success' : 'info'"
                effect="light"
                round
              >
                {{ recruit.status === 'OPEN' ? '招募中' : '已关闭' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Quick Actions -->
      <el-col :xs="24" :md="8">
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <div class="header-dot accent-green" />
                <span>快捷操作</span>
              </div>
            </div>
          </template>
          <div class="quick-actions">
            <div
              v-for="action in quickActions"
              :key="action.label"
              class="qa-item"
              @click="router.push(action.path)"
            >
              <div class="qa-icon" :style="{ background: action.gradient }">
                <el-icon :size="18" color="#fff"><component :is="action.icon" /></el-icon>
              </div>
              <div class="qa-info">
                <div class="qa-title">{{ action.label }}</div>
                <div class="qa-desc">{{ action.desc }}</div>
              </div>
              <el-icon class="qa-arrow" :size="16"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboard } from '@/api/dashboard'
import {
  Timer, Lightning, User, Plus, Search, ArrowRight,
  UserFilled, List, Collection, Bell, Trophy,
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

interface StatCard {
  label: string
  value: number | string
  icon: any
  accent: string
}

const statCards = reactive<StatCard[]>([])
const recentRecruits = ref<any[]>([])

const iconMap: Record<string, any> = {
  UserFilled: markRaw(UserFilled),
  List: markRaw(List),
  Bell: markRaw(Bell),
  Search: markRaw(Search),
}

const quickActions = [
  { label: '发布招募', desc: '寻找志同道合的队友', path: '/recruit/create', icon: markRaw(Plus), gradient: 'linear-gradient(135deg, #5B6FF5, #8B5CF6)' },
  { label: '浏览招募', desc: '查看最新的组队信息', path: '/recruit', icon: markRaw(Search), gradient: 'linear-gradient(135deg, #22C55E, #10B981)' },
  { label: '我的团队', desc: '管理团队成员', path: '/teams', icon: markRaw(UserFilled), gradient: 'linear-gradient(135deg, #F59E0B, #F97316)' },
  { label: '任务中心', desc: '查看和管理任务', path: '/tasks', icon: markRaw(List), gradient: 'linear-gradient(135deg, #8B5CF6, #A78BFA)' },
  { label: '竞赛中心', desc: '了解最新竞赛资讯', path: '/competitions', icon: markRaw(Trophy), gradient: 'linear-gradient(135deg, #3B82F6, #06B6D4)' },
  { label: '成果展厅', desc: '展示项目成果', path: '/achievements', icon: markRaw(Collection), gradient: 'linear-gradient(135deg, #EC4899, #F43F5E)' },
]

function updateStatCards(counts: any) {
  const cards: StatCard[] = [
    { label: '我的团队', value: counts.teamCount ?? counts.teams?.length ?? 0, icon: iconMap.UserFilled, accent: '#5B6FF5' },
    { label: '我的任务', value: counts.taskCount ?? counts.tasks?.length ?? 0, icon: iconMap.List, accent: '#22C55E' },
    { label: '未读通知', value: counts.unreadNotificationCount ?? 0, icon: iconMap.Bell, accent: '#F59E0B' },
    { label: '活跃招募', value: counts.activeRecruitCount ?? 0, icon: iconMap.Search, accent: '#8B5CF6' },
  ]
  statCards.length = 0
  statCards.push(...cards)
}

function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function fetchDashboard() {
  try {
    const res = await getDashboard()
    const data = res.data || {}
    recentRecruits.value = data.recentRecruits ?? data.recentActivities ?? []
    updateStatCards(data)
  } catch { /* handled by interceptor */ }
}

onMounted(() => { fetchDashboard() })
</script>

<style scoped>
.dashboard { padding: 0; }

/* Welcome Banner */
.welcome-banner {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: 36px 40px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}
.welcome-banner::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--gradient-primary);
  border-radius: 4px 0 0 4px;
}
.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
  flex-wrap: wrap;
  gap: 16px;
}
.banner-text h2 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--color-text-primary);
}
.banner-text p {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-muted);
}
.banner-btn {
  border-radius: var(--radius-sm) !important;
  font-weight: 600 !important;
}

/* Stat Cards */
.stat-row { margin-bottom: 24px; }
.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 16px;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px; height: 40%;
  background: var(--card-accent);
  border-radius: 0 2px 2px 0;
}
.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.stat-inner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.stat-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-label {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}
.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1;
  font-feature-settings: 'tnum';
  letter-spacing: -0.5px;
}
.stat-icon-wrap {
  width: 44px; height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  color: var(--card-accent);
}

/* Content Cards */
.main-row { margin-bottom: 24px; }
.content-card {
  border-radius: var(--radius-lg) !important;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.header-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
}
.header-dot.accent-green { background: var(--color-success); }
.empty-state { padding: 20px 0; }

/* Recruit List */
.recruit-list { display: flex; flex-direction: column; }
.recruit-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.recruit-item:hover { background: var(--color-bg-hover); }
.recruit-item + .recruit-item { margin-top: 2px; }
.recruit-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.recruit-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.recruit-dot.open { background: var(--color-success); }
.recruit-dot.closed { background: var(--color-text-placeholder); }
.recruit-info { flex: 1; min-width: 0; }
.recruit-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recruit-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}
.meta-divider { margin: 0 2px; }

/* Quick Actions */
.quick-actions { display: flex; flex-direction: column; gap: 4px; }
.qa-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
}
.qa-item:hover {
  background: var(--color-bg);
  border-color: var(--color-border-light);
}
.qa-icon {
  width: 42px; height: 42px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.qa-info { flex: 1; }
.qa-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}
.qa-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}
.qa-arrow {
  color: var(--color-text-placeholder);
  transition: transform var(--transition-fast);
}
.qa-item:hover .qa-arrow {
  transform: translateX(3px);
  color: var(--color-primary);
}

@media (max-width: 768px) {
  .welcome-banner { padding: 24px; }
  .banner-text h2 { font-size: 20px; }
}
</style>
