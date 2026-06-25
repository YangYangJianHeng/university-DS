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
::-webkit-scrollbar-thumb { background: #d0d5dd; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #b0b5bd; }

/* Page enter animation */
.page-enter-active {
  transition: all var(--transition-normal);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

/* Card hover effect */
.card-hover {
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.card-hover:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* Section header with gradient underline */
.section-header {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  padding-bottom: 12px;
  margin-bottom: 20px;
  border-bottom: 2px solid var(--color-border);
  position: relative;
}
.section-header::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 60px;
  height: 2px;
  background: var(--gradient-primary);
  border-radius: 2px;
}

/* Gradient text - cross-browser compatible */
.gradient-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

/* Stat value */
.stat-value-num {
  font-size: 28px;
  font-weight: 700;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

/* Element Plus overrides */
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-dark);
  --el-button-hover-border-color: var(--color-primary-dark);
}

.el-card {
  border: 1px solid var(--color-border-light);
}

.el-menu {
  border-right: none !important;
}
</style>
