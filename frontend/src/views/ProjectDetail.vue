<template>
  <div v-loading="loading" class="project-detail-page">
    <div class="back-nav">
      <el-button text @click="router.push('/projects')">
        <el-icon><ArrowLeft /></el-icon> 返回项目列表
      </el-button>
    </div>

    <!-- Project Header -->
    <el-card class="project-header-card" shadow="never">
      <div class="header-content">
        <div class="header-left">
          <div class="project-avatar">{{ project.name?.charAt(0) || 'P' }}</div>
          <div class="header-text">
            <div class="header-title-row">
              <h2>{{ project.name }}</h2>
              <el-tag :type="projectStatusType(project.status)" size="small" effect="dark">
                {{ projectStatusLabel(project.status) }}
              </el-tag>
            </div>
            <p class="project-desc" v-if="project.description">{{ project.description }}</p>
            <div class="header-meta">
              <span><el-icon><UserFilled /></el-icon> {{ project.team?.name || '--' }}</span>
              <span><el-icon><Clock /></el-icon> {{ dayjs(project.createdAt).format('YYYY-MM-DD') }}</span>
              <span v-if="project.deadline"><el-icon><Warning /></el-icon> {{ dayjs(project.deadline).format('YYYY-MM-DD') }}</span>
            </div>
          </div>
        </div>
        <div class="header-right">
          <el-button
            v-if="project.status !== 'CLOSED'"
            type="warning"
            size="small"
            @click="handleCloseProject"
            :loading="actionLoading"
          >
            <el-icon><SwitchButton /></el-icon> 关闭项目
          </el-button>
          <el-button
            v-if="project.status === 'CLOSED'"
            type="success"
            size="small"
            @click="handleReopenProject"
            :loading="actionLoading"
          >
            <el-icon><VideoPlay /></el-icon> 开启项目
          </el-button>
          <el-button
            v-if="isAdmin || isProjectCreator"
            type="danger"
            size="small"
            @click="handleDeleteProject"
            :loading="actionLoading"
          >
            <el-icon><Delete /></el-icon> 删除项目
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="project-tabs" type="border-card">
      <!-- Members -->
      <el-tab-pane label="团队成员" name="members">
        <div class="tab-toolbar">
          <div class="toolbar-title">
            <span>团队成员</span>
            <el-tag size="small" type="info">{{ projectMembers.length }}</el-tag>
          </div>
          <el-button type="primary" size="small" @click="showAddMemberDialog = true">
            <el-icon><Plus /></el-icon>添加成员
          </el-button>
        </div>
        <div class="member-grid" v-if="projectMembers.length > 0">
          <div v-for="m in projectMembers" :key="m.id" class="member-card" @click="openMemberDetail(m)">
            <el-avatar :size="48" :src="m.avatar || undefined" class="member-avatar" :style="{ background: m.avatar ? undefined : avatarGradient(m.userName) }">
              {{ m.userName?.charAt(0) }}
            </el-avatar>
            <div class="member-info">
              <span class="member-name">{{ m.userName }}</span>
              <span class="member-major" v-if="m.major">{{ m.major }}{{ m.grade ? ' · ' + m.grade : '' }}</span>
              <div class="member-tags">
                <el-tag size="small" :type="m.role === 'LEADER' ? 'danger' : 'primary'">
                  {{ m.role === 'LEADER' ? '队长' : '成员' }}
                </el-tag>
                <el-tag v-for="sk in (m.skills || []).slice(0, 2)" :key="sk.id" size="small" effect="plain" type="info">
                  {{ sk.skillName }}
                </el-tag>
              </div>
            </div>
            <span class="member-arrow"><el-icon><MoreFilled /></el-icon></span>
          </div>
        </div>
        <el-empty v-else description="暂无团队成员" />
      </el-tab-pane>

      <!-- Tasks -->
      <el-tab-pane label="任务" name="tasks">
        <div class="tab-toolbar">
          <div class="toolbar-title">
            <span>任务列表</span>
            <el-tag size="small" type="info">{{ tasks.length }}</el-tag>
          </div>
          <el-button type="primary" size="small" @click="showTaskDialog = true">
            <el-icon><Plus /></el-icon>新建任务
          </el-button>
        </div>
        <el-table :data="tasks" stripe size="small" class="task-table">
          <el-table-column prop="title" label="任务名称" min-width="200" />
          <el-table-column label="负责人" width="130">
            <template #default="{ row }">
              <span class="assignee-cell">
                <el-avatar :size="24" style="margin-right:6px">{{ (row.assigneeName || row.assignee?.username || '').charAt(0) }}</el-avatar>
                {{ row.assigneeName || row.assignee?.username || '--' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="priorityType(row.priority)" size="small" effect="plain">{{ priorityLabel(row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag
                :type="statusType(row.status)" size="small"
                style="cursor:pointer" @click="handleToggleStatus(row)"
              >{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止日期" width="120">
            <template #default="{ row }">
              <span :class="row.status !== 'DONE' && row.deadline && dayjs(row.deadline).isBefore(dayjs()) ? 'overdue' : ''">
                {{ row.deadline ? dayjs(row.deadline).format('MM-DD') : '--' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Milestones -->
      <el-tab-pane label="里程碑" name="milestones">
        <el-timeline>
          <el-timeline-item
            v-for="m in milestones" :key="m.id"
            :timestamp="dayjs(m.dueDate).format('YYYY-MM-DD')"
            :color="m.status === 'COMPLETED' ? '#67c23a' : '#e6a23c'"
          >
            <div class="milestone-item">
              <span class="milestone-name">{{ m.name }}</span>
              <el-tag :type="m.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
                {{ m.status === 'COMPLETED' ? '已完成' : '进行中' }}
              </el-tag>
            </div>
            <p v-if="m.description" class="milestone-desc">{{ m.description }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!milestones.length" description="暂无里程碑" />
      </el-tab-pane>

      <!-- Gantt -->
      <el-tab-pane name="gantt">
        <template #label>
          <span><el-icon style="vertical-align:middle"><DataAnalysis /></el-icon> 甘特图</span>
        </template>
        <div class="gantt-container">
          <div class="gantt-header">
            <div class="gantt-task-header">任务</div>
            <div class="gantt-bar-area">
              <div v-for="day in ganttDays" :key="day" class="gantt-day-label" :style="{ width: ganttDayWidth + 'px' }">
                {{ dayjs(day).format('MM/DD') }}
              </div>
            </div>
          </div>
          <div class="gantt-body">
            <div v-for="row in ganttData" :key="row.id" class="gantt-row">
              <div class="gantt-task-name" :title="row.name">{{ row.name }}</div>
              <div class="gantt-bar-area">
                <div class="gantt-bar" :style="ganttBarStyle(row)" :class="{ 'gantt-bar-done': row.progress === 100 }">
                  <span class="gantt-bar-label">{{ row.progress }}%</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="!ganttData.length" description="暂无甘特图数据" />
        </div>
      </el-tab-pane>

      <!-- Risk -->
      <el-tab-pane label="风险评估" name="risk">
        <div class="risk-overall" v-if="riskData.overall">
          <span>综合风险等级：</span>
          <el-tag :type="riskLevelType(riskData.overall)" size="large" effect="dark">{{ riskLabel(riskData.overall) }}</el-tag>
          <span class="risk-score">风险指数：{{ riskData.score }}%</span>
        </div>
        <div class="risk-list" v-if="riskData.items && riskData.items.length">
          <el-card v-for="(r, idx) in riskData.items" :key="idx" shadow="hover" class="risk-card">
            <div class="risk-header">
              <span class="risk-name">{{ r.type }}</span>
              <el-tag :type="riskLevelType(r.level)" size="small">{{ riskLabel(r.level) }}</el-tag>
            </div>
            <p class="risk-desc">{{ r.description }}</p>
          </el-card>
        </div>
        <el-empty v-if="!riskData.overall && (!riskData.items || !riskData.items.length)" description="暂无风险评估数据" />
      </el-tab-pane>

      <!-- Files -->
      <el-tab-pane label="项目资料" name="files">
        <div class="tab-toolbar">
          <div class="toolbar-title">
            <span>项目资料</span>
            <el-tag size="small" type="info">{{ projectFiles.length }}</el-tag>
          </div>
          <div class="toolbar-actions">
            <input ref="fileInputRef" type="file"
              accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.zip,.rar,.7z,.tar,.gz,.txt,.md,.jpg,.jpeg,.png,.gif,.bmp,.svg,.webp,.mp4,.avi,.mov,.mp3,.wav"
              style="display:none" @change="handleFileChange" />
            <el-button type="primary" size="small" :loading="uploading" @click="fileInputRef?.click()">
              <el-icon><Upload /></el-icon>上传文件
            </el-button>
          </div>
        </div>
        <el-table :data="projectFiles" stripe size="small" class="file-table">
          <el-table-column label="文件名" min-width="220">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon :size="18" :style="{ color: getFileColor(row.name) }">
                  <component :is="getFileIcon(row.name)" />
                </el-icon>
                <el-link type="primary" :underline="false" @click="handleDownloadFile(row)">{{ row.name }}</el-link>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getFileTagType(row.name)" effect="plain">
                {{ getFileExtension(row.name) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="100">
            <template #default="{ row }">
              <span class="file-size">{{ formatFileSize(row.fileSize) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="170">
            <template #default="{ row }">
              <span class="file-time">{{ dayjs(row.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button text type="danger" size="small" @click="handleDeleteProjectFile(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!projectFiles.length" description="暂无项目资料，点击上传添加" />
      </el-tab-pane>
    </el-tabs>

    <!-- New Task Dialog -->
    <el-dialog v-model="showTaskDialog" title="新建任务" width="520px">
      <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="100px">
        <el-form-item label="任务名称" prop="title">
          <el-input v-model="taskForm.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="assigneeId">
          <el-select v-model="taskForm.assigneeId" placeholder="请选择负责人" filterable>
            <el-option v-for="m in project.members || []" :key="m.id" :label="m.name || m.username" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="taskForm.priority" placeholder="请选择优先级">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="taskForm.deadline" type="date" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="taskForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTaskDialog = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitting" @click="handleCreateTask">创建</el-button>
      </template>
    </el-dialog>

    <!-- Add Member Dialog -->
    <el-dialog v-model="showAddMemberDialog" title="添加团队成员" width="420px">
      <el-form @submit.prevent>
        <el-form-item label="用户ID">
          <el-input-number v-model="addMemberId" placeholder="输入用户ID" :min="1" style="width:100%" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMemberDialog = false">取消</el-button>
        <el-button type="primary" :loading="addMemberLoading" @click="handleAddMember">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- User Detail Dialog -->
    <el-dialog v-model="showMemberDetail" :title="selectedMember?.userName" width="520px" destroy-on-close>
      <div v-if="selectedMember" class="member-detail-content">
        <div class="detail-header">
          <el-avatar :size="72" :src="selectedMember.avatar || undefined" class="detail-avatar" :style="{ background: selectedMember.avatar ? undefined : avatarGradient(selectedMember.userName) }">
            {{ selectedMember.userName?.charAt(0) }}
          </el-avatar>
          <div class="detail-header-info">
            <h3 class="detail-name">{{ selectedMember.userName }}</h3>
            <el-tag :type="selectedMember.role === 'LEADER' ? 'danger' : 'primary'">
              {{ selectedMember.role === 'LEADER' ? '队长' : '成员' }}
            </el-tag>
          </div>
        </div>
        <el-divider />
        <div class="detail-section" v-if="selectedMember.major || selectedMember.grade || selectedMember.studentId || selectedMember.phone">
          <h4 class="section-title"><el-icon><InfoFilled /></el-icon> 基本信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-if="selectedMember.studentId" label="学号">{{ selectedMember.studentId }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedMember.phone" label="手机">{{ selectedMember.phone }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedMember.major" label="专业">{{ selectedMember.major }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedMember.grade" label="年级">{{ selectedMember.grade }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="detail-section" v-if="selectedMember.bio">
          <h4 class="section-title"><el-icon><ChatLineSquare /></el-icon> 个人简介</h4>
          <p class="detail-bio">{{ selectedMember.bio }}</p>
        </div>
        <div class="detail-section">
          <h4 class="section-title"><el-icon><Aim /></el-icon> 技能标签</h4>
          <div class="detail-skills" v-if="(selectedMember.skills || []).length > 0">
            <div v-for="sk in selectedMember.skills" :key="sk.id" class="skill-item">
              <span class="skill-name">{{ sk.skillName }}</span>
              <el-tag size="small" :type="skillLevelType(sk.level)">{{ skillLevelLabel(sk.level) }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无技能" :image-size="40" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getProject, getGanttData, getRiskAssessment, addProjectMember, getProjectMembers, closeProject, reopenProject, deleteProject } from '@/api/project'
import { listTasks, createTask, updateTaskStatus } from '@/api/task'
import { getProjectFiles, uploadFile, deleteFile, getDownloadUrl } from '@/api/file'
import { withToken } from '@/utils/url'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const projectId = Number(route.params.id)

const loading = ref(false)
const actionLoading = ref(false)
const project = ref<any>({})
const activeTab = ref('tasks')

// 权限判断
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const isProjectCreator = computed(() => project.value?.leaderId === userStore.userInfo?.id)

// 项目状态显示
function projectStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'PLANNING': case 'IN_PROGRESS': return 'success'
    case 'REVIEW': return ''
    case 'COMPLETED': return ''
    case 'ARCHIVED': return 'warning'
    case 'CLOSED': return 'danger'
    default: return 'info'
  }
}

function projectStatusLabel(status: string): string {
  switch (status) {
    case 'PLANNING': return '规划中'
    case 'IN_PROGRESS': return '进行中'
    case 'REVIEW': return '评审中'
    case 'COMPLETED': return '已完成'
    case 'ARCHIVED': return '已归档'
    case 'CLOSED': return '已关闭'
    default: return status || '未知'
  }
}

// Members
const projectMembers = ref<any[]>([])
const showAddMemberDialog = ref(false)
const addMemberId = ref<number | null>(null)
const addMemberLoading = ref(false)

// Member detail
const showMemberDetail = ref(false)
const selectedMember = ref<any>(null)

function openMemberDetail(member: any) {
  selectedMember.value = member
  showMemberDetail.value = true
}

function skillLevelType(level: string): 'success' | 'warning' | 'danger' | 'info' {
  if (level === 'EXPERT') return 'danger'
  if (level === 'ADVANCED') return 'success'
  if (level === 'INTERMEDIATE') return 'warning'
  return 'info'
}

function skillLevelLabel(level: string): string {
  const map: Record<string, string> = { BEGINNER: '入门', INTERMEDIATE: '中级', ADVANCED: '高级', EXPERT: '专家' }
  return map[level] || level || '入门'
}

// 根据用户名生成稳定的渐变色头像背景
const AVATAR_PALETTES = [
  'linear-gradient(135deg, #667eea, #764ba2)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #a18cd1, #fbc2eb)',
  'linear-gradient(135deg, #fccb90, #d57eeb)',
  'linear-gradient(135deg, #e0c3fc, #8ec5fc)',
]
function avatarGradient(name?: string): string {
  if (!name) return AVATAR_PALETTES[0]
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_PALETTES[Math.abs(hash) % AVATAR_PALETTES.length]
}

const tasks = ref<any[]>([])
const showTaskDialog = ref(false)
const taskSubmitting = ref(false)
const taskForm = ref({ title: '', assigneeId: null as number | null, priority: 'MEDIUM', deadline: null as string | null, description: '' })
const taskRules = { title: [{ required: true, message: '请输入任务名称', trigger: 'blur' }] }

// Files
const projectFiles = ref<any[]>([])
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

// 里程碑数据存储在 description 字段中（JSON数组），需要从description中解析
const milestones = computed(() => {
  const raw = project.value.description
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.map((m: any) => ({
      id: m.id,
      name: m.name,
      description: m.description,
      dueDate: m.deadline,  // 后端存的是deadline，前端用dueDate
      status: m.status     // PENDING / COMPLETED
    }))
  } catch {
    return []
  }
})

const ganttData = ref<any[]>([])
const ganttDayWidth = 60

const ganttDays = computed(() => {
  if (!ganttData.value.length) return []
  const allDates: number[] = []
  ganttData.value.forEach((r: any) => {
    if (r.start) allDates.push(new Date(r.start).getTime())
    if (r.end) allDates.push(new Date(r.end).getTime())
  })
  if (!allDates.length) return []
  const min = Math.min(...allDates); const max = Math.max(...allDates)
  const days: number[] = []; let cur = new Date(min)
  while (cur.getTime() <= max) { days.push(cur.getTime()); cur = new Date(cur.getTime() + 86400000) }
  return days
})

function ganttBarStyle(row: any) {
  if (!row.start || !row.end || !ganttDays.value.length) return { display: 'none' }
  const totalMs = ganttDays.value[ganttDays.value.length - 1] - ganttDays.value[0] + 86400000
  if (totalMs <= 0) return { display: 'none' }
  const startMs = new Date(row.start).getTime()
  const endMs = new Date(row.end).getTime()
  const left = Math.max(0, ((startMs - ganttDays.value[0]) / totalMs) * 100)
  const width = Math.min(100 - left, Math.max(((endMs - startMs) / totalMs) * 100, 3))
  return { left: left + '%', width: width + '%' }
}

const riskData = ref<Record<string, any>>({})

const statusMap: Record<string, string> = { PENDING: 'info', DOING: 'primary', DONE: 'success', OVERDUE: 'danger' }
const statusLabelMap: Record<string, string> = { PENDING: '待处理', DOING: '进行中', DONE: '已完成', OVERDUE: '已逾期' }
function statusType(s: string) { return statusMap[s] || 'info' }
function statusLabel(s: string) { return statusLabelMap[s] || s }
function priorityType(p: string) { if (p === 'HIGH') return 'danger'; if (p === 'MEDIUM') return 'warning'; return 'primary' }
function priorityLabel(p: string) { if (p === 'HIGH') return '高'; if (p === 'MEDIUM') return '中'; return '低' }
function riskLevelType(l: string) { if (l === 'HIGH') return 'danger'; if (l === 'MEDIUM') return 'warning'; return 'info' }
function riskLabel(l: string) { if (l === 'HIGH') return '高风险'; if (l === 'MEDIUM') return '中风险'; return '低风险' }

onMounted(async () => {
  if (isNaN(projectId) || projectId <= 0) {
    ElMessage.error('无效的项目ID')
    router.replace('/projects')
    return
  }
  await loadProject(); await loadTasks(); await loadGantt(); await loadRisk(); await loadMembers(); await loadFiles()
})

async function loadProject() { loading.value = true; try { const res = await getProject(projectId); project.value = res.data || {} } catch (_) { project.value = {} } finally { loading.value = false } }
async function loadMembers() { try { const res = await getProjectMembers(projectId); projectMembers.value = res.data || [] } catch (_) { projectMembers.value = [] } }
async function loadTasks() { try { const res = await listTasks({ projectId }); tasks.value = res.data || [] } catch (_) { tasks.value = [] } }
async function loadGantt() { try { const res = await getGanttData(projectId); ganttData.value = res.data || [] } catch (_) { ganttData.value = [] } }
async function loadRisk() { try { const res = await getRiskAssessment(projectId); riskData.value = res.data || {} } catch (_) { riskData.value = {} } }

async function handleToggleStatus(row: any) {
  const nextStatus = row.status === 'PENDING' ? 'DOING' : row.status === 'DOING' ? 'DONE' : row.status === 'DONE' ? 'PENDING' : 'PENDING'
  try { await updateTaskStatus(row.id, nextStatus); ElMessage.success('状态已更新'); row.status = nextStatus } catch (_) {}
}

// ---- 项目关闭/开启/删除 ----
async function handleCloseProject() {
  try {
    await ElMessageBox.confirm(
      '关闭项目后，关联团队将同步关闭，且无法编辑任何状态。确定要继续吗？',
      '关闭项目确认',
      { confirmButtonText: '确认关闭', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  actionLoading.value = true
  try {
    await closeProject(projectId)
    ElMessage.success('项目已关闭，关联团队同步关闭')
    await loadProject()
    await loadMembers()
  } catch (_) {}
  finally { actionLoading.value = false }
}

async function handleReopenProject() {
  try {
    await ElMessageBox.confirm(
      '重新开启项目后，关联团队将同步开启，可以恢复编辑。确定要继续吗？',
      '开启项目确认',
      { confirmButtonText: '确认开启', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }

  actionLoading.value = true
  try {
    await reopenProject(projectId)
    ElMessage.success('项目已重新开启，关联团队同步开启')
    await loadProject()
    await loadMembers()
  } catch (_) {}
  finally { actionLoading.value = false }
}

async function handleDeleteProject() {
  try {
    await ElMessageBox.confirm(
      '删除项目将同时删除关联的团队、任务、文件等所有数据，此操作不可撤销！确定要继续吗？',
      '删除项目确认',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch { return }

  actionLoading.value = true
  try {
    await deleteProject(projectId)
    ElMessage.success('项目及关联团队、任务已全部删除')
    router.push('/projects')
  } catch (_) {}
  finally { actionLoading.value = false }
}

async function handleCreateTask() {
  taskSubmitting.value = true
  try {
    await createTask({ ...taskForm.value, projectId })
    ElMessage.success('任务创建成功')
    showTaskDialog.value = false
    taskForm.value = { title: '', assigneeId: null, priority: 'MEDIUM', deadline: null, description: '' }
    await loadTasks()
  } catch (_) {} finally { taskSubmitting.value = false }
}

async function handleAddMember() {
  if (!addMemberId.value) { ElMessage.warning('请输入用户ID'); return }
  addMemberLoading.value = true
  try {
    await addProjectMember(projectId, addMemberId.value)
    ElMessage.success('成员添加成功')
    showAddMemberDialog.value = false
    addMemberId.value = null
    await loadMembers()
  } catch (_) {} finally { addMemberLoading.value = false }
}

// ---------- 项目资料 ----------
async function loadFiles() {
  try {
    const teamId = project.value.teamId || project.value.team?.id
    if (!teamId) return
    const res = await getProjectFiles(teamId, projectId)
    projectFiles.value = res.data || []
  } catch (_) { projectFiles.value = [] }
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const teamId = project.value.teamId || project.value.team?.id
  if (!teamId) { ElMessage.warning('无法获取团队信息'); return }
  uploading.value = true
  try {
    await uploadFile(file, teamId, projectId)
    ElMessage.success('文件上传成功')
    await loadFiles()
  } catch (_) {} finally {
    uploading.value = false
    // 重置 input 以允许重复上传同一文件
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

function handleDownloadFile(row: any) {
  const a = document.createElement('a')
  a.href = withToken(getDownloadUrl(row.id))
  a.download = row.name
  a.click()
}

async function handleDeleteProjectFile(row: any) {
  try { await ElMessageBox.confirm(`确认删除文件 "${row.name}"？`, '删除确认', { type: 'warning' }) }
  catch { return }
  try {
    await deleteFile(row.id)
    ElMessage.success('文件已删除')
    await loadFiles()
  } catch (_) {}
}

function formatFileSize(bytes: number | undefined): string {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ========== 文件类型图标/颜色 ==========
function getFileExtension(filename: string): string {
  if (!filename) return '未知'
  const dotIdx = filename.lastIndexOf('.')
  if (dotIdx === -1) return '文件'
  return filename.substring(dotIdx + 1).toUpperCase()
}

function getFileIcon(filename: string): string {
  const ext = getFileExtension(filename).toLowerCase()
  // 文档类
  if (['doc', 'docx'].includes(ext)) return 'Document'
  if (['xls', 'xlsx', 'csv'].includes(ext)) return 'Grid'
  if (['ppt', 'pptx'].includes(ext)) return 'DataAnalysis'
  if (ext === 'pdf') return 'Tickets'
  // 图片类
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(ext)) return 'Picture'
  // 视频类
  if (['mp4', 'avi', 'mov', 'mkv', 'wmv', 'flv'].includes(ext)) return 'VideoCamera'
  // 音频类
  if (['mp3', 'wav', 'aac', 'flac', 'ogg'].includes(ext)) return 'Headset'
  // 压缩包
  if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(ext)) return 'FolderOpened'
  // 代码/文本
  if (['txt', 'md', 'json', 'xml', 'java', 'ts', 'js', 'py', 'c', 'cpp', 'html', 'css', 'sql'].includes(ext)) return 'EditPen'
  return 'Document'
}

function getFileColor(filename: string): string {
  const ext = getFileExtension(filename).toLowerCase()
  if (['doc', 'docx'].includes(ext)) return '#2b5fdc'       // Word - 蓝
  if (['xls', 'xlsx', 'csv'].includes(ext)) return '#217346'  // Excel - 绿
  if (['ppt', 'pptx'].includes(ext)) return '#d24726'         // PPT - 橙红
  if (ext === 'pdf') return '#e44d4d'                         // PDF - 红
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(ext)) return '#e6a23c'
  if (['mp4', 'avi', 'mov', 'mkv', 'wmv', 'flv'].includes(ext)) return '#8b5cf6'
  if (['mp3', 'wav', 'aac', 'flac', 'ogg'].includes(ext)) return '#10b981'
  if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(ext)) return '#f59e0b'  // 压缩包 - 琥珀
  return '#909399'
}

function getFileTagType(filename: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' | '' {
  const ext = getFileExtension(filename).toLowerCase()
  if (['doc', 'docx'].includes(ext)) return 'primary'
  if (['xls', 'xlsx', 'csv'].includes(ext)) return 'success'
  if (['ppt', 'pptx'].includes(ext)) return 'warning'
  if (ext === 'pdf') return 'danger'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(ext)) return ''
  if (['mp4', 'avi', 'mov', 'mkv', 'wmv', 'flv'].includes(ext)) return ''
  if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(ext)) return 'warning'
  return 'info'
}
</script>

<style scoped>
.project-detail-page { max-width: 1200px; margin: 0 auto; }

.back-nav { margin-bottom: 16px; }

/* ===== Project Header ===== */
.project-header-card {
  border-radius: 14px;
  margin-bottom: 16px;
}

.header-content { display: flex; align-items: center; justify-content: space-between; }
.header-left { display: flex; align-items: center; gap: 20px; }

.project-avatar {
  width: 56px; height: 56px; border-radius: 14px;
  background: var(--gradient-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}

.header-text { flex: 1; }
.header-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.header-title-row h2 { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0; }

.project-desc { font-size: 14px; color: #606266; margin: 0 0 8px; }

.header-meta { display: flex; gap: 20px; font-size: 13px; color: #909399; }
.header-meta span { display: flex; align-items: center; gap: 4px; }

/* ===== Tabs ===== */
.project-tabs { margin-top: 16px; border-radius: 12px; overflow: hidden; }

.tab-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }

.toolbar-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1a1a2e; }

.task-table :deep(.el-table__header) th { background: #f8f9fb !important; color: #606266; font-weight: 600; }

.assignee-cell { display: flex; align-items: center; }

.overdue { color: #f56c6c; font-weight: 600; }

/* Milestones */
.milestone-item { display: flex; align-items: center; gap: 8px; }
.milestone-name { font-weight: 600; font-size: 14px; }
.milestone-desc { font-size: 12px; color: #909399; margin-top: 4px; }

/* Gantt */
.gantt-container { overflow-x: auto; }
.gantt-header, .gantt-row { display: flex; align-items: center; min-height: 36px; }
.gantt-task-header { width: 160px; flex-shrink: 0; font-weight: 600; font-size: 13px; color: #606266; }
.gantt-task-name { width: 160px; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; color: #303133; }
.gantt-bar-area { display: flex; flex: 1; position: relative; height: 36px; }
.gantt-day-label { font-size: 10px; color: #c0c4cc; flex-shrink: 0; border-left: 1px solid #ebeef5; padding-left: 2px; line-height: 36px; }
.gantt-bar { position: absolute; top: 8px; height: 20px; background: linear-gradient(90deg, #409eff, #79bbff); border-radius: 10px; display: flex; align-items: center; justify-content: center; min-width: 40px; }
.gantt-bar-done { background: linear-gradient(90deg, #67c23a, #95d475); }
.gantt-bar-label { font-size: 11px; color: #fff; white-space: nowrap; }
.gantt-body .gantt-row:nth-child(even) { background: #fafafa; }

/* Risk */
.risk-overall {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 20px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  margin-bottom: 16px; font-size: 15px;
}
.risk-overall span:first-child { font-weight: 600; color: #303133; }
.risk-score { margin-left: auto; font-weight: 600; color: #e6a23c; }
.risk-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.risk-card { cursor: default; border-radius: 10px; }
.risk-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.risk-name { font-weight: 600; font-size: 14px; color: #1a1a2e; }
.risk-desc { font-size: 13px; color: #606266; margin-bottom: 8px; }
.risk-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: #909399; }

/* Members */
.member-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.member-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: all 0.2s ease;
}
.member-card:hover { border-color: var(--color-primary); box-shadow: 0 2px 12px rgba(102,126,234,0.15); transform: translateY(-1px); }
.member-avatar { font-weight: 700; font-size: 18px; color: #fff; flex-shrink: 0; }
.member-avatar :deep(img) { object-fit: cover; }
.member-info { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.member-name { font-weight: 600; font-size: 14px; color: var(--color-text-primary); }
.member-major { font-size: 12px; color: #909399; }
.member-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.member-arrow { color: #c0c4cc; flex-shrink: 0; }

/* Member detail dialog */
.member-detail-content { padding: 0; }
.detail-header { display: flex; align-items: center; gap: 16px; margin-bottom: 4px; }
.detail-avatar { font-weight: 700; font-size: 28px; color: #fff; flex-shrink: 0; }
.detail-avatar :deep(img) { object-fit: cover; }
.detail-header-info { display: flex; flex-direction: column; gap: 6px; }
.detail-name { margin: 0; font-size: 18px; font-weight: 700; color: #1a1a2e; }
.detail-section { margin-bottom: 20px; }
.section-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: #303133; margin: 0 0 10px 0; }
.detail-bio { font-size: 14px; color: #606266; line-height: 1.6; margin: 0; padding: 10px 12px; background: #f8f9fb; border-radius: 8px; }
.detail-skills { display: flex; flex-wrap: wrap; gap: 8px; }
.skill-item { display: flex; align-items: center; gap: 8px; padding: 6px 12px; background: #f0f2f5; border-radius: 8px; }
.skill-name { font-size: 13px; color: #303133; font-weight: 500; }

/* Files */
.file-table :deep(.el-table__header) th { background: #f8f9fb !important; color: #606266; font-weight: 600; }
.file-name-cell { display: flex; align-items: center; gap: 8px; }
.file-size { color: #909399; font-size: 13px; }
.file-time { color: #909399; font-size: 13px; }
.toolbar-actions { display: flex; gap: 8px; }
</style>
