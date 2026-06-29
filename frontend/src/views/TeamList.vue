<template>
  <div class="team-list-container">
    <div class="page-header-bar">
      <div>
        <h2>我的团队</h2>
        <p>管理和查看你创建或加入的所有团队</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog" class="header-btn">
        创建团队
      </el-button>
    </div>

    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" class="team-tabs">
        <el-tab-pane label="我创建的" name="created">
          <div v-loading="loading.created">
            <el-row v-if="createdTeams.length > 0" :gutter="20">
              <el-col v-for="team in createdTeams" :key="team.id" :xs="24" :sm="12" :lg="8" style="margin-bottom:20px">
                <div class="team-card card-hover" @click="router.push(`/team/${team.id}`)">
                  <div class="card-top">
                    <div class="card-avatar" style="background:var(--gradient-primary)">
                      {{ team.name?.charAt(0) || 'T' }}
                    </div>
                    <div class="card-titles">
                      <div class="card-name">{{ team.name }}</div>
                      <el-tag :type="teamStatusType(team.status)" size="small" effect="light" round>
                        {{ teamStatusLabel(team.status) }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="card-stats">
                    <div class="card-stat">
                      <span class="stat-num">{{ team.memberCount || team.memberNum || 0 }}</span>
                      <span class="stat-lbl">成员</span>
                    </div>
                    <div class="card-stat">
                      <span class="stat-num">{{ team.projectCount || 0 }}</span>
                      <span class="stat-lbl">项目</span>
                    </div>
                    <div class="card-stat">
                      <span class="stat-num">{{ dayjs(team.createdAt).format('MM-DD') }}</span>
                      <span class="stat-lbl">创建</span>
                    </div>
                  </div>
                  <div class="card-footer" v-if="team.projectName">
                    <el-icon :size="14"><FolderOpened /></el-icon>
                    {{ team.projectName }}
                  </div>
                </div>
              </el-col>
            </el-row>
            <el-empty v-else description="暂无创建的团队" :image-size="100" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我加入的" name="joined">
          <div v-loading="loading.joined">
            <el-row v-if="joinedTeams.length > 0" :gutter="20">
              <el-col v-for="team in joinedTeams" :key="team.id" :xs="24" :sm="12" :lg="8" style="margin-bottom:20px">
                <div class="team-card card-hover" @click="router.push(`/team/${team.id}`)">
                  <div class="card-top">
                    <div class="card-avatar card-avatar-joined">
                      {{ team.name?.charAt(0) || 'T' }}
                    </div>
                    <div class="card-titles">
                      <div class="card-name">{{ team.name }}</div>
                      <el-tag :type="teamStatusType(team.status)" size="small" effect="light" round>
                        {{ teamStatusLabel(team.status) }}
                      </el-tag>
                    </div>
                  </div>
                  <div class="card-stats">
                    <div class="card-stat">
                      <span class="stat-num">{{ team.memberCount || team.memberNum || 0 }}</span>
                      <span class="stat-lbl">成员</span>
                    </div>
                    <div class="card-stat">
                      <span class="stat-num">{{ team.projectCount || 0 }}</span>
                      <span class="stat-lbl">项目</span>
                    </div>
                    <div class="card-stat">
                      <span class="stat-num">{{ dayjs(team.createdAt).format('MM-DD') }}</span>
                      <span class="stat-lbl">创建</span>
                    </div>
                  </div>
                  <div class="card-footer" v-if="team.leaderName">
                    <el-icon :size="14"><Star /></el-icon>
                    队长：{{ team.leaderName }}
                  </div>
                </div>
              </el-col>
            </el-row>
            <el-empty v-else description="暂未加入任何团队" :image-size="100" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Create Team Dialog -->
    <el-dialog v-model="createDialogVisible" title="创建团队" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px" @submit.prevent>
        <el-form-item label="团队名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入团队名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="关联招募" prop="recruitId">
          <el-input-number v-model="createForm.recruitId" placeholder="可选，关联的招募ID" :min="0" style="width:100%" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { Plus, FolderOpened, Star } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getMyTeams, getJoinedTeams, createTeam } from '@/api/team'

const router = useRouter()

const activeTab = ref('created')
const createdTeams = ref<any[]>([])
const joinedTeams = ref<any[]>([])

const loading = reactive({ created: false, joined: false })

const createDialogVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ name: '', recruitId: null as number | null })

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入团队名称', trigger: 'blur' },
    { min: 2, max: 50, message: '团队名称长度需要2-50位', trigger: 'blur' },
  ],
}

function openCreateDialog() {
  createForm.name = ''
  createForm.recruitId = null
  createDialogVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: any = { name: createForm.name }
    if (createForm.recruitId) payload.recruitId = createForm.recruitId
    await createTeam(payload)
    ElMessage.success('团队创建成功')
    createDialogVisible.value = false
    await fetchMyTeams()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally { submitting.value = false }
}

async function fetchMyTeams() {
  loading.created = true
  try { const res = await getMyTeams(); createdTeams.value = res.data || [] }
  catch { createdTeams.value = [] }
  finally { loading.created = false }
}

async function fetchJoinedTeams() {
  loading.joined = true
  try { const res = await getJoinedTeams(); joinedTeams.value = res.data || [] }
  catch { joinedTeams.value = [] }
  finally { loading.joined = false }
}

function teamStatusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, any> = { ACTIVE: 'success', NORMAL: 'success', INACTIVE: 'warning', DISBANDED: 'info', CLOSED: 'danger' }
  return map[status] || 'info'
}
function teamStatusLabel(status: string): string {
  const map: Record<string, string> = { ACTIVE: '活跃中', NORMAL: '活跃中', INACTIVE: '已停用', DISBANDED: '已解散', CLOSED: '已关闭' }
  return map[status] || status || '未知'
}

onMounted(() => { Promise.all([fetchMyTeams(), fetchJoinedTeams()]) })
</script>

<style scoped>
.team-list-container { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: 28px 36px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
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
.page-header-bar h2 { font-size: 24px; font-weight: 700; margin: 0 0 4px; color: var(--color-text-primary); }
.page-header-bar p { margin: 0; font-size: 14px; color: var(--color-text-muted); }
.header-btn {
  border-radius: var(--radius-sm) !important;
  font-weight: 600 !important;
}
.header-btn:hover { background: rgba(255,255,255,0.3) !important; }

.main-card { border-radius: var(--radius-lg) !important; }
.team-tabs { min-height: 300px; }

.team-card {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  height: 100%;
}
.card-top { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.card-avatar {
  width: 48px; height: 48px;
  border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700; color: #fff; flex-shrink: 0;
}
.card-avatar-joined { background: linear-gradient(135deg, #22C55E, #10B981); }
.card-titles { flex: 1; min-width: 0; }
.card-name {
  font-size: 16px; font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-stats {
  display: flex; gap: 28px; margin-bottom: 18px;
  padding: 14px 0;
  border-top: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
}
.card-stat { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 20px; font-weight: 700; color: var(--color-text-primary); line-height: 1.2; }
.stat-lbl { font-size: 12px; color: var(--color-text-muted); margin-top: 2px; }
.card-footer {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .page-header-bar { padding: 24px; }
}
</style>
