<template>
  <div class="landing-page" @mousemove="handleMouseMove" @mouseleave="handleMouseLeave">
    <!-- 3D Background -->
    <ThreeBackground />

    <!-- ===== NAVIGATION ===== -->
    <nav class="nav" :class="{ 'nav-scrolled': scrolled }">
      <div class="nav-inner">
        <a href="/" class="nav-logo" @click.prevent>
          <svg viewBox="0 0 32 32" width="28" height="28" fill="none" class="logo-icon">
            <rect width="32" height="32" rx="8" fill="url(#logoGrad)"/>
            <path d="M10 22V12l6 4 6-4v10" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="32" y2="32">
                <stop offset="0%" stop-color="#7c3aed"/>
                <stop offset="100%" stop-color="#6366f1"/>
              </linearGradient>
            </defs>
          </svg>
          <span class="logo-text">TeamStar</span>
        </a>
        <div class="nav-center">
          <a href="#" class="nav-link" @click.prevent>赛事</a>
          <a href="#" class="nav-link" @click.prevent>发现</a>
          <a href="#" class="nav-link" @click.prevent>社区</a>
        </div>
        <div class="nav-right">
          <button class="nav-cta" @click="$router.push('/login')">
            开始使用
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>
    </nav>

    <!-- ===== HERO ===== -->
    <section class="hero">
      <div class="hero-grid">
        <!-- Left: Content -->
        <div class="hero-content">
          <div class="hero-badge">
            <span class="badge-dot"></span>
            大学生计算机竞赛 · 组队匹配平台
          </div>
          <h1 class="hero-title">
            找到你的<span class="title-accent">梦之队</span>
          </h1>
          <p class="hero-desc">
            基于技能图谱与竞赛偏好的智能匹配引擎，<br/>
            让每一次组队都精准高效。
          </p>
          <div class="hero-actions">
            <button class="hero-cta-primary" @click="$router.push('/login')">
              立即开始
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
            <button class="hero-cta-secondary" @click="scrollToContent">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8" fill="currentColor" stroke="none"/>
              </svg>
              观看演示
            </button>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalUsers.toLocaleString() }}+</span>
              <span class="stat-label">注册用户</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalTeams.toLocaleString() }}+</span>
              <span class="stat-label">成功组队</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalCompetitions.toLocaleString() }}+</span>
              <span class="stat-label">竞赛项目</span>
            </div>
          </div>
        </div>

        <!-- Right: Visual space (3D shapes render behind this) -->
        <div class="hero-visual"></div>
      </div>
    </section>

    <!-- ===== FEATURES ===== -->
    <section ref="featuresRef" class="features-section">
      <div class="section-header">
        <h2 class="section-title">智能匹配，精准组队</h2>
        <p class="section-desc">从技能评估到团队组建，一站式解决方案</p>
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="(feat, i) in features" :key="i" :style="{ '--delay': i * 0.1 + 's' }">
          <!--
            安全说明: feat.icon 为硬编码SVG字符串，非用户输入，不存在XSS风险
            若将来改为从API获取，请务必使用DOMPurify/sanitize-html净化
          -->
          <div class="feature-icon" v-html="feat.icon"></div>
          <h3 class="feature-title">{{ feat.title }}</h3>
          <p class="feature-desc">{{ feat.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ===== CTA ===== -->
    <section class="cta-section">
      <div class="cta-card">
        <h2 class="cta-heading">准备好找到你的队友了吗？</h2>
        <p class="cta-sub">加入大学生竞赛社区，开启你的团队征程</p>
        <button class="cta-btn" @click="$router.push('/login')">
          免费注册
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </button>
      </div>
    </section>

    <!-- ===== FOOTER ===== -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">TeamStar</span>
          <span class="footer-tagline">大学生计算机竞赛组队匹配平台</span>
        </div>
        <div class="footer-links">
          <a href="#">赛事日历</a>
          <a href="#">平台规则</a>
          <a href="#">联系我们</a>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 TeamStar. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import ThreeBackground from '@/components/ThreeBackground.vue'
import { getPlatformStats } from '@/api/public'

// ===== State =====
const scrolled = ref(false)
const featuresRef = ref<HTMLElement>()
const stats = ref({ totalUsers: 0, totalTeams: 0, totalCompetitions: 0 })

// ===== Features =====
const features = [
  {
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
    title: '智能匹配',
    desc: '基于技能标签与竞赛偏好，算法自动推荐最佳队友',
  },
  {
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
    title: '团队协作',
    desc: '内置项目管理工具，支持任务分配与进度追踪',
  },
  {
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    title: '竞赛日历',
    desc: '实时同步各大竞赛时间节点，不再错过报名截止',
  },
  {
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" stroke-width="1.5"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></svg>',
    title: '数据分析',
    desc: '可视化团队技能分布，精准定位能力短板',
  },
  {
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>',
    title: '在线协作',
    desc: '支持代码共享、文档协同与实时通讯的完整工作区',
  },
  {
    icon: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#7c3aed" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
    title: '认证保障',
    desc: '实名认证与竞赛成绩背书，确保队友真实可靠',
  },
]

// ===== Scroll =====
function handleScroll() {
  scrolled.value = window.scrollY > 40
}

function scrollToContent() {
  featuresRef.value?.scrollIntoView({ behavior: 'smooth' })
}

// ===== Mouse move =====
function handleMouseMove(_e: MouseEvent) {
  // Mouse tracking is handled by ThreeBackground
}

function handleMouseLeave() {
  // Mouse tracking is handled by ThreeBackground
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

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800;900&family=Noto+Sans+SC:wght@400;500;700;900&display=swap');
</style>

<style scoped>
/* ===== BASE ===== */
.landing-page {
  position: relative;
  min-height: 100vh;
  background: #f5f6fa;
  color: #0f0f1a;
  font-family: 'Inter', 'Noto Sans SC', -apple-system, sans-serif;
  overflow-x: hidden;
  -webkit-font-smoothing: antialiased;
}

/* ===== NAVIGATION ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  padding: 16px 0;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.nav-scrolled {
  background: rgba(255,255,255,0.78);
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
  color: #0f0f1a;
}
.logo-icon {
  flex-shrink: 0;
}
.logo-text {
  font-family: 'Inter', sans-serif;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.6px;
}
.nav-center {
  display: flex;
  align-items: center;
  gap: 36px;
}
.nav-link {
  font-size: 14px;
  font-weight: 500;
  color: #555;
  text-decoration: none;
  transition: color 0.2s;
  letter-spacing: 0.2px;
}
.nav-link:hover {
  color: #7c3aed;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #7c3aed 0%, #6366f1 100%);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  letter-spacing: 0.2px;
}
.nav-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(124,58,237,0.35);
}
.nav-cta:active {
  transform: scale(0.97);
}

/* ===== HERO ===== */
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
.hero-content {
  max-width: 560px;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #7c3aed;
  background: rgba(124,58,237,0.06);
  border: 1px solid rgba(124,58,237,0.12);
  border-radius: 100px;
  margin-bottom: 32px;
  letter-spacing: 0.3px;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #7c3aed;
}
.hero-title {
  font-family: 'Inter', 'Noto Sans SC', sans-serif;
  font-size: clamp(44px, 6.5vw, 76px);
  font-weight: 900;
  letter-spacing: -2.5px;
  line-height: 1.1;
  margin-bottom: 24px;
  color: #0f0f1a;
}
.title-accent {
  background: linear-gradient(135deg, #7c3aed 0%, #6366f1 40%, #06b6d4 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-desc {
  font-size: clamp(16px, 1.4vw, 19px);
  color: #666;
  line-height: 1.75;
  font-weight: 400;
  margin-bottom: 40px;
  max-width: 460px;
}
.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 52px;
}
.hero-cta-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 15px 32px;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #7c3aed 0%, #6366f1 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  letter-spacing: 0.3px;
  box-shadow: 0 4px 24px rgba(124,58,237,0.3);
}
.hero-cta-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px rgba(124,58,237,0.4);
}
.hero-cta-primary:active {
  transform: scale(0.97);
}
.hero-cta-secondary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 15px 24px;
  font-size: 15px;
  font-weight: 600;
  color: #555;
  background: transparent;
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
}
.hero-cta-secondary:hover {
  color: #7c3aed;
  border-color: rgba(124,58,237,0.3);
  background: rgba(124,58,237,0.03);
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
  font-family: 'Inter', sans-serif;
  font-size: 28px;
  font-weight: 800;
  color: #0f0f1a;
  letter-spacing: -1px;
}
.stat-label {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}
.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(0,0,0,0.08);
}
.hero-visual {
  position: relative;
  min-height: 500px;
}

/* ===== FEATURES ===== */
.features-section {
  position: relative;
  z-index: 1;
  padding: 100px 48px 100px;
  max-width: 1400px;
  margin: 0 auto;
}
.section-header {
  text-align: center;
  margin-bottom: 64px;
}
.section-title {
  font-family: 'Inter', 'Noto Sans SC', sans-serif;
  font-size: clamp(28px, 4vw, 44px);
  font-weight: 800;
  color: #0f0f1a;
  margin-bottom: 12px;
  letter-spacing: -1px;
}
.section-desc {
  font-size: 17px;
  color: #777;
  font-weight: 400;
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.feature-card {
  background: #fff;
  border: 1px solid rgba(0,0,0,0.05);
  border-radius: 20px;
  padding: 36px 32px;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  opacity: 0;
  transform: translateY(24px);
  animation: card-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  animation-delay: var(--delay);
}
@keyframes card-in {
  to { opacity: 1; transform: translateY(0); }
}
.feature-card:hover {
  transform: translateY(-4px);
  border-color: rgba(124,58,237,0.15);
  box-shadow: 0 12px 40px rgba(0,0,0,0.06);
}
.feature-icon {
  display: flex;
  width: 52px;
  height: 52px;
  align-items: center;
  justify-content: center;
  background: rgba(124,58,237,0.06);
  border-radius: 14px;
  margin-bottom: 20px;
}
.feature-title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: #0f0f1a;
  margin-bottom: 8px;
  letter-spacing: -0.3px;
}
.feature-desc {
  font-size: 14px;
  color: #777;
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
  background: linear-gradient(135deg, rgba(124,58,237,0.04) 0%, rgba(99,102,241,0.04) 100%);
  border: 1px solid rgba(124,58,237,0.08);
  border-radius: 28px;
}
.cta-heading {
  font-family: 'Inter', 'Noto Sans SC', sans-serif;
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 800;
  color: #0f0f1a;
  margin-bottom: 12px;
  letter-spacing: -1px;
}
.cta-sub {
  font-size: 16px;
  color: #777;
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
  background: linear-gradient(135deg, #7c3aed 0%, #6366f1 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  box-shadow: 0 4px 24px rgba(124,58,237,0.3);
}
.cta-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px rgba(124,58,237,0.4);
}
.cta-btn:active {
  transform: scale(0.97);
}

/* ===== FOOTER ===== */
.footer {
  position: relative;
  z-index: 1;
  padding: 48px 48px 32px;
  background: #fff;
  border-top: 1px solid rgba(0,0,0,0.04);
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
  color: #0f0f1a;
}
.footer-tagline {
  font-size: 12px;
  color: #999;
}
.footer-links {
  display: flex;
  gap: 28px;
}
.footer-links a {
  font-size: 13px;
  color: #777;
  text-decoration: none;
  transition: color 0.2s;
  font-weight: 500;
}
.footer-links a:hover {
  color: #7c3aed;
}
.footer-bottom {
  text-align: center;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(0,0,0,0.03);
}
.footer-bottom p {
  font-size: 12px;
  color: #aaa;
}

/* ===== MOBILE ===== */
@media (max-width: 1024px) {
  .hero-grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }
  .hero-visual {
    min-height: 300px;
  }
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .nav-inner {
    padding: 0 20px;
  }
  .nav-center {
    display: none;
  }
  .hero {
    padding: 120px 20px 60px;
  }
  .hero-actions {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-stats {
    flex-wrap: wrap;
    gap: 16px;
  }
  .stat-divider {
    display: none;
  }
  .features-section {
    padding: 60px 20px;
  }
  .features-grid {
    grid-template-columns: 1fr;
  }
  .cta-section {
    padding: 60px 20px 80px;
  }
  .cta-card {
    padding: 40px 24px;
  }
  .footer {
    padding: 36px 20px 24px;
  }
  .footer-inner {
    flex-direction: column;
    text-align: center;
  }
}
</style>
