<template>
  <div class="teacher-list-page">
    <div class="page-header-bar">
      <div>
        <h2>导师库</h2>
        <p class="page-desc">浏览各院系导师，寻找适合的竞赛指导</p>
      </div>
      <el-button v-if="inviteTargetTeam" @click="router.back()" style="color:#fff" text>
        <el-icon><ArrowLeft /></el-icon> 返回团队
      </el-button>
    </div>

    <!-- Search -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" size="default">
        <el-form-item label="所属院系">
          <el-select v-model="department" placeholder="全部院系" clearable filterable style="width:200px">
            <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTeachers">搜索</el-button>
          <el-button @click="department = ''; loadTeachers()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Teacher Cards -->
    <div v-loading="loading">
      <el-empty v-if="!loading && teachers.length === 0" description="暂无导师信息" />
      <div v-else class="teacher-grid">
        <el-card v-for="item in teachers" :key="item.id" class="teacher-card" shadow="hover">
          <div class="card-top">
            <el-avatar :size="52" class="teacher-avatar">{{ item.name?.charAt(0) || '?' }}</el-avatar>
            <div class="teacher-meta">
              <span class="teacher-name">{{ item.name }}</span>
              <span class="teacher-dept">{{ item.department }}</span>
            </div>
          </div>
          <div class="card-title">
            <el-tag size="small" type="info">{{ item.title || '--' }}</el-tag>
          </div>
          <div class="card-fields">
            <el-tag v-for="f in parseResearchFields(item.researchFields)" :key="f" size="small" type="success" effect="plain" class="field-tag">{{ f }}</el-tag>
            <span v-if="parseResearchFields(item.researchFields).length === 0" class="no-data">暂无研究方向</span>
          </div>
          <div class="card-rating">
            <el-rate v-if="item.rating" :model-value="item.rating" disabled score-template="{value}分" show-score size="small" />
            <span v-else class="no-data">暂无评分</span>
          </div>
          <div class="card-teams">
            <div class="team-info">
              <span>当前带团队：{{ item.currentTeams || 0 }} / {{ item.maxTeams || 5 }}</span>
            </div>
            <el-progress :percentage="maxTeamPercent(item)" :stroke-width="6" size="small" />
          </div>
          <div class="card-actions" v-if="inviteTargetTeam">
            <el-button type="primary" size="small" @click.stop="openInviteDialog(item)">
              邀请导师 <el-icon style="margin-left:4px"><Reading /></el-icon>
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 邀请导师对话框 -->
    <el-dialog v-model="inviteDialogVisible" title="邀请导师" width="480px" :close-on-click-modal="false">
      <div class="invite-dialog-content">
        <div class="invite-preview">
          <el-avatar :size="48" class="invite-avatar">{{ selectedTeacher?.name?.charAt(0) || '?' }}</el-avatar>
          <div>
            <div class="invite-name">{{ selectedTeacher?.name }}</div>
            <div class="invite-dept">{{ selectedTeacher?.department }} · {{ selectedTeacher?.title }}</div>
          </div>
        </div>
        <el-form label-position="top" class="invite-form">
          <el-form-item label="邀请留言">
            <el-input
              v-model="inviteMessage"
              type="textarea"
              :rows="3"
              placeholder="向导师介绍你的团队和项目，留下联系方式等…"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="inviting" @click="doInvite">确认邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listTeachers, inviteTeacher } from '@/api/teacher'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const teachers = ref<any[]>([])
const department = ref('')
const departments = ref<string[]>([])

const inviteTargetTeam = ref<string | null>(null)

// 邀请对话框
const inviteDialogVisible = ref(false)
const selectedTeacher = ref<any>(null)
const inviteMessage = ref('')
const inviting = ref(false)

onMounted(() => {
  inviteTargetTeam.value = route.query.teamId as string || null
  loadTeachers()
})

async function loadTeachers() {
  loading.value = true
  try {
    const params: any = {}
    if (department.value) params.department = department.value
    const res = await listTeachers(params)
    const data = res.data || []
    teachers.value = data
    // 动态提取院系列表
    if (departments.value.length === 0 && data.length > 0) {
      const deptSet = new Set(data.map((t: any) => t.department).filter(Boolean))
      departments.value = Array.from(deptSet) as string[]
    }
  } catch (_) { teachers.value = [] }
  finally { loading.value = false }
}

function openInviteDialog(teacher: any) {
  selectedTeacher.value = teacher
  inviteMessage.value = ''
  inviteDialogVisible.value = true
}

async function doInvite() {
  if (!selectedTeacher.value || !inviteTargetTeam.value) return
  const teamId = Number(inviteTargetTeam.value)
  if (isNaN(teamId) || teamId <= 0) {
    ElMessage.error('无效的团队ID')
    return
  }
  inviting.value = true
  try {
    await inviteTeacher(teamId, selectedTeacher.value.id, inviteMessage.value)
    ElMessage.success('已向导师发送邀请')
    inviteDialogVisible.value = false
  } catch (_) { /* error handled by request interceptor */ }
  finally { inviting.value = false }
}

function maxTeamPercent(item: any) { const max = item.maxTeams || 5; if (max === 0) return 0; return Math.round(((item.currentTeams || 0) / max) * 100) }

/** 解析研究方向字段：支持 JSON数组字符串 / 逗号分隔字符串 / 数组 */
function parseResearchFields(val: any): string[] {
  if (!val) return []
  if (Array.isArray(val)) return val
  if (typeof val !== 'string') return []
  const trimmed = val.trim()
  // 尝试 JSON 解析
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed)
      return Array.isArray(parsed) ? parsed : [trimmed]
    } catch {}
  }
  // 逗号分隔
  return trimmed.split(/[,，]/).map(s => s.trim()).filter(Boolean)
}
</script>

<style scoped>
.teacher-list-page { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
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

.filter-card { border-radius: var(--radius-lg); margin-bottom: 16px; border: 1px solid var(--color-border-light); }

.teacher-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }

.teacher-card { transition: all var(--transition-base); border-radius: var(--radius-lg); border: 1px solid var(--color-border-light); }
.teacher-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); border-color: var(--color-border); }

.card-top { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }

.teacher-avatar { background: var(--gradient-primary) !important; font-size: 20px; font-weight: 600; color: #fff; flex-shrink: 0; }

.teacher-meta { display: flex; flex-direction: column; }
.teacher-name { font-size: 16px; font-weight: 600; color: var(--color-text-primary); }
.teacher-dept { font-size: 12px; color: var(--color-text-muted); margin-top: 2px; }

.card-title { margin-bottom: 10px; }

.card-fields { margin-bottom: 10px; display: flex; flex-wrap: wrap; gap: 4px; }
.field-tag { margin: 0 !important; }
.no-data { font-size: 12px; color: var(--color-text-placeholder); }

.card-rating { margin-bottom: 10px; }
.card-teams { font-size: 12px; color: var(--color-text-secondary); }
.team-info { margin-bottom: 4px; }

.card-actions { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--color-border-light); text-align: center; }

/* invite dialog */
.invite-dialog-content { padding: 4px 0; }
.invite-preview {
  display: flex; align-items: center; gap: 14px; margin-bottom: 20px;
  padding: 16px; background: var(--color-bg); border-radius: var(--radius-md);
}
.invite-avatar { background: var(--gradient-primary) !important; font-size: 20px; color: #fff; }
.invite-name { font-size: 16px; font-weight: 600; color: var(--color-text-primary); }
.invite-dept { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; }
.invite-form { margin-top: 4px; }
</style>
