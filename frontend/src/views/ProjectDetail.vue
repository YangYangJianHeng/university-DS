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
              <el-tag :type="project.status === 'ARCHIVED' ? 'warning' : 'success'" size="small" effect="dark">
                {{ project.status === 'ARCHIVED' ? '已归档' : '进行中' }}
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
      </div>
    </el-card>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="project-tabs" type="border-card">
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
              <el-tag :type="priorityType(row.priority)" size="small" effect="plain">{{ row.priority }}</el-tag>
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
            <div v-for="row in ganttData" :key="row.taskId" class="gantt-row">
              <div class="gantt-task-name" :title="row.taskName">{{ row.taskName }}</div>
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
        <div class="risk-list">
          <el-card v-for="r in riskData" :key="r.id" shadow="hover" class="risk-card">
            <div class="risk-header">
              <span class="risk-name">{{ r.name }}</span>
              <el-tag :type="riskLevelType(r.level)" size="small">{{ r.level }}</el-tag>
            </div>
            <p class="risk-desc">{{ r.description }}</p>
            <div class="risk-meta">
              <span>概率：{{ r.probability }}%</span>
              <span>影响：{{ r.impact }}%</span>
              <span>状态：<el-tag :type="r.status === 'MITIGATED' ? 'success' : 'danger'" size="small">{{ r.status === 'MITIGATED' ? '已缓解' : '待处理' }}</el-tag></span>
            </div>
          </el-card>
        </div>
        <el-empty v-if="!riskData.length" description="暂无风险评估数据" />
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
            <el-option label="HIGH" value="HIGH" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="LOW" value="LOW" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getProject, getGanttData, getRiskAssessment } from '@/api/project'
import { listTasks, createTask, updateTaskStatus } from '@/api/task'

const router = useRouter()
const route = useRoute()
const projectId = Number(route.params.id)

const loading = ref(false)
const project = ref<any>({})
const activeTab = ref('tasks')

const tasks = ref<any[]>([])
const showTaskDialog = ref(false)
const taskSubmitting = ref(false)
const taskForm = ref({ title: '', assigneeId: null as number | null, priority: 'MEDIUM', deadline: null as string | null, description: '' })
const taskRules = { title: [{ required: true, message: '请输入任务名称', trigger: 'blur' }] }

const milestones = computed(() => project.value.milestones || [])

const ganttData = ref<any[]>([])
const ganttDayWidth = 60

const ganttDays = computed(() => {
  if (!ganttData.value.length) return []
  const allDates: number[] = []
  ganttData.value.forEach((r: any) => {
    if (r.startDate) allDates.push(new Date(r.startDate).getTime())
    if (r.endDate) allDates.push(new Date(r.endDate).getTime())
  })
  if (!allDates.length) return []
  const min = Math.min(...allDates); const max = Math.max(...allDates)
  const days: number[] = []; let cur = new Date(min)
  while (cur.getTime() <= max) { days.push(cur.getTime()); cur = new Date(cur.getTime() + 86400000) }
  return days
})

function ganttBarStyle(row: any) {
  if (!row.startDate || !row.endDate || !ganttDays.value.length) return { display: 'none' }
  const totalMs = ganttDays.value[ganttDays.value.length - 1] - ganttDays.value[0] + 86400000
  if (totalMs <= 0) return { display: 'none' }
  const startMs = new Date(row.startDate).getTime()
  const endMs = new Date(row.endDate).getTime()
  const left = Math.max(0, ((startMs - ganttDays.value[0]) / totalMs) * 100)
  const width = Math.min(100 - left, Math.max(((endMs - startMs) / totalMs) * 100, 3))
  return { left: left + '%', width: width + '%' }
}

const riskData = ref<any[]>([])

const statusMap: Record<string, string> = { PENDING: 'info', DOING: 'primary', DONE: 'success', OVERDUE: 'danger' }
const statusLabelMap: Record<string, string> = { PENDING: '待处理', DOING: '进行中', DONE: '已完成', OVERDUE: '已逾期' }
function statusType(s: string) { return statusMap[s] || 'info' }
function statusLabel(s: string) { return statusLabelMap[s] || s }
function priorityType(p: string) { if (p === 'HIGH') return 'danger'; if (p === 'MEDIUM') return 'warning'; return 'primary' }
function riskLevelType(l: string) { if (l === 'HIGH') return 'danger'; if (l === 'MEDIUM') return 'warning'; return 'info' }

onMounted(async () => { await loadProject(); await loadTasks(); await loadGantt(); await loadRisk() })

async function loadProject() { loading.value = true; try { const res = await getProject(projectId); project.value = res.data || {} } catch (_) { project.value = {} } finally { loading.value = false } }
async function loadTasks() { try { const res = await listTasks({ projectId }); tasks.value = res.data || [] } catch (_) { tasks.value = [] } }
async function loadGantt() { try { const res = await getGanttData(projectId); ganttData.value = res.data || [] } catch (_) { ganttData.value = [] } }
async function loadRisk() { try { const res = await getRiskAssessment(projectId); riskData.value = res.data || [] } catch (_) { riskData.value = [] } }

async function handleToggleStatus(row: any) {
  const nextStatus = row.status === 'PENDING' ? 'DOING' : row.status === 'DOING' ? 'DONE' : row.status === 'DONE' ? 'PENDING' : 'PENDING'
  try { await updateTaskStatus(row.id, nextStatus); ElMessage.success('状态已更新'); row.status = nextStatus } catch (_) {}
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
.risk-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.risk-card { cursor: default; border-radius: 10px; }
.risk-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.risk-name { font-weight: 600; font-size: 14px; color: #1a1a2e; }
.risk-desc { font-size: 13px; color: #606266; margin-bottom: 8px; }
.risk-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: #909399; }
</style>
