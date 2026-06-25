<template>
  <div class="dashboard">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="banner-content">
        <div>
          <h2>欢迎回来，{{ userStore.username }}</h2>
          <p class="welcome-sub">今日又是充满活力的一天！</p>
        </div>
        <div class="banner-actions">
          <el-button type="primary" size="large" :icon="Plus" @click="router.push('/recruit/create')" class="banner-btn">
            发布招募
          </el-button>
        </div>
      </div>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
        <el-card class="stat-card" shadow="never" :style="{ background: card.bg }">
          <div class="stat-content">
            <div class="stat-info">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
            <div class="stat-icon-wrap">
              <el-icon :size="32" class="stat-icon">
                <component :is="card.icon" />
              </el-icon>
            </div>
          </div>
          <div class="stat-trend" v-if="card.trend !== undefined">
            <span :class="card.trend >= 0 ? 'up' : 'down'">
              {{ card.trend >= 0 ? '↑' : '↓' }} {{ Math.abs(card.trend) }}%
            </span>
            <span>较上周</span>
          </div>
        </el-card>
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
                <div class="header-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)">
                  <el-icon :size="16" color="#fff"><Timer /></el-icon>
                </div>
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
              class="recruit-card-item"
              @click="router.push(`/recruit/${recruit.id}`)"
            >
              <div class="recruit-item-left">
                <div class="recruit-status-dot" :class="recruit.status === 'OPEN' ? 'open' : 'closed'" />
                <div class="recruit-item-info">
                  <div class="recruit-item-title">{{ recruit.title }}</div>
                  <div class="recruit-item-meta">
                    <span v-if="recruit.creator?.username">
                      <el-icon><User /></el-icon>{{ recruit.creator.username }}
                    </span>
                    <span>{{ formatDate(recruit.createdAt) }}</span>
                  </div>
                </div>
              </div>
              <el-tag
                size="small"
                :type="recruit.status === 'OPEN' ? 'success' : 'info'"
                effect="plain"
              >
                {{ recruit.status === 'OPEN' ? '招募中' : '已关闭' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Quick Actions + Info -->
      <el-col :xs="24" :md="8">
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <div class="header-icon" style="background:linear-gradient(135deg,#43e97b,#38f9d7)">
                  <el-icon :size="16" color="#fff"><Lightning /></el-icon>
                </div>
                <span>快捷操作</span>
              </div>
            </div>
          </template>
          <div class="quick-actions">
            <div class="quick-action-item" @click="router.push('/recruit/create')">
              <div class="qa-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)">
                <el-icon color="#fff"><Plus /></el-icon>
              </div>
              <div class="qa-info">
                <div class="qa-title">发布招募</div>
                <div class="qa-desc">寻找志同道合的队友</div>
              </div>
            </div>
            <div class="quick-action-item" @click="router.push('/recruit')">
              <div class="qa-icon" style="background:linear-gradient(135deg,#43e97b,#38f9d7)">
                <el-icon color="#fff"><Search /></el-icon>
              </div>
              <div class="qa-info">
                <div class="qa-title">浏览招募</div>
                <div class="qa-desc">查看最新的组队信息</div>
              </div>
            </div>
            <div class="quick-action-item" @click="router.push('/teams')">
              <div class="qa-icon" style="background:linear-gradient(135deg,#fa709a,#fee140)">
                <el-icon color="#fff"><UserFilled /></el-icon>
              </div>
              <div class="qa-info">
                <div class="qa-title">我的团队</div>
                <div class="qa-desc">管理你的团队成员</div>
              </div>
            </div>
            <div class="quick-action-item" @click="router.push('/tasks')">
              <div class="qa-icon" style="background:linear-gradient(135deg,#a18cd1,#fbc2eb)">
                <el-icon color="#fff"><List /></el-icon>
              </div>
              <div class="qa-info">
                <div class="qa-title">任务中心</div>
                <div class="qa-desc">查看和管理你的任务</div>
              </div>
            </div>
            <div class="quick-action-item" @click="router.push('/achievements')">
              <div class="qa-icon" style="background:linear-gradient(135deg,#f093fb,#f5576c)">
                <el-icon color="#fff"><Collection /></el-icon>
              </div>
              <div class="qa-info">
                <div class="qa-title">成果展厅</div>
                <div class="qa-desc">展示你的项目成果</div>
              </div>
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
  Timer, Lightning, User, Plus, Search,
  UserFilled, List, Collection, Bell,
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

interface StatCard {
  label: string
  value: number | string
  icon: any
  bg: string
  trend?: number
}

const statCards = reactive<StatCard[]>([])
const recentRecruits = ref<any[]>([])

// Map icon names to actual component references
const iconMap: Record<string, any> = {
  UserFilled: markRaw(UserFilled),
  List: markRaw(List),
  Bell: markRaw(Bell),
  Search: markRaw(Search),
}

function updateStatCards(counts: any) {
  const cards: StatCard[] = [
    {
      label: '我的团队',
      value: counts.teamCount ?? counts.teams?.length ?? 0,
      icon: iconMap.UserFilled,
      bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    },
    {
      label: '我的任务',
      value: counts.taskCount ?? counts.tasks?.length ?? 0,
      icon: iconMap.List,
      bg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    },
    {
      label: '未读通知',
      value: counts.unreadNotificationCount ?? 0,
      icon: iconMap.Bell,
      bg: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    },
    {
      label: '活跃招募',
      value: counts.activeRecruitCount ?? 0,
      icon: iconMap.Search,
      bg: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
    },
  ]
  statCards.length = 0
  statCards.push(...cards)
}

function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function fetchDashboard() {
  try {
    const res = await getDashboard()
    const data = res.data || {}
    recentRecruits.value = data.recentRecruits ?? data.recentActivities ?? []
    updateStatCards(data)
  } catch {
    // Error handled by interceptor
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style scoped>
.dashboard { padding: 0; }

/* ===== Welcome Banner ===== */
.welcome-banner {
  background: var(--gradient-primary);
  border-radius: var(--radius-md);
  padding: 32px 36px;
  margin-bottom: 24px;
  color: #fff;
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  top: -100px;
  right: -50px;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  bottom: -60px;
  right: 200px;
}

.banner-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.banner-content h2 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
}

.welcome-sub {
  margin: 0;
  font-size: 15px;
  opacity: 0.85;
}

.banner-btn {
  border-radius: 10px;
  font-weight: 600;
  border: none;
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(4px);
  transition: all 0.3s;
}

.banner-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-1px);
}

/* ===== Stat Cards ===== */
.stat-row { margin-bottom: 24px; }

.stat-card {
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  margin-bottom: 16px;
  padding: 4px;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255,255,255,0.08);
  bottom: -30px;
  right: -20px;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-info { flex: 1; }

.stat-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.15);
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-trend {
  margin-top: 12px;
  font-size: 12px;
  opacity: 0.8;
  display: flex;
  gap: 6px;
}

.stat-trend .up { color: var(--color-success); }
.stat-trend .down { color: var(--color-danger); }

/* ===== Content Cards ===== */
.main-row { margin-bottom: 24px; }

.content-card {
  border-radius: var(--radius-md);
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

.header-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.empty-state { padding: 20px 0; }

/* ===== Recruit List ===== */
.recruit-list {
  display: flex;
  flex-direction: column;
}

.recruit-card-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.recruit-card-item:hover {
  background: var(--color-bg);
  border-color: var(--color-border);
}

.recruit-card-item + .recruit-card-item {
  margin-top: 4px;
}

.recruit-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.recruit-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.recruit-status-dot.open { background: var(--color-success); }
.recruit-status-dot.closed { background: var(--color-text-placeholder); }

.recruit-item-info { flex: 1; min-width: 0; }

.recruit-item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recruit-item-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.recruit-item-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ===== Quick Actions ===== */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-action-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-action-item:hover {
  background: var(--color-bg);
}

.qa-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
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
</style>
