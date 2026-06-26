// pages/index/index.js
const app = getApp()
const { formatDate } = require('../../utils/util')

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
    recentRecruits: []
  },

  onShow() {
    this.checkLoginStatus()
    this.setData({ todayDate: formatDate(new Date(), 'yyyy年MM月dd日') })
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
    this.setData({
      isLoggedIn: app.globalData.isLoggedIn,
      userInfo: app.globalData.userInfo,
      isAdmin: app.globalData.userInfo?.role === 'ADMIN'
    })
  },

  async fetchDashboard() {
    try {
      const { dashboardApi } = require('../../utils/api')
      const res = await dashboardApi.getDashboard()
      if (res.code === 200) {
        const data = res.data
        this.setData({
          dashboard: {
            teamCount: data.teamCount || 0,
            taskCount: data.taskCount || 0,
            activeRecruitCount: data.activeRecruitCount || 0
          },
          recentRecruits: data.recentRecruits || []
        })
      }
    } catch (e) {
      // ignore
    }
  },

  async fetchUnreadCount() {
    try {
      const { notificationApi } = require('../../utils/api')
      const res = await notificationApi.getUnreadCount()
      if (res.code === 200) {
        this.setData({ unreadCount: res.data || 0 })
      }
    } catch (e) {
      // ignore
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
    wx.showToast({ title: '请通过招募创建团队', icon: 'none' })
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
    wx.navigateTo({ url: '/pages/competition/list/list?tab=calendar' })
  },
  goCreateProject() {
    wx.navigateTo({ url: '/pages/project/list/list' })
  },
  goMore() {
    wx.navigateTo({ url: '/pages/profile/profile' })
  },
  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/dashboard/dashboard' })
  }
})
