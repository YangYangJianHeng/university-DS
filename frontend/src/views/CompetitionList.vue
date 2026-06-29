<template>
  <div class="competition-list-page">
    <div class="page-header-bar">
      <div>
        <h2>竞赛中心</h2>
        <p class="page-desc">浏览各类学科竞赛，找到适合你的挑战</p>
      </div>
    </div>

    <!-- Filter Bar -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="filters" size="default">
        <el-form-item label="竞赛级别">
          <el-select v-model="filters.level" placeholder="全部" clearable style="width:140px">
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="省级" value="PROVINCIAL" />
            <el-option label="校级" value="SCHOOL" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="filters.category" placeholder="全部" clearable style="width:140px">
            <el-option label="学科竞赛" value="ACADEMIC" />
            <el-option label="创新创业" value="INNOVATION" />
            <el-option label="文体竞赛" value="SPORTS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:140px">
            <el-option label="报名中" value="REGISTERING" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="ENDED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Competition Cards -->
    <div v-loading="loading">
      <el-empty v-if="!loading && competitions.length === 0" description="暂无竞赛信息" />
      <div v-else class="competition-grid">
        <el-card v-for="item in competitions" :key="item.id" class="competition-card" shadow="hover" @click="router.push(`/competition/${item.id}`)">
          <div class="card-level-strip" :class="'level-' + (item.level || '').toLowerCase()"></div>
          <div class="card-body">
            <div class="card-top">
              <el-tag :type="levelType(item.level)" size="small" effect="dark">{{ levelLabel(item.level) }}</el-tag>
              <el-tag :type="statusType(item.status)" size="small" effect="plain">{{ statusLabel(item.status) }}</el-tag>
            </div>
            <div class="card-name">{{ item.name }}</div>
            <div class="card-dates">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              {{ item.eventStart ? dayjs(item.eventStart).format('YYYY-MM-DD') : '--' }} ~ {{ item.eventEnd ? dayjs(item.eventEnd).format('YYYY-MM-DD') : '--' }}
            </div>
            <div class="card-organizer">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
              {{ item.organizer }}
            </div>
            <div class="card-stats">
              <span><el-icon><View /></el-icon> {{ item.viewCount || 0 }}</span>
              <span><el-icon><Star /></el-icon> {{ item.collectCount || 0 }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { listCompetitions } from '@/api/competition'

const router = useRouter()
const loading = ref(false)
const competitions = ref<any[]>([])
const filters = ref({ level: '', category: '', status: '' })

function levelType(level: string) { if (level === 'NATIONAL') return 'danger'; if (level === 'PROVINCIAL') return 'warning'; return 'primary' }
function levelLabel(level: string) { if (level === 'NATIONAL') return '国家级'; if (level === 'PROVINCIAL') return '省级'; if (level === 'SCHOOL') return '校级'; return level }
function statusType(s: string) { if (s === 'REGISTERING') return 'success'; if (s === 'ONGOING') return 'primary'; return 'info' }
function statusLabel(s: string) { if (s === 'REGISTERING') return '报名中'; if (s === 'ONGOING') return '进行中'; if (s === 'ENDED') return '已结束'; return s }

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params: any = {}
    if (filters.value.level) params.level = filters.value.level
    if (filters.value.category) params.category = filters.value.category
    if (filters.value.status) params.status = filters.value.status
    const res = await listCompetitions(params)
    competitions.value = res.data?.records || []
  } catch (_) { competitions.value = [] }
  finally { loading.value = false }
}

function resetFilters() { filters.value = { level: '', category: '', status: '' }; loadData() }
</script>

<style scoped>
.competition-list-page { max-width: 1200px; margin: 0 auto; }

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

.competition-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.competition-card {
  cursor: pointer;
  transition: all var(--transition-base);
  border-radius: var(--radius-lg);
  overflow: hidden;
  padding: 0;
  border: 1px solid var(--color-border-light);
}
.competition-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); border-color: var(--color-border); }

.card-level-strip { height: 4px; }
.card-level-strip.level-national { background: linear-gradient(90deg, #f56c6c, #fa709a); }
.card-level-strip.level-provincial { background: linear-gradient(90deg, #e6a23c, #fee140); }
.card-level-strip.level-school { background: linear-gradient(90deg, var(--color-primary), #a5b4fc); }

.card-body { padding: 16px; }

.card-top { display: flex; gap: 6px; margin-bottom: 10px; }

.card-name {
  font-size: 15px; font-weight: 600; color: var(--color-text-primary);
  margin-bottom: 10px; overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}

.card-dates, .card-organizer {
  font-size: 13px; color: var(--color-text-secondary);
  margin-bottom: 6px; display: flex; align-items: center; gap: 4px;
}

.card-stats { display: flex; gap: 16px; font-size: 12px; color: var(--color-text-muted); margin-top: 10px; }
.card-stats span { display: flex; align-items: center; gap: 4px; }
</style>
