// pages/admin/task-manage/task-manage.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    tasks: [],
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
    this.loadTasks()
  },

  async loadTasks(page = 1) {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getTasks({ page, size: 20 })
      if (res.code === 200) {
        const data = res.data
        this.setData({
          tasks: (data.records || []).map(t => ({
            ...t,
            _editPriority: t.priority,
            _editStatus: t.status
          })),
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
      await adminApi.updateTask(id, { status })
      wx.showToast({ title: '已更新', icon: 'success' })
      this.loadTasks(this.data.page)
    } catch (e) {}
  },

  async handleUpdatePriority(e) {
    const { id } = e.currentTarget.dataset
    const priority = e.detail.value
    try {
      await adminApi.updateTask(id, { priority })
      wx.showToast({ title: '已更新', icon: 'success' })
      this.loadTasks(this.data.page)
    } catch (e) {}
  },

  async handleDelete(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '确认删除',
      content: '确定删除该任务？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await adminApi.deleteTask(id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadTasks(this.data.page)
          } catch (e) {}
        }
      }
    })
  },

  statusLabel(status) {
    const map = { 'TODO': '待办', 'DOING': '进行中', 'DONE': '已完成' }
    return map[status] || status || '--'
  },

  statusTheme(status) {
    const map = { 'TODO': 'default', 'DOING': 'primary', 'DONE': 'success' }
    return map[status] || 'default'
  },

  priorityLabel(priority) {
    const map = { 'HIGH': '高', 'MEDIUM': '中', 'LOW': '低' }
    return map[priority] || priority || '--'
  },

  priorityTheme(priority) {
    const map = { 'HIGH': 'danger', 'MEDIUM': 'warning', 'LOW': 'default' }
    return map[priority] || 'default'
  },

  onReachBottom() {
    if (this.data.tasks.length < this.data.total) {
      this.loadTasks(this.data.page + 1)
    }
  }
})
