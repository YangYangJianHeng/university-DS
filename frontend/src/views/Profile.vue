<template>
  <div class="profile-page">
    <!-- User Header with avatar upload -->
    <el-card class="profile-header" shadow="never">
      <div class="header-content">
        <div class="avatar-section">
          <el-avatar :size="80" :src="userInfo.avatar" class="user-avatar">
            {{ userInfo.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <div class="avatar-overlay" @click="triggerUpload">
            <el-icon :size="20"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            style="display:none"
            @change="handleAvatarChange"
          />
        </div>
        <div class="header-info">
          <h2>{{ userInfo.username || '未知用户' }}</h2>
          <p class="header-meta">
            <span v-if="userInfo.major">{{ userInfo.major }}</span>
            <span v-if="userInfo.grade"> | {{ userInfo.grade }}</span>
            <span v-if="userInfo.studentId"> | {{ userInfo.studentId }}</span>
          </p>
          <p class="header-bio" v-if="userInfo.bio">{{ userInfo.bio }}</p>
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
              <el-form-item label="手机号">
                <el-input v-model="profileForm.phone" maxlength="11" disabled />
              </el-form-item>
              <el-form-item label="学号">
                <el-input v-model="profileForm.studentId" />
              </el-form-item>
              <el-form-item label="专业">
                <el-input v-model="profileForm.major" />
              </el-form-item>
              <el-form-item label="年级">
                <el-select v-model="profileForm.grade" style="width:100%">
                  <el-option label="大一" value="大一" /><el-option label="大二" value="大二" /><el-option label="大三" value="大三" /><el-option label="大四" value="大四" />
                  <el-option label="研一" value="研一" /><el-option label="研二" value="研二" /><el-option label="研三" value="研三" />
                </el-select>
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" />
              </el-form-item>
              <el-form-item label="个人简介">
                <el-input v-model="profileForm.bio" type="textarea" :rows="3" maxlength="200" show-word-limit />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="handleUpdateProfile">保存修改</el-button>
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

        <!-- Tab 3: Change Password -->
        <el-tab-pane label="修改密码" name="password">
          <div class="tab-content">
            <h3 class="tab-title">修改密码</h3>
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" size="large" class="profile-form">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" maxlength="32" />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-32位新密码" maxlength="32" />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" maxlength="32" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
                <el-button @click="handleResetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Tab 4: Stats -->
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
                <div class="badge-icon-wrapper">
                  <el-icon :size="36" :color="'#4a6b7e'"><Trophy /></el-icon>
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
import { getProfile, updateProfile, addSkill, deleteSkill, uploadAvatar, changePassword } from '@/api/user'
import { getStatistics, getSkillRadar, getBadges } from '@/api/dashboard'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Delete, Trophy, Camera, Lock } from '@element-plus/icons-vue'

import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart, BarChart } from 'echarts/charts'
import { RadarComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, RadarChart, BarChart, RadarComponent, TooltipComponent, LegendComponent, GridComponent])

const userStore = useUserStore()
const profileFormRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement>()
const activeTab = ref('info')
const userInfo = reactive<any>({})
const saving = ref(false)

const profileForm = reactive({ username: '', phone: '', studentId: '', major: '', grade: '', email: '', bio: '' })
const profileRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 20, message: '长度在 2-20 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

const skills = ref<any[]>([])
const newSkill = reactive({ name: '', level: '熟练' })

// 修改密码
const passwordFormRef = ref<FormInstance>()
const changingPwd = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需要6-32位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

function levelType(level: string): 'info' | 'success' | 'warning' | 'danger' | '' {
  const map: Record<string, 'info' | 'success' | 'warning' | 'danger' | ''> = { '入门': 'info', '熟练': '', '掌握': 'success', '精通': 'warning', '大师': 'danger' }
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
    splitArea: { areaStyle: { color: ['rgba(74,107,126,0.02)', 'rgba(74,107,126,0.05)'] } },
  },
  series: [{ type: 'radar', data: [{ value: radarData.value.map((s: any) => s.value ?? s.level ?? 0), name: '技能水平', areaStyle: { color: 'rgba(74,107,126,0.15)' }, lineStyle: { color: '#4a6b7e', width: 2 }, itemStyle: { color: '#4a6b7e' } }] }],
}))

const statsOption = computed(() => ({
  tooltip: { trigger: 'axis' as const },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  xAxis: { type: 'category' as const, data: statsLabels.value, axisLabel: { rotate: 30, fontSize: 12 } },
  yAxis: { type: 'value' as const },
  series: [{ type: 'bar', data: statsValues.value, barWidth: '50%', itemStyle: { borderRadius: [4, 4, 0, 0], color: '#4a6b7e' } }],
}))

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleAvatarChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // Validate file type and size
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadAvatar(formData)
    const avatarUrl = res.data?.avatar || res.data?.url || res.data
    userInfo.avatar = avatarUrl
    ElMessage.success('头像更新成功')
    userStore.fetchProfile()
  } catch (err: any) {
    ElMessage.error(err?.message || '头像上传失败')
  } finally {
    // Reset input so same file can be re-selected
    target.value = ''
  }
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
    profileForm.bio = data.bio || ''
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
    await updateProfile({ username: profileForm.username, studentId: profileForm.studentId, major: profileForm.major, grade: profileForm.grade, email: profileForm.email, bio: profileForm.bio })
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

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  changingPwd.value = true
  try {
    await changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功，请妥善保管新密码')
    handleResetPasswordForm()
  } finally { changingPwd.value = false }
}

function handleResetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.resetFields()
}

onMounted(() => { fetchProfileData(); fetchStatsData() })
</script>

<style scoped>
.profile-page { max-width: 960px; margin: 0 auto; }

.profile-header {
  margin-bottom: 20px;
  border-radius: var(--radius-md);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 28px;
}

/* Avatar section with upload overlay */
.avatar-section {
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
}

.user-avatar {
  background: var(--color-primary) !important;
  font-size: 30px;
  color: #fff;
  border: 3px solid var(--color-border-light);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity var(--transition-fast);
  color: #fff;
  font-size: 12px;
}
.avatar-section:hover .avatar-overlay {
  opacity: 1;
}

.header-info h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--color-text-primary);
}

.header-meta {
  margin: 0 0 6px;
  font-size: 14px;
  color: var(--color-text-muted);
}

.header-bio {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.profile-body {
  border-radius: var(--radius-md);
  min-height: 480px;
}

.profile-tabs { min-height: 440px; }
.profile-tabs :deep(.el-tabs__item) { font-size: 14px; height: 44px; line-height: 44px; color: var(--color-text-secondary); }
.profile-tabs :deep(.el-tabs__item.is-active) { color: var(--color-primary); font-weight: 600; }
.profile-tabs :deep(.el-tabs__active-bar) { background-color: var(--color-primary); }

.tab-content { padding: 0 8px; }

.tab-title {
  margin: 0 0 20px;
  font-size: 17px;
  color: var(--color-text-primary);
  font-weight: 600;
  padding-left: 12px;
  border-left: 3px solid var(--color-primary);
}

.profile-form { max-width: 480px; }

.save-btn { border-radius: 6px; }

.skill-input-row { display: flex; gap: 12px; margin-bottom: 20px; align-items: flex-start; }
.skill-input { flex: 1; }
.skill-level-select { width: 120px; flex-shrink: 0; }

.skills-list { display: flex; flex-direction: column; gap: 8px; }

.skill-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: var(--color-bg); border-radius: 6px; transition: background 0.2s;
}
.skill-item:hover { background: var(--color-primary-light); }

.skill-info { display: flex; align-items: center; gap: 12px; }
.skill-name { font-size: 14px; color: var(--color-text-primary); font-weight: 500; }

.empty-hint { text-align: center; padding: 40px 0; color: var(--color-text-muted); font-size: 14px; }

.chart-container { height: 320px; margin-bottom: 16px; }
.chart { width: 100%; height: 100%; }

.stats-summary { margin-top: 24px; }

.summary-card { text-align: center; border-radius: var(--radius-sm); margin-bottom: 12px; }
.summary-value { font-size: 28px; font-weight: 700; color: var(--color-primary); }
.summary-label { font-size: 13px; color: var(--color-text-muted); margin-top: 4px; }

.badges-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 20px; }

.badge-item {
  text-align: center;
  padding: 20px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  transition: all 0.2s;
}
.badge-item:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); }

.badge-icon-wrapper {
  width: 64px; height: 64px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-name { font-size: 14px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 4px; }
.badge-desc { font-size: 12px; color: var(--color-text-muted); line-height: 1.4; margin-bottom: 4px; }
.badge-date { font-size: 11px; color: var(--color-text-placeholder); margin-top: 4px; }
</style>
