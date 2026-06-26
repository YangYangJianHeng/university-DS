<template>
  <div class="team-list-container">
    <div class="page-header-bar">
      <div>
        <h2>我的团队</h2>
        <p class="page-desc">管理和查看你创建或加入的所有团队</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建团队</el-button>
    </div>

    <el-card shadow="never" class="main-card">
      <el-tabs v-model="activeTab" class="team-tabs">
        <el-tab-pane label="我创建的" name="created">
          <div v-loading="loading.created">
            <el-row v-if="createdTeams.length > 0" :gutter="20">
              <el-col v-for="team in createdTeams" :key="team.id" :xs="24" :sm="12" :lg="8" style="margin-bottom:20px">
                <el-card shadow="hover" class="team-card" @click="router.push(`/team/${team.id}`)">
                  <div class="card-top">
                    <div class="card-avatar" style="background:linear-gradient(135deg,#667eea,#764ba2)">
                      {{ team.name?.charAt(0) || 'T' }}
                    </div>
                    <div class="card-titles">
                      <div class="card-name">{{ team.name }}</div>
                      <el-tag :type="teamStatusType(team.status)" size="small" effect="plain">{{ teamStatusLabel(team.status) }}</el-tag>
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
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                    {{ team.projectName }}
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-empty v-else description="暂无创建的团队" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="我加入的" name="joined">
          <div v-loading="loading.joined">
            <el-row v-if="joinedTeams.length > 0" :gutter="20">
              <el-col v-for="team in joinedTeams" :key="team.id" :xs="24" :sm="12" :lg="8" style="margin-bottom:20px">
                <el-card shadow="hover" class="team-card" @click="router.push(`/team/${team.id}`)">
                  <div class="card-top">
                    <div class="card-avatar" style="background:linear-gradient(135deg,#43e97b,#38f9d7)">
                      {{ team.name?.charAt(0) || 'T' }}
                    </div>
                    <div class="card-titles">
                      <div class="card-name">{{ team.name }}</div>
                      <el-tag :type="teamStatusType(team.status)" size="small" effect="plain">{{ teamStatusLabel(team.status) }}</el-tag>
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
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                    队长：{{ team.leaderName }}
                  </div>
                </el-card>
              </el-col>
            </el-row>
            <el-empty v-else description="暂未加入任何团队" />
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
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getMyTeams, getJoinedTeams, createTeam } from '@/api/team'

const router = useRouter()

const activeTab = ref('created')
const createdTeams = ref<any[]>([])
const joinedTeams = ref<any[]>([])

const loading = reactive({ created: false, joined: false })

// --- Create Team ---
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
  } finally {
    submitting.value = false
  }
}

async function fetchMyTeams() {
  loading.created = true
  try { const res = await getMyTeams(); createdTeams.value = res.data || [] }
  catch (_) { createdTeams.value = [] }
  finally { loading.created = false }
}

async function fetchJoinedTeams() {
  loading.joined = true
  try { const res = await getJoinedTeams(); joinedTeams.value = res.data || [] }
  catch (_) { joinedTeams.value = [] }
  finally { loading.joined = false }
}

function teamStatusType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'ACTIVE': case 'NORMAL': return 'success'
    case 'INACTIVE': return 'warning'
    case 'DISBANDED': return 'info'
    case 'CLOSED': return 'danger'
    default: return 'info'
  }
}

function teamStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE': case 'NORMAL': return '活跃中'
    case 'INACTIVE': return '已停用'
    case 'DISBANDED': return '已解散'
    case 'CLOSED': return '已关闭'
    default: return status || '未知'
  }
}

onMounted(() => { fetchMyTeams(); fetchJoinedTeams() })
</script>

<style scoped>
.team-list-container { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
  background: var(--gradient-primary);
  border-radius: 16px;
  padding: 24px 32px;
  margin-bottom: 20px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-desc { margin: 0; font-size: 14px; opacity: 0.8; }

.main-card { border-radius: 14px; }

.team-tabs { min-height: 300px; }

.team-card { cursor: pointer; transition: all 0.25s; height: 100%; border-radius: 12px; }
.team-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }

.card-top { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }

.card-avatar {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff;
  flex-shrink: 0;
}

.card-titles { flex: 1; min-width: 0; }
.card-name { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.card-stats { display: flex; gap: 24px; margin-bottom: 16px; }
.card-stat { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 18px; font-weight: 700; color: #1a1a2e; }
.stat-lbl { font-size: 11px; color: #909399; margin-top: 2px; }

.card-footer {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #909399;
  padding-top: 12px; border-top: 1px solid #f0f2f5;
}
</style>
