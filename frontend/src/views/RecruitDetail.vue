<template>
  <div class="recruit-detail-container" v-loading="loading">
    <template v-if="recruit">
      <!-- Back -->
      <div class="back-nav">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回招募列表
        </el-button>
      </div>

      <el-row :gutter="24">
        <!-- Main Info -->
        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="main-card">
            <div class="detail-header">
              <div class="detail-header-left">
                <h1 class="detail-title">{{ recruit.title }}</h1>
                <div class="header-tags">
                  <el-tag :type="statusType(recruit.status)" effect="dark" size="small">{{ statusLabel(recruit.status) }}</el-tag>
                  <el-tag :type="projectTypeType(recruit.projectType)" effect="plain" size="small">{{ recruit.projectType }}</el-tag>
                </div>
              </div>
            </div>

            <!-- Meta Grid -->
            <div class="meta-grid">
              <div class="meta-item">
                <div class="meta-label">发布人</div>
                <div class="meta-value">
                  <el-avatar :size="24" :src="recruit.publisherAvatar" style="vertical-align:middle;margin-right:6px;background:var(--gradient-primary)">
                    {{ recruit.publisherName?.charAt(0) || '?' }}
                  </el-avatar>
                  {{ recruit.publisherName || '未知' }}
                </div>
              </div>
              <div class="meta-item">
                <div class="meta-label">截止日期</div>
                <div class="meta-value">{{ recruit.deadline ? dayjs(recruit.deadline).format('YYYY-MM-DD') : '无' }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">发布时间</div>
                <div class="meta-value">{{ dayjs(recruit.createdAt).format('YYYY-MM-DD HH:mm') }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">浏览次数</div>
                <div class="meta-value">{{ recruit.viewCount || 0 }} 次</div>
              </div>
            </div>

            <!-- Member Progress -->
            <div class="detail-section">
              <h3 class="section-title">成员进度</h3>
              <div class="member-progress-bar">
                <div class="progress-header">
                  <span>{{ recruit.currentMembers || 0 }} / {{ recruit.maxMembers }} 人</span>
                  <span :class="memberStatusClass">{{ memberStatusText }}</span>
                </div>
                <el-progress
                  :percentage="memberPercent"
                  :stroke-width="10"
                  :status="memberPercent >= 100 ? 'exception' : ''"
                  :format="() => ''"
                />
              </div>
            </div>

            <!-- Skills -->
            <div class="detail-section" v-if="recruit.requiredSkills?.length">
              <h3 class="section-title">所需技能</h3>
              <div class="skills-wrapper">
                <el-tag v-for="skill in recruit.requiredSkills" :key="skill" type="info" class="skill-tag">
                  {{ skill }}
                </el-tag>
              </div>
            </div>

            <!-- Description -->
            <div class="detail-section">
              <h3 class="section-title">项目描述</h3>
              <div class="detail-description">{{ recruit.description }}</div>
            </div>

            <!-- Apply -->
            <div class="detail-actions" v-if="recruit.status === 'OPEN'">
              <el-button
                type="primary"
                size="large"
                class="apply-btn"
                :loading="applying"
                :disabled="recruit.currentMembers >= recruit.maxMembers"
                @click="handleApply"
              >
                <el-icon><Plus /></el-icon>
                {{ recruit.currentMembers >= recruit.maxMembers ? '已满员' : '申请加入' }}
              </el-button>
              <p class="apply-hint" v-if="recruit.currentMembers < recruit.maxMembers">提交申请后，等待发布人审核</p>
            </div>
            <div class="detail-actions closed-actions" v-else>
              <el-tag type="info" size="large" effect="plain">该招募已{{ statusLabel(recruit.status) }}</el-tag>
            </div>
          </el-card>
        </el-col>

        <!-- Sidebar -->
        <el-col :xs="24" :lg="8">
          <!-- 申请管理（仅发布者可见） -->
          <el-card shadow="never" class="sidebar-card" v-if="isPublisher && applications.length > 0">
            <template #header>
              <div class="sidebar-header">
                <div class="sidebar-header-left">
                  <div class="sidebar-icon" style="background:linear-gradient(135deg,#fa8c16,#fa709a)">
                    <el-icon :size="16" color="#fff"><List /></el-icon>
                  </div>
                  <span>申请管理 ({{ applications.length }})</span>
                </div>
              </div>
            </template>
            <div class="application-list">
              <div v-for="app in applications" :key="app.id" class="application-item">
                <div class="app-user">
                  <el-avatar :size="32" class="app-avatar" :src="app.applicantAvatar">{{ app.applicantName?.charAt(0) || '?' }}</el-avatar>
                  <span class="app-name">{{ app.applicantName || '未知用户' }}</span>
                </div>
                <div class="app-actions" v-if="app.status === 'PENDING'">
                  <el-button size="small" type="success" @click="handleApproveApp(app)">通过</el-button>
                  <el-button size="small" type="danger" @click="handleRejectApp(app)">拒绝</el-button>
                </div>
                <div v-else class="app-status">
                  <el-tag :type="app.status === 'ACCEPTED' ? 'success' : 'danger'" size="small">
                    {{ app.status === 'ACCEPTED' ? '已通过' : '已拒绝' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-card>

          <!-- 转为项目（发布者/管理员可见，只要未转项目都显示） -->
          <el-card shadow="never" class="sidebar-card" style="margin-top:16px"
                   v-if="isPublisher && !recruit.projectId">
            <template #header>
              <div class="sidebar-header">
                <div class="sidebar-header-left">
                  <div class="sidebar-icon" style="background:linear-gradient(135deg,#52c41a,#237804)">
                    <el-icon :size="16" color="#fff"><Switch /></el-icon>
                  </div>
                  <span>项目转换</span>
                </div>
              </div>
            </template>
            <div class="convert-section">
              <p class="convert-desc">将当前招募转为正式项目，自动创建团队并将已通过的成员加入团队。</p>
              <el-button type="primary" class="convert-btn" @click="showConvertDialog = true">
                <el-icon><Switch /></el-icon> 转为项目
              </el-button>
            </div>
          </el-card>

          <!-- 已转为项目提示 -->
          <el-card shadow="never" class="sidebar-card" style="margin-top:16px"
                   v-if="recruit.projectId">
            <template #header>
              <div class="sidebar-header">
                <div class="sidebar-header-left">
                  <div class="sidebar-icon" style="background:linear-gradient(135deg,#52c41a,#237804)">
                    <el-icon :size="16" color="#fff"><CircleCheckFilled /></el-icon>
                  </div>
                  <span>已转为项目</span>
                </div>
              </div>
            </template>
            <div class="converted-info">
              <p>该招募已成功转为项目，项目ID: {{ recruit.projectId }}</p>
              <p class="recruit-status-tip">
                <el-tag :type="recruit.continueRecruit === 1 ? 'success' : 'info'" size="small">
                  {{ recruit.continueRecruit === 1 ? '继续招募中' : '招募已停止' }}
                </el-tag>
                <el-button
                  v-if="canToggleContinueRecruit"
                  type="warning" plain size="small" style="margin-left:8px"
                  :loading="togglingContinue"
                  @click="handleToggleContinueRecruit"
                >
                  {{ recruit.continueRecruit === 1 ? '停止招募' : '开启招募' }}
                </el-button>
              </p>
              <el-button type="primary" plain size="small" @click="router.push('/project/' + recruit.projectId)">
                查看项目
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="sidebar-card" style="margin-top:16px">
            <template #header>
              <div class="sidebar-header">
                <div class="sidebar-header-left">
                  <div class="sidebar-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)">
                    <el-icon :size="16" color="#fff"><Lightning /></el-icon>
                  </div>
                  <span>智能匹配</span>
                </div>
                <el-button text type="primary" size="small" :loading="matchingLoading" @click="loadMatches">
                  刷新
                </el-button>
              </div>
            </template>

            <div v-if="matchingLoading" class="match-loading">
              <el-icon class="is-loading" :size="28" color="#667eea"><Loading /></el-icon>
              <p>正在智能匹配...</p>
            </div>

            <div v-else-if="matchResults.length === 0" class="match-empty">
              <el-icon :size="40" color="#d0d5dd"><UserFilled /></el-icon>
              <p>暂无匹配结果</p>
            </div>

            <div v-else class="match-list">
              <div v-for="item in matchResults" :key="item.userId || item.id" class="match-card">
                <div class="match-user">
                  <el-avatar :size="40" class="match-avatar" :src="item.avatar">{{ item.username?.charAt(0) || '?' }}</el-avatar>
                  <div class="match-user-info">
                    <span class="match-username">{{ item.username || '未知' }}</span>
                    <span class="match-desc">{{ item.major || '' }}</span>
                  </div>
                </div>
                <div class="match-score-circle">
                  <svg viewBox="0 0 36 36" class="score-svg">
                    <path class="score-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                    <path class="score-fill" :stroke-dasharray="`${Math.round((item.similarity || 0) * 100)}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                    <text x="18" y="21" class="score-text">{{ Math.round((item.similarity || 0) * 100) }}%</text>
                  </svg>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-else-if="!loading" description="招募信息不存在" />

    <!-- 转为项目弹窗 -->
    <el-dialog
      v-model="showConvertDialog"
      title="招募转项目"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="convertForm" label-width="110px" label-position="top">
        <el-form-item label="项目名称">
          <el-input v-model="convertForm.projectName" :placeholder="recruit?.title || '请输入项目名称'" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="convertForm.projectDescription" type="textarea" :rows="3"
                    :placeholder="recruit?.description || '请输入项目描述'" />
        </el-form-item>
        <el-form-item label="是否继续招募">
          <el-radio-group v-model="convertForm.continueRecruit">
            <el-radio :value="1">继续招募</el-radio>
            <el-radio :value="0">停止招募</el-radio>
          </el-radio-group>
          <div class="form-tip">
            <template v-if="convertForm.continueRecruit === 1">
              继续招募：用户仍可申请，通过后自动加入项目团队
            </template>
            <template v-else>
              停止招募：关闭招募通道，项目团队固定
            </template>
          </div>
        </el-form-item>

        <el-divider content-position="left">
          <span style="font-size:14px;font-weight:600">为团队成员创建任务（可选）</span>
        </el-divider>

        <div class="task-list">
          <div v-for="(task, idx) in convertForm.tasks" :key="idx" class="task-item">
            <div class="task-item-header">
              <span class="task-index">任务 {{ idx + 1 }}</span>
              <el-button text type="danger" size="small" @click="removeTask(idx)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-form-item label="任务标题" style="margin-bottom:8px">
              <el-input v-model="task.title" placeholder="请输入任务标题" />
            </el-form-item>
            <el-form-item label="任务描述" style="margin-bottom:8px">
              <el-input v-model="task.description" type="textarea" :rows="2" placeholder="任务描述（可选）" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="优先级" style="margin-bottom:0">
                  <el-select v-model="task.priority" placeholder="选择优先级" style="width:100%">
                    <el-option label="低" value="LOW" />
                    <el-option label="中" value="MEDIUM" />
                    <el-option label="高" value="HIGH" />
                    <el-option label="紧急" value="URGENT" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="截止日期" style="margin-bottom:0">
                  <el-date-picker v-model="task.deadline" type="datetime" placeholder="选择截止日期"
                                  style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>
        <el-button type="primary" plain dashed style="width:100%;margin-top:8px" @click="addTask">
          <el-icon><Plus /></el-icon> 添加任务
        </el-button>
      </el-form>

      <template #footer>
        <el-button @click="showConvertDialog = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="handleConvertToProject">
          确认转换
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getRecruit, applyRecruit, matchRecruit, getApplications, approveApplication, convertRecruitToProject, toggleContinueRecruit } from '@/api/recruit'

const route = useRoute()
const router = useRouter()

/** 校验并获取路由ID，无效时返回 NaN（调用方需自行处理） */
function getRouteId(): number {
  const id = Number(route.params.id)
  if (isNaN(id) || id <= 0) {
    ElMessage.error('无效的招新ID')
    router.replace('/recruit')
  }
  return id
}

const loading = ref(false)
const recruit = ref<any>(null)
const applying = ref(false)
const matchingLoading = ref(false)
const matchResults = ref<any[]>([])
const applications = ref<any[]>([])
const isPublisher = computed(() => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const currentUserId = Number(userInfo.id || '0')
  const isAdmin = userInfo.role === 'ADMIN'
  return recruit.value && currentUserId > 0 && (recruit.value.publisherId === currentUserId || isAdmin)
})

const memberPercent = computed(() => {
  if (!recruit.value) return 0
  return Math.min(Math.round(((recruit.value.currentMembers || 0) / (recruit.value.maxMembers || 1)) * 100), 100)
})

const memberStatusText = computed(() => {
  if (!recruit.value) return ''
  if (recruit.value.currentMembers >= recruit.value.maxMembers) return '已满员'
  return `剩余 ${recruit.value.maxMembers - recruit.value.currentMembers} 个名额`
})

const memberStatusClass = computed(() => {
  if (!recruit.value) return ''
  return recruit.value.currentMembers >= recruit.value.maxMembers ? 'status-full' : 'status-open'
})

// 是否允许切换继续招募状态（已转化项目 + 发布者或管理员）
const canToggleContinueRecruit = computed(() => {
  if (!recruit.value) return false
  const hasProject = recruit.value.projectId != null
  if (!hasProject) return false
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return recruit.value.publisherId === Number(userInfo.id || '0') || userInfo.role === 'ADMIN'
})

const togglingContinue = ref(false)

async function handleToggleContinueRecruit() {
  const newStatus = recruit.value.continueRecruit === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '开启招募' : '停止招募'
  try {
    await ElMessageBox.confirm(`确认${actionText}？`, '操作确认', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning',
    })
  } catch { return }

  togglingContinue.value = true
  try {
    const id = getRouteId()
    if (isNaN(id)) return
    await toggleContinueRecruit(id, newStatus)
    recruit.value.continueRecruit = newStatus
    recruit.value.status = newStatus === 0 ? 'CLOSED' : 'OPEN'
    ElMessage.success(`已${actionText}`)
  } catch (_) {
    // 错误已在拦截器中处理
  } finally {
    togglingContinue.value = false
  }
}

async function fetchDetail() {
  loading.value = true
  const id = getRouteId()
  if (isNaN(id)) { loading.value = false; return }
  try {
    const res = await getRecruit(id)
    recruit.value = res.data
  } catch (_) {
    recruit.value = null
  } finally {
    loading.value = false
  }
}

async function handleApply() {
  try {
    await ElMessageBox.confirm('确认申请加入该项目？', '申请确认', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'info',
    })
  } catch { return }

  applying.value = true
  try {
    const id = getRouteId()
    if (isNaN(id)) return
    await applyRecruit(id)
    ElMessage.success('申请已提交，请等待审核')
  } catch (_) {} finally {
    applying.value = false
  }
}

async function loadApplications() {
  const id = getRouteId()
  if (isNaN(id)) return
  try {
    const res = await getApplications(id)
    const data = res.data || []
    // 加载申请人名称
    applications.value = data.map((app: any) => ({
      ...app,
      applicantName: app.applicantName || '用户' + app.applicantId
    }))
  } catch (_) {
    applications.value = []
  }
}

async function handleApproveApp(app: any) {
  try {
    await approveApplication(app.id, true)
    ElMessage.success('已通过申请')
    await loadApplications()
    await fetchDetail()
  } catch (_) {}
}

async function handleRejectApp(app: any) {
  try {
    await approveApplication(app.id, false)
    ElMessage.success('已拒绝申请')
    await loadApplications()
  } catch (_) {}
}

async function loadMatches() {
  const id = getRouteId()
  if (isNaN(id)) return
  matchingLoading.value = true
  try {
    const res = await matchRecruit(id)
    matchResults.value = Array.isArray(res.data) ? res.data : []
  } catch (_) {
    matchResults.value = []
  } finally {
    matchingLoading.value = false
  }
}

function statusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'OPEN': return 'success'
    case 'CLOSED': return 'info'
    case 'FULL': return 'warning'
    default: return 'info'
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'OPEN': return '招募中'
    case 'CLOSED': return '已关闭'
    case 'FULL': return '已满员'
    default: return status || '未知'
  }
}

function projectTypeType(type: string): 'success' | 'warning' | 'primary' {
  switch (type) {
    case '竞赛': return 'warning'
    case '科研': return 'primary'
    case '创新创业': return 'success'
    default: return 'primary'
  }
}

// ---------- 招募转项目 ----------
const showConvertDialog = ref(false)
const converting = ref(false)
const convertForm = reactive({
  projectName: '',
  projectDescription: '',
  continueRecruit: 1 as number,
  tasks: [] as Array<{
    title: string
    description: string
    priority: string
    deadline: string
  }>
})

function addTask() {
  convertForm.tasks.push({
    title: '',
    description: '',
    priority: 'MEDIUM',
    deadline: ''
  })
}

function removeTask(index: number) {
  convertForm.tasks.splice(index, 1)
}

async function handleConvertToProject() {
  // 验证：如果有任务，至少标题不为空
  const validTasks = convertForm.tasks.filter(t => t.title.trim())
  if (convertForm.tasks.length > 0 && validTasks.length !== convertForm.tasks.length) {
    ElMessage.warning('请填写所有任务的标题，或移除空任务')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认将当前招募转为正式项目？系统将自动创建团队并将已通过的成员加入团队。',
      '确认转换',
      { confirmButtonText: '确认转换', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }

  converting.value = true
  try {
    const id = getRouteId()
    if (isNaN(id)) return
    const payload = {
      projectName: convertForm.projectName || undefined,
      projectDescription: convertForm.projectDescription || undefined,
      continueRecruit: convertForm.continueRecruit,
      tasks: validTasks.length > 0 ? validTasks : undefined
    }
    await convertRecruitToProject(id, payload)
    ElMessage.success('招募已成功转为项目！')
    showConvertDialog.value = false
    await fetchDetail()
    await loadApplications()
  } catch (_) {
    // error handled by interceptor
  } finally {
    converting.value = false
  }
}

onMounted(async () => {
  await fetchDetail()
  // 并行加载申请列表和匹配列表
  await Promise.all([loadApplications(), loadMatches()])
})
</script>

<style scoped>
.recruit-detail-container { max-width: 1100px; margin: 0 auto; }

.back-nav { margin-bottom: 16px; }

/* ===== Main Card ===== */
.main-card {
  border-radius: 14px;
  margin-bottom: 20px;
}

.detail-header { margin-bottom: 20px; }

.detail-header-left { display: flex; flex-direction: column; gap: 8px; }

.detail-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.header-tags { display: flex; gap: 8px; }

/* ===== Meta Grid ===== */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px;
  background: #f8f9fb;
  border-radius: 10px;
  margin-bottom: 24px;
}

.meta-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.meta-value {
  font-size: 14px;
  color: #1a1a2e;
  font-weight: 500;
}

/* ===== Sections ===== */
.detail-section { margin-bottom: 24px; }

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 12px;
  padding-left: 12px;
  border-left: 3px solid var(--color-primary);
}

.member-progress-bar {
  padding: 4px 0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.status-full { color: #f56c6c; font-weight: 600; }
.status-open { color: #67c23a; }

.skills-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag { margin: 0 !important; }

.detail-description {
  font-size: 14px;
  color: #4a4a6a;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ===== Apply ===== */
.detail-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e8ecf1;
}

.apply-btn {
  border-radius: 10px;
  font-weight: 600;
  height: 46px;
  padding: 0 32px;
  font-size: 16px;
  background: var(--gradient-primary);
  border: none;
}
.apply-btn:hover {
  box-shadow: 0 4px 20px rgba(102,126,234,0.4);
  transform: translateY(-1px);
}

.apply-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}

.closed-actions { text-align: center; }

/* ===== Sidebar ===== */
.sidebar-card {
  border-radius: 14px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.sidebar-icon {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.match-loading, .match-empty {
  text-align: center;
  padding: 32px 0;
  color: #909399;
}
.match-loading p, .match-empty p { margin-top: 8px; font-size: 13px; }

.match-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.match-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #f0f2f5;
  border-radius: 10px;
  transition: all 0.2s;
}
.match-card:hover {
  border-color: #e0e4ed;
  background: #f8f9fb;
}

.match-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.match-avatar {
  background: var(--gradient-primary) !important;
  flex-shrink: 0;
}

.match-user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.match-username { font-size: 14px; font-weight: 500; color: #1a1a2e; }
.match-desc { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.match-score-circle { margin-left: 12px; flex-shrink: 0; }

.score-svg { width: 44px; height: 44px; transform: rotate(-90deg); }
.score-bg { fill: none; stroke: #f0f2f5; stroke-width: 3; }
.score-fill { fill: none; stroke: var(--color-primary); stroke-width: 3; stroke-linecap: round; }
.score-text { transform: rotate(90deg); transform-origin: 18px 18px; fill: #667eea; font-size: 8px; font-weight: 700; text-anchor: middle; }

/* ===== Convert Section ===== */
.convert-section {
  text-align: center;
  padding: 4px 0;
}
.convert-desc {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px;
  line-height: 1.6;
}
.convert-btn {
  width: 100%;
  border-radius: 10px;
  font-weight: 600;
  height: 42px;
}

.converted-info {
  text-align: center;
}
.converted-info p {
  font-size: 13px;
  color: #606266;
  margin: 0 0 8px;
}
.recruit-status-tip {
  margin-bottom: 12px !important;
}

/* ===== Convert Dialog ===== */
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.task-item {
  padding: 12px;
  border: 1px solid #e8ecf1;
  border-radius: 10px;
  background: #fafbfc;
}
.task-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.task-index {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
}

/* ===== Application List ===== */
.application-list { display: flex; flex-direction: column; gap: 8px; }
.application-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f5f5f5; }
.application-item:last-child { border-bottom: none; }
.app-user { display: flex; align-items: center; gap: 8px; }
.app-avatar { background: var(--gradient-primary) !important; }
.app-name { font-size: 13px; color: #1a1a2e; }
.app-actions { display: flex; gap: 6px; }
</style>
