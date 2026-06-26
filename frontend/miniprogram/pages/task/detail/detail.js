// pages/task/detail/detail.js
const { taskApi } = require('../../../utils/api')
const { getStatusLabel } = require('../../../utils/util')

Page({
  data: {
    id: '',
    task: {},
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id })
      this.fetchDetail()
    }
  },

  async fetchDetail() {
    this.setData({ loading: true })
    try {
      const res = await taskApi.getDetail(this.data.id)
      if (res.code === 200) {
        this.setData({ task: res.data })
      }
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  statusLabel(status) {
    return getStatusLabel('task', status)
  },

  statusTheme(status) {
    const map = { TODO: 'default', IN_PROGRESS: 'primary', DONE: 'success', OVERDUE: 'danger' }
    return map[status] || 'default'
  },

  priorityTheme(priority) {
    const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success', high: 'danger', medium: 'warning', low: 'success' }
    return map[priority] || 'default'
  },

  async updateStatus(e) {
    const status = e.currentTarget.dataset.status
    try {
      const res = await taskApi.updateStatus(this.data.id, status)
      if (res.code === 200) {
        wx.showToast({ title: '状态已更新', icon: 'success' })
        this.fetchDetail()
      }
    } catch (e) {
      // ignore
    }
  }
})
