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
              <div v-for="badge in badges" :key="badge.id" class="badge-item" :class="{ 'badge-locked': !badge.unlocked }">
                <div class="badge-icon-wrapper" :class="{ 'badge-icon-locked': !badge.unlocked }" :style="{ background: badge.unlocked ? iconColor(badge.icon) + '20' : '#f0f2f5' }">
                  <el-icon :size="36" :color="badge.unlocked ? iconColor(badge.icon) : '#c0c4cc'">
                    <component :is="badgeIcon(badge.icon)" />
                  </el-icon>
                </div>
                <div class="badge-name">{{ badge.name }}</div>
                <div class="badge-desc">{{ badge.description || '' }}</div>
                <div class="badge-date">
                  <el-tag :type="badge.unlocked ? 'success' : 'info'" size="small" effect="plain">
                    {{ badge.unlocked ? '已解锁' : '未解锁' }}
                  </el-tag>
                  <el-button v-if="badge.unlocked" size="small" type="warning" link @click="viewBadgeCertificate(badge)">
                    查看奖状
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 5: My Certificates -->
        <el-tab-pane label="我的奖状" name="certificates">
          <div class="tab-content">
            <h3 class="tab-title">我的奖状</h3>
            <div v-if="userCertificates.length === 0" class="empty-hint">暂未获得奖状，解锁徽章后将自动生成</div>
            <div v-else class="certificates-grid">
              <el-card v-for="cert in userCertificates" :key="cert.id" shadow="hover" class="cert-card">
                <div class="cert-preview" @click="previewCertificate(cert)">
                  <img :src="cert.imagePath" alt="奖状" class="cert-thumb" />
                  <div class="cert-overlay">
                    <el-icon :size="32"><ZoomIn /></el-icon>
                    <span>点击查看大图</span>
                  </div>
                </div>
                <div class="cert-info">
                  <div class="cert-badge-name">{{ cert.badgeName }}</div>
                  <div class="cert-date">{{ formatDate(cert.awardedAt) }}</div>
                </div>
              </el-card>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Certificate Preview Dialog -->
    <el-dialog v-model="certDialogVisible" title="奖状" width="950px" destroy-on-close center>
      <div class="cert-full-preview" v-if="certPreviewUrl">
        <img :src="certPreviewUrl" alt="奖状大图" style="width:100%; border-radius:8px; box-shadow:0 4px 20px rgba(0,0,0,0.15);" />
      </div>
      <template #footer>
        <el-button @click="certDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- New Certificate Popup Notification -->
    <el-dialog v-model="newCertPopupVisible" title="🎉 恭喜获得新奖状！" width="750px" destroy-on-close center
      :close-on-click-modal="false" :close-on-press-escape="false">
      <div class="new-cert-content" v-if="newCertData">
        <div class="new-cert-congrats">
          <p class="congrats-text">你在平台上的出色表现为你赢得了荣誉！</p>
        </div>
        <div class="new-cert-image-wrapper">
          <img :src="newCertData.imagePath" alt="奖状" style="width:100%; border-radius:8px; box-shadow:0 4px 20px rgba(0,0,0,0.15);" />
        </div>
      </div>
      <template #footer>
        <el-button type="primary" size="large" @click="newCertPopupVisible = false">太棒了！</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getProfile, updateProfile, addSkill, deleteSkill, uploadAvatar, changePassword } from '@/api/user'
import { getStatistics, getSkillRadar, getBadges } from '@/api/dashboard'
import { checkNewCertificates, getUserCertificates } from '@/api/certificate'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Delete, Trophy, Camera, Lock, Medal, Star, Aim, Stamp, Flag, Connection, Monitor, DataLine, Collection, Promotion, ZoomIn } from '@element-plus/icons-vue'

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

/** 根据徽章icon字符串返回对应的Element Plus图标组件 */
function badgeIcon(icon: string) {
  const map: Record<string, any> = {
    'badge_newbie': Trophy,
    'badge_task': Aim,
    'badge_task_master': Medal,
    'badge_team': Connection,
    'badge_social': Star,
    'badge_skill': Monitor,
    'badge_skill_master': DataLine,
    'badge_achievement': Collection,
    'badge_achievement_senior': Stamp,
    'badge_competition': Promotion,
    'badge_honor': Medal,
    'badge_recruit': Flag,
    'badge_project': DataLine,
    'badge_experience': Star,
    'badge_all_rounder': Trophy,
  }
  return map[icon] || Trophy
}

/** 根据徽章icon返回主题色 */
function iconColor(icon: string) {
  const map: Record<string, string> = {
    'badge_newbie': '#67c23a',
    'badge_task': '#409eff',
    'badge_task_master': '#6366f1',
    'badge_team': '#e6a23c',
    'badge_social': '#f39c12',
    'badge_skill': '#909399',
    'badge_skill_master': '#f56c6c',
    'badge_achievement': '#67c23a',
    'badge_achievement_senior': '#2ecc71',
    'badge_competition': '#e04040',
    'badge_honor': '#ffd700',
    'badge_recruit': '#409eff',
    'badge_project': '#e6a23c',
    'badge_experience': '#3498db',
    'badge_all_rounder': '#8b5cf6',
  }
  return map[icon] || '#4a6b7e'
}

const radarData = ref<any[]>([])
const statsLabels = ref<string[]>([])
const statsValues = ref<number[]>([])
const statSummary = ref<any[]>([])
const badges = ref<any[]>([])

// Certificate
const userCertificates = ref<any[]>([])
const certDialogVisible = ref(false)
const certPreviewUrl = ref('')
const newCertPopupVisible = ref(false)
const newCertData = ref<any>(null)

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
    // 同步刷新 store 中的用户信息，确保顶部栏头像也更新
    await userStore.fetchProfile()
    ElMessage.success('头像更新成功')
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
    // 检查新证书
    await checkAndShowNewCertificates()
  } catch (_) {}
}

/** 检查并弹出新证书 */
async function checkAndShowNewCertificates() {
  try {
    const res = await checkNewCertificates()
    const data = res.data || {}
    const newCerts = data.newCertificates || []
    if (newCerts.length > 0) {
      // 只显示第一个新证书的弹窗
      newCertData.value = newCerts[0]
      newCertPopupVisible.value = true
    }
  } catch (_) {}
}

/** 查看徽章对应的奖状 */
async function viewBadgeCertificate(badge: any) {
  try {
    const res = await getUserCertificates()
    const certs = res.data || []
    const cert = certs.find((c: any) => c.badgeId === badge.id)
    if (cert) {
      certPreviewUrl.value = cert.imagePath
      certDialogVisible.value = true
    } else {
      ElMessage.info('该徽章的奖状尚未生成，请稍后刷新或联系管理员同步')
    }
  } catch (_) {
    ElMessage.error('获取奖状失败')
  }
}

/** 预览证书图片 */
function previewCertificate(cert: any) {
  certPreviewUrl.value = cert.imagePath
  certDialogVisible.value = true
}

/** 加载用户证书列表 */
async function loadUserCertificates() {
  try {
    const res = await getUserCertificates()
    userCertificates.value = res.data || []
  } catch (_) {
    userCertificates.value = []
  }
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

onMounted(() => { fetchProfileData(); fetchStatsData(); loadUserCertificates() })

// 监听tab切换，加载对应数据
watch(activeTab, (tab) => {
  if (tab === 'certificates') loadUserCertificates()
  if (tab === 'badges') { fetchStatsData() }
})
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
.badge-locked { opacity: 0.55; }
.badge-locked:hover { transform: none; box-shadow: none; }

.badge-icon-wrapper {
  width: 64px; height: 64px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: var(--color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}
.badge-icon-locked { background: #f0f2f5; }

.badge-name { font-size: 14px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 4px; }
.badge-desc { font-size: 12px; color: var(--color-text-muted); line-height: 1.4; margin-bottom: 4px; }
.badge-date { font-size: 11px; color: var(--color-text-placeholder); margin-top: 4px; display: flex; flex-direction: column; align-items: center; gap: 6px; }

/* Certificate styles */
.certificates-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }

.cert-card { border-radius: var(--radius-sm); cursor: pointer; transition: transform 0.2s; }
.cert-card:hover { transform: translateY(-4px); }

.cert-preview { position: relative; border-radius: 4px; overflow: hidden; }
.cert-thumb { width: 100%; height: 140px; object-fit: cover; border-radius: 4px; }
.cert-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 8px; color: #fff; opacity: 0;
  transition: opacity 0.2s;
  font-size: 14px;
}
.cert-preview:hover .cert-overlay { opacity: 1; }

.cert-info { padding: 10px 0 0; text-align: center; }
.cert-badge-name { font-size: 14px; font-weight: 600; color: var(--color-text-primary); }
.cert-date { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; }

/* New cert popup */
.new-cert-content { text-align: center; }
.new-cert-congrats { margin-bottom: 16px; }
.congrats-text { font-size: 18px; color: var(--color-primary); font-weight: 600; margin: 0; }
.new-cert-image-wrapper { border-radius: 8px; overflow: hidden; }

.cert-full-preview { text-align: center; }
</style>
