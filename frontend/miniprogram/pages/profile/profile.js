// pages/profile/profile.js
const app = getApp()
const { userApi, teamApi, taskApi, achievementApi } = require('../../utils/api')

Page({
  data: {
    isLoggedIn: false,
    userInfo: {},
    isAdmin: false,
    stats: { teamCount: 0, projectCount: 0, achievementCount: 0 },
    roleLabel: '普通用户',
    showEditDialog: false,
    showPwdDialog: false,
    editForm: { username: '', major: '', grade: '', bio: '' },
    pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
    gradeOptions: ['大一', '大二', '大三', '大四', '研一', '研二', '研三']
  },

  onShow() {
    this.checkLogin()
    if (app.globalData.isLoggedIn) {
      this.fetchProfile()
      this.fetchStats()
    }
  },

  checkLogin() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const role = userInfo.role || 'USER'
    const roleMap = { ADMIN: '管理员', TEACHER: '导师', REVIEWER: '审核员', USER: '普通用户' }
    this.setData({
      isLoggedIn: !!wx.getStorageSync('token'),
      userInfo,
      isAdmin: role === 'ADMIN',
      roleLabel: roleMap[role] || '普通用户'
    })
  },

  async fetchProfile() {
    try {
      const user = await userApi.getProfile()
      if (user) {
        app.globalData.userInfo = user
        wx.setStorageSync('userInfo', user)
        this.setData({ userInfo: user })
      }
    } catch (e) { /* 使用缓存数据 */ }
  },

  async fetchStats() {
    try {
      const [teams, tasks] = await Promise.allSettled([
        teamApi.getMyTeams().catch(() => []),
        taskApi.getList({ page: 1, size: 99 }).catch(() => ({ records: [] }))
      ])
      const teamData = teams.status === 'fulfilled' ? (teams.value || []) : []
      const taskData = tasks.status === 'fulfilled' ? (tasks.value || {}) : {}
      this.setData({ stats: {
        teamCount: Array.isArray(teamData) ? teamData.length : 0,
        projectCount: 0,
        achievementCount: 0
      }})
    } catch (e) { /* 忽略 */ }
  },

  // 快捷入口
  goMyTeams() { wx.navigateTo({ url: '/pages/team/list/list' }) },
  goMyProjects() { wx.navigateTo({ url: '/pages/project/list/list' }) },
  goMyTasks() { wx.navigateTo({ url: '/pages/task/list/list' }) },
  goFavorites() { wx.navigateTo({ url: '/pages/competition/list/list' }) },
  goTeacher() { wx.navigateTo({ url: '/pages/teacher/list/list' }) },
  goFiles() { wx.navigateTo({ url: '/pages/achievement/list/list' }) },
  goSettings() { this.setData({ showEditDialog: true, editForm: { username: this.data.userInfo.username || '', major: this.data.userInfo.major || '', grade: this.data.userInfo.grade || '', bio: this.data.userInfo.bio || '' } }) },
  goAbout() { wx.showModal({ title: '青创协同', content: '大学生竞赛智能组队平台 v2.0\n基于SpringBoot+Vue 3+微信小程序', showCancel: false }) },
  goAdmin() { wx.navigateTo({ url: '/pages/admin/dashboard/dashboard' }) },
  goAchievements() { wx.navigateTo({ url: '/pages/achievement/list/list' }) },
  goSkillRadar() { wx.navigateTo({ url: '/pages/achievement/list/list' }) },
  goBadges() { wx.navigateTo({ url: '/pages/achievement/list/list' }) },
  goCalendar() { wx.navigateTo({ url: '/pages/competition/list/list' }) },
  goAiChat() { wx.navigateTo({ url: '/pages/ai-chat/index/index' }) },

  // 编辑个人信息
  onEditFieldChange(e) { const { field } = e.currentTarget.dataset; this.setData({ [`editForm.${field}`]: e.detail.value }) },
  async handleSaveProfile() {
    try {
      await userApi.updateProfile(this.data.editForm)
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.setData({ showEditDialog: false })
      this.fetchProfile()
    } catch (e) { /* 忽略 */ }
  },

  // 修改密码
  onPwdFieldChange(e) { const { field } = e.currentTarget.dataset; this.setData({ [`pwdForm.${field}`]: e.detail.value }) },
  async handleChangePwd() {
    const { oldPassword, newPassword, confirmPassword } = this.data.pwdForm
    if (!oldPassword || !newPassword || !confirmPassword) { wx.showToast({ title: '请填写完整', icon: 'none' }); return }
    if (newPassword.length < 6) { wx.showToast({ title: '新密码至少6位', icon: 'none' }); return }
    if (newPassword !== confirmPassword) { wx.showToast({ title: '两次密码不一致', icon: 'none' }); return }
    try {
      await userApi.changePassword({ oldPassword, newPassword })
      wx.showToast({ title: '密码修改成功', icon: 'success' })
      this.setData({ showPwdDialog: false, pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' } })
    } catch (e) { /* 忽略 */ }
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          app.logout()
        }
      }
    })
  }
})
