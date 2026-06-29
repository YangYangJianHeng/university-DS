import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ===== PUBLIC ROUTES =====
    {
      path: '/',
      name: 'Landing',
      component: () => import('@/views/LandingPage.vue'),
      meta: { title: '青创协同' },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { title: '注册' },
    },
    // ===== AUTHENTICATED ROUTES =====
    {
      path: '/dashboard',
      component: () => import('@/views/layout/Layout.vue'),
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '工作台', icon: 'HomeFilled' },
        },
        {
          path: '/recruit',
          name: 'Recruit',
          component: () => import('@/views/RecruitList.vue'),
          meta: { title: '招募广场', icon: 'Search' },
        },
        {
          path: '/recruit/create',
          name: 'CreateRecruit',
          component: () => import('@/views/CreateRecruit.vue'),
          meta: { title: '发布招募' },
        },
        {
          path: '/recruit/:id',
          name: 'RecruitDetail',
          component: () => import('@/views/RecruitDetail.vue'),
          meta: { title: '招募详情' },
        },
        {
          path: '/teams',
          name: 'Teams',
          component: () => import('@/views/TeamList.vue'),
          meta: { title: '我的团队', icon: 'UserFilled' },
        },
        {
          path: '/team/:id',
          name: 'TeamDetail',
          component: () => import('@/views/TeamDetail.vue'),
          meta: { title: '团队详情' },
        },
        {
          path: '/projects',
          name: 'Projects',
          component: () => import('@/views/ProjectList.vue'),
          meta: { title: '项目管理', icon: 'FolderOpened' },
        },
        {
          path: '/project/:id',
          name: 'ProjectDetail',
          component: () => import('@/views/ProjectDetail.vue'),
          meta: { title: '项目详情' },
        },
        {
          path: '/tasks',
          name: 'Tasks',
          component: () => import('@/views/TaskList.vue'),
          meta: { title: '任务中心', icon: 'List' },
        },
        {
          path: '/competitions',
          name: 'Competitions',
          component: () => import('@/views/CompetitionList.vue'),
          meta: { title: '竞赛中心', icon: 'Trophy' },
        },
        {
          path: '/competition/:id',
          name: 'CompetitionDetail',
          component: () => import('@/views/CompetitionDetail.vue'),
          meta: { title: '竞赛详情' },
        },
        {
          path: '/teachers',
          name: 'Teachers',
          component: () => import('@/views/TeacherList.vue'),
          meta: { title: '导师库', icon: 'Reading' },
        },
        {
          path: '/achievements',
          name: 'Achievements',
          component: () => import('@/views/AchievementHall.vue'),
          meta: { title: '成果展厅', icon: 'Collection' },
        },
        {
          path: '/profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { title: '个人中心', icon: 'User' },
        },
        {
          path: '/ai-chat',
          name: 'AiChat',
          component: () => import('@/views/AiChat.vue'),
          meta: { title: 'AI 助手', icon: 'ChatDotRound' },
        },
        {
          path: '/admin',
          name: 'Admin',
          component: () => import('@/views/admin/AdminDashboard.vue'),
          meta: { title: '管理后台', icon: 'Setting', admin: true },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const title = to.meta.title as string
  if (title === '青创协同') {
    document.title = '青创协同 - 大学生计算机竞赛组队匹配平台'
  } else if (title) {
    document.title = title + ' - 青创协同'
  }
  const token = localStorage.getItem('token')
  const isPublicRoute = to.path === '/' || to.path === '/login' || to.path === '/register'
  if (!isPublicRoute && !token) {
    next('/')
    return
  }
  // 管理后台路由需要 ADMIN 角色
  if (to.meta.admin) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.role !== 'ADMIN') {
      next('/')
      return
    }
  }
  next()
})

export default router
