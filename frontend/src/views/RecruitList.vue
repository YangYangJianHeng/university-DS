<template>
  <div class="recruit-list-container">
    <!-- Page Header -->
    <div class="page-header-bar">
      <div class="header-content">
        <div>
          <h2>招募广场</h2>
          <p>浏览所有招募项目，找到适合你的团队</p>
        </div>
        <el-button class="header-btn" size="large" @click="router.push('/recruit/create')">
          <el-icon><Plus /></el-icon>发布招募
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
            style="width:220px"
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
    <div v-loading="loading" class="recruit-grid-wrapper">
      <el-row v-if="recruits.length > 0" :gutter="20">
        <el-col v-for="item in recruits" :key="item.id" :xs="24" :sm="12" :lg="8" style="margin-bottom:20px">
          <div class="recruit-card card-hover" @click="router.push(`/recruit/${item.id}`)">
            <!-- Type Badge -->
            <div class="card-top">
              <el-tag :type="projectTypeTag(item.projectType)" size="small" effect="dark" round>
                {{ item.projectType || '其他' }}
              </el-tag>
            </div>
            <!-- Card Body -->
            <div class="card-body">
              <h3 class="card-title">{{ item.title }}</h3>
              <div class="card-publisher">
                <el-avatar :size="20" :src="item.publisherAvatar" class="pub-avatar">
                  {{ item.publisherName?.charAt(0) || '?' }}
                </el-avatar>
                <span>{{ item.publisherName || item.publisher?.username || '未知' }}</span>
              </div>
              <div class="card-skills" v-if="item.requiredSkills?.length">
                <el-tag
                  v-for="skill in item.requiredSkills.slice(0, 4)"
                  :key="skill"
                  size="small"
                  class="skill-tag"
                >
                  {{ skill }}
                </el-tag>
                <span v-if="item.requiredSkills.length > 4" class="more-tag">
                  +{{ item.requiredSkills.length - 4 }}
                </span>
              </div>
            </div>
            <!-- Card Footer -->
            <div class="card-footer">
              <div class="footer-stats">
                <span class="footer-stat">
                  <el-icon :size="14"><User /></el-icon>
                  {{ item.currentMembers || 0 }}/{{ item.maxMembers }}
                </span>
                <span class="footer-stat">
                  <el-icon :size="14"><Clock /></el-icon>
                  {{ item.deadline ? getCountdown(item.deadline) : '无截止' }}
                </span>
              </div>
              <el-tag :type="statusTag(item.status)" size="small" effect="dark" round>
                {{ statusLabel(item.status) }}
              </el-tag>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-empty v-else-if="!loading" description="暂无招募信息" :image-size="120" />

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
import { Search, User, Clock } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const recruits = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const filters = ref({ projectType: '', status: '', keyword: '' })

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
  } catch { recruits.value = [] } finally { loading.value = false }
}

function handleSearch() { currentPage.value = 1; fetchRecruits() }
function handleReset() {
  filters.value = { projectType: '', status: '', keyword: '' }
  currentPage.value = 1
  fetchRecruits()
}
function handlePageChange(page: number) { currentPage.value = page; fetchRecruits() }

function statusTag(status: string) {
  const map: Record<string, any> = { OPEN: 'success', CLOSED: 'info', FULL: 'warning' }
  return map[status] || 'info'
}
function statusLabel(status: string) {
  const map: Record<string, string> = { OPEN: '招募中', CLOSED: '已关闭', FULL: '已满员' }
  return map[status] || status || '未知'
}
function projectTypeTag(type: string) {
  const map: Record<string, any> = { '竞赛': '', '科研': '', '创新创业': 'success' }
  return map[type] || ''
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

/* Page Header */
.page-header-bar {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: 32px 36px;
  margin-bottom: 24px;
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
.page-header-bar::after {
  content: '';
  position: absolute;
  width: 200px; height: 200px;
  border-radius: 50%;
  background: rgba(91,111,245,0.03);
  top: -60px; right: -40px;
}
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
  flex-wrap: wrap;
  gap: 16px;
}
.header-content h2 { font-size: 24px; font-weight: 700; margin: 0 0 4px; color: var(--color-text-primary); }
.header-content p { margin: 0; font-size: 14px; color: var(--color-text-muted); }
.header-btn {
  border-radius: var(--radius-sm) !important;
  font-weight: 600 !important;
}
.header-btn:hover { opacity: 0.9; }

/* Filter */
.filter-card {
  border-radius: var(--radius-lg) !important;
  margin-bottom: 24px;
}

/* Recruit Cards */
.recruit-grid-wrapper { min-height: 200px; }
.recruit-card {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.recruit-card:hover {
  border-color: rgba(91,111,245,0.15);
}

.card-top {
  padding: 16px 20px 0;
}
.card-body {
  padding: 12px 20px;
  flex: 1;
}
.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-publisher {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 10px;
}
.pub-avatar { background: var(--gradient-primary) !important; font-size: 12px; font-weight: 600; color: #fff; }
.card-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.skill-tag {
  background: var(--color-bg) !important;
  border-color: var(--color-border-light) !important;
  color: var(--color-text-secondary) !important;
}
.more-tag {
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 2px 8px;
  background: var(--color-bg);
  border-radius: 10px;
}

/* Card Footer */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg);
}
.footer-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}
.footer-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .page-header-bar { padding: 24px; }
}
</style>
