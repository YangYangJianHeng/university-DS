// pages/team/detail/detail.js
const app = getApp()
const { teamApi } = require('../../../utils/api')

Page({
  data: {
    id: '',
    team: {},
    members: [],
    timeline: [],
    isMember: false,
    isLeader: false,
    loading: true,
    joining: false
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
      const team = await teamApi.getTeam(this.data.id)
      const members = team.members || []
      const uid = app.globalData.userInfo?.id

      this.setData({
        team,
        members,
        isMember: uid ? members.some(m => (m.userId || m.id) === uid) : false,
        isLeader: uid ? members.some(m => (m.userId || m.id) === uid && (m.role === 'LEADER' || m.role === 'leader')) : false
      })

      this.fetchTimeline()
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  async fetchTimeline() {
    try {
      const res = await teamApi.getTimeline(this.data.id)
      this.setData({ timeline: res || [] })
    } catch (e) {
      // ignore
    }
  },

  async handleJoin() {
    if (!app.globalData.isLoggedIn) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }

    this.setData({ joining: true })
    try {
      await teamApi.joinTeam(this.data.id)
      wx.showToast({ title: '加入成功', icon: 'success' })
      this.fetchDetail()
    } catch (e) {
      // ignore
    } finally {
      this.setData({ joining: false })
    }
  },

  handleLeave() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出该团队吗？',
      success: async (modalRes) => {
        if (!modalRes.confirm) return
        try {
          await teamApi.quitTeam(this.data.id)
          wx.showToast({ title: '已退出团队', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 500)
        } catch (e) {
          // ignore
        }
      }
    })
  },

  handleRemoveMember(e) {
    const memberId = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认移除',
      content: '确定要移除该成员吗？',
      success: async (modalRes) => {
        if (!modalRes.confirm) return
        try {
          await teamApi.removeMember(this.data.id, memberId)
          wx.showToast({ title: '已移除', icon: 'success' })
          this.fetchDetail()
        } catch (e) {
          // ignore
        }
      }
    })
  },

  goTasks() {
    wx.navigateTo({ url: `/pages/task/list/list?teamId=${this.data.id}` })
  },

  goProjects() {
    wx.navigateTo({ url: `/pages/project/list/list?teamId=${this.data.id}` })
  }
})
