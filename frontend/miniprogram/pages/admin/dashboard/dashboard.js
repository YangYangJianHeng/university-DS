// pages/admin/dashboard/dashboard.js
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    stats: { userCount: 0, recruitCount: 0, projectCount: 0, teacherCount: 0 },
    weekly: { newUsers: 0, newRecruits: 0, newProjects: 0, newTeams: 0 },
    loading: true
  },

  onShow() { this.fetchData() },

  onPullDownRefresh() {
    this.fetchData().finally(() => wx.stopPullDownRefresh())
  },

  async fetchData() {
    this.setData({ loading: true })
    try {
      const [stats, weekly] = await Promise.allSettled([
        adminApi.getStats().catch(() => ({})),
        adminApi.getWeeklyReport().catch(() => ({}))
      ])
      const statsData = stats.status === 'fulfilled' ? (stats.value || {}) : {}
      const weeklyData = weekly.status === 'fulfilled' ? (weekly.value || {}) : {}

      this.setData({
        stats: {
          userCount: statsData.userCount || statsData.totalUsers || 0,
          recruitCount: statsData.recruitCount || statsData.activeRecruits || 0,
          projectCount: statsData.projectCount || statsData.activeProjects || 0,
          teacherCount: statsData.teacherCount || statsData.teachers || 0
        },
        weekly: {
          newUsers: weeklyData.newUsers || 0,
          newRecruits: weeklyData.newRecruits || 0,
          newProjects: weeklyData.newProjects || 0,
          newTeams: weeklyData.newTeams || 0
        }
      })
    } catch (e) {
      console.error('获取管理统计数据失败:', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  goUsers() { wx.navigateTo({ url: '/pages/admin/user-manage/user-manage' }) },
  goRecruits() { wx.navigateTo({ url: '/pages/admin/recruit-audit/recruit-audit' }) },
  goProjects() { wx.navigateTo({ url: '/pages/admin/project-manage/project-manage' }) },
  goTeams() { wx.navigateTo({ url: '/pages/admin/team-manage/team-manage' }) },
  goTasks() { wx.navigateTo({ url: '/pages/admin/task-manage/task-manage' }) },
  goTeachers() { wx.navigateTo({ url: '/pages/admin/teacher-manage/teacher-manage' }) },
  goAudit() { wx.navigateTo({ url: '/pages/admin/audit-log/audit-log' }) },
  goAnnouncement() { wx.navigateTo({ url: '/pages/admin/announcement/announcement' }) }
})
