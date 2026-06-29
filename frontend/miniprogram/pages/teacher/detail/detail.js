// pages/teacher/detail/detail.js
const { teacherApi, teamApi } = require('../../../utils/api')

Page({
  data: {
    id: '',
    teacher: {},
    loading: true,
    inviting: false,
    myTeams: [],
    showTeamPicker: false
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
      const res = await teacherApi.getDetail(this.data.id)
      this.setData({ teacher: res })
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleInvite() {
    const app = getApp()
    if (!app.globalData.isLoggedIn) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }

    // 先加载用户的团队列表
    try {
      const res = await teamApi.getMyTeams()
      const teams = res || []
      if (teams.length === 0) {
        wx.showModal({
          title: '提示',
          content: '您还没有创建团队，请先创建团队后再邀请教师指导',
          showCancel: false
        })
        return
      }
      if (teams.length === 1) {
        // 只有一个团队，直接发送邀请
        this.sendInvite(teams[0].id)
      } else {
        // 多个团队，显示选择器
        this.setData({ myTeams: teams, showTeamPicker: true })
      }
    } catch (e) {
      wx.showToast({ title: '加载团队失败', icon: 'none' })
    }
  },

  async sendInvite(teamId) {
    this.setData({ inviting: true, showTeamPicker: false })
    try {
      await teacherApi.invite(teamId, this.data.id)
      wx.showToast({ title: '邀请已发送', icon: 'success' })
    } catch (e) {
      wx.showToast({ title: '邀请失败', icon: 'none' })
    } finally {
      this.setData({ inviting: false })
    }
  },

  onSelectTeam(e) {
    const teamId = e.currentTarget.dataset.id
    this.sendInvite(teamId)
  },

  onClosePicker() {
    this.setData({ showTeamPicker: false })
  }
})
