// pages/admin/project-manage/project-manage.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    projects: [],
    page: 1,
    total: 0,
    loading: true,
    statusOptions: ['PLANNING', 'IN_PROGRESS', 'REVIEW', 'COMPLETED', 'ARCHIVED'],
    statusLabels: {
      'PLANNING': '规划中',
      'IN_PROGRESS': '进行中',
      'REVIEW': '审核中',
      'COMPLETED': '已完成',
      'ARCHIVED': '已归档'
    }
  },

  onShow() {
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadProjects()
  },

  async loadProjects(page = 1) {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getProjects({ page, size: 20 })
      if (res.code === 200) {
        const data = res.data
        this.setData({
          projects: (data.records || []).map(p => ({ ...p, _editStatus: p.status })),
          total: data.total || 0,
          page,
          loading: false
        })
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  async handleUpdateStatus(e) {
    const { id } = e.currentTarget.dataset
    const status = e.detail.value
    try {
      await adminApi.updateProject(id, { status })
      wx.showToast({ title: '已更新', icon: 'success' })
      this.loadProjects(this.data.page)
    } catch (e) {}
  },

  async handleDelete(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '确认删除',
      content: '删除项目将同时删除关联的任务，确定继续？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await adminApi.deleteProject(id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadProjects(this.data.page)
          } catch (e) {}
        }
      }
    })
  },

  statusLabel(status) {
    return this.data.statusLabels[status] || status || '--'
  },

  statusTheme(status) {
    const map = {
      'PLANNING': 'default',
      'IN_PROGRESS': 'primary',
      'REVIEW': 'warning',
      'COMPLETED': 'success',
      'ARCHIVED': 'danger'
    }
    return map[status] || 'default'
  },

  onReachBottom() {
    if (this.data.projects.length < this.data.total) {
      this.loadProjects(this.data.page + 1)
    }
  }
})
