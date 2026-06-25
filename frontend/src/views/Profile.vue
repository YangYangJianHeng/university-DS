<template>
  <div class="profile-page">
    <!-- User Header -->
    <el-card class="profile-header" shadow="never">
      <div class="header-content">
        <el-avatar :size="72" :src="userInfo.avatar" class="user-avatar">
          {{ userInfo.username?.charAt(0)?.toUpperCase() }}
        </el-avatar>
        <div class="header-info">
          <h2>{{ userInfo.username || '未知用户' }}</h2>
          <p class="header-meta">
            <span v-if="userInfo.major">{{ userInfo.major }}</span>
            <span v-if="userInfo.grade"> | {{ userInfo.grade }}</span>
            <span v-if="userInfo.studentId"> | {{ userInfo.studentId }}</span>
          </p>
        </div>
      </div>
    </el-card>

    <!-- Tabs -->
    <el-card class="profile-body" shadow="never">
      <el-tabs v-model="activeTab" tab-position="left" class="profile-tabs">
        <!-- Tab 1: Profile Info -->
        <el-tab-pane label="个人信息" name="info">
          <div class="tab-content">
            <h3 class="tab-title">编辑资料</h3>
            <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px" size="large" class="profile-form">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="profileForm.username" maxlength="20" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" maxlength="11" disabled />
              </el-form-item>
              <el-form-item label="学号" prop="studentId">
                <el-input v-model="profileForm.studentId" />
              </el-form-item>
              <el-form-item label="专业" prop="major">
                <el-input v-model="profileForm.major" />
              </el-form-item>
              <el-form-item label="年级" prop="grade">
                <el-select v-model="profileForm.grade" style="width:100%">
                  <el-option label="大一" value="大一" /><el-option label="大二" value="大二" /><el-option label="大三" value="大三" /><el-option label="大四" value="大四" />
                  <el-option label="研一" value="研一" /><el-option label="研二" value="研二" /><el-option label="研三" value="研三" />
                </el-select>
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" class="save-btn" @click="handleUpdateProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Tab 2: Skills -->
        <el-tab-pane label="技能特长" name="skills">
          <div class="tab-content">
            <h3 class="tab-title">我的技能</h3>
            <div class="skill-input-row">
              <el-input v-model="newSkill.name" placeholder="技能名称" class="skill-input" clearable @keyup.enter="handleAddSkill" />
              <el-select v-model="newSkill.level" placeholder="熟练度" class="skill-level-select">
                <el-option label="入门" value="入门" /><el-option label="熟练" value="熟练" /><el-option label="掌握" value="掌握" />
                <el-option label="精通" value="精通" /><el-option label="大师" value="大师" />
              </el-select>
              <el-button type="primary" :icon="Plus" @click="handleAddSkill">添加</el-button>
            </div>
            <div v-if="skills.length === 0" class="empty-hint">暂无技能，请添加你的技能特长</div>
            <div v-else class="skills-list">
              <div v-for="skill in skills" :key="skill.id" class="skill-item">
                <div class="skill-info">
                  <span class="skill-name">{{ skill.skillName }}</span>
                  <el-tag :type="levelType(skill.level)" size="small" effect="plain">{{ skill.level }}</el-tag>
                </div>
                <el-button text type="danger" :icon="Delete" size="small" @click="handleDeleteSkill(skill.id)" />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 3: Stats -->
        <el-tab-pane label="数据统计" name="stats">
          <div class="tab-content">
            <h3 class="tab-title">技能雷达</h3>
            <div class="chart-container" v-if="radarData.length > 0"><VChart :option="radarOption" autoresize class="chart" /></div>
            <div class="empty-hint" v-else>暂无技能数据</div>
            <h3 class="tab-title" style="margin-top:32px">活动统计</h3>
            <div class="chart-container" v-if="statsLabels.length > 0"><VChart :option="statsOption" autoresize class="chart" /></div>
            <div class="empty-hint" v-else>暂无统计数据</div>
            <el-row :gutter="16" class="stats-summary">
              <el-col :span="8" v-for="item in statSummary" :key="item.label">
                <el-card shadow="hover" class="summary-card">
                  <div class="summary-value">{{ item.value }}</div>
                  <div class="summary-label">{{ item.label }}</div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>

        <!-- Tab 4: Badges -->
        <el-tab-pane label="成就徽章" name="badges">
          <div class="tab-content">
            <h3 class="tab-title">我的徽章</h3>
            <div v-if="badges.length === 0" class="empty-hint">暂无成就徽章，继续努力吧！</div>
            <div v-else class="badges-grid">
              <div v-for="badge in badges" :key="badge.id" class="badge-item">
                <div class="badge-icon-wrapper" :style="{ background: badge.color || '#e6f7ff' }">
                  <el-icon :size="36" :color="badge.iconColor || '#409eff'"><Trophy /></el-icon>
                </div>
                <div class="badge-name">{{ badge.name }}</div>
                <div class="badge-desc">{{ badge.description || '' }}</div>
                <div class="badge-date" v-if="badge.earnedAt">获得于 {{ formatDate(badge.earnedAt) }}</div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, addSkill, deleteSkill } from '@/api/user'
import { getStatistics, getSkillRadar, getBadges } from '@/api/dashboard'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Delete, Trophy } from '@element-plus/icons-vue'

import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart, BarChart } from 'echarts/charts'
import { RadarComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, RadarChart, BarChart, RadarComponent, TooltipComponent, LegendComponent, GridComponent])

const userStore = useUserStore()
const profileFormRef = ref<FormInstance>()
const activeTab = ref('info')
const userInfo = reactive<any>({})
const saving = ref(false)

const profileForm = reactive({ username: '', phone: '', studentId: '', major: '', grade: '', email: '' })
const profileRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 20, message: '长度在 2-20 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

const skills = ref<any[]>([])
const newSkill = reactive({ name: '', level: '熟练' })

function levelType(level: string): 'info' | 'success' | 'warning' | 'danger' {
  const map: Record<string, 'info' | 'success' | 'warning' | 'danger'> = { '入门': 'info', '熟练': '', '掌握': 'success', '精通': 'warning', '大师': 'danger' }
  return map[level] || 'info'
}

const radarData = ref<any[]>([])
const statsLabels = ref<string[]>([])
const statsValues = ref<number[]>([])
const statSummary = ref<any[]>([])
const badges = ref<any[]>([])

const radarOption = computed(() => ({
  tooltip: { trigger: 'item' as const },
  legend: { data: ['技能水平'], bottom: 0 },
  radar: {
    indicator: radarData.value.map((s: any) => ({ name: s.name || s.skillName || '', max: 100 })),
    center: ['50%', '50%'], radius: '60%', shape: 'polygon' as const,
    splitArea: { areaStyle: { color: ['rgba(64,158,255,0.02)', 'rgba(64,158,255,0.05)'] } },
  },
  series: [{ type: 'radar', data: [{ value: radarData.value.map((s: any) => s.value ?? s.level ?? 0), name: '技能水平', areaStyle: { color: 'rgba(64,158,255,0.2)' }, lineStyle: { color: '#409eff', width: 2 }, itemStyle: { color: '#409eff' } }] }],
}))

const statsOption = computed(() => ({
  tooltip: { trigger: 'axis' as const },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  xAxis: { type: 'category' as const, data: statsLabels.value, axisLabel: { rotate: 30, fontSize: 12 } },
  yAxis: { type: 'value' as const },
  series: [{ type: 'bar', data: statsValues.value, barWidth: '50%', itemStyle: { borderRadius: [4, 4, 0, 0], color: { type: 'linear' as const, x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }] } } }],
}))

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

async function fetchProfileData() {
  try {
    const res = await getProfile()
    const data = res.data || res
    Object.assign(userInfo, data)
    profileForm.username = data.username || ''
    profileForm.phone = data.phone || ''
    profileForm.studentId = data.studentId || ''
    profileForm.major = data.major || ''
    profileForm.grade = data.grade || ''
    profileForm.email = data.email || ''
    skills.value = data.skills || data.skillList || []
  } catch (_) {}
}

async function fetchStatsData() {
  try {
    const [statsRes, radarRes, badgesRes] = await Promise.all([
      getStatistics().catch(() => ({ data: {} })),
      getSkillRadar().catch(() => ({ data: [] })),
      getBadges().catch(() => ({ data: [] })),
    ])
    const radar = radarRes.data || []; radarData.value = Array.isArray(radar) ? radar : []
    const stats = statsRes.data || {}
    if (Array.isArray(stats)) { statsLabels.value = stats.map((s: any) => s.label || s.name || ''); statsValues.value = stats.map((s: any) => s.value || 0) }
    else { statsLabels.value = stats.labels || stats.categories || []; statsValues.value = stats.values || stats.counts || [] }
    statSummary.value = [
      { label: '总项目', value: stats.totalProjects ?? stats.projectCount ?? 0 },
      { label: '总任务', value: stats.totalTasks ?? stats.taskCount ?? 0 },
      { label: '成果数', value: stats.totalAchievements ?? stats.achievementCount ?? 0 },
    ]
    const badgeData = badgesRes.data || []; badges.value = Array.isArray(badgeData) ? badgeData : []
  } catch (_) {}
}

async function handleUpdateProfile() {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await updateProfile({ username: profileForm.username, studentId: profileForm.studentId, major: profileForm.major, grade: profileForm.grade, email: profileForm.email })
    ElMessage.success('资料更新成功')
    userStore.fetchProfile()
  } finally { saving.value = false }
}

async function handleAddSkill() {
  if (!newSkill.name.trim()) { ElMessage.warning('请输入技能名称'); return }
  try {
    await addSkill(newSkill.name.trim(), newSkill.level)
    ElMessage.success('技能添加成功'); newSkill.name = ''
    const res = await getProfile(); skills.value = res.data?.skills || res.data?.skillList || []
  } catch (_) {}
}

async function handleDeleteSkill(id: number) {
  try { await ElMessageBox.confirm('确定要删除该技能吗？', '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }); await deleteSkill(id); ElMessage.success('技能已删除'); skills.value = skills.value.filter((s) => s.id !== id) } catch (_) {}
}

onMounted(() => { fetchProfileData(); fetchStatsData() })
</script>

<style scoped>
.profile-page { max-width: 960px; margin: 0 auto; }

.profile-header {
  margin-bottom: 20px;
  border-radius: 14px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-avatar {
  flex-shrink: 0;
  background: var(--gradient-primary) !important;
  font-size: 28px;
  color: #fff;
}

.header-info h2 {
  margin: 0 0 4px;
  font-size: 22px;
  color: #1a1a2e;
}

.header-meta {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.profile-body {
  border-radius: 14px;
  min-height: 480px;
}

.profile-tabs { min-height: 440px; }
.profile-tabs :deep(.el-tabs__item) { font-size: 14px; height: 44px; line-height: 44px; }
.profile-tabs :deep(.el-tabs__item.is-active) { color: var(--color-primary); font-weight: 600; }

.tab-content { padding: 0 8px; }

.tab-title {
  margin: 0 0 20px;
  font-size: 18px;
  color: #1a1a2e;
  font-weight: 600;
  padding-left: 12px;
  border-left: 3px solid var(--color-primary);
}

.profile-form { max-width: 480px; }

.save-btn { border-radius: 8px; }

.skill-input-row { display: flex; gap: 12px; margin-bottom: 20px; align-items: flex-start; }
.skill-input { flex: 1; }
.skill-level-select { width: 120px; flex-shrink: 0; }

.skills-list { display: flex; flex-direction: column; gap: 8px; }

.skill-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: #f5f7fa; border-radius: 8px; transition: background 0.2s;
}
.skill-item:hover { background: #ecf5ff; }

.skill-info { display: flex; align-items: center; gap: 12px; }
.skill-name { font-size: 15px; color: #1a1a2e; font-weight: 500; }

.empty-hint { text-align: center; padding: 40px 0; color: #909399; font-size: 14px; }

.chart-container { height: 320px; margin-bottom: 16px; }
.chart { width: 100%; height: 100%; }

.stats-summary { margin-top: 24px; }

.summary-card { text-align: center; border-radius: 10px; margin-bottom: 12px; }
.summary-value { font-size: 28px; font-weight: 700; color: var(--color-primary); }
.summary-label { font-size: 13px; color: #909399; margin-top: 4px; }

.badges-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 20px; }

.badge-item { text-align: center; padding: 20px 12px; border-radius: 12px; background: #fafafa; border: 1px solid #ebeef5; transition: all 0.2s; }
.badge-item:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }

.badge-icon-wrapper { width: 64px; height: 64px; margin: 0 auto 12px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }

.badge-name { font-size: 14px; font-weight: 600; color: #1a1a2e; margin-bottom: 4px; }
.badge-desc { font-size: 12px; color: #909399; line-height: 1.4; margin-bottom: 4px; }
.badge-date { font-size: 11px; color: #c0c4cc; margin-top: 4px; }
</style>
