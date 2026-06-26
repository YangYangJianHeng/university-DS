// pages/project/list/list.js
const app = getApp()
const { projectApi } = require('../../../utils/api')

Page({
  data: {
    teamId: '',
    projects: [],
    loading: true
  },

  onLoad(options) {
    if (options.teamId) this.setData({ teamId: options.teamId })
  },

  onShow() {
    this.fetchProjects()
  },

  onPullDownRefresh() {
    this.fetchProjects().finally(() => wx.stopPullDownRefresh())
  },

  async fetchProjects() {
    if (!app.globalData.isLoggedIn) {
      this.setData({ projects: [], loading: false })
      return
    }

    this.setData({ loading: true })
    try {
      const res = await projectApi.getList()
      if (res.code === 200) {
        this.setData({ projects: res.data || [] })
      }
    } catch (e) {
      this.setData({ projects: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/project/detail/detail?id=${id}` })
  }
})
