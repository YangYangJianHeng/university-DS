// pages/admin/audit-log/audit-log.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    logs: [],
    filteredLogs: [],
    currentFilter: 'all',
    loading: true
  },

  onShow() {
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadLogs()
  },

  async loadLogs() {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getAuditLogs({ page: 1, size: 100 })
      if (res.code === 200) {
        const data = res.data
        const logs = data.records || data.list || data || []
        this.setData({
          logs,
          loading: false
        })
        this.applyFilter()
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  applyFilter() {
    let list = [...this.data.logs]
    const { currentFilter } = this.data

    if (currentFilter === 'PENDING') {
      list = list.filter(l => l.afterStatus === 'PENDING')
    } else if (currentFilter === 'APPROVED') {
      list = list.filter(l => l.afterStatus === 'APPROVED')
    } else if (currentFilter === 'REJECTED') {
      list = list.filter(l => l.afterStatus === 'REJECTED')
    }

    this.setData({ filteredLogs: list })
  },

  onFilterChange(e) {
    const filter = e.currentTarget.dataset.filter
    this.setData({ currentFilter: filter })
    this.applyFilter()
  },

  statusTheme(status) {
    const map = {
      'PENDING': 'warning',
      'APPROVED': 'success',
      'REJECTED': 'danger'
    }
    return map[status] || 'default'
  },

  statusLabel(status) {
    const map = {
      'PENDING': '待处理',
      'APPROVED': '已通过',
      'REJECTED': '已驳回'
    }
    return map[status] || status
  },

  statusDotClass(status) {
    const map = {
      'PENDING': 'warning',
      'APPROVED': 'success',
      'REJECTED': 'danger'
    }
    return map[status] || 'default'
  },

  targetTypeLabel(type) {
    const map = {
      'RECRUIT': '招募审核',
      'TEAM': '团队审核',
      'PROJECT': '项目审核',
      'USER': '用户审核'
    }
    return map[type] || type || '未知类型'
  }
})
