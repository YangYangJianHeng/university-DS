// pages/task/list/list.js
const app = getApp()
const { taskApi } = require('../../../utils/api')
const { getStatusLabel } = require('../../../utils/util')

Page({
  data: {
    teamId: '',
    filterStatus: '',
    tasks: [],
    loading: true
  },

  computed: {
    filteredTasks() {
      const { tasks, filterStatus } = this.data
      if (!filterStatus) return tasks
      return tasks.filter(t => t.status === filterStatus)
    }
  },

  onLoad(options) {
    if (options.teamId) this.setData({ teamId: options.teamId })
  },

  onShow() {
    this.fetchTasks()
  },

  onPullDownRefresh() {
    this.fetchTasks().finally(() => wx.stopPullDownRefresh())
  },

  get filteredTasks() {
    const { tasks, filterStatus } = this.data
    if (!filterStatus) return tasks
    return tasks.filter(t => t.status === filterStatus)
  },

  async fetchTasks() {
    if (!app.globalData.isLoggedIn) {
      this.setData({ tasks: [], loading: false })
      return
    }

    this.setData({ loading: true })
    try {
      const res = await taskApi.getMyTasks()
      if (res.code === 200) {
        this.setData({ tasks: res.data || [] })
      }
    } catch (e) {
      this.setData({ tasks: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  onFilterStatus(e) {
    const status = e.currentTarget.dataset.status
    this.setData({ filterStatus: status === this.data.filterStatus ? '' : status })
  },

  statusLabel(status) {
    return getStatusLabel('task', status)
  },

  statusTheme(status) {
    const map = { TODO: 'default', IN_PROGRESS: 'primary', DONE: 'success', OVERDUE: 'danger' }
    return map[status] || 'default'
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/task/detail/detail?id=${id}` })
  }
})
