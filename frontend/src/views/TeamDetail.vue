<template>
  <div class="team-detail-container" v-loading="loading">
    <template v-if="team">
      <div class="back-nav">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>返回团队列表
        </el-button>
      </div>

      <el-row :gutter="24">
        <el-col :xs="24" :lg="16">
          <!-- Team Info -->
          <el-card shadow="never" class="info-card">
            <div class="team-header">
              <div class="team-header-top">
                <div class="team-avatar">{{ team.name?.charAt(0) || 'T' }}</div>
                <div class="team-title-area">
                  <div class="team-title-row">
                    <h2 class="team-name">{{ team.name }}</h2>
                    <el-tag :type="teamStatusType(team.status)" effect="dark" size="small" round>
                      {{ teamStatusLabel(team.status) }}
                    </el-tag>
                  </div>
                  <p class="team-desc" v-if="team.description">{{ team.description }}</p>
                  <div class="team-meta">
                    <span><el-icon><UserFilled /></el-icon>{{ members.length }} 人</span>
                    <span><el-icon><Clock /></el-icon>创建于 {{ dayjs(team.createdAt).format('YYYY-MM-DD') }}</span>
                  </div>
                </div>
              </div>
              <div class="team-actions">
                <el-button v-if="team.status !== 'CLOSED'" type="primary" @click="router.push('/projects')">
                  <el-icon><FolderOpened /></el-icon>创建项目
                </el-button>
                <el-button v-if="team.status !== 'CLOSED'" @click="handleInviteTeacher">
                  <el-icon><Reading /></el-icon>邀请导师
                </el-button>
                <el-button v-if="isMember && team.status !== 'CLOSED'" type="danger" plain @click="handleQuitTeam">
                  退出团队
                </el-button>
              </div>
            </div>
          </el-card>

          <!-- Projects -->
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-header-title">
                <span>关联项目</span>
                <el-tag size="small" effect="light" round>{{ teamProjects.length }}</el-tag>
              </div>
            </template>
            <div v-if="teamProjects.length === 0" class="empty-hint">暂无关联项目</div>
            <div v-else class="project-list">
              <div v-for="p in teamProjects" :key="p.id" class="project-item" @click="router.push(`/project/${p.id}`)">
                <span class="project-name">{{ p.name }}</span>
                <el-tag size="small" :type="projectStatusTagType(p.status)" effect="light" round>
                  {{ projectStatusLabel(p.status) }}
                </el-tag>
              </div>
            </div>
          </el-card>

          <!-- Members -->
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-header-title">
                <span>团队成员</span>
                <el-tag size="small" effect="light" round>{{ members.length }}</el-tag>
              </div>
            </template>
            <div v-if="members.length === 0" class="empty-hint">暂无成员</div>
            <div v-else class="member-list">
              <div v-for="member in members" :key="member.userId || member.id" class="member-item">
                <el-avatar :size="42" :src="member.avatar" class="member-avatar">
                  {{ (member.userName || member.name || '?').charAt(0) }}
                </el-avatar>
                <div class="member-info">
                  <span class="member-name">{{ member.userName || member.name || '未知' }}</span>
                  <el-tag v-if="member.role === 'LEADER' || member.role === 'leader'" type="warning" size="small" effect="light" round>队长</el-tag>
                  <el-tag v-else size="small" effect="plain" round>成员</el-tag>
                </div>
                <el-button
                  v-if="isLeader && member.role !== 'LEADER' && member.role !== 'leader'"
                  text type="danger" size="small"
                  @click="handleRemoveMember(member)"
                >
                  移除
                </el-button>
              </div>
            </div>
          </el-card>

          <!-- Timeline -->
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-header-title">
                <span>团队动态</span>
              </div>
            </template>
            <div v-if="timeline.length === 0" class="empty-hint">暂无动态</div>
            <el-timeline v-else>
              <el-timeline-item
                v-for="(event, index) in timeline"
                :key="index"
                :timestamp="formatTime(event.timestamp)"
                :type="timelineType(event.type)"
                placement="top"
              >
                <div class="timeline-content">
                  <span class="timeline-desc">{{ event.description }}</span>
                  <el-tag v-if="event.projectName" size="small" effect="plain" type="info" round>
                    {{ event.projectName }}
                  </el-tag>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>

        <!-- Sidebar -->
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="sidebar-card">
            <template #header>
              <div class="section-header-title">
                <span>团队概况</span>
              </div>
            </template>
            <div class="summary-list">
              <div class="summary-row">
                <span class="summary-label">团队名称</span>
                <span class="summary-value">{{ team.name }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">团队状态</span>
                <el-tag :type="teamStatusType(team.status)" size="small" round>
                  {{ teamStatusLabel(team.status) }}
                </el-tag>
              </div>
              <div class="summary-row">
                <span class="summary-label">成员数量</span>
                <span class="summary-value">{{ members.length }} 人</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">创建时间</span>
                <span class="summary-value">{{ dayjs(team.createdAt).format('YYYY-MM-DD') }}</span>
              </div>
              <div class="summary-row" v-if="team.updatedAt">
                <span class="summary-label">最近更新</span>
                <span class="summary-value">{{ dayjs(team.updatedAt).format('YYYY-MM-DD') }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-else-if="!loading" description="团队不存在" :image-size="120" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, FolderOpened, Reading, UserFilled, Clock } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getTeam, quitTeam, removeMember } from '@/api/team'
import request from '@/api/request'

const SKIP_TOAST = { skipErrorToast: true }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const team = ref<any>(null)
const members = ref<any[]>([])
const timeline = ref<any[]>([])
const teamProjects = ref<any[]>([])

const isMember = computed(() => {
  const uid = userStore.userInfo?.id
  if (!uid) return false
  return members.value.some((m: any) => (m.userId || m.id) === uid)
})
const isLeader = computed(() => {
  const uid = userStore.userInfo?.id
  if (!uid) return false
  return members.value.some((m: any) => (m.userId || m.id) === uid && (m.role === 'LEADER' || m.role === 'leader'))
})

async function fetchTeam() {
  loading.value = true
  const id = Number(route.params.id)
  if (isNaN(id) || id <= 0) {
    ElMessage.error('无效的团队ID')
    router.replace('/teams')
    loading.value = false
    return
  }
  try {
    team.value = (await getTeam(id)).data
  } catch {
    team.value = null; loading.value = false; return
  }
  try { members.value = (await request.get('/team/' + id + '/members', SKIP_TOAST)).data || [] } catch { members.value = [] }
  try { timeline.value = (await request.get('/team/' + id + '/timeline', SKIP_TOAST)).data || [] } catch { timeline.value = [] }
  try {
    const allProjects = (await request.get('/project/my', SKIP_TOAST)).data || []
    teamProjects.value = allProjects.filter((p: any) => p.teamId === id)
  } catch { teamProjects.value = [] }
  loading.value = false
}

async function handleQuitTeam() {
  try { await ElMessageBox.confirm('确认退出该团队？', '退出确认', { confirmButtonText: '确认退出', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await quitTeam(Number(route.params.id)); ElMessage.success('已退出团队'); await fetchTeam() } catch {}
}

async function handleRemoveMember(member: any) {
  try { await ElMessageBox.confirm(`确认移除成员 ${member.username || member.name || '未知'}？`, '移除确认', { confirmButtonText: '确认移除', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await removeMember(Number(route.params.id), member.userId || member.id); ElMessage.success('已移除该成员'); await fetchTeam() } catch {}
}

function handleInviteTeacher() { router.push(`/teachers?teamId=${team.value?.id}`) }

function teamStatusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, any> = { ACTIVE: 'success', NORMAL: 'success', INACTIVE: 'warning', DISBANDED: 'info', CLOSED: 'danger' }
  return map[status] || 'info'
}
function teamStatusLabel(status: string): string {
  const map: Record<string, string> = { ACTIVE: '活跃中', NORMAL: '活跃中', INACTIVE: '已停用', DISBANDED: '已解散', CLOSED: '已关闭' }
  return map[status] || status || '未知'
}
function projectStatusLabel(status: string): string {
  const map: Record<string, string> = { PLANNING: '规划中', IN_PROGRESS: '进行中', REVIEW: '评审中', COMPLETED: '已完成', ARCHIVED: '已归档', CLOSED: '已关闭' }
  return map[status] || status || '未知'
}
function projectStatusTagType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, any> = { IN_PROGRESS: 'success', PLANNING: 'success', COMPLETED: '', ARCHIVED: 'warning', REVIEW: 'warning', CLOSED: 'danger' }
  return map[status] || 'info'
}
function formatTime(ts: any): string { return ts ? dayjs(ts).format('YYYY-MM-DD HH:mm') : '' }
function timelineType(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, any> = {
    TEAM_CREATED: 'primary', PROJECT_CREATED: 'primary', TASK_CREATED: 'primary',
    LEADER_JOINED: 'success', MEMBER_JOINED: 'success', TASK_COMPLETED: 'success', TEACHER_APPROVED: 'success',
    TEACHER_INVITED: 'warning', MEMBER_APPLIED: 'warning',
  }
  return map[type] || 'info'
}

onMounted(() => fetchTeam())
</script>

<style scoped>
.team-detail-container { max-width: 1100px; margin: 0 auto; }
.back-nav { margin-bottom: 16px; }

.info-card { border-radius: var(--radius-lg) !important; margin-bottom: 20px; }
.team-header-top { display: flex; gap: 18px; margin-bottom: 0; }
.team-avatar {
  width: 64px; height: 64px;
  border-radius: var(--radius-lg);
  background: var(--gradient-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 700; color: #fff; flex-shrink: 0;
}
.team-title-area { flex: 1; min-width: 0; }
.team-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.team-name { font-size: 22px; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.team-desc { font-size: 14px; color: var(--color-text-secondary); line-height: 1.6; margin: 0 0 8px; }
.team-meta { display: flex; gap: 20px; font-size: 13px; color: var(--color-text-muted); }
.team-meta span { display: flex; align-items: center; gap: 4px; }
.team-actions { display: flex; gap: 12px; flex-wrap: wrap; padding-top: 20px; border-top: 1px solid var(--color-border-light); margin-top: 20px; }

.section-card { border-radius: var(--radius-lg) !important; margin-bottom: 20px; }
.section-header-title {
  display: flex; align-items: center; gap: 10px;
  font-size: 16px; font-weight: 600; color: var(--color-text-primary);
}

.empty-hint { text-align: center; padding: 32px; color: var(--color-text-muted); font-size: 14px; }

.member-list { display: flex; flex-direction: column; }
.member-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0; border-bottom: 1px solid var(--color-border-light);
}
.member-item:last-child { border-bottom: none; }
.member-avatar { background: var(--gradient-primary) !important; flex-shrink: 0; font-weight: 600; }
.member-info { flex: 1; display: flex; align-items: center; gap: 10px; }
.member-name { font-size: 14px; font-weight: 500; color: var(--color-text-primary); }

.project-list { display: flex; flex-direction: column; gap: 8px; }
.project-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: var(--color-bg); border-radius: var(--radius-sm);
  cursor: pointer; transition: all var(--transition-fast);
  border: 1px solid var(--color-border-light);
}
.project-item:hover { background: var(--color-bg-hover); border-color: var(--color-primary); }
.project-name { font-weight: 500; font-size: 14px; color: var(--color-text-primary); }

.timeline-content { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.timeline-desc { font-size: 14px; color: var(--color-text-primary); line-height: 1.5; }

.sidebar-card { border-radius: var(--radius-lg) !important; }
.summary-list { display: flex; flex-direction: column; }
.summary-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 0; border-bottom: 1px solid var(--color-border-light);
}
.summary-row:last-child { border-bottom: none; }
.summary-label { font-size: 13px; color: var(--color-text-muted); }
.summary-value { font-size: 14px; color: var(--color-text-primary); font-weight: 500; }
</style>
