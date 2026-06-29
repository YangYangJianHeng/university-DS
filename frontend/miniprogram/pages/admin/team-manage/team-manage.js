// pages/admin/team-manage/team-manage.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    teams: [],
    page: 1,
    total: 0,
    loading: true
  },

  onShow() {
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadTeams()
  },

  async loadTeams(page = 1) {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getAllTeams({ page, size: 20 })
      // request.js 已自动解包 Result.data
      const data = res.records || res
      this.setData({
        teams: (Array.isArray(data) ? data : (data.records || [])).map(t => ({ ...t, _editStatus: t.status })),
        total: res.total || 0,
        page,
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  async handleUpdateStatus(e) {
    const { id } = e.currentTarget.dataset
    const status = e.detail.value
    try {
      await adminApi.updateTeam(id, { status })
      wx.showToast({ title: '已更新', icon: 'success' })
      this.loadTeams(this.data.page)
    } catch (e) {}
  },

  async handleDelete(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '确认删除',
      content: '删除团队将同时删除关联的项目和任务，确定继续？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await adminApi.deleteTeam(id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadTeams(this.data.page)
          } catch (e) {}
        }
      }
    })
  },

  statusLabel(status) {
    const map = { 'ACTIVE': '正常', 'DISBANDED': '已解散' }
    return map[status] || status || '--'
  },

  statusTheme(status) {
    const map = { 'ACTIVE': 'success', 'DISBANDED': 'danger' }
    return map[status] || 'default'
  },

  onReachBottom() {
    if (this.data.teams.length < this.data.total) {
      this.loadTeams(this.data.page + 1)
    }
  }
})
