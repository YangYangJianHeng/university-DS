<template>
  <div class="landing-page">
    <ThreeBackground />

    <!-- Navigation -->
    <nav class="nav" :class="{ 'nav-scrolled': scrolled }">
      <div class="nav-inner">
        <div class="nav-logo">
          <div class="logo-icon-wrap">
            <img src="/icon.png" width="24" height="24" alt="青创协同" />
          </div>
          <span class="logo-text">青创协同</span>
        </div>
        <div class="nav-links">
          <a href="#" class="nav-link" @click.prevent>首页</a>
          <a href="#" class="nav-link" @click.prevent>竞赛</a>
          <a href="#" class="nav-link" @click.prevent>社区</a>
        </div>
        <div class="nav-actions">
          <button class="nav-btn-secondary" @click="$router.push('/login')">登录</button>
          <button class="nav-btn-primary" @click="$router.push('/login')">
            开始使用
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>
    </nav>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-grid">
        <div class="hero-content">
          <div class="hero-badge">
            <span class="badge-dot" />
            <span>大学生竞赛 · 智能组队平台</span>
          </div>
          <h1 class="hero-title">
            找到你的<span class="title-gradient">梦之队</span>
          </h1>
          <p class="hero-desc">
            基于技能图谱与AI匹配引擎，让每一次组队都精准高效，<br />
            帮助大学生找到最合适的竞赛搭档。
          </p>
          <div class="hero-actions">
            <button class="hero-btn-primary" @click="$router.push('/login')">
              立即开始
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
            <button class="hero-btn-secondary" @click="scrollToFeatures">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
              </svg>
              了解更多
            </button>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalUsers.toLocaleString() }}+</span>
              <span class="stat-label">注册用户</span>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalTeams.toLocaleString() }}+</span>
              <span class="stat-label">成功组队</span>
            </div>
            <div class="stat-divider" />
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalCompetitions.toLocaleString() }}+</span>
              <span class="stat-label">竞赛项目</span>
            </div>
          </div>
        </div>
        <div class="hero-visual" />
      </div>
    </section>

    <!-- Features -->
    <section ref="featuresRef" class="features-section">
      <div class="features-header">
        <span class="section-label">核心功能</span>
        <h2 class="section-title">智能匹配，精准组队</h2>
        <p class="section-desc">从技能评估到团队组建，一站式解决竞赛组队难题</p>
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="(feat, i) in features" :key="i" :style="{ '--i': i }">
          <div class="feature-icon" v-html="feat.icon" />
          <h3 class="feature-title">{{ feat.title }}</h3>
          <p class="feature-desc">{{ feat.desc }}</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section">
      <div class="cta-card">
        <h2 class="cta-heading">准备好找到你的队友了吗？</h2>
        <p class="cta-sub">加入青创协同，开启你的竞赛征程</p>
        <button class="cta-btn" @click="$router.push('/login')">
          免费注册
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </button>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">青创协同</span>
          <span class="footer-tagline">大学生竞赛智能组队匹配平台</span>
        </div>
        <div class="footer-links">
          <a href="#">赛事日历</a>
          <a href="#">平台规则</a>
          <a href="#">关于我们</a>
          <a href="#">联系我们</a>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 青创协同. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import ThreeBackground from '@/components/ThreeBackground.vue'
import { getPlatformStats } from '@/api/public'

const scrolled = ref(false)
const featuresRef = ref<HTMLElement>()
const stats = ref({ totalUsers: 0, totalTeams: 0, totalCompetitions: 0 })

const features = [
  {
    icon: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#5B6FF5" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
    title: '智能匹配',
    desc: '基于技能标签与竞赛偏好，AI算法自动推荐最合适的队友',
  },
  {
    icon: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#5B6FF5" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
    title: '团队协作',
    desc: '内置项目管理工具，任务分配、进度追踪一站搞定',
  },
  {
    icon: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#5B6FF5" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    title: '竞赛日历',
    desc: '实时同步各大竞赛时间节点，不再错过报名截止日期',
  },
  {
    icon: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#5B6FF5" stroke-width="1.5"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></svg>',
    title: '数据分析',
    desc: '可视化团队技能分布，精准定位团队能力短板',
  },
  {
    icon: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#5B6FF5" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>',
    title: '在线协作',
    desc: '代码共享、文档协同与实时通讯，完整协作工作区',
  },
  {
    icon: '<svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#5B6FF5" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
    title: '认证保障',
    desc: '实名认证与竞赛成绩背书，确保每位队友真实可靠',
  },
]

function handleScroll() {
  scrolled.value = window.scrollY > 40
}

function scrollToFeatures() {
  featuresRef.value?.scrollIntoView({ behavior: 'smooth' })
}

async function fetchStats() {
  try {
    const res = await getPlatformStats()
    if (res.code === 200 && res.data) {
      stats.value = res.data
    }
  } catch { /* keep defaults */ }
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  fetchStats()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.landing-page {
  position: relative;
  min-height: 100vh;
  background: var(--color-bg);
  color: var(--color-text-primary);
  font-family: var(--font-family);
  overflow-x: hidden;
}

/* ===== Navigation ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  padding: 16px 0;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.nav-scrolled {
  background: var(--glass-bg);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding: 10px 0;
}
.nav-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--color-text-primary);
}
.logo-icon-wrap { flex-shrink: 0; }
.logo-text {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.5px;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 36px;
}
.nav-link {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: color var(--transition-fast);
  letter-spacing: 0.2px;
}
.nav-link:hover { color: var(--color-primary); }
.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nav-btn-secondary {
  padding: 9px 20px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
}
.nav-btn-secondary:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}
.nav-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: inherit;
  box-shadow: 0 2px 10px rgba(91,111,245,0.3);
}
.nav-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(91,111,245,0.4);
}
.nav-btn-primary:active { transform: scale(0.97); }

/* ===== Hero ===== */
.hero {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding: 100px 48px 80px;
}
.hero-grid {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: center;
  gap: 60px;
}
.hero-content { max-width: 560px; }
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border: 1px solid rgba(91,111,245,0.15);
  border-radius: 100px;
  margin-bottom: 32px;
  letter-spacing: 0.3px;
}
.badge-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}
.hero-title {
  font-size: clamp(44px, 6.5vw, 76px);
  font-weight: 900;
  letter-spacing: -2.5px;
  line-height: 1.1;
  margin-bottom: 24px;
  color: var(--color-text-primary);
}
.title-gradient {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-desc {
  font-size: clamp(16px, 1.4vw, 18px);
  color: var(--color-text-secondary);
  line-height: 1.75;
  font-weight: 400;
  margin-bottom: 40px;
  max-width: 480px;
}
.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 52px;
}
.hero-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 15px 32px;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-primary);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  letter-spacing: 0.3px;
  box-shadow: 0 4px 24px rgba(91,111,245,0.3);
}
.hero-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px rgba(91,111,245,0.4);
}
.hero-btn-primary:active { transform: scale(0.97); }
.hero-btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 15px 24px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
}
.hero-btn-secondary:hover {
  color: var(--color-primary);
  border-color: rgba(91,111,245,0.3);
  background: var(--color-primary-light);
}
.hero-stats {
  display: flex;
  align-items: center;
  gap: 28px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text-primary);
  letter-spacing: -1px;
  font-feature-settings: 'tnum';
}
.stat-label {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}
.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--color-border);
}
.hero-visual {
  position: relative;
  min-height: 500px;
}

/* ===== Features ===== */
.features-section {
  position: relative;
  z-index: 1;
  padding: 100px 48px 100px;
  max-width: 1400px;
  margin: 0 auto;
}
.features-header {
  text-align: center;
  margin-bottom: 64px;
}
.section-label {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  padding: 4px 14px;
  background: var(--color-primary-light);
  border-radius: 100px;
  margin-bottom: 16px;
}
.section-title {
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 800;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  letter-spacing: -1px;
}
.section-desc {
  font-size: 16px;
  color: var(--color-text-muted);
  font-weight: 400;
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.feature-card {
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: 36px 32px;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  opacity: 0;
  transform: translateY(24px);
  animation: card-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: calc(var(--i) * 0.1s);
}
@keyframes card-in {
  to { opacity: 1; transform: translateY(0); }
}
.feature-card:hover {
  transform: translateY(-4px);
  border-color: rgba(91,111,245,0.2);
  box-shadow: 0 12px 40px rgba(0,0,0,0.06);
}
.feature-icon {
  display: flex;
  width: 52px; height: 52px;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}
.feature-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.3px;
}
.feature-desc {
  font-size: 14px;
  color: var(--color-text-muted);
  line-height: 1.7;
}

/* ===== CTA ===== */
.cta-section {
  position: relative;
  z-index: 1;
  padding: 80px 48px 100px;
}
.cta-card {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  padding: 64px 48px;
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-md);
}
.cta-heading {
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 800;
  color: var(--color-text-primary);
  margin-bottom: 12px;
  letter-spacing: -1px;
}
.cta-sub {
  font-size: 16px;
  color: var(--color-text-muted);
  margin-bottom: 36px;
}
.cta-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 16px 44px;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-primary);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  box-shadow: 0 4px 24px rgba(91,111,245,0.3);
}
.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px rgba(91,111,245,0.4);
}

/* ===== Footer ===== */
.footer {
  position: relative;
  z-index: 1;
  padding: 48px 48px 32px;
  background: var(--color-card);
  border-top: 1px solid var(--color-border-light);
}
.footer-inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
}
.footer-brand {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.footer-logo {
  font-weight: 800;
  font-size: 18px;
  color: var(--color-text-primary);
}
.footer-tagline {
  font-size: 12px;
  color: var(--color-text-muted);
}
.footer-links {
  display: flex;
  gap: 28px;
}
.footer-links a {
  font-size: 13px;
  color: var(--color-text-muted);
  text-decoration: none;
  transition: color var(--transition-fast);
  font-weight: 500;
}
.footer-links a:hover { color: var(--color-primary); }
.footer-bottom {
  text-align: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light);
}
.footer-bottom p {
  font-size: 12px;
  color: var(--color-text-placeholder);
}

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .hero-grid { grid-template-columns: 1fr; gap: 40px; }
  .hero-visual { min-height: 300px; }
  .features-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .nav-inner { padding: 0 20px; }
  .nav-links { display: none; }
  .hero { padding: 120px 20px 60px; }
  .hero-actions { flex-direction: column; align-items: flex-start; }
  .hero-stats { flex-wrap: wrap; gap: 16px; }
  .stat-divider { display: none; }
  .features-section { padding: 60px 20px; }
  .features-grid { grid-template-columns: 1fr; }
  .cta-section { padding: 60px 20px 80px; }
  .cta-card { padding: 40px 24px; }
}
</style>
