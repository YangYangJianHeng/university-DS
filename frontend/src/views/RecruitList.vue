<template>
  <div class="recruit-list-container">
    <!-- Page Header with Gradient -->
    <div class="page-header-bar">
      <div class="page-header-content">
        <div>
          <h2>招募广场</h2>
          <p class="page-desc">浏览所有招募项目，找到适合你的团队</p>
        </div>
        <el-button type="primary" size="large" @click="router.push('/recruit/create')" class="create-btn">
          <el-icon><Plus /></el-icon>
          发布招募
        </el-button>
      </div>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" label-width="auto">
        <el-form-item label="项目类型">
          <el-select v-model="filters.projectType" placeholder="全部类型" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="竞赛" value="竞赛" />
            <el-option label="科研" value="科研" />
            <el-option label="创新创业" value="创新创业" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部状态" clearable @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="招募中" value="OPEN" />
            <el-option label="已关闭" value="CLOSED" />
            <el-option label="已满员" value="FULL" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索标题..."
            clearable
            style="width: 220px"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Recruit Cards -->
    <div v-loading="loading">
      <el-row v-if="recruits.length > 0" :gutter="20">
        <el-col v-for="item in recruits" :key="item.id" :xs="24" :sm="12" :lg="8" style="margin-bottom: 20px">
          <el-card shadow="hover" class="recruit-card" @click="router.push(`/recruit/${item.id}`)">
            <div class="card-type-badge">
              <el-tag :type="projectTypeType(item.projectType)" size="small" effect="dark">
                {{ item.projectType }}
              </el-tag>
            </div>
            <div class="card-body">
              <div class="card-title">{{ item.title }}</div>
              <div class="card-publisher">
                <el-icon><UserFilled /></el-icon>
                {{ item.publisherName || item.publisher?.username || '未知' }}
              </div>
              <div class="card-skills" v-if="item.requiredSkills?.length">
                <el-tag
                  v-for="skill in item.requiredSkills.slice(0, 4)"
                  :key="skill"
                  type="info"
                  size="small"
                  effect="plain"
                >
                  {{ skill }}
                </el-tag>
                <span v-if="item.requiredSkills.length > 4" class="more-skills">+{{ item.requiredSkills.length - 4 }}</span>
              </div>
            </div>
            <div class="card-footer">
              <div class="footer-left">
                <div class="member-count">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                  {{ item.currentMembers || 0 }}/{{ item.maxMembers }}
                </div>
                <div class="deadline">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  <template v-if="item.deadline">{{ getCountdown(item.deadline) }}</template>
                  <template v-else>无截止</template>
                </div>
              </div>
              <el-tag
                :type="statusType(item.status)"
                size="small"
                effect="dark"
              >
                {{ statusLabel(item.status) }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-else-if="!loading" description="暂无招募信息" />

      <!-- Pagination -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { listRecruits } from '@/api/recruit'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const recruits = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const filters = ref({
  projectType: '',
  status: '',
  keyword: '',
})

async function fetchRecruits() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize.value }
    if (filters.value.projectType) params.projectType = filters.value.projectType
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.keyword) params.keyword = filters.value.keyword
    const res = await listRecruits(params)
    const data = res.data || {}
    recruits.value = data.records || data.list || []
    total.value = data.total || 0
  } catch (_) {
    recruits.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { currentPage.value = 1; fetchRecruits() }

function handleReset() {
  filters.value.projectType = ''
  filters.value.status = ''
  filters.value.keyword = ''
  currentPage.value = 1
  fetchRecruits()
}

function handlePageChange(page: number) { currentPage.value = page; fetchRecruits() }

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

function getCountdown(deadline: string): string {
  const diff = dayjs(deadline).diff(dayjs(), 'day')
  if (diff < 0) return '已截止'
  if (diff === 0) return '今日截止'
  if (diff === 1) return '明天截止'
  return `剩余 ${diff} 天`
}

onMounted(() => fetchRecruits())
</script>

<style scoped>
.recruit-list-container { max-width: 1200px; margin: 0 auto; }

/* ===== Page Header ===== */
.page-header-bar {
  background: var(--gradient-primary);
  border-radius: var(--radius-md);
  padding: 28px 32px;
  margin-bottom: 20px;
  color: #fff;
}

.page-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header-content h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 4px;
}

.page-desc {
  margin: 0;
  font-size: 14px;
  opacity: 0.8;
}

.create-btn {
  border-radius: 10px;
  font-weight: 600;
  border: none;
  background: rgba(255,255,255,0.2) !important;
  backdrop-filter: blur(4px);
}
.create-btn:hover {
  background: rgba(255,255,255,0.3) !important;
  transform: translateY(-1px);
}

/* ===== Filter ===== */
.filter-card {
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

/* ===== Recruit Cards ===== */
.recruit-card {
  cursor: pointer;
  transition: all 0.25s;
  height: 100%;
  border-radius: var(--radius-sm);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.recruit-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.card-type-badge {
  padding: 12px 16px 0;
}

.card-body {
  padding: 8px 16px 12px;
  flex: 1;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-publisher {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 10px;
}

.card-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.more-skills {
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 2px 6px;
  background: var(--color-bg);
  border-radius: 4px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-light);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.member-count, .deadline {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 20px 0;
}
</style>
