// pages/index/index.js
const app = getApp()
const { recruitApi, teamApi, taskApi, notificationApi } = require('../../utils/api')

Page({
  data: {
    isLoggedIn: false,
    userInfo: null,
    isAdmin: false,
    unreadCount: 0,
    todayDate: '',
    dashboard: {
      teamCount: 0,
      taskCount: 0,
      activeRecruitCount: 0
    },
    recentRecruits: [],
    loading: true
  },

  onShow() {
    this.checkLoginStatus()
    const now = new Date()
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    this.setData({
      todayDate: `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
    })
    if (app.globalData.isLoggedIn) {
      this.fetchDashboard()
      this.fetchUnreadCount()
    }
  },

  onPullDownRefresh() {
    if (app.globalData.isLoggedIn) {
      Promise.all([this.fetchDashboard(), this.fetchUnreadCount()]).finally(() => {
        wx.stopPullDownRefresh()
      })
    } else {
      wx.stopPullDownRefresh()
    }
  },

  checkLoginStatus() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    this.setData({
      isLoggedIn: app.globalData.isLoggedIn || !!wx.getStorageSync('token'),
      userInfo: userInfo,
      isAdmin: userInfo.role === 'ADMIN'
    })
  },

  async fetchDashboard() {
    this.setData({ loading: true })
    try {
      // 并行获取各模块数据
      const [teams, recruits, tasks, unread] = await Promise.allSettled([
        teamApi.getMyTeams().catch(() => []),
        recruitApi.getList({ page: 1, size: 5 }).catch(() => ({ records: [] })),
        taskApi.getList({ page: 1, size: 99 }).catch(() => ({ records: [] })),
        notificationApi.getUnreadCount().catch(() => 0)
      ])

      const teamData = teams.status === 'fulfilled' ? (teams.value || []) : []
      const recruitData = recruits.status === 'fulfilled' ? (recruits.value || {}) : {}
      const taskData = tasks.status === 'fulfilled' ? (taskDataVal = tasks.value || {}) : {}
      const unreadVal = unread.status === 'fulfilled' ? (unread.value || 0) : 0

      const taskRecords = taskData.records || taskData || []
      const recruitRecords = recruitData.records || recruitData || []

      this.setData({
        dashboard: {
          teamCount: Array.isArray(teamData) ? teamData.length : (teamData.total || 0),
          taskCount: Array.isArray(taskRecords) ? taskRecords.length : (taskData.total || 0),
          activeRecruitCount: Array.isArray(recruitRecords) ? recruitRecords.length : (recruitData.total || 0)
        },
        recentRecruits: Array.isArray(recruitRecords) ? recruitRecords.slice(0, 5) : [],
        unreadCount: typeof unreadVal === 'number' ? unreadVal : 0
      })
    } catch (e) {
      console.error('获取仪表盘数据失败:', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  async fetchUnreadCount() {
    try {
      const count = await notificationApi.getUnreadCount()
      this.setData({ unreadCount: count || 0 })
    } catch (e) {
      // 忽略
    }
  },

  // 跳转方法
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },
  goMyTeams() {
    wx.navigateTo({ url: '/pages/team/list/list' })
  },
  goMyTasks() {
    wx.navigateTo({ url: '/pages/task/list/list' })
  },
  goNotifications() {
    wx.switchTab({ url: '/pages/notification/index/index' })
  },
  goRecruit() {
    wx.switchTab({ url: '/pages/recruit/list/list' })
  },
  goRecruitDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/recruit/detail/detail?id=${id}` })
  },
  goCreateRecruit() {
    wx.navigateTo({ url: '/pages/recruit/create/create' })
  },
  goCreateTeam() {
    wx.showToast({ title: '可通过招募创建团队', icon: 'none' })
  },
  goCompetitions() {
    wx.navigateTo({ url: '/pages/competition/list/list' })
  },
  goTeachers() {
    wx.navigateTo({ url: '/pages/teacher/list/list' })
  },
  goAchievements() {
    wx.navigateTo({ url: '/pages/achievement/list/list' })
  },
  goCalendar() {
    wx.navigateTo({ url: '/pages/competition/list/list' })
  },
  goCreateProject() {
    wx.navigateTo({ url: '/pages/project/list/list' })
  },
  goMore() {
    wx.switchTab({ url: '/pages/profile/profile' })
  },
  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/dashboard/dashboard' })
  },
  goAiChat() {
    wx.navigateTo({ url: '/pages/ai-chat/index/index' })
  }
})
