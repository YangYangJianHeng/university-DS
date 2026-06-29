// pages/team/list/list.js
const app = getApp()
const { teamApi } = require('../../../utils/api')
const { formatDate } = require('../../../utils/util')

Page({
  data: {
    currentTab: 'my',
    teams: [],
    loading: true
  },

  onShow() {
    this.fetchTeams()
  },

  onPullDownRefresh() {
    this.fetchTeams().finally(() => wx.stopPullDownRefresh())
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    this.setData({ currentTab: tab, teams: [] })
    this.fetchTeams()
  },

  async fetchTeams() {
    if (!app.globalData.isLoggedIn) {
      this.setData({ teams: [], loading: false })
      return
    }

    this.setData({ loading: true })
    try {
      const api = this.data.currentTab === 'my' ? teamApi.getMyTeams : teamApi.getJoinedTeams
      const res = await api()
      // request.js 已自动解包 Result.data
      this.setData({ teams: res || [] })
    } catch (e) {
      this.setData({ teams: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/team/detail/detail?id=${id}` })
  },

  goRecruit() {
    wx.switchTab({ url: '/pages/recruit/list/list' })
  }
})
