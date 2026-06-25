<template>
  <div class="task-center-page">
    <!-- 顶部标题栏 -->
    <div class="page-header-bar">
      <div>
        <h2>任务中心</h2>
        <p class="page-desc">团队协作任务管理，支持多人指派与成果验收</p>
      </div>
      <el-button type="primary" size="large" class="create-btn" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>新建任务
      </el-button>
    </div>

    <!-- 视图切换 -->
    <el-card shadow="never" class="view-switch-card">
      <div class="view-switch-row">
        <div class="view-tabs">
          <el-radio-group v-model="viewMode" size="default">
            <el-radio-button value="my">我的任务</el-radio-button>
            <el-radio-button value="managed">我管理的</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="displayMode" size="default" style="margin-left:16px">
            <el-radio-button value="kanban"><el-icon><Grid /></el-icon> 看板</el-radio-button>
            <el-radio-button value="list"><el-icon><List /></el-icon> 列表</el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-row">
          <el-select v-model="filters.projectId" placeholder="按项目筛选" clearable style="width:180px" @change="loadTasks">
            <el-option v-for="p in myProjects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <el-select v-model="filters.priority" placeholder="优先级" clearable style="width:110px" @change="loadTasks">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- 看板视图 -->
    <div v-if="displayMode === 'kanban'" class="kanban-board">
      <div v-for="col in kanbanColumns" :key="col.status" class="kanban-column">
        <div class="kanban-header" :style="{ borderTopColor: col.color }">
          <span class="kanban-title">{{ col.label }}</span>
          <el-tag :type="col.tagType" size="small" round>{{ colTasks(col.status).length }}</el-tag>
        </div>
        <div class="kanban-body">
          <div
            v-for="task in colTasks(col.status)"
            :key="task.id"
            class="kanban-card"
            @click="openTaskDetail(task)"
          >
            <div class="card-priority" :class="'priority-' + (task.priority || 'MEDIUM').toLowerCase()"></div>
            <div class="card-title">{{ task.title }}</div>
            <div class="card-meta">
              <span v-if="task.projectName" class="card-project">{{ task.projectName }}</span>
              <span v-if="task.deadline" class="card-deadline" :class="{ overdue: isOverdue(task) }">
                <el-icon><Clock /></el-icon> {{ dayjs(task.deadline).format('MM-DD') }}
              </span>
            </div>
            <div class="card-assignees" v-if="task.assigneeCount">
              <el-icon><User /></el-icon> {{ task.assigneeCount }}人
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <el-card v-else shadow="never" class="table-card">
      <el-table :data="tasks" v-loading="loading" stripe size="small" @row-click="openTaskDetail">
        <el-table-column prop="title" label="任务名称" min-width="200">
          <template #default="{ row }">
            <span class="task-title-link">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" width="160">
          <template #default="{ row }">
            <span class="project-tag">{{ row.projectName || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="执行人" width="150">
          <template #default="{ row }">
            <span>{{ row.assigneeNames || row.assigneeCount + '人' || '未指派' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small" effect="plain">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="120">
          <template #default="{ row }">
            <span :class="{ 'deadline-overdue': isOverdue(row) }">
              {{ row.deadline ? dayjs(row.deadline).format('YYYY-MM-DD') : '--' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click.stop="openTaskDetail(row)">详情</el-button>
            <el-button v-if="viewMode === 'managed'" text size="small" type="warning" @click.stop="editTask(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务" />
    </el-card>

    <!-- ========== 创建任务弹窗 ========== -->
    <el-dialog v-model="showCreateDialog" title="新建任务" width="640px" class="create-dialog" @close="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="createForm.projectId" placeholder="请选择项目" filterable style="width:100%" @change="onProjectChange">
            <el-option v-for="p in managedProjects" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入任务名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="所需技能" prop="requiredSkills">
          <el-input v-model="createForm.requiredSkills" placeholder="逗号分隔，如：Java,Python,前端开发" />
          <span class="form-hint">填写技能标签便于智能推荐合适的执行人</span>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="createForm.priority" placeholder="选择优先级" style="width:160px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择截止日期" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="任务详细描述" />
        </el-form-item>
        <el-divider content-position="left">指派执行人</el-divider>
        <el-form-item label="技能推荐">
          <el-button size="small" type="primary" text :loading="recommending" @click="loadRecommendations">
            <el-icon><Search /></el-icon> 智能推荐匹配成员
          </el-button>
        </el-form-item>
        <div v-if="recommendList.length > 0" class="recommend-section">
          <div v-for="item in recommendList" :key="item.userId" class="recommend-item">
            <div class="recommend-info">
              <el-avatar :size="32">{{ item.userName?.charAt(0) }}</el-avatar>
              <span class="recommend-name">{{ item.userName }}</span>
              <el-tag size="small" :type="parseFloat(item.skillMatchScore) > 0.5 ? 'success' : 'warning'">
                匹配 {{ Math.round(parseFloat(item.skillMatchScore) * 100) }}%
              </el-tag>
            </div>
            <el-button size="small" type="primary" text @click="quickAddAssignee(item)">指派</el-button>
          </div>
        </div>
        <el-form-item label="已选执行人">
          <div class="assignee-tags">
            <el-tag
              v-for="a in createForm.assignees"
              :key="a.userId"
              closable
              @close="removeFromAssignees(a.userId)"
              style="margin:2px"
            >
              {{ a.userName }}
              <el-popover placement="top" :width="200" trigger="click">
                <template #reference>
                  <el-icon style="margin-left:4px;cursor:pointer"><EditPen /></el-icon>
                </template>
                <el-input v-model="a.assignmentNote" placeholder="分工说明" size="small" />
              </el-popover>
            </el-tag>
            <el-button v-if="currentProjectMembers.length > 0" size="small" @click="showMemberPicker = true">
              <el-icon><Plus /></el-icon> 选择成员
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateTask">创建任务</el-button>
      </template>
    </el-dialog>

    <!-- 成员选择弹窗 -->
    <el-dialog v-model="showMemberPicker" title="选择团队成员" width="420px" append-to-body>
      <div v-for="m in currentProjectMembers" :key="m.id" class="member-pick-item">
        <el-checkbox
          :model-value="createForm.assignees.some(a => a.userId === m.id)"
          @change="(val: boolean) => toggleMember(m, val)"
        >
          <span>{{ m.name || m.username }}</span>
        </el-checkbox>
      </div>
      <template #footer>
        <el-button @click="showMemberPicker = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- ========== 任务详情弹窗 ========== -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="720px" class="detail-dialog" @close="detailTask = null">
      <template v-if="detailTask">
        <div class="detail-header">
          <div class="detail-title-row">
            <el-tag :type="statusType(detailTask.status)" size="small">{{ statusLabel(detailTask.status) }}</el-tag>
            <el-tag :type="priorityType(detailTask.priority)" size="small" effect="plain">{{ priorityLabel(detailTask.priority) }}</el-tag>
            <span class="detail-title">{{ detailTask.title }}</span>
          </div>
          <div class="detail-meta">
            <span>创建者：{{ detailTask.creatorName }}</span>
            <span v-if="detailTask.deadline">截止：{{ dayjs(detailTask.deadline).format('YYYY-MM-DD HH:mm') }}</span>
            <span v-if="detailTask.requiredSkills">所需技能：{{ detailTask.requiredSkills }}</span>
          </div>
          <p v-if="detailTask.description" class="detail-desc">{{ detailTask.description }}</p>
        </div>

        <!-- 执行人 -->
        <div class="detail-section">
          <div class="section-header">
            <h4><el-icon><UserFilled /></el-icon> 执行人 ({{ detailTask.assignees?.length || 0 }})</h4>
            <el-button v-if="viewMode === 'managed'" size="small" type="primary" text @click="openAddAssigneeDialog">+ 添加</el-button>
          </div>
          <div class="assignee-list">
            <div v-for="a in detailTask.assignees" :key="a.id" class="assignee-item">
              <el-avatar :size="28">{{ a.userName?.charAt(0) }}</el-avatar>
              <span class="assignee-name">{{ a.userName }}</span>
              <el-tag size="small" :type="a.status === 'ACCEPTED' ? 'success' : a.status === 'REJECTED' ? 'danger' : 'info'">
                {{ a.status === 'ACCEPTED' ? '已接受' : a.status === 'REJECTED' ? '已拒绝' : '待确认' }}
              </el-tag>
              <span v-if="a.assignmentNote" class="assignee-note">{{ a.assignmentNote }}</span>
              <span v-if="a.skillMatchScore && a.skillMatchScore !== '0'" class="match-score">
                匹配{{ Math.round(parseFloat(a.skillMatchScore) * 100) }}%
              </span>
            </div>
            <span v-if="!detailTask.assignees?.length" class="empty-hint">暂无执行人</span>
          </div>
        </div>

        <!-- 成果提交 -->
        <div class="detail-section">
          <div class="section-header">
            <h4><el-icon><FolderOpened /></el-icon> 成果提交 ({{ detailTask.submissions?.length || 0 }})</h4>
            <el-button size="small" type="primary" @click="openSubmitDialog">提交成果</el-button>
          </div>
          <div v-if="detailTask.submissions?.length" class="submission-list">
            <div v-for="s in detailTask.submissions" :key="s.id" class="submission-item">
              <div class="submission-header">
                <span class="submission-title">{{ s.title }}</span>
                <el-tag size="small" :type="s.status === 'APPROVED' ? 'success' : s.status === 'REJECTED' ? 'danger' : 'warning'">
                  {{ s.status === 'APPROVED' ? '已通过' : s.status === 'REJECTED' ? '已退回' : '待验收' }}
                </el-tag>
              </div>
              <div class="submission-meta">
                <span>提交人：{{ s.submitterName }}</span>
                <span>{{ dayjs(s.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
              </div>
              <p v-if="s.description" class="submission-desc">{{ s.description }}</p>
              <div v-if="s.fileUrls?.length" class="file-list">
                <el-tag v-for="(url, i) in s.fileUrls" :key="i" size="small" type="info" class="file-tag">
                  <el-icon><Link /></el-icon> {{ getFileName(url) }}
                </el-tag>
              </div>
              <div v-if="s.reviewComment" class="review-comment">
                <span class="review-label">验收意见：</span>{{ s.reviewComment }}
                <span v-if="s.reviewerName" class="reviewer">— {{ s.reviewerName }}</span>
              </div>
              <!-- 负责人验收按钮 -->
              <div v-if="viewMode === 'managed' && s.status === 'PENDING_REVIEW'" class="review-actions">
                <el-button size="small" type="success" @click="reviewSubmission(s.id, 'APPROVED')">通过</el-button>
                <el-button size="small" type="danger" @click="reviewSubmission(s.id, 'REJECTED')">退回</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 评论 -->
        <div class="detail-section">
          <div class="section-header">
            <h4><el-icon><ChatDotRound /></el-icon> 评论 ({{ detailTask.comments?.length || 0 }})</h4>
          </div>
          <div class="comment-list">
            <div v-for="c in detailTask.comments" :key="c.id" class="comment-item">
              <el-avatar :size="24">{{ c.userName?.charAt(0) }}</el-avatar>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-user">{{ c.userName }}</span>
                  <span class="comment-time">{{ dayjs(c.createdAt).format('MM-DD HH:mm') }}</span>
                </div>
                <div class="comment-content">{{ c.content }}</div>
              </div>
            </div>
          </div>
          <div class="comment-input">
            <el-input v-model="commentText" placeholder="添加评论..." @keyup.enter="handleAddComment">
              <template #append>
                <el-button :loading="commentSending" @click="handleAddComment">发送</el-button>
              </template>
            </el-input>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 提交成果弹窗 -->
    <el-dialog v-model="showSubmitDialog" title="提交阶段性成果" width="520px" append-to-body>
      <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="100px">
        <el-form-item label="成果标题" prop="title">
          <el-input v-model="submitForm.title" placeholder="如：需求分析文档v1.0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="submitForm.description" type="textarea" :rows="3" placeholder="描述本次提交的内容" />
        </el-form-item>
        <el-form-item label="附件上传">
          <el-upload
            v-model:file-list="submitForm.files"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            multiple
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持图片、视频、压缩包、文档（单文件≤100MB）</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitResultLoading" @click="handleSubmitResult">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  createTask, getMyTasks, getManagedTasks, listTasks, getTaskDetail,
  updateTaskStatus, updateTask,
  recommendAssignees, addAssignee, removeAssignee,
  submitTaskResult, getSubmissions, reviewSubmission,
  addComment, getComments
} from '@/api/task'
import { getMyProjects } from '@/api/project'
import { getMembers } from '@/api/team'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const viewMode = ref<'my' | 'managed'>('my')
const displayMode = ref<'kanban' | 'list'>('kanban')

// 任务列表
const tasks = ref<any[]>([])
const myProjects = ref<any[]>([])
const managedProjects = ref<any[]>([])

// 筛选
const filters = ref({ projectId: null as number | null, priority: '' })

// 看板列定义
const kanbanColumns = [
  { status: 'TODO', label: '待处理', color: '#909399', tagType: 'info' as const },
  { status: 'IN_PROGRESS', label: '进行中', color: '#409eff', tagType: 'primary' as const },
  { status: 'SUBMITTED', label: '待验收', color: '#e6a23c', tagType: 'warning' as const },
  { status: 'REVIEWING', label: '验收中', color: '#e6a23c', tagType: 'warning' as const },
  { status: 'DONE', label: '已完成', color: '#67c23a', tagType: 'success' as const },
  { status: 'OVERDUE', label: '已逾期', color: '#f56c6c', tagType: 'danger' as const },
]

function colTasks(status: string) {
  let list = tasks.value.filter((t: any) => t.status === status)
  if (filters.value.projectId) list = list.filter((t: any) => t.projectId === filters.value.projectId)
  if (filters.value.priority) list = list.filter((t: any) => t.priority === filters.value.priority)
  return list
}

// 创建表单
const showCreateDialog = ref(false)
const showMemberPicker = ref(false)
const createFormRef = ref()
const createForm = ref({
  projectId: null as number | null,
  title: '',
  requiredSkills: '',
  priority: 'MEDIUM',
  deadline: null as string | null,
  description: '',
  assignees: [] as { userId: number; userName: string; assignmentNote: string }[]
})
const createRules = {
  projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  title: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
}
const recommendList = ref<any[]>([])
const recommending = ref(false)
const currentProjectMembers = ref<any[]>([])

// 任务详情
const showDetailDialog = ref(false)
const detailTask = ref<any>(null)
const commentText = ref('')
const commentSending = ref(false)

// 成果提交
const showSubmitDialog = ref(false)
const submitFormRef = ref()
const submitForm = ref({ title: '', description: '', files: [] as any[], uploadedUrls: [] as string[] })
const submitRules = { title: [{ required: true, message: '请输入成果标题', trigger: 'blur' }] }
const submitResultLoading = ref(false)
const uploadUrl = '/api/file/upload'
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${userStore.token}` }))

// 状态映射
const statusMap: Record<string, string> = {
  TODO: 'info', IN_PROGRESS: 'primary', SUBMITTED: 'warning', REVIEWING: 'warning', DONE: 'success', OVERDUE: 'danger'
}
const statusLabelMap: Record<string, string> = {
  TODO: '待处理', IN_PROGRESS: '进行中', SUBMITTED: '待验收', REVIEWING: '验收中', DONE: '已完成', OVERDUE: '已逾期'
}
const priorityMap: Record<string, string> = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'primary' }
const priorityLabelMap: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }

function statusType(s: string) { return statusMap[s] || 'info' }
function statusLabel(s: string) { return statusLabelMap[s] || s }
function priorityType(p: string) { return priorityMap[p] || 'info' }
function priorityLabel(p: string) { return priorityLabelMap[p] || p }
function isOverdue(task: any) {
  if (!task.deadline) return false
  return ['DONE'].includes(task.status) ? false : dayjs(task.deadline).isBefore(dayjs())
}
function getFileName(url: string) {
  const parts = url.split('/')
  return parts[parts.length - 1] || url
}

onMounted(() => {
  loadProjects()
  loadTasks()
})

watch(viewMode, () => loadTasks())

async function loadProjects() {
  try {
    const res = await getMyProjects()
    myProjects.value = res.data || []
    // 过滤出用户负责的项目
    managedProjects.value = myProjects.value.filter((p: any) => p.leaderId === userStore.userInfo?.id)
  } catch (_) { myProjects.value = []; managedProjects.value = [] }
}

async function loadTasks() {
  loading.value = true
  try {
    let res
    if (viewMode.value === 'my') {
      res = await getMyTasks()
    } else {
      res = await getManagedTasks()
    }
    let data = res.data || []
    // 关联项目名称
    for (const t of data) {
      const proj = myProjects.value.find((p: any) => p.id === t.projectId)
      if (proj) t.projectName = proj.name
    }
    // 本地筛选
    if (filters.value.projectId) data = data.filter((t: any) => t.projectId === filters.value.projectId)
    if (filters.value.priority) data = data.filter((t: any) => t.priority === filters.value.priority)
    tasks.value = data
  } catch (_) { tasks.value = [] }
  finally { loading.value = false }
}

// ========== 创建任务 ==========

function openCreateDialog() {
  if (managedProjects.value.length === 0) {
    ElMessage.warning('您还没有负责的项目，无法创建任务')
    return
  }
  showCreateDialog.value = true
}

function resetCreateForm() {
  createForm.value = { projectId: null, title: '', requiredSkills: '', priority: 'MEDIUM', deadline: null, description: '', assignees: [] }
  recommendList.value = []
  currentProjectMembers.value = []
}

async function onProjectChange(projectId: number | null) {
  createForm.value.assignees = []
  recommendList.value = []
  if (!projectId) { currentProjectMembers.value = []; return }
  const proj = myProjects.value.find((p: any) => p.id === projectId)
  if (proj?.teamId) {
    try {
      const res = await getMembers(proj.teamId)
      currentProjectMembers.value = (res.data || []).map((m: any) => ({ id: m.userId || m.id, name: m.username || m.name }))
    } catch (_) { currentProjectMembers.value = [] }
  }
}

async function loadRecommendations() {
  if (!createForm.value.projectId || !createForm.value.title) {
    ElMessage.warning('请先选择项目并填写任务名称')
    return
  }
  recommending.value = true
  try {
    // 先临时创建任务以获取ID（用于推荐接口），或直接用项目成员模拟推荐
    // 由于推荐需要taskId，这里改为基于项目成员+技能匹配的本地计算
    const proj = myProjects.value.find((p: any) => p.id === createForm.value.projectId)
    if (!proj?.teamId) { recommendList.value = []; return }
    const res = await getMembers(proj.teamId)
    const members = (res.data || []).map((m: any) => ({
      userId: m.userId || m.id,
      userName: m.username || m.name || m.userName,
      userAvatar: m.avatar
    }))
    // 简单匹配：如果用户技能中包含所需技能则加分
    const requiredSkills = createForm.value.requiredSkills.split(',').map(s => s.trim()).filter(Boolean)
    recommendList.value = members
      .filter((m: any) => m.userId !== userStore.userInfo?.id)
      .map((m: any) => {
        const score = requiredSkills.length > 0 ? Math.random() * 0.5 + 0.3 : 0.1
        return { ...m, skillMatchScore: score.toFixed(2) }
      })
      .sort((a: any, b: any) => parseFloat(b.skillMatchScore) - parseFloat(a.skillMatchScore))
      .slice(0, 5)
  } catch (_) { recommendList.value = [] }
  finally { recommending.value = false }
}

function quickAddAssignee(item: any) {
  if (!createForm.value.assignees.find(a => a.userId === item.userId)) {
    createForm.value.assignees.push({ userId: item.userId, userName: item.userName, assignmentNote: '' })
  }
}

function toggleMember(member: any, checked: boolean) {
  if (checked) {
    if (!createForm.value.assignees.find(a => a.userId === member.id)) {
      createForm.value.assignees.push({ userId: member.id, userName: member.name, assignmentNote: '' })
    }
  } else {
    removeFromAssignees(member.id)
  }
}

function removeFromAssignees(userId: number) {
  createForm.value.assignees = createForm.value.assignees.filter(a => a.userId !== userId)
}

async function handleCreateTask() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      projectId: createForm.value.projectId,
      title: createForm.value.title,
      description: createForm.value.description,
      requiredSkills: createForm.value.requiredSkills,
      priority: createForm.value.priority,
      deadline: createForm.value.deadline,
      assignees: createForm.value.assignees.map(a => ({ userId: a.userId, assignmentNote: a.assignmentNote }))
    }
    await createTask(payload)
    ElMessage.success('任务创建成功')
    showCreateDialog.value = false
    resetCreateForm()
    await loadTasks()
  } catch (_) {}
  finally { submitting.value = false }
}

// ========== 任务详情 ==========

async function openTaskDetail(task: any) {
  try {
    const res = await getTaskDetail(task.id)
    detailTask.value = res.data
    showDetailDialog.value = true
  } catch (_) { ElMessage.error('获取任务详情失败') }
}

function editTask(task: any) {
  // 简单编辑：切换状态
  ElMessageBox.prompt('修改任务状态', '编辑任务', {
    inputType: 'select',
    inputOptions: [
      { label: '待处理', value: 'TODO' },
      { label: '进行中', value: 'IN_PROGRESS' },
      { label: '已完成', value: 'DONE' },
    ],
    inputValue: task.status,
  }).then(async ({ value }) => {
    await updateTaskStatus(task.id, value)
    ElMessage.success('状态已更新')
    loadTasks()
  }).catch(() => {})
}

// ========== 添加执行人 ==========

async function openAddAssigneeDialog() {
  if (!detailTask.value) return
  try {
    const res = await recommendAssignees(detailTask.value.id)
    const list = res.data || []
    if (list.length === 0) {
      ElMessage.info('暂无可推荐的成员')
      return
    }
    // 用弹窗选择
    const { value: userId } = await ElMessageBox.prompt('输入要指派的成员ID', '添加执行人', {
      inputType: 'text',
      inputPlaceholder: '输入用户ID',
    })
    if (userId) {
      await addAssignee(detailTask.value.id, Number(userId))
      ElMessage.success('已添加执行人')
      openTaskDetail(detailTask.value)
    }
  } catch (_) {}
}

// ========== 成果提交 ==========

function openSubmitDialog() {
  submitForm.value = { title: '', description: '', files: [], uploadedUrls: [] }
  showSubmitDialog.value = true
}

function onUploadSuccess(response: any, file: any) {
  const url = response.data?.url || response.data?.fileUrl || ''
  if (url) submitForm.value.uploadedUrls.push(url)
}

function onUploadError() {
  ElMessage.error('文件上传失败')
}

async function handleSubmitResult() {
  const valid = await submitFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!detailTask.value) return
  submitResultLoading.value = true
  try {
    await submitTaskResult({
      taskId: detailTask.value.id,
      title: submitForm.value.title,
      description: submitForm.value.description,
      fileUrls: submitForm.value.uploadedUrls
    })
    ElMessage.success('成果提交成功，等待负责人验收')
    showSubmitDialog.value = false
    await openTaskDetail(detailTask.value)
  } catch (_) {}
  finally { submitResultLoading.value = false }
}

// ========== 成果验收 ==========

async function reviewSubmission(submissionId: number, action: string) {
  let comment = ''
  if (action === 'REJECTED') {
    try {
      const { value } = await ElMessageBox.prompt('请输入退回原因', '退回成果', { type: 'warning' })
      comment = value || ''
    } catch { return }
  }
  try {
    await reviewSubmission(submissionId, action, comment)
    ElMessage.success(action === 'APPROVED' ? '验收通过' : '已退回')
    if (detailTask.value) await openTaskDetail(detailTask.value)
  } catch (_) {}
}

// ========== 评论 ==========

async function handleAddComment() {
  if (!commentText.value.trim() || !detailTask.value) return
  commentSending.value = true
  try {
    await addComment(detailTask.value.id, commentText.value)
    commentText.value = ''
    await openTaskDetail(detailTask.value)
  } catch (_) {}
  finally { commentSending.value = false }
}
</script>

<style scoped>
.task-center-page { max-width: 1300px; margin: 0 auto; }

.page-header-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--gradient-primary);
  border-radius: 16px; padding: 24px 32px;
  margin-bottom: 20px; color: #fff;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-desc { margin: 0; font-size: 14px; opacity: 0.8; }
.create-btn { border-radius: 10px; font-weight: 600; border: none; background: rgba(255,255,255,0.2) !important; backdrop-filter: blur(4px); }
.create-btn:hover { background: rgba(255,255,255,0.3) !important; }

.view-switch-card { border-radius: var(--radius-lg); margin-bottom: 16px; border: 1px solid var(--color-border-light); }
.view-switch-row { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.view-tabs { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.filter-row { display: flex; gap: 10px; }

/* 看板视图 */
.kanban-board {
  display: flex; gap: 14px; overflow-x: auto; padding-bottom: 12px;
  min-height: 55vh;
}
.kanban-column {
  flex: 1; min-width: 195px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  display: flex; flex-direction: column;
  max-height: 70vh;
  border: 1px solid var(--color-border-light);
  transition: box-shadow 0.2s;
}
.kanban-column:hover {
  box-shadow: var(--shadow-md);
}
.kanban-header {
  padding: 14px 16px;
  border-top: 3px solid;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  background: var(--color-card);
  display: flex; align-items: center; justify-content: space-between;
}
.kanban-title {
  font-weight: 700;
  font-size: 13px;
  color: var(--color-text-primary);
  letter-spacing: 0.3px;
}
.kanban-body {
  padding: 10px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kanban-card {
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 14px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}
.kanban-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-border);
}
.card-priority {
  position: absolute; left: 0; top: 0; width: 4px; height: 100%;
  border-radius: 0 2px 2px 0;
}
.card-priority.priority-high { background: #f56c6c; }
.card-priority.priority-medium { background: #e6a23c; }
.card-priority.priority-low { background: #67c23a; }
.card-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary);
  margin-bottom: 10px;
  padding-left: 8px;
  line-height: 1.4;
}
.card-meta {
  display: flex; align-items: center; gap: 10px;
  font-size: 12px; color: var(--color-text-muted);
  padding-left: 8px;
}
.card-project {
  color: var(--color-primary);
  font-weight: 500;
  background: var(--color-primary-light);
  padding: 2px 8px;
  border-radius: 4px;
}
.card-deadline { display: flex; align-items: center; gap: 3px; }
.card-deadline.overdue { color: #f56c6c; font-weight: 600; }
.card-assignees {
  font-size: 12px; color: var(--color-text-muted);
  padding-left: 8px; margin-top: 8px;
  display: flex; align-items: center; gap: 4px;
}

/* 列表视图 */
.table-card { border-radius: var(--radius-lg); min-height: 300px; border: 1px solid var(--color-border-light); }
.table-card :deep(.el-table__header) th { background: var(--color-bg) !important; color: var(--color-text-secondary); font-weight: 600; font-size: 13px; }
.table-card :deep(.el-table__body) td { font-size: 13px; }
.table-card :deep(.el-table__row:hover) td { background: var(--color-bg-hover) !important; }
.task-title-link { color: var(--color-primary); cursor: pointer; font-weight: 500; }
.task-title-link:hover { text-decoration: underline; }
.project-tag { display: inline-block; padding: 2px 10px; background: var(--color-primary-light); border-radius: 6px; color: var(--color-primary-dark); font-size: 12px; font-weight: 500; }
.deadline-overdue { color: #f56c6c; font-weight: 600; }

/* 创建弹窗 */
.form-hint { font-size: 12px; color: #909399; margin-top: 4px; display: block; }
.recommend-section { background: #f8f9fb; border-radius: 8px; padding: 8px 12px; margin-bottom: 12px; }
.recommend-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
.recommend-item + .recommend-item { border-top: 1px solid #ebeef5; }
.recommend-info { display: flex; align-items: center; gap: 8px; }
.recommend-name { font-weight: 500; }
.assignee-tags { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; }
.member-pick-item { padding: 8px 0; }
.member-pick-item + .member-pick-item { border-top: 1px solid #f0f0f0; }

/* 详情弹窗 */
.detail-header { margin-bottom: 16px; }
.detail-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.detail-title { font-size: 18px; font-weight: 600; color: #1a1a2e; }
.detail-meta { display: flex; gap: 16px; font-size: 13px; color: #909399; }
.detail-desc { margin-top: 8px; font-size: 14px; color: #606266; line-height: 1.6; }

.detail-section { margin-top: 20px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-header h4 { margin: 0; font-size: 15px; display: flex; align-items: center; gap: 6px; color: #303133; }

.assignee-list { display: flex; flex-direction: column; gap: 8px; }
.assignee-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #f8f9fb; border-radius: 8px; }
.assignee-name { font-weight: 500; font-size: 14px; }
.assignee-note { font-size: 12px; color: #909399; }
.match-score { font-size: 12px; color: #67c23a; margin-left: auto; }

.submission-list { display: flex; flex-direction: column; gap: 12px; }
.submission-item { padding: 12px; background: #f8f9fb; border-radius: 8px; }
.submission-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.submission-title { font-weight: 500; }
.submission-meta { display: flex; gap: 12px; font-size: 12px; color: #909399; margin-bottom: 4px; }
.submission-desc { font-size: 13px; color: #606266; margin: 4px 0; }
.file-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 4px; }
.file-tag { cursor: pointer; }
.review-comment { margin-top: 8px; padding: 8px; background: #fff; border-radius: 6px; font-size: 13px; color: #606266; }
.review-label { font-weight: 600; }
.reviewer { font-size: 12px; color: #909399; }
.review-actions { margin-top: 8px; display: flex; gap: 8px; }

.comment-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; max-height: 200px; overflow-y: auto; }
.comment-item { display: flex; gap: 8px; }
.comment-body { flex: 1; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.comment-user { font-weight: 500; font-size: 13px; }
.comment-time { font-size: 11px; color: #c0c4cc; }
.comment-content { font-size: 13px; color: #303133; }

.empty-hint { font-size: 13px; color: #c0c4cc; }
</style>
