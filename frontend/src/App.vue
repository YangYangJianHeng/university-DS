<template>
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
</script>

<style>
@import '@/assets/styles/global.css';

/* ===== Reset ===== */
*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
  -webkit-text-size-adjust: 100%;
}

html, body, #app {
  height: 100%;
  font-family: var(--font-family);
  background: var(--color-bg);
  color: var(--color-text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.6;
}

/* ===== Global Link Styles ===== */
a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast);
}
a:hover {
  color: var(--color-primary-dark);
}

/* ===== Selection ===== */
::selection {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

/* ===== Scrollbar ===== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #D1D5DB;
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: #9CA3AF;
}

/* ===== Page Transition ===== */
.page-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ===== Common Utility Classes ===== */
.glass-card {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: var(--radius-lg);
}

.gradient-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.card-hover {
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.card-hover:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card-hover);
}

/* Section header with accent line */
.section-header {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-primary);
  padding-bottom: 14px;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--color-border-light);
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}
.section-header::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 48px;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

/* Stat number */
.stat-value-num {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-text-primary);
  font-feature-settings: 'tnum';
  letter-spacing: -0.5px;
}

/* Tag pill */
.tag-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: var(--color-primary-light);
  color: var(--color-primary);
}

/* Badge dot */
.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

/* Skeleton shimmer */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}

/* ===== Element Plus Card Overrides ===== */
.el-card {
  border: 1px solid var(--color-border-light) !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-card) !important;
  transition: box-shadow var(--transition-base), transform var(--transition-base) !important;
}
.el-card:hover {
  box-shadow: var(--shadow-md) !important;
}
.el-card__header {
  border-bottom: 1px solid var(--color-border-light) !important;
  padding: 18px 24px !important;
  font-weight: 600 !important;
  font-size: 15px !important;
}
.el-card__body {
  padding: 24px !important;
}

/* ===== Element Plus Button Overrides ===== */
.el-button {
  font-weight: 600 !important;
  letter-spacing: 0.01em;
  border-radius: var(--radius-sm) !important;
  transition: all var(--transition-base) !important;
}
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-dark);
  --el-button-hover-border-color: var(--color-primary-dark);
  --el-button-active-bg-color: var(--color-primary-darker);
  --el-button-active-border-color: var(--color-primary-darker);
  box-shadow: 0 2px 8px rgba(91,111,245,0.25) !important;
}
.el-button--primary:hover {
  box-shadow: 0 4px 16px rgba(91,111,245,0.35) !important;
}
.el-button--primary.is-plain {
  --el-button-text-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  background: var(--color-primary-light) !important;
  box-shadow: none !important;
}

/* ===== Element Plus Menu Overrides ===== */
.el-menu {
  border-right: none !important;
}

/* ===== Element Plus Dialog Overrides ===== */
.el-dialog {
  border-radius: var(--radius-xl) !important;
  overflow: hidden;
  box-shadow: var(--shadow-xl) !important;
}
.el-dialog__header {
  padding: 24px 28px 0 !important;
  font-weight: 700 !important;
  font-size: 18px !important;
}
.el-dialog__body {
  padding: 24px 28px !important;
}
.el-dialog__footer {
  padding: 0 28px 24px !important;
}

/* ===== Element Plus Table Overrides ===== */
.el-table {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: #F8FAFC;
  border-radius: var(--radius-md) !important;
  overflow: hidden;
}
.el-table th.el-table__cell {
  font-weight: 600 !important;
  font-size: 13px !important;
  color: var(--color-text-secondary) !important;
}

/* ===== Element Plus Input Overrides ===== */
.el-input__wrapper {
  border-radius: var(--radius-sm) !important;
  box-shadow: 0 0 0 1px var(--color-border-input) inset !important;
  transition: all var(--transition-base) !important;
}
.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--color-primary) inset !important;
}
.el-textarea__inner {
  border-radius: var(--radius-sm) !important;
}

/* ===== Element Plus Select & Date Picker ===== */
.el-select .el-input__wrapper,
.el-date-editor .el-input__wrapper {
  border-radius: var(--radius-sm) !important;
}

/* ===== Element Plus Pagination ===== */
.el-pagination .el-pager li.is-active {
  background-color: var(--color-primary) !important;
  border-radius: var(--radius-sm) !important;
}

/* ===== Element Plus Tag ===== */
.el-tag {
  border-radius: 6px !important;
  font-weight: 500 !important;
}

/* ===== Element Plus Tabs ===== */
.el-tabs__active-bar {
  background: var(--gradient-primary) !important;
  height: 3px !important;
  border-radius: 2px !important;
}
.el-tabs__item {
  font-weight: 500 !important;
  font-size: 14px !important;
  transition: all var(--transition-fast) !important;
}
.el-tabs__item.is-active {
  color: var(--color-primary) !important;
  font-weight: 600 !important;
}
</style>
