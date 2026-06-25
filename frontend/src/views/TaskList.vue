<template>
  <div class="task-list-page">
    <div class="page-header-bar">
      <div>
        <h2>任务中心</h2>
        <p class="page-desc">查看和管理你所有的项目任务</p>
      </div>
      <el-button type="primary" size="large" class="create-btn" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>新建任务
      </el-button>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="进行中" value="DOING" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已逾期" value="OVERDUE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="filters.priority" placeholder="全部优先级" clearable style="width:140px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTasks">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Task Table -->
    <el-card shadow="never" class="table-card">
      <el-table :data="tasks" v-loading="loading" stripe size="small" class="task-table">
        <el-table-column prop="title" label="任务名称" min-width="200">
          <template #default="{ row }">
            <span class="task-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" width="150">
          <template #default="{ row }">
            <span class="project-tag">{{ row.project?.name || row.projectName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">
            <span>{{ row.assignee?.name || row.assignee?.username || row.assigneeName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'MEDIUM' ? 'warning' : 'primary'" size="small" effect="plain">
              {{ row.priority === 'HIGH' ? '高' : row.priority === 'MEDIUM' ? '中' : '低' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" style="cursor:pointer" @click="handleToggleStatus(row)">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="120">
          <template #default="{ row }">
            <span :class="{ 'deadline-overdue': row.status !== 'DONE' && row.deadline && dayjs(row.deadline).isBefore(dayjs()) }">
              {{ row.deadline ? dayjs(row.deadline).format('YYYY-MM-DD') : '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="handleToggleStatus(row)">
              {{ row.status === 'DONE' ? '重开' : row.status === 'PENDING' ? '开始' : '完成' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务" />
    </el-card>

    <!-- Create Task Dialog -->
    <el-dialog v-model="showCreateDialog" title="新建任务" width="540px" class="create-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable>
            <el-option v-for="p in myProjects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="assigneeId">
          <el-select v-model="form.assigneeId" placeholder="请选择负责人" filterable>
            <el-option v-for="m in currentMembers" :key="m.id" :label="m.name || m.username" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="请选择优先级">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="form.deadline" type="date" placeholder="选择截止日期" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getMyTasks, createTask, updateTaskStatus } from '@/api/task'
import { getMyProjects } from '@/api/project'

const loading = ref(false)
const tasks = ref<any[]>([])
const myProjects = ref<any[]>([])
const showCreateDialog = ref(false)
const submitting = ref(false)

const filters = ref({ status: '', priority: '' })
const form = ref({ title: '', projectId: null as number | null, assigneeId: null as number | null, priority: 'MEDIUM', deadline: null as string | null, description: '' })
const rules = {
  title: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
}

const currentMembers = computed(() => {
  const selected = myProjects.value.find((p) => p.id === form.value.projectId)
  return selected?.members || []
})

const statusMap: Record<string, string> = { PENDING: 'info', DOING: 'primary', DONE: 'success', OVERDUE: 'danger' }
const statusLabelMap: Record<string, string> = { PENDING: '待处理', DOING: '进行中', DONE: '已完成', OVERDUE: '已逾期' }

function statusType(s: string) { return statusMap[s] || 'info' }
function statusLabel(s: string) { return statusLabelMap[s] || s }

onMounted(async () => {
  await loadTasks()
  try { const res = await getMyProjects(); myProjects.value = res.data || [] } catch (_) { myProjects.value = [] }
})

async function loadTasks() {
  loading.value = true
  try {
    const res = await getMyTasks()
    let data = res.data || []
    // 前端本地筛选（API暂不支持筛选参数时使用）
    if (filters.value.status) {
      data = data.filter((t: any) => t.status === filters.value.status)
    }
    if (filters.value.priority) {
      data = data.filter((t: any) => t.priority === filters.value.priority)
    }
    tasks.value = data
  } catch (_) { tasks.value = [] }
  finally { loading.value = false }
}

function resetFilters() {
  filters.value.status = ''
  filters.value.priority = ''
  loadTasks()
}

async function handleToggleStatus(row: any) {
  const nextStatus = row.status === 'PENDING' ? 'DOING' : row.status === 'DOING' ? 'DONE' : row.status === 'DONE' ? 'PENDING' : 'PENDING'
  try { await updateTaskStatus(row.id, nextStatus); ElMessage.success('状态已更新'); row.status = nextStatus } catch (_) {}
}

async function handleCreate() {
  submitting.value = true
  try {
    await createTask(form.value)
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    form.value = { title: '', projectId: null, assigneeId: null, priority: 'MEDIUM', deadline: null, description: '' }
    await loadTasks()
  } catch (_) {} finally { submitting.value = false }
}
</script>

<style scoped>
.task-list-page { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--gradient-primary);
  border-radius: 16px; padding: 24px 32px;
  margin-bottom: 20px; color: #fff;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-desc { margin: 0; font-size: 14px; opacity: 0.8; }

.create-btn { border-radius: 10px; font-weight: 600; border: none; background: rgba(255,255,255,0.2) !important; backdrop-filter: blur(4px); }
.create-btn:hover { background: rgba(255,255,255,0.3) !important; transform: translateY(-1px); }

.filter-card { border-radius: 14px; margin-bottom: 16px; }

.table-card { border-radius: 14px; min-height: 300px; }
.table-card :deep(.el-table__header) th { background: #f8f9fb !important; color: #606266; font-weight: 600; }

.task-title { font-weight: 500; color: #1a1a2e; }

.project-tag { display: inline-block; padding: 2px 8px; background: #f0f5ff; border-radius: 4px; color: var(--color-primary); font-size: 12px; }

.deadline-overdue { color: #f56c6c; font-weight: 600; }

.create-dialog :deep(.el-dialog__body) { padding: 24px 32px; }
</style>
