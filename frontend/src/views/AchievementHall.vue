<template>
  <div class="achievement-hall-page">
    <div class="page-header-bar">
      <div>
        <h2>成果展厅</h2>
        <p class="page-desc">展示你和团队的竞赛成果与项目荣誉</p>
      </div>
    </div>

    <!-- Filter -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" size="default">
        <el-form-item label="成果类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width:140px">
            <el-option label="论文" value="PAPER" />
            <el-option label="专利" value="PATENT" />
            <el-option label="竞赛获奖" value="AWARD" />
            <el-option label="软件著作" value="SOFTWARE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖级别">
          <el-select v-model="filters.awardLevel" placeholder="全部" clearable style="width:140px">
            <el-option label="国家级" value="NATIONAL" />
            <el-option label="省级" value="PROVINCIAL" />
            <el-option label="校级" value="SCHOOL" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Achievement Grid -->
    <div v-loading="loading">
      <el-empty v-if="!loading && achievements.length === 0" description="暂无成果" />
      <div v-else class="achievement-grid">
        <el-card v-for="item in achievements" :key="item.id" class="achievement-card" shadow="hover" @click="showDetail(item)">
          <div class="card-type">
            <el-tag size="small" effect="dark" :type="typeTagType(item.type)">{{ typeLabel(item.type) }}</el-tag>
            <el-tag v-if="item.awardLevel" size="small" :type="awardLevelType(item.awardLevel)">{{ awardLevelLabel(item.awardLevel) }}</el-tag>
          </div>
          <div class="card-title">{{ item.title }}</div>
          <div class="card-team" v-if="item.team?.name">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
            {{ item.team.name }}
          </div>
          <div class="card-footer">
            <span><el-icon><View /></el-icon> {{ item.viewCount || 0 }}</span>
            <span @click.stop="handleLike(item)" class="like-btn" :class="{ liked: item.isLiked }">
              <el-icon><ThumbsUp /></el-icon> {{ item.likeCount || 0 }}
            </span>
          </div>
        </el-card>
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="成果详情" width="600px" class="detail-dialog">
      <template v-if="detailData.id">
        <div class="detail-section"><span class="detail-label">成果名称</span><span class="detail-value">{{ detailData.title }}</span></div>
        <div class="detail-section"><span class="detail-label">成果类型</span><span class="detail-value">{{ typeLabel(detailData.type) }}</span></div>
        <div class="detail-section" v-if="detailData.awardLevel"><span class="detail-label">获奖级别</span><span class="detail-value">{{ awardLevelLabel(detailData.awardLevel) }}</span></div>
        <div class="detail-section" v-if="detailData.team?.name"><span class="detail-label">所属团队</span><span class="detail-value">{{ detailData.team.name }}</span></div>
        <div class="detail-section" v-if="detailData.members?.length">
          <span class="detail-label">团队成员</span>
          <span class="detail-value">
            <el-tag v-for="m in detailData.members" :key="m.id || m" size="small" style="margin-right:4px;margin-bottom:4px">{{ m.name || m.username || m }}</el-tag>
          </span>
        </div>
        <div class="detail-section" v-if="detailData.description"><span class="detail-label">成果描述</span><p class="detail-desc">{{ detailData.description }}</p></div>
        <div class="detail-section" v-if="detailData.publishDate"><span class="detail-label">发布时间</span><span class="detail-value">{{ dayjs(detailData.publishDate).format('YYYY-MM-DD') }}</span></div>
      </template>
      <el-empty v-else description="暂无详情" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { listAchievements, getAchievement, likeAchievement } from '@/api/achievement'

const loading = ref(false)
const achievements = ref<any[]>([])
const detailVisible = ref(false)
const detailData = ref<any>({})
const filters = ref({ type: '', awardLevel: '' })

function typeLabel(t: string) { const map: Record<string, string> = { PAPER: '论文', PATENT: '专利', AWARD: '竞赛获奖', SOFTWARE: '软件著作', OTHER: '其他' }; return map[t] || t }
function typeTagType(t: string) { const map: Record<string, string> = { PAPER: 'primary', PATENT: 'success', AWARD: 'danger', SOFTWARE: 'warning', OTHER: 'info' }; return map[t] || 'info' }
function awardLevelLabel(l: string) { if (l === 'NATIONAL') return '国家级'; if (l === 'PROVINCIAL') return '省级'; if (l === 'SCHOOL') return '校级'; return l }
function awardLevelType(l: string) { if (l === 'NATIONAL') return 'danger'; if (l === 'PROVINCIAL') return 'warning'; return 'primary' }

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (filters.value.type) params.type = filters.value.type
    if (filters.value.awardLevel) params.awardLevel = filters.value.awardLevel
    const res = await listAchievements(params)
    // 后端返回分页对象 { records: [...] }
    achievements.value = res.data?.records || res.data || []
  } catch (_) {
    // 静默处理，可能是没有数据
    achievements.value = []
  }
  finally { loading.value = false }
}

function resetFilters() { filters.value = { type: '', awardLevel: '' }; loadData() }

async function showDetail(item: any) { detailVisible.value = true; detailData.value = {}; try { const res = await getAchievement(item.id); detailData.value = res.data || {} } catch (_) { detailData.value = {} } }

async function handleLike(item: any) {
  try { await likeAchievement(item.id); item.isLiked = !item.isLiked; item.likeCount = (item.likeCount || 0) + (item.isLiked ? 1 : -1); ElMessage.success(item.isLiked ? '点赞成功' : '已取消点赞') } catch (_) {}
}
</script>

<style scoped>
.achievement-hall-page { max-width: 1200px; margin: 0 auto; }

.page-header-bar {
  background: var(--gradient-primary);
  border-radius: 16px; padding: 24px 32px;
  margin-bottom: 20px; color: #fff;
}
.page-header-bar h2 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
.page-desc { margin: 0; font-size: 14px; opacity: 0.8; }

.filter-card { border-radius: 14px; margin-bottom: 16px; }

.achievement-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }

.achievement-card { cursor: pointer; transition: all 0.25s; border-radius: 12px; }
.achievement-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }

.card-type { display: flex; gap: 6px; margin-bottom: 10px; }

.card-title { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }

.card-team { font-size: 13px; color: #606266; margin-bottom: 10px; display: flex; align-items: center; gap: 4px; }

.card-footer { display: flex; justify-content: space-between; font-size: 12px; color: #909399; padding-top: 10px; border-top: 1px solid #f0f2f5; }
.card-footer span { display: flex; align-items: center; gap: 4px; }

.like-btn { cursor: pointer; transition: color 0.2s; }
.like-btn:hover { color: var(--color-primary); }
.like-btn.liked { color: var(--color-primary); }

.detail-dialog :deep(.el-dialog__body) { padding: 24px 32px; }
.detail-section { margin-bottom: 16px; display: flex; flex-direction: column; }
.detail-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.detail-value { font-size: 14px; color: #1a1a2e; }
.detail-desc { font-size: 14px; color: #606266; line-height: 1.6; margin: 0; }
</style>
