<template>
  <div class="project-list-page">
    <div class="page-header-bar">
      <div>
        <h2>项目管理</h2>
        <p class="page-desc">管理你参与的所有项目</p>
      </div>
      <el-button type="primary" size="large" class="create-btn" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>创建项目
      </el-button>
    </div>

    <!-- 骨架屏加载 -->
    <div v-if="loading" class="project-grid">
      <div v-for="n in 3" :key="'sk-' + n" class="skeleton-card">
        <div class="card-top">
          <div class="skeleton-avatar skeleton-pulse"></div>
          <div class="skeleton-info">
            <div class="skeleton-line skeleton-pulse" style="width: 70%"></div>
            <div class="skeleton-line skeleton-pulse" style="width: 40%; margin-top: 6px"></div>
          </div>
        </div>
        <div class="skeleton-body">
          <div class="skeleton-line skeleton-pulse" style="width: 55%"></div>
          <div class="skeleton-line skeleton-pulse" style="width: 45%; margin-top: 8px"></div>
        </div>
        <div class="skeleton-footer">
          <div class="skeleton-line skeleton-pulse" style="width: 30%"></div>
          <div class="skeleton-line skeleton-pulse" style="width: 30%"></div>
        </div>
      </div>
    </div>

    <el-empty v-else-if="projects.length === 0" description="暂无项目" />
    <div v-else class="project-grid">
      <el-card
        v-for="item in projects"
        :key="item.id"
        class="project-card"
        shadow="hover"
        @click="router.push(`/project/${item.id}`)"
      >
        <div class="card-top">
          <div class="project-avatar" :style="projectAvatarStyle(item)">
            {{ item.name?.charAt(0) || 'P' }}
          </div>
          <div class="project-info">
            <div class="project-name">{{ item.name }}</div>
            <el-tag :type="projectStatusType(item.status)" size="small" effect="plain">
              {{ projectStatusLabel(item.status) }}
            </el-tag>
          </div>
        </div>
        <div class="card-body">
          <div class="card-stat-row">
            <span :title="item.leaderName ? '队长: ' + item.leaderName : ''">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-3-3.87"/><path d="M4 21v-2a4 4 0 0 1 3-3.87"/><circle cx="12" cy="7" r="4"/></svg>
              {{ item.leaderName || '未指定' }}
            </span>
            <span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              {{ dayjs(item.createdAt).format('YYYY-MM-DD') }}
            </span>
          </div>
        </div>
        <div class="card-footer">
          <div class="footer-stat">
            <span class="footer-num">{{ item.taskCount ?? 0 }}</span>
            <span class="footer-lbl">任务</span>
          </div>
          <div class="footer-stat">
            <span class="footer-num">{{ item.memberCount ?? 0 }}</span>
            <span class="footer-lbl">成员</span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Create Project Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建项目" width="520px" class="create-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目描述" />
        </el-form-item>
        <el-form-item label="所属团队">
          <el-select v-model="form.teamId" placeholder="不选则自动创建团队" filterable clearable>
            <el-option v-for="t in userTeams" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <span class="form-hint">不选择则自动创建同名团队，创建者自动成为队长</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate" class="dialog-btn">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getMyProjects, createProject } from '@/api/project'
import { getMyTeams } from '@/api/team'

const router = useRouter()

const loading = ref(false)
const projects = ref<any[]>([])
const showCreateDialog = ref(false)
const submitting = ref(false)
const userTeams = ref<any[]>([])

const form = ref({ name: '', description: '', teamId: null as number | null })
const rules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
}

const avatarColors = [
  'linear-gradient(135deg,#667eea,#764ba2)',
  'linear-gradient(135deg,#43e97b,#38f9d7)',
  'linear-gradient(135deg,#fa709a,#fee140)',
  'linear-gradient(135deg,#a18cd1,#fbc2eb)',
  'linear-gradient(135deg,#f093fb,#f5576c)',
]

function projectStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'PLANNING': case 'IN_PROGRESS': return 'success'
    case 'REVIEW': return 'warning'
    case 'COMPLETED': return 'info'
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

function projectAvatarStyle(item: any) {
  const idx = (item.id || 0) % avatarColors.length
  return { background: avatarColors[idx] }
}

onMounted(() => { Promise.all([loadProjects(), loadTeams()]) })

async function loadProjects() {
  loading.value = true
  try { const res = await getMyProjects(); projects.value = res.data || [] }
  catch (_) { projects.value = [] }
  finally { loading.value = false }
}

async function loadTeams() {
  try { const res = await getMyTeams(); userTeams.value = res.data || [] }
  catch (_) { userTeams.value = [] }
}

async function handleCreate() {
  submitting.value = true
  try {
    await createProject(form.value)
    ElMessage.success('项目创建成功')
    showCreateDialog.value = false
    form.value = { name: '', description: '', teamId: null }
    await loadProjects()
  } catch (_) {} finally { submitting.value = false }
}
</script>

<style scoped>
.project-list-page { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: 16px; padding: 24px 32px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}
.page-header-bar::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--gradient-primary);
  border-radius: 4px 0 0 4px;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; color: var(--color-text-primary); }
.page-desc { margin: 0; font-size: 14px; color: var(--color-text-muted); }

.create-btn {
  border-radius: 10px; font-weight: 600;
  border: none; background: rgba(255,255,255,0.2) !important;
  backdrop-filter: blur(4px);
}
.create-btn:hover { background: rgba(255,255,255,0.3) !important; transform: translateY(-1px); }

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.project-card {
  cursor: pointer; transition: all var(--transition-base);
  border-radius: var(--radius-lg); border: 1px solid var(--color-border-light);
}
.project-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); border-color: var(--color-border); }

.card-top { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }

.project-avatar {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}

.project-info { flex: 1; min-width: 0; }
.project-name {
  font-size: 15px; font-weight: 600; color: var(--color-text-primary);
  margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.card-body { margin-bottom: 12px; }

.card-stat-row { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--color-text-muted); }
.card-stat-row span { display: flex; align-items: center; gap: 4px; }

.card-footer {
  display: flex; gap: 24px;
  padding-top: 12px; border-top: 1px solid var(--color-border-light);
}

.footer-stat { display: flex; flex-direction: column; align-items: center; }
.footer-num { font-size: 16px; font-weight: 700; color: var(--color-text-primary); }
.footer-lbl { font-size: 11px; color: var(--color-text-muted); margin-top: 2px; }

.create-dialog :deep(.el-dialog__body) { padding: 24px 32px; }
.dialog-btn { border-radius: var(--radius-md); }
.form-hint { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; display: block; }

/* ========== 骨架屏 ========== */
.skeleton-card {
  background: var(--color-card); border-radius: var(--radius-lg); padding: 20px;
  box-shadow: var(--shadow-sm);
  display: flex; flex-direction: column; gap: 14px;
}
.skeleton-avatar {
  width: 44px; height: 44px; border-radius: 10px; flex-shrink: 0;
}
.skeleton-info { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.skeleton-body { display: flex; flex-direction: column; }
.skeleton-footer {
  display: flex; gap: 24px;
  padding-top: 12px; border-top: 1px solid var(--color-border-light);
}
.skeleton-line { height: 14px; border-radius: 4px; }
.skeleton-pulse {
  background: linear-gradient(90deg, var(--color-bg) 25%, var(--color-border-light) 50%, var(--color-bg) 75%);
  background-size: 200% 100%;
  animation: skeletonShimmer 1.5s infinite ease-in-out;
}
@keyframes skeletonShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
