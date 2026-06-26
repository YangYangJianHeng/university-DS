<template>
  <div v-loading="loading" class="competition-detail-page">
    <div class="back-nav">
      <el-button text @click="router.push('/competitions')">
        <el-icon><ArrowLeft /></el-icon> 返回竞赛列表
      </el-button>
    </div>

    <el-card shadow="never" class="detail-card" v-if="competition.id">
      <div class="detail-header">
        <div class="detail-header-left">
          <div class="comp-level-badge" :class="'level-' + (competition.level || 'school').toLowerCase()">
            {{ levelLabel(competition.level) }}
          </div>
          <div class="header-text">
            <h2>{{ competition.name }}</h2>
            <div class="header-tags">
              <el-tag :type="statusType(competition.status)" size="small">{{ statusLabel(competition.status) }}</el-tag>
              <el-tag type="info" size="small" effect="plain">{{ categoryLabel(competition.category) }}</el-tag>
            </div>
          </div>
        </div>
        <el-button
          :type="collected ? 'warning' : 'default'"
          class="collect-btn"
          @click="handleCollect"
          :loading="collectLoading"
        >
          <el-icon><Star /></el-icon>
          {{ collected ? '已收藏' : '收藏' }}
        </el-button>
      </div>

      <!-- Info Grid -->
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">主办单位</span>
          <span class="info-value">{{ competition.organizer || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">竞赛级别</span>
          <el-tag :type="levelType(competition.level)" size="small">{{ levelLabel(competition.level) }}</el-tag>
        </div>
        <div class="info-item">
          <span class="info-label">报名时间</span>
          <span class="info-value">
            {{ competition.registerStart ? dayjs(competition.registerStart).format('YYYY-MM-DD') : '--' }}
            ~ {{ competition.registerEnd ? dayjs(competition.registerEnd).format('YYYY-MM-DD') : '--' }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">竞赛时间</span>
          <span class="info-value">
            {{ competition.eventStart ? dayjs(competition.eventStart).format('YYYY-MM-DD') : '--' }}
            ~ {{ competition.eventEnd ? dayjs(competition.eventEnd).format('YYYY-MM-DD') : '--' }}
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">浏览量</span>
          <span class="info-value">{{ competition.viewCount || 0 }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">收藏数</span>
          <span class="info-value">{{ competition.collectCount || 0 }}</span>
        </div>
      </div>

      <el-divider />

      <div class="section">
        <h3 class="section-title">竞赛简介</h3>
        <p class="section-content">{{ competition.description || '暂无简介' }}</p>
      </div>

      <div class="section" v-if="competition.officialWebsite">
        <h3 class="section-title">官方网站</h3>
        <el-link :href="competition.officialWebsite" target="_blank" type="primary" :underline="false">
          <el-icon><Link /></el-icon> {{ competition.officialWebsite }}
        </el-link>
      </div>
    </el-card>

    <el-empty v-if="!loading && !competition.id" description="竞赛不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getCompetition, collectCompetition } from '@/api/competition'

const router = useRouter()
const route = useRoute()
const compId = Number(route.params.id)

const loading = ref(false)
const competition = ref<any>({})
const collected = ref(false)
const collectLoading = ref(false)

function levelType(level: string) { if (level === 'NATIONAL') return 'danger'; if (level === 'PROVINCIAL') return 'warning'; return 'primary' }
function levelLabel(level: string) { if (level === 'NATIONAL') return '国家级'; if (level === 'PROVINCIAL') return '省级'; if (level === 'SCHOOL') return '校级'; return level }
function statusType(s: string) { if (s === 'REGISTERING') return 'success'; if (s === 'ONGOING') return 'primary'; return 'info' }
function statusLabel(s: string) { if (s === 'REGISTERING') return '报名中'; if (s === 'ONGOING') return '进行中'; if (s === 'ENDED') return '已结束'; return s }
function categoryLabel(c: string) { if (c === 'ACADEMIC') return '学科竞赛'; if (c === 'INNOVATION') return '创新创业'; if (c === 'SPORTS') return '文体竞赛'; return c || '其他' }

onMounted(async () => await loadCompetition())

async function loadCompetition() {
  if (isNaN(compId) || compId <= 0) {
    ElMessage.error('无效的竞赛ID')
    router.replace('/competitions')
    return
  }
  loading.value = true
  try { const res = await getCompetition(compId); competition.value = res.data || {}; collected.value = !!res.data?.isCollected }
  catch (_) { competition.value = {} }
  finally { loading.value = false }
}

async function handleCollect() {
  collectLoading.value = true
  try {
    await collectCompetition(compId)
    collected.value = !collected.value
    if (collected.value) { competition.value.collectCount = (competition.value.collectCount || 0) + 1; ElMessage.success('收藏成功') }
    else { competition.value.collectCount = Math.max(0, (competition.value.collectCount || 1) - 1); ElMessage.success('已取消收藏') }
  } catch (_) {} finally { collectLoading.value = false }
}
</script>

<style scoped>
.competition-detail-page { max-width: 900px; margin: 0 auto; }

.back-nav { margin-bottom: 16px; }

.detail-card { border-radius: 14px; padding: 4px; }

.detail-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 24px; }

.detail-header-left { display: flex; gap: 16px; }

.comp-level-badge {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; color: #fff;
  flex-shrink: 0; text-align: center; line-height: 1.2;
}
.comp-level-badge.level-national { background: linear-gradient(135deg, #f56c6c, #fa709a); }
.comp-level-badge.level-provincial { background: linear-gradient(135deg, #e6a23c, #fee140); }
.comp-level-badge.level-school { background: linear-gradient(135deg, #409eff, #79bbff); }

.header-text { flex: 1; }
.header-text h2 { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0 0 6px; }
.header-tags { display: flex; gap: 6px; }

.collect-btn { border-radius: 8px; flex-shrink: 0; }

.info-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; margin-bottom: 8px; }

.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: #909399; }
.info-value { font-size: 14px; color: #1a1a2e; }

.section { margin-bottom: 20px; }
.section-title { font-size: 16px; font-weight: 600; color: #1a1a2e; margin-bottom: 8px; padding-left: 12px; border-left: 3px solid var(--color-primary); }
.section-content { font-size: 14px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
</style>
