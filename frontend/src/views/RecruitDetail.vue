<template>
  <div class="recruit-detail-container" v-loading="loading">
    <template v-if="recruit">
      <!-- Back -->
      <div class="back-nav">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回招募列表
        </el-button>
      </div>

      <el-row :gutter="24">
        <!-- Main Info -->
        <el-col :xs="24" :lg="16">
          <el-card shadow="never" class="main-card">
            <div class="detail-header">
              <div class="detail-header-left">
                <h1 class="detail-title">{{ recruit.title }}</h1>
                <div class="header-tags">
                  <el-tag :type="statusType(recruit.status)" effect="dark" size="small">{{ statusLabel(recruit.status) }}</el-tag>
                  <el-tag :type="projectTypeType(recruit.projectType)" effect="plain" size="small">{{ recruit.projectType }}</el-tag>
                </div>
              </div>
            </div>

            <!-- Meta Grid -->
            <div class="meta-grid">
              <div class="meta-item">
                <div class="meta-label">发布人</div>
                <div class="meta-value">
                  <el-avatar :size="24" style="vertical-align:middle;margin-right:6px;background:var(--gradient-primary)">
                    {{ recruit.publisherName?.charAt(0) || '?' }}
                  </el-avatar>
                  {{ recruit.publisherName || '未知' }}
                </div>
              </div>
              <div class="meta-item">
                <div class="meta-label">截止日期</div>
                <div class="meta-value">{{ recruit.deadline ? dayjs(recruit.deadline).format('YYYY-MM-DD') : '无' }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">发布时间</div>
                <div class="meta-value">{{ dayjs(recruit.createdAt).format('YYYY-MM-DD HH:mm') }}</div>
              </div>
              <div class="meta-item">
                <div class="meta-label">浏览次数</div>
                <div class="meta-value">{{ recruit.viewCount || 0 }} 次</div>
              </div>
            </div>

            <!-- Member Progress -->
            <div class="detail-section">
              <h3 class="section-title">成员进度</h3>
              <div class="member-progress-bar">
                <div class="progress-header">
                  <span>{{ recruit.currentMembers || 0 }} / {{ recruit.maxMembers }} 人</span>
                  <span :class="memberStatusClass">{{ memberStatusText }}</span>
                </div>
                <el-progress
                  :percentage="memberPercent"
                  :stroke-width="10"
                  :status="memberPercent >= 100 ? 'exception' : ''"
                  :format="() => ''"
                />
              </div>
            </div>

            <!-- Skills -->
            <div class="detail-section" v-if="recruit.requiredSkills?.length">
              <h3 class="section-title">所需技能</h3>
              <div class="skills-wrapper">
                <el-tag v-for="skill in recruit.requiredSkills" :key="skill" type="info" class="skill-tag">
                  {{ skill }}
                </el-tag>
              </div>
            </div>

            <!-- Description -->
            <div class="detail-section">
              <h3 class="section-title">项目描述</h3>
              <div class="detail-description">{{ recruit.description }}</div>
            </div>

            <!-- Apply -->
            <div class="detail-actions" v-if="recruit.status === 'OPEN'">
              <el-button
                type="primary"
                size="large"
                class="apply-btn"
                :loading="applying"
                :disabled="recruit.currentMembers >= recruit.maxMembers"
                @click="handleApply"
              >
                <el-icon><Plus /></el-icon>
                {{ recruit.currentMembers >= recruit.maxMembers ? '已满员' : '申请加入' }}
              </el-button>
              <p class="apply-hint" v-if="recruit.currentMembers < recruit.maxMembers">提交申请后，等待发布人审核</p>
            </div>
            <div class="detail-actions closed-actions" v-else>
              <el-tag type="info" size="large" effect="plain">该招募已{{ statusLabel(recruit.status) }}</el-tag>
            </div>
          </el-card>
        </el-col>

        <!-- Sidebar -->
        <el-col :xs="24" :lg="8">
          <el-card shadow="never" class="sidebar-card">
            <template #header>
              <div class="sidebar-header">
                <div class="sidebar-header-left">
                  <div class="sidebar-icon" style="background:linear-gradient(135deg,#667eea,#764ba2)">
                    <el-icon :size="16" color="#fff"><Lightning /></el-icon>
                  </div>
                  <span>智能匹配</span>
                </div>
                <el-button text type="primary" size="small" :loading="matchingLoading" @click="loadMatches">
                  刷新
                </el-button>
              </div>
            </template>

            <div v-if="matchingLoading" class="match-loading">
              <el-icon class="is-loading" :size="28" color="#667eea"><Loading /></el-icon>
              <p>正在智能匹配...</p>
            </div>

            <div v-else-if="matchResults.length === 0" class="match-empty">
              <el-icon :size="40" color="#d0d5dd"><UserFilled /></el-icon>
              <p>暂无匹配结果</p>
            </div>

            <div v-else class="match-list">
              <div v-for="item in matchResults" :key="item.userId || item.id" class="match-card">
                <div class="match-user">
                  <el-avatar :size="40" class="match-avatar">{{ item.username?.charAt(0) || '?' }}</el-avatar>
                  <div class="match-user-info">
                    <span class="match-username">{{ item.username || '未知' }}</span>
                    <span class="match-desc">{{ item.major || '' }}</span>
                  </div>
                </div>
                <div class="match-score-circle">
                  <svg viewBox="0 0 36 36" class="score-svg">
                    <path class="score-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                    <path class="score-fill" :stroke-dasharray="`${Math.round((item.similarity || 0) * 100)}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"/>
                    <text x="18" y="21" class="score-text">{{ Math.round((item.similarity || 0) * 100) }}%</text>
                  </svg>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-else-if="!loading" description="招募信息不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { getRecruit, applyRecruit, matchRecruit } from '@/api/recruit'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const recruit = ref<any>(null)
const applying = ref(false)
const matchingLoading = ref(false)
const matchResults = ref<any[]>([])

const memberPercent = computed(() => {
  if (!recruit.value) return 0
  return Math.min(Math.round(((recruit.value.currentMembers || 0) / (recruit.value.maxMembers || 1)) * 100), 100)
})

const memberStatusText = computed(() => {
  if (!recruit.value) return ''
  if (recruit.value.currentMembers >= recruit.value.maxMembers) return '已满员'
  return `剩余 ${recruit.value.maxMembers - recruit.value.currentMembers} 个名额`
})

const memberStatusClass = computed(() => {
  if (!recruit.value) return ''
  return recruit.value.currentMembers >= recruit.value.maxMembers ? 'status-full' : 'status-open'
})

async function fetchDetail() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getRecruit(id)
    recruit.value = res.data
  } catch (_) {
    recruit.value = null
  } finally {
    loading.value = false
  }
}

async function handleApply() {
  try {
    await ElMessageBox.confirm('确认申请加入该项目？', '申请确认', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'info',
    })
  } catch { return }

  applying.value = true
  try {
    const id = Number(route.params.id)
    await applyRecruit(id)
    ElMessage.success('申请已提交，请等待审核')
  } catch (_) {} finally {
    applying.value = false
  }
}

async function loadMatches() {
  const id = Number(route.params.id)
  if (!id) return
  matchingLoading.value = true
  try {
    const res = await matchRecruit(id)
    matchResults.value = Array.isArray(res.data) ? res.data : []
  } catch (_) {
    matchResults.value = []
  } finally {
    matchingLoading.value = false
  }
}

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

onMounted(() => { fetchDetail(); loadMatches() })
</script>

<style scoped>
.recruit-detail-container { max-width: 1100px; margin: 0 auto; }

.back-nav { margin-bottom: 16px; }

/* ===== Main Card ===== */
.main-card {
  border-radius: 14px;
  margin-bottom: 20px;
}

.detail-header { margin-bottom: 20px; }

.detail-header-left { display: flex; flex-direction: column; gap: 8px; }

.detail-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.header-tags { display: flex; gap: 8px; }

/* ===== Meta Grid ===== */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px;
  background: #f8f9fb;
  border-radius: 10px;
  margin-bottom: 24px;
}

.meta-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.meta-value {
  font-size: 14px;
  color: #1a1a2e;
  font-weight: 500;
}

/* ===== Sections ===== */
.detail-section { margin-bottom: 24px; }

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 12px;
  padding-left: 12px;
  border-left: 3px solid var(--color-primary);
}

.member-progress-bar {
  padding: 4px 0;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.status-full { color: #f56c6c; font-weight: 600; }
.status-open { color: #67c23a; }

.skills-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag { margin: 0 !important; }

.detail-description {
  font-size: 14px;
  color: #4a4a6a;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* ===== Apply ===== */
.detail-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e8ecf1;
}

.apply-btn {
  border-radius: 10px;
  font-weight: 600;
  height: 46px;
  padding: 0 32px;
  font-size: 16px;
  background: var(--gradient-primary);
  border: none;
}
.apply-btn:hover {
  box-shadow: 0 4px 20px rgba(102,126,234,0.4);
  transform: translateY(-1px);
}

.apply-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}

.closed-actions { text-align: center; }

/* ===== Sidebar ===== */
.sidebar-card {
  border-radius: 14px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.sidebar-icon {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.match-loading, .match-empty {
  text-align: center;
  padding: 32px 0;
  color: #909399;
}
.match-loading p, .match-empty p { margin-top: 8px; font-size: 13px; }

.match-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.match-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #f0f2f5;
  border-radius: 10px;
  transition: all 0.2s;
}
.match-card:hover {
  border-color: #e0e4ed;
  background: #f8f9fb;
}

.match-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.match-avatar {
  background: var(--gradient-primary) !important;
  flex-shrink: 0;
}

.match-user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.match-username { font-size: 14px; font-weight: 500; color: #1a1a2e; }
.match-desc { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.match-score-circle { margin-left: 12px; flex-shrink: 0; }

.score-svg { width: 44px; height: 44px; transform: rotate(-90deg); }
.score-bg { fill: none; stroke: #f0f2f5; stroke-width: 3; }
.score-fill { fill: none; stroke: var(--color-primary); stroke-width: 3; stroke-linecap: round; }
.score-text { transform: rotate(90deg); transform-origin: 18px 18px; fill: #667eea; font-size: 8px; font-weight: 700; text-anchor: middle; }
</style>
