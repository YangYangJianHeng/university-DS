<template>
  <div class="achievement-hall-page">
    <!-- Hero Banner -->
    <section class="hero-banner">
      <div class="hero-content">
        <div class="hero-badge">
          <el-icon><TrophyBase /></el-icon>
          <span>成果展厅</span>
        </div>
        <h1 class="hero-title">荣耀殿堂</h1>
        <p class="hero-subtitle">展示你和团队的竞赛成果与学术荣誉，记录每一份荣耀时刻</p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-num">{{ stats.total }}</span>
            <span class="hero-stat-label">成果总数</span>
          </div>
          <div class="hero-divider" />
          <div class="hero-stat">
            <span class="hero-stat-num">{{ stats.featured }}</span>
            <span class="hero-stat-label">精选成果</span>
          </div>
          <div class="hero-divider" />
          <div class="hero-stat">
            <span class="hero-stat-num">{{ stats.views }}</span>
            <span class="hero-stat-label">总浏览量</span>
          </div>
        </div>
      </div>
      <div class="hero-decoration">
        <div class="deco-circle c1" />
        <div class="deco-circle c2" />
        <div class="deco-circle c3" />
      </div>
    </section>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-radio-group v-model="filters.type" size="default" @change="loadData">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="AWARD">🏆 竞赛获奖</el-radio-button>
          <el-radio-button value="PAPER">📄 论文</el-radio-button>
          <el-radio-button value="PATENT">💡 专利</el-radio-button>
          <el-radio-button value="SOFTWARE">💻 软件著作</el-radio-button>
          <el-radio-button value="OTHER">📋 其他</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-right">
        <el-select v-model="filters.awardLevel" placeholder="获奖级别" clearable @change="loadData" class="level-select">
          <el-option label="国家级" value="NATIONAL">
            <span class="level-option"><span class="level-dot national" />国家级</span>
          </el-option>
          <el-option label="省级" value="PROVINCIAL">
            <span class="level-option"><span class="level-dot provincial" />省级</span>
          </el-option>
          <el-option label="校级" value="SCHOOL">
            <span class="level-option"><span class="level-dot school" />校级</span>
          </el-option>
        </el-select>
      </div>
    </div>

    <!-- Achievement Grid -->
    <div v-loading="loading" class="main-content">
      <el-empty v-if="!loading && achievements.length === 0" description="暂无成果展示" :image-size="120">
        <template #image>
          <el-icon :size="80" color="#c0c4cc"><FolderOpened /></el-icon>
        </template>
      </el-empty>
      <div v-else class="achievement-masonry">
        <div
          v-for="(item, idx) in achievements"
          :key="item.id"
          class="achievement-card"
          :class="{ featured: item.isFeatured === 1 }"
          :style="{ animationDelay: idx * 0.05 + 's' }"
          @click="showDetail(item)"
        >
          <!-- Card top accent stripe -->
          <div class="card-accent" :style="{ background: accentColor(item.type) }" />

          <!-- Featured badge -->
          <div v-if="item.isFeatured === 1" class="featured-ribbon">
            <el-icon><StarFilled /></el-icon>
            <span>精选</span>
          </div>

          <!-- Card body -->
          <div class="card-body">
            <div class="card-tags">
              <span class="type-tag" :style="{ background: tagBg(item.type), color: tagColor(item.type) }">
                {{ typeEmoji(item.type) }} {{ typeLabel(item.type) }}
              </span>
              <span v-if="item.awardLevel" class="level-tag" :class="'level-' + item.awardLevel.toLowerCase()">
                {{ awardLevelLabel(item.awardLevel) }}
              </span>
            </div>

            <h3 class="card-title">{{ item.title }}</h3>
            <p class="card-desc" v-if="item.description">{{ item.description }}</p>

            <div class="card-meta-row" v-if="item.team?.name">
              <div class="card-team">
                <el-icon :size="14"><UserFilled /></el-icon>
                <span>{{ item.team.name }}</span>
              </div>
              <div class="card-date" v-if="item.publishDate">
                <el-icon :size="14"><Calendar /></el-icon>
                <span>{{ dayjs(item.publishDate).format('YYYY-MM-DD') }}</span>
              </div>
            </div>

            <div class="card-footer">
              <div class="card-engagement">
                <span class="eng-item" @click.stop="handleLike(item)">
                  <el-icon :size="16" :class="{ liked: item.isLiked }"><ThumbsUp /></el-icon>
                  <span>{{ item.likeCount || 0 }}</span>
                </span>
                <span class="eng-divider">·</span>
                <span class="eng-item">
                  <el-icon :size="16"><View /></el-icon>
                  <span>{{ item.viewCount || 0 }}</span>
                </span>
              </div>
              <div class="card-arrow">
                <el-icon :size="18"><ArrowRight /></el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" width="640px" class="achievement-detail-dialog" :show-close="true" destroy-on-close>
      <template #header>
        <div class="dialog-header-custom" v-if="detailData.id">
          <span class="dialog-type-badge" :style="{ background: tagBg(detailData.type), color: tagColor(detailData.type) }">
            {{ typeEmoji(detailData.type) }} {{ typeLabel(detailData.type) }}
          </span>
          <span v-if="detailData.awardLevel" class="dialog-level-badge" :class="'level-' + (detailData.awardLevel || '').toLowerCase()">
            {{ awardLevelLabel(detailData.awardLevel) }}
          </span>
        </div>
      </template>
      <template v-if="detailData.id">
        <h2 class="dialog-title">{{ detailData.title }}</h2>

        <div class="dialog-meta-grid">
          <div class="meta-item" v-if="detailData.team?.name">
            <div class="meta-icon-box"><el-icon :size="20"><UserFilled /></el-icon></div>
            <div class="meta-text">
              <span class="meta-label">所属团队</span>
              <span class="meta-value">{{ detailData.team.name }}</span>
            </div>
          </div>
          <div class="meta-item" v-if="detailData.publishDate">
            <div class="meta-icon-box"><el-icon :size="20"><Calendar /></el-icon></div>
            <div class="meta-text">
              <span class="meta-label">发布时间</span>
              <span class="meta-value">{{ dayjs(detailData.publishDate).format('YYYY年MM月DD日') }}</span>
            </div>
          </div>
          <div class="meta-item" v-if="detailData.projectId">
            <div class="meta-icon-box"><el-icon :size="20"><FolderOpened /></el-icon></div>
            <div class="meta-text">
              <span class="meta-label">关联项目</span>
              <span class="meta-value">项目 #{{ detailData.projectId }}</span>
            </div>
          </div>
          <div class="meta-item">
            <div class="meta-icon-box"><el-icon :size="20"><View /></el-icon></div>
            <div class="meta-text">
              <span class="meta-label">浏览量</span>
              <span class="meta-value">{{ detailData.viewCount || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="dialog-section" v-if="detailData.members?.length">
          <h4 class="di-section-title">团队成员</h4>
          <div class="di-member-list">
            <el-tag v-for="m in detailData.members" :key="m.id || m" size="default" effect="plain" round class="member-tag">
              {{ m.name || m.username || m }}
            </el-tag>
          </div>
        </div>

        <div class="dialog-section" v-if="detailData.description">
          <h4 class="di-section-title">成果描述</h4>
          <p class="di-description">{{ detailData.description }}</p>
        </div>

        <div class="dialog-section" v-if="detailData.fileUrls">
          <h4 class="di-section-title">附件资料</h4>
          <div class="di-files">
            <el-link v-for="(url, i) in parseFileUrls(detailData.fileUrls)" :key="i" :href="url" target="_blank" type="primary" :underline="false" class="file-link">
              <el-icon><Link /></el-icon> {{ getFileName(url) }}
            </el-link>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无详情" :image-size="80" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { listAchievements, getAchievement, likeAchievement } from '@/api/achievement'

const loading = ref(false)
const achievements = ref<any[]>([])
const detailVisible = ref(false)
const detailData = ref<any>({})
const filters = ref({ type: '', awardLevel: '' })

const stats = computed(() => {
  const list = achievements.value
  return {
    total: list.length,
    featured: list.filter((a: any) => a.isFeatured === 1).length,
    views: list.reduce((sum: number, a: any) => sum + (a.viewCount || 0), 0),
  }
})

function typeLabel(t: string) { const map: Record<string, string> = { PAPER: '论文', PATENT: '专利', AWARD: '竞赛获奖', SOFTWARE: '软件著作', OTHER: '其他' }; return map[t] || t }
function typeEmoji(t: string) { const map: Record<string, string> = { PAPER: '📄', PATENT: '💡', AWARD: '🏆', SOFTWARE: '💻', OTHER: '📋' }; return map[t] || '📋' }
function awardLevelLabel(l: string) { const map: Record<string, string> = { NATIONAL: '国家级', PROVINCIAL: '省级', SCHOOL: '校级' }; return map[l] || l }

// Color schemes per type
function accentColor(t: string) { const map: Record<string, string> = { AWARD: 'linear-gradient(135deg, #f7971e, #ffd200)', PAPER: 'linear-gradient(135deg, #667eea, #764ba2)', PATENT: 'linear-gradient(135deg, #11998e, #38ef7d)', SOFTWARE: 'linear-gradient(135deg, #4facfe, #00f2fe)', OTHER: 'linear-gradient(135deg, #a18cd1, #fbc2eb)' }; return map[t] || 'linear-gradient(135deg, #a18cd1, #fbc2eb)' }
function tagBg(t: string) { const map: Record<string, string> = { AWARD: '#fef3e2', PAPER: '#f0edff', PATENT: '#e6faf2', SOFTWARE: '#e6f4ff', OTHER: '#fdf0f5' }; return map[t] || '#fdf0f5' }
function tagColor(t: string) { const map: Record<string, string> = { AWARD: '#e67e22', PAPER: '#7c3aed', PATENT: '#059669', SOFTWARE: '#2563eb', OTHER: '#db2777' }; return map[t] || '#db2777' }

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: 1, size: 50 }
    if (filters.value.type) params.type = filters.value.type
    if (filters.value.awardLevel) params.awardLevel = filters.value.awardLevel
    const res = await listAchievements(params)
    achievements.value = res.data?.records || res.data || []
  } catch (_) {
    achievements.value = []
  }
  finally { loading.value = false }
}

async function showDetail(item: any) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await getAchievement(item.id)
    detailData.value = res.data || {}
  } catch (_) { detailData.value = {} }
}

async function handleLike(item: any) {
  try {
    await likeAchievement(item.id)
    item.isLiked = !item.isLiked
    item.likeCount = (item.likeCount || 0) + (item.isLiked ? 1 : -1)
    ElMessage.success(item.isLiked ? '点赞成功' : '已取消点赞')
  } catch (_) {}
}

function parseFileUrls(val: any): string[] {
  if (!val) return []
  if (Array.isArray(val)) return val
  if (typeof val === 'string') {
    try { const arr = JSON.parse(val); return Array.isArray(arr) ? arr : [] } catch { return [] }
  }
  return []
}

function getFileName(url: string): string {
  try {
    const parts = url.split('/')
    return parts[parts.length - 1] || url
  } catch {
    return '附件'
  }
}
</script>

<style scoped>
/* ============= Page Container ============= */
.achievement-hall-page {
  max-width: 1280px;
  margin: 0 auto;
  padding-bottom: 40px;
}

/* ============= Hero Banner ============= */
.hero-banner {
  position: relative;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 100%);
  border-radius: 20px;
  padding: 40px 48px;
  margin-bottom: 28px;
  overflow: hidden;
}
.hero-content {
  position: relative;
  z-index: 1;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 13px;
  color: #ffd700;
  font-weight: 500;
  margin-bottom: 16px;
  backdrop-filter: blur(8px);
}
.hero-title {
  font-size: 34px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 1px;
}
.hero-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.65);
  margin: 0 0 24px;
  max-width: 480px;
  line-height: 1.5;
}
.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}
.hero-stat {
  display: flex;
  flex-direction: column;
}
.hero-stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}
.hero-stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  margin-top: 4px;
}
.hero-divider {
  width: 1px;
  height: 36px;
  background: rgba(255, 255, 255, 0.15);
}

/* Decoration */
.hero-decoration {
  position: absolute;
  top: -40px;
  right: -20px;
  width: 280px;
  height: 280px;
}
.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
}
.deco-circle.c1 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, #ffd700, transparent 70%);
  top: 0;
  right: 40px;
}
.deco-circle.c2 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, #4facfe, transparent 70%);
  top: 80px;
  right: 0;
}
.deco-circle.c3 {
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, #f093fb, transparent 70%);
  top: 30px;
  right: 160px;
}

/* ============= Filter Bar ============= */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  flex-wrap: wrap;
  gap: 12px;
}
.filter-left :deep(.el-radio-button__inner) {
  border-radius: 8px !important;
  border: none;
  padding: 8px 16px;
  font-size: 13px;
}
.filter-left :deep(.el-radio-button) {
  margin-right: 4px;
}
.level-select {
  width: 150px;
}
.level-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.level-dot.national { background: #f56c6c; }
.level-dot.provincial { background: #e6a23c; }
.level-dot.school { background: #409eff; }

/* ============= Masonry Grid ============= */
.achievement-masonry {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

/* ============= Card ============= */
.achievement-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
  position: relative;
  animation: cardFadeUp 0.5s ease both;
}
@keyframes cardFadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.achievement-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.12), 0 4px 8px rgba(0,0,0,0.06);
}
.achievement-card.featured {
  border: 1.5px solid #ffd700;
}

/* Card accent stripe */
.card-accent {
  height: 4px;
  width: 100%;
}

/* Featured ribbon */
.featured-ribbon {
  position: absolute;
  top: 16px;
  right: -4px;
  background: linear-gradient(135deg, #f7971e, #ffd200);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 12px 4px 10px;
  border-radius: 4px 0 0 4px;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 2px 6px rgba(247, 151, 30, 0.3);
  z-index: 2;
}

/* Card body */
.card-body {
  padding: 20px 22px 16px;
}
.card-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.type-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 6px;
  display: inline-block;
}
.level-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 6px;
  color: #fff;
}
.level-tag.level-national { background: #f56c6c; }
.level-tag.level-provincial { background: #e6a23c; }
.level-tag.level-school { background: #409eff; }

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin: 0 0 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Card meta */
.card-meta-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #909399;
}
.card-team, .card-date {
  display: flex;
  align-items: center;
  gap: 4px;
}
.card-team span, .card-date span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

/* Card footer */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}
.card-engagement {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}
.eng-item {
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}
.eng-item:hover { color: var(--color-primary, #409eff); }
.eng-item .liked { color: var(--color-primary, #409eff); }
.eng-divider { font-size: 14px; }
.card-arrow {
  color: #c0c4cc;
  transition: all 0.3s;
}
.achievement-card:hover .card-arrow {
  color: var(--color-primary, #409eff);
  transform: translateX(4px);
}

/* ============= Detail Dialog ============= */
.achievement-detail-dialog :deep(.el-dialog__header) { padding: 20px 28px 0; }
.achievement-detail-dialog :deep(.el-dialog__body) { padding: 8px 28px 28px; }

.dialog-header-custom {
  display: flex;
  gap: 8px;
  align-items: center;
}
.dialog-type-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 8px;
}
.dialog-level-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 8px;
  color: #fff;
}
.dialog-level-badge.level-national { background: #f56c6c; }
.dialog-level-badge.level-provincial { background: #e6a23c; }
.dialog-level-badge.level-school { background: #409eff; }

.dialog-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 8px 0 20px;
  line-height: 1.4;
}

.dialog-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}
.meta-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.meta-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  flex-shrink: 0;
}
.meta-text {
  display: flex;
  flex-direction: column;
}
.meta-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 2px;
}
.meta-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.dialog-section {
  margin-bottom: 20px;
}
.di-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px;
  padding-left: 12px;
  border-left: 3px solid var(--color-primary, #409eff);
}
.di-member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.member-tag {
  font-size: 13px;
}
.di-description {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin: 0;
  background: #f8f9fc;
  border-radius: 10px;
  padding: 14px 16px;
}
.di-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.file-link {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ============= Responsive ============= */
@media (max-width: 768px) {
  .hero-banner { padding: 28px 24px; border-radius: 14px; }
  .hero-title { font-size: 24px; }
  .hero-stats { gap: 16px; }
  .hero-stat-num { font-size: 22px; }
  .achievement-masonry { grid-template-columns: 1fr; }
  .dialog-meta-grid { grid-template-columns: 1fr; }
  .filter-bar { flex-direction: column; align-items: flex-start; }
}
</style>
