// pages/admin/dashboard/dashboard.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    stats: {},
    weeklyReport: null,
    announcements: []
  },

  onShow() {
    // 校验管理员权限
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadStats()
    this.loadWeeklyReport()
    this.loadAnnouncements()
  },

  async loadStats() {
    try {
      const res = await adminApi.getStats()
      if (res.code === 200) {
        this.setData({ stats: res.data || {} })
      }
    } catch (e) {
      // ignore
    }
  },

  async loadWeeklyReport() {
    try {
      const res = await adminApi.getWeeklyReport()
      if (res.code === 200) {
        this.setData({ weeklyReport: res.data || {} })
      }
    } catch (e) {
      // ignore
    }
  },

  async loadAnnouncements() {
    try {
      const res = await adminApi.getAnnouncements()
      if (res.code === 200) {
        this.setData({ announcements: res.data || [] })
      }
    } catch (e) {
      // ignore
    }
  },

  navigateTo(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      wx.navigateTo({ url })
    }
  }
})
