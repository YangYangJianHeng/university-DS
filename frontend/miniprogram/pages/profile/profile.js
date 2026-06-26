// pages/profile/profile.js
const app = getApp()
const { getRoleLabel } = require('../../utils/util')

Page({
  data: {
    userInfo: null,
    roleLabel: '学生',
    isAdmin: false,
    stats: {
      teamCount: 0,
      projectCount: 0,
      achievementCount: 0
    }
  },

  onShow() {
    this.loadUserInfo()
    this.loadStats()
  },

  loadUserInfo() {
    const userInfo = app.globalData.userInfo
    if (userInfo) {
      this.setData({
        userInfo,
        roleLabel: getRoleLabel(userInfo.role),
        isAdmin: userInfo.role === 'ADMIN' || userInfo.role === 'REVIEWER'
      })
    } else {
      this.setData({
        userInfo: { username: '未登录' },
        roleLabel: '学生',
        isAdmin: false
      })
    }
  },

  async loadStats() {
    if (!app.globalData.isLoggedIn) return
    try {
      const { dashboardApi } = require('../../utils/api')
      const res = await dashboardApi.getDashboard()
      if (res.code === 200) {
        const data = res.data
        this.setData({
          stats: {
            teamCount: data.teamCount || 0,
            projectCount: data.projectCount || 0,
            achievementCount: data.achievementCount || 0
          }
        })
      }
    } catch (e) {
      // ignore
    }
  },

  // 快捷入口
  goExperiences() {
    wx.navigateTo({ url: '/pages/profile/profile?tab=experiences' })
  },
  goSkillRadar() {
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },
  goBadges() {
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },
  goCalendar() {
    wx.navigateTo({ url: '/pages/competition/list/list?tab=calendar' })
  },

  // 菜单跳转
  goMyTeams() {
    wx.navigateTo({ url: '/pages/team/list/list' })
  },
  goMyProjects() {
    wx.navigateTo({ url: '/pages/project/list/list' })
  },
  goMyTasks() {
    wx.navigateTo({ url: '/pages/task/list/list' })
  },
  goFavorites() {
    wx.navigateTo({ url: '/pages/competition/list/list?tab=favorites' })
  },
  goTeacher() {
    wx.navigateTo({ url: '/pages/teacher/list/list' })
  },
  goFiles() {
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },
  goSettings() {
    wx.showToast({ title: '功能开发中', icon: 'none' })
  },
  goAbout() {
    wx.showModal({
      title: '关于平台',
      content: '大学生竞赛/科研项目组队匹配与过程管理平台 v1.0\n\n为高校学生提供标准化的组队信息发布与智能检索，全流程团队协作管理。',
      showCancel: false
    })
  },
  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/dashboard/dashboard' })
  },

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
