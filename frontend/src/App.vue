<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
@import '@/assets/styles/global.css';

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: var(--font-family);
  background: var(--color-bg);
  color: var(--color-text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Global scrollbar */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c8ccd0; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #a8b0b8; }

/* Page enter animation */
.page-enter-active {
  transition: all var(--transition-normal);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

/* Card hover effect */
.card-hover {
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.card-hover:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* Section header */
.section-header {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  padding-bottom: 12px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
  position: relative;
}
.section-header::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 48px;
  height: 2px;
  background: var(--color-primary);
  border-radius: 1px;
}

/* Gradient text - replaced with solid accent */
.accent-text {
  color: var(--color-primary);
  font-weight: 600;
}

/* Stat value */
.stat-value-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
}

/* Element Plus card override */
.el-card {
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-card);
}
.el-card__header {
  border-bottom: 1px solid var(--color-border-light);
  padding: 16px 20px;
}

/* Element Plus menu */
.el-menu {
  border-right: none !important;
}

/* Element Plus button overrides */
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-dark);
  --el-button-hover-border-color: var(--color-primary-dark);
}
.el-button--primary.is-plain {
  --el-button-text-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
}
</style>
