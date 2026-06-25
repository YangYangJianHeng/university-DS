<template>
  <div class="teacher-list-page">
    <div class="page-header-bar">
      <div>
        <h2>导师库</h2>
        <p class="page-desc">浏览各院系导师，寻找适合的竞赛指导</p>
      </div>
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
            <el-tag v-for="f in item.researchFields || []" :key="f" size="small" type="success" effect="plain" class="field-tag">{{ f }}</el-tag>
            <span v-if="!item.researchFields?.length" class="no-data">暂无研究方向</span>
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
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listTeachers } from '@/api/teacher'

const loading = ref(false)
const teachers = ref<any[]>([])
const department = ref('')
const departments = ref<string[]>([])

onMounted(() => loadTeachers())

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

function maxTeamPercent(item: any) { const max = item.maxTeams || 5; if (max === 0) return 0; return Math.round(((item.currentTeams || 0) / max) * 100) }
</script>

<style scoped>
.teacher-list-page { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
  background: var(--gradient-primary);
  border-radius: 16px; padding: 24px 32px;
  margin-bottom: 20px; color: #fff;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-desc { margin: 0; font-size: 14px; opacity: 0.8; }

.filter-card { border-radius: 14px; margin-bottom: 16px; }

.teacher-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }

.teacher-card { transition: all 0.25s; border-radius: 12px; }
.teacher-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }

.card-top { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }

.teacher-avatar { background: var(--gradient-primary) !important; font-size: 20px; font-weight: 600; color: #fff; flex-shrink: 0; }

.teacher-meta { display: flex; flex-direction: column; }
.teacher-name { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.teacher-dept { font-size: 12px; color: #909399; margin-top: 2px; }

.card-title { margin-bottom: 10px; }

.card-fields { margin-bottom: 10px; display: flex; flex-wrap: wrap; gap: 4px; }
.field-tag { margin: 0 !important; }
.no-data { font-size: 12px; color: #c0c4cc; }

.card-rating { margin-bottom: 10px; }
.card-teams { font-size: 12px; color: #606266; }
.team-info { margin-bottom: 4px; }
</style>
