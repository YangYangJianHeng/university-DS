<template>
  <div class="admin-dashboard">
    <h2 class="page-title">管理后台</h2>

    <!-- Stats Cards -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#ecf5ff;">
              <el-icon :size="28" color="#409eff"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalUsers ?? '--' }}</span>
              <span class="stat-label">总用户数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#f0f9eb;">
              <el-icon :size="28" color="#67c23a"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalTeams ?? '--' }}</span>
              <span class="stat-label">总团队数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#fdf6ec;">
              <el-icon :size="28" color="#e6a23c"><FolderOpened /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalProjects ?? '--' }}</span>
              <span class="stat-label">总项目数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background:#fef0f0;">
              <el-icon :size="28" color="#f56c6c"><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.totalCompetitions ?? '--' }}</span>
              <span class="stat-label">总竞赛数</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tabs: 招募审核 | 用户管理 | 审核日志 -->
    <el-card shadow="never" class="section-card">
      <el-tabs v-model="activeTab">
        <!-- Tab 1: Recruit Audit -->
        <el-tab-pane label="招募审核" name="recruit">
          <div class="section-header">
            <span>待审核招募（{{ pendingRecruits.length }}）</span>
          </div>
          <el-table :data="pendingRecruits" v-loading="recruitLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="projectType" label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.projectType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布人" width="100">
              <template #default="{ row }">
                {{ publisherNames[row.id] || '--' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="提交时间" width="130">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('MM-DD HH:mm') : '--' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApproveRecruit(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleRejectRecruit(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!recruitLoading && pendingRecruits.length === 0" description="暂无待审核招募" />
        </el-tab-pane>

        <!-- Tab 2: User Management -->
        <el-tab-pane label="用户管理" name="users">
          <div class="section-header">
            <span>用户列表</span>
            <el-input
              v-model="userSearch"
              placeholder="搜索用户名"
              clearable
              style="width:240px"
              size="small"
              @input="loadUsers"
            />
          </div>
          <el-table :data="users" v-loading="userLoading" stripe size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="phone" label="手机号" width="120" />
            <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
            <el-table-column label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="roleType(row.role)">
                  {{ roleLabel(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核权限" width="120">
              <template #default="{ row }">
                <el-switch
                  v-if="row.role !== 'ADMIN'"
                  :model-value="row.role === 'REVIEWER'"
                  @change="(val: boolean) => handleToggleReviewer(row, val)"
                />
                <span v-else style="color:#999;font-size:12px;">--</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.status === 1"
                  :loading="row._toggleLoading"
                  @change="(val: boolean) => handleToggleUserStatus(row, val)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="120">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('YYYY-MM-DD') : '--' }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: Audit Log -->
        <el-tab-pane label="审核日志" name="auditLog">
          <el-tabs v-model="auditTab">
            <el-tab-pane label="待审核" name="pending" />
            <el-tab-pane label="已审核" name="done" />
          </el-tabs>
          <el-table :data="auditList" v-loading="auditListLoading" stripe size="small">
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="type" label="类型" width="90">
              <template #default="{ row }">
                <el-tag size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitterName" label="提交人" width="100" />
            <el-table-column prop="createdAt" label="提交时间" width="120">
              <template #default="{ row }">
                {{ row.createdAt ? dayjs(row.createdAt).format('YYYY-MM-DD') : '--' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'"
                  size="small"
                >
                  {{ row.status === 'APPROVED' ? '已通过' : row.status === 'REJECTED' ? '已驳回' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" v-if="auditTab === 'pending'">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
                <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import request from '@/api/request'

const activeTab = ref('recruit')

// Stats
const stats = ref<Record<string, number>>({})

// Users
const users = ref<any[]>([])
const userLoading = ref(false)
const userSearch = ref('')

// Recruit Audit
const pendingRecruits = ref<any[]>([])
const recruitLoading = ref(false)
const publisherNames = ref<Record<number, string>>({})

// Audit Log
const auditTab = ref('pending')
const auditList = ref<any[]>([])
const auditListLoading = ref(false)

onMounted(async () => {
  await loadStats()
  await loadPendingRecruits()
  await loadUsers()
  await loadAuditList()
})

watch(activeTab, async (tab) => {
  if (tab === 'recruit') await loadPendingRecruits()
  if (tab === 'users') await loadUsers()
  if (tab === 'auditLog') await loadAuditList()
})

watch(auditTab, () => {
  loadAuditList()
})

async function loadStats() {
  try {
    const res = await request.get('/admin/dashboard/stats')
    stats.value = res.data || {}
  } catch (_) {
    stats.value = {}
  }
}

// ---- Recruit Audit ----

async function loadPendingRecruits() {
  recruitLoading.value = true
  try {
    const res = await request.get('/admin/recruits/pending', { params: { page: 1, size: 20 } })
    const data = res.data
    const records = data?.records || []
    pendingRecruits.value = records

    // Fetch publisher names
    if (records.length > 0) {
      const names: Record<number, string> = {}
      await Promise.all(records.map(async (r: any) => {
        try {
          const userRes = await request.get(`/user/profile/${r.publisherId}`)
          names[r.id] = userRes.data?.username || '--'
        } catch {
          names[r.id] = '--'
        }
      }))
      publisherNames.value = names
    }
  } catch (_) {
    pendingRecruits.value = []
  } finally {
    recruitLoading.value = false
  }
}

async function handleApproveRecruit(row: any) {
  try {
    await request.put(`/admin/recruits/${row.id}/audit`, null, { params: { action: 'APPROVE' } })
    ElMessage.success('招募已通过审核')
    await loadPendingRecruits()
    await loadStats()
  } catch (_) {}
}

async function handleRejectRecruit(row: any) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回招募', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (val: string) => !!val || '请填写驳回原因',
    })
    await request.put(`/admin/recruits/${row.id}/audit`, null, { params: { action: 'REJECT', reason } })
    ElMessage.success('招募已驳回')
    await loadPendingRecruits()
    await loadStats()
  } catch (_) {}
}

// ---- User Management ----

async function loadUsers() {
  userLoading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (userSearch.value) params.keyword = userSearch.value
    const res = await request.get('/admin/users', { params })
    const data = res.data
    users.value = data?.records || []
  } catch (_) {
    users.value = []
  } finally {
    userLoading.value = false
  }
}

async function handleToggleUserStatus(row: any, val: boolean) {
  row._toggleLoading = true
  try {
    await request.put(`/admin/users/${row.id}/status`, null, { params: { status: val ? 1 : 0 } })
    row.status = val ? 1 : 0
    ElMessage.success('状态已更新')
  } catch (_) {
  } finally {
    row._toggleLoading = false
  }
}

async function handleToggleReviewer(row: any, val: boolean) {
  try {
    const role = val ? 'REVIEWER' : 'USER'
    await request.put(`/admin/users/${row.id}/role`, null, { params: { role } })
    row.role = role
    ElMessage.success(val ? '已授予审核权限' : '已撤销审核权限')
  } catch (_) {}
}

function roleType(role: string) {
  if (role === 'ADMIN') return 'danger'
  if (role === 'REVIEWER') return 'warning'
  if (role === 'TEACHER') return 'success'
  return 'primary'
}

function roleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '管理员',
    REVIEWER: '审核员',
    TEACHER: '导师',
    USER: '学生',
  }
  return map[role] || role
}

// ---- Audit Log ----

async function loadAuditList() {
  auditListLoading.value = true
  try {
    const params: any = {}
    if (auditTab.value === 'done') params.status = 'APPROVED,REJECTED'
    else params.status = 'PENDING'
    const res = await request.get('/admin/audit/list', { params })
    const data = res.data
    auditList.value = data?.records || []
  } catch (_) {
    auditList.value = []
  } finally {
    auditListLoading.value = false
  }
}

async function handleApprove(row: any) {
  try {
    await request.put(`/admin/audit/${row.id}`, null, { params: { action: 'APPROVE' } })
    ElMessage.success('已通过')
    await loadAuditList()
    await loadStats()
  } catch (_) {}
}

async function handleReject(row: any) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入驳回原因', '驳回', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
    await request.put(`/admin/audit/${row.id}`, null, { params: { action: 'REJECT', reason } })
    ElMessage.success('已驳回')
    await loadAuditList()
    await loadStats()
  } catch (_) {}
}
</script>

<style scoped>
.admin-dashboard {
  padding: 0;
}
.page-title {
  font-size: 20px;
  color: #303133;
  margin-bottom: 20px;
}
.stats-row {
  margin-bottom: 16px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}
.section-card {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
