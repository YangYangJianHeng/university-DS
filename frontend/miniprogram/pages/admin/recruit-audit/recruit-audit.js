// pages/admin/recruit-audit/recruit-audit.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    recruits: [],
    filteredList: [],
    currentFilter: 'all',
    totalCount: 0,
    loading: true
  },

  onShow() {
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadData()
  },

  async loadData() {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getPendingRecruits({ page: 1, size: 100 })
      if (res.code === 200) {
        const data = res.data
        const recruits = data.records || data.list || data || []
        this.setData({
          recruits,
          totalCount: recruits.length,
          loading: false
        })
        this.applyFilter()
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  applyFilter() {
    let list = [...this.data.recruits]
    const { currentFilter } = this.data

    if (currentFilter === 'PENDING') {
      list = list.filter(r => r.auditStatus === 'PENDING')
    } else if (currentFilter === 'APPROVED') {
      list = list.filter(r => r.auditStatus === 'APPROVED')
    } else if (currentFilter === 'REJECTED') {
      list = list.filter(r => r.auditStatus === 'REJECTED')
    }

    this.setData({ filteredList: list })
  },

  onFilterChange(e) {
    const filter = e.currentTarget.dataset.filter
    this.setData({ currentFilter: filter })
    this.applyFilter()
  },

  async handleApprove(e) {
    const id = e.currentTarget.dataset.id
    try {
      await adminApi.auditRecruit(id, 'APPROVE')
      wx.showToast({ title: '已通过', icon: 'success' })
      this.loadData()
    } catch (e) {
      // ignore
    }
  },

  async handleReject(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认驳回',
      content: '确定要驳回这条招募信息吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await adminApi.auditRecruit(id, 'REJECT')
            wx.showToast({ title: '已驳回', icon: 'success' })
            this.loadData()
          } catch (e) {
            // ignore
          }
        }
      }
    })
  },

  auditTheme(status) {
    const map = {
      'PENDING': 'warning',
      'APPROVED': 'success',
      'REJECTED': 'danger'
    }
    return map[status] || 'default'
  },

  auditLabel(status) {
    const map = {
      'PENDING': '待审核',
      'APPROVED': '已通过',
      'REJECTED': '已驳回'
    }
    return map[status] || status
  }
})
