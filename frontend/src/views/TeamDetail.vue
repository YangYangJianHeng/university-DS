<template>
  <div class="team-detail-container" v-loading="loading">
    <template v-if="team">
      <div class="back-nav">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回团队列表
        </el-button>
      </div>

      <el-row :gutter="24">
        <!-- Main Column -->
        <el-col :xs="24" :lg="16">
          <!-- Team Info Card -->
          <el-card shadow="never" class="info-card">
            <div class="team-header">
              <div class="team-header-top">
                <div class="team-avatar-large">{{ team.name?.charAt(0) || 'T' }}</div>
                <div class="team-title-area">
                  <div class="team-title-row">
                    <h2 class="team-name">{{ team.name }}</h2>
                    <el-tag :type="teamStatusType(team.status)" effect="dark" size="small">{{ teamStatusLabel(team.status) }}</el-tag>
                  </div>
                  <p class="team-description" v-if="team.description">{{ team.description }}</p>
                  <div class="team-meta">
                    <span><el-icon><UserFilled /></el-icon> {{ members.length }} 人</span>
                    <span><el-icon><Clock /></el-icon> 创建于 {{ dayjs(team.createdAt).format('YYYY-MM-DD') }}</span>
                  </div>
                </div>
              </div>
              <div class="team-actions">
                <el-button type="primary" class="action-primary" @click="router.push(`/project/create?teamId=${team.id}`)">
                  <el-icon><FolderOpened /></el-icon> 创建项目
                </el-button>
                <el-button @click="handleInviteTeacher">
                  <el-icon><Reading /></el-icon> 邀请导师
                </el-button>
                <el-button v-if="isMember" type="danger" plain @click="handleQuitTeam">
                  退出团队
                </el-button>
              </div>
            </div>
          </el-card>

          <!-- Associated Projects -->
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-header-title">
                <div class="section-header-icon" style="background:linear-gradient(135deg,#409eff,#79bbff)">
                  <el-icon :size="14" color="#fff"><FolderOpened /></el-icon>
                </div>
                <span>关联项目</span>
                <el-tag size="small" type="info">{{ teamProjects.length }}</el-tag>
              </div>
            </template>
            <div v-if="teamProjects.length === 0" class="empty-hint">暂无关联项目</div>
            <div v-else class="project-mini-list">
              <div v-for="p in teamProjects" :key="p.id" class="project-mini-item" @click="router.push(`/project/${p.id}`)">
                <div class="project-mini-name">{{ p.name }}</div>
                <el-tag size="small" :type="p.status === 'ARCHIVED' ? 'info' : 'success'" effect="plain">
                  {{ p.status === 'ARCHIVED' ? '已归档' : p.status === 'COMPLETED' ? '已完成' : '进行中' }}
                </el-tag>
              </div>
            </div>
          </el-card>

          <!-- Members -->
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-header-title">
                <div class="section-header-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)">
                  <el-icon :size="14" color="#fff"><UserFilled /></el-icon>
                </div>
                <span>团队成员</span>
                <el-tag size="small" type="info">{{ members.length }}</el-tag>
              </div>
            </template>
            <div v-if="members.length === 0" class="empty-hint">暂无成员</div>
            <div v-else class="member-list">
              <div v-for="member in members" :key="member.userId || member.id" class="member-item">
                <el-avatar :size="42" class="member-avatar">{{ (member.username || member.name || '?').charAt(0) }}</el-avatar>
                <div class="member-info">
                  <span class="member-name">{{ member.username || member.name || '未知' }}</span>
                  <span class="member-role-tag">
                    <el-tag v-if="member.role === 'LEADER' || member.role === 'leader'" type="warning" size="small" effect="dark">队长</el-tag>
                    <el-tag v-else type="info" size="small" effect="plain">成员</el-tag>
                  </span>
                </div>
                <div class="member-actions" v-if="isLeader && member.role !== 'LEADER' && member.role !== 'leader'">
                  <el-button text type="danger" size="small" @click="handleRemoveMember(member)">移除</el-button>
                </div>
              </div>
            </div>
          </el-card>

          <!-- Timeline -->
          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-header-title">
                <div class="section-header-icon" style="background:linear-gradient(135deg,#43e97b,#38f9d7)">
                  <el-icon :size="14" color="#fff"><Timer /></el-icon>
                </div>
                <span>团队动态</span>
              </div>
            </template>
            <div v-if="timeline.length === 0" class="empty-hint">暂无动态</div>
            <el-timeline v-else>
              <el-timeline-item
                v-for="(event, index) in timeline"
                :key="event.id || index"
                :timestamp="dayjs(event.createdAt || event.time).format('YYYY-MM-DD HH:mm')"
                :type="event.type === 'CREATE' || event.type === 'JOIN' ? 'primary' : 'info'"
              >
                {{ event.content || event.description || event.message }}
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </el-col>

        <!-- Sidebar -->
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="sidebar-card">
            <template #header>
              <div class="section-header-title">
                <div class="section-header-icon" style="background:linear-gradient(135deg,#fa709a,#fee140)">
                  <el-icon :size="14" color="#fff"><InfoFilled /></el-icon>
                </div>
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
                <el-tag :type="teamStatusType(team.status)" size="small">{{ teamStatusLabel(team.status) }}</el-tag>
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

    <el-empty v-else-if="!loading" description="团队不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { getTeam, quitTeam, removeMember } from '@/api/team'
import { getMyProjects } from '@/api/project'
import axios from 'axios'

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
  try {
    // 主数据：团队信息必须成功
    const teamRes = await getTeam(id)
    team.value = teamRes.data
  } catch (_) {
    team.value = null
    loading.value = false
    return
  }

  // 附属数据：独立加载，互不影响，静默失败不弹错误提示
  try { const res = await axios.get('/api/team/' + id + '/members', SKIP_TOAST); members.value = res.data?.data || [] } catch (_) { members.value = [] }
  try { const res = await axios.get('/api/team/' + id + '/timeline', SKIP_TOAST); timeline.value = res.data?.data || [] } catch (_) { timeline.value = [] }
  try {
    const res = await axios.get('/api/project/my', SKIP_TOAST)
    const allProjects = res.data?.data || []
    teamProjects.value = allProjects.filter((p: any) => p.teamId === id)
  } catch (_) { teamProjects.value = [] }

  loading.value = false
}

async function handleQuitTeam() {
  try { await ElMessageBox.confirm('确认退出该团队？', '退出确认', { confirmButtonText: '确认退出', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await quitTeam(Number(route.params.id)); ElMessage.success('已退出团队'); await fetchTeam() } catch (_) {}
}

async function handleRemoveMember(member: any) {
  try { await ElMessageBox.confirm(`确认移除成员 ${member.username || member.name || '未知'}？`, '移除确认', { confirmButtonText: '确认移除', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await removeMember(Number(route.params.id), member.userId || member.id); ElMessage.success('已移除该成员'); await fetchTeam() } catch (_) {}
}

function handleInviteTeacher() { router.push(`/teachers?teamId=${team.value?.id}`) }

function teamStatusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) { case 'ACTIVE': case 'NORMAL': return 'success'; case 'INACTIVE': return 'warning'; case 'DISBANDED': return 'info'; default: return 'info' }
}

function teamStatusLabel(status: string): string {
  switch (status) { case 'ACTIVE': case 'NORMAL': return '活跃中'; case 'INACTIVE': return '已停用'; case 'DISBANDED': return '已解散'; default: return status || '未知' }
}

onMounted(() => fetchTeam())
</script>

<style scoped>
.team-detail-container { max-width: 1100px; margin: 0 auto; }
.back-nav { margin-bottom: 16px; }

.info-card { border-radius: 14px; margin-bottom: 20px; }

.team-header { margin-bottom: 0; }

.team-header-top { display: flex; gap: 16px; margin-bottom: 20px; }

.team-avatar-large {
  width: 60px; height: 60px; border-radius: 14px;
  background: var(--gradient-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}

.team-title-area { flex: 1; min-width: 0; }

.team-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }

.team-name { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0; }

.team-description { font-size: 14px; color: #606266; line-height: 1.6; margin: 0 0 8px; }

.team-meta { display: flex; gap: 20px; font-size: 13px; color: #909399; }
.team-meta span { display: flex; align-items: center; gap: 4px; }

.team-actions { display: flex; gap: 12px; flex-wrap: wrap; padding-top: 16px; border-top: 1px solid #e8ecf1; }

.action-primary {
  background: var(--gradient-primary);
  border: none;
  border-radius: 8px;
}

.section-card { border-radius: 14px; margin-bottom: 20px; }

.section-header-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; }

.section-header-icon { width: 28px; height: 28px; border-radius: 7px; display: flex; align-items: center; justify-content: center; }

.empty-hint { text-align: center; padding: 24px; color: #909399; font-size: 14px; }

.member-list { display: flex; flex-direction: column; }

.member-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #f5f7fa; }
.member-item:last-child { border-bottom: none; }

.member-avatar { background: var(--gradient-primary) !important; flex-shrink: 0; }

.member-info { flex: 1; margin-left: 12px; display: flex; align-items: center; gap: 8px; }
.member-name { font-size: 14px; font-weight: 500; color: #1a1a2e; }

.member-actions { margin-left: auto; }

/* Sidebar */
.sidebar-card { border-radius: 14px; }

.summary-list { display: flex; flex-direction: column; }

.summary-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #f5f7fa; }
.summary-row:last-child { border-bottom: none; }

.summary-label { font-size: 13px; color: #909399; }
.summary-value { font-size: 14px; color: #1a1a2e; font-weight: 500; }

/* Project mini list */
.project-mini-list { display: flex; flex-direction: column; gap: 8px; }
.project-mini-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; background: var(--color-bg); border-radius: var(--radius-sm);
  cursor: pointer; transition: all 0.2s; border: 1px solid var(--color-border-light);
}
.project-mini-item:hover { background: var(--color-bg-hover); border-color: var(--color-primary); }
.project-mini-name { font-weight: 500; font-size: 14px; color: var(--color-text-primary); }
</style>
