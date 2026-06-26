// pages/admin/teacher-manage/teacher-manage.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    teachers: [],
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
    this.loadTeachers()
  },

  async loadTeachers(page = 1) {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getTeachers({ page, size: 20 })
      if (res.code === 200) {
        const data = res.data
        this.setData({
          teachers: data.records || [],
          total: data.total || 0,
          page,
          loading: false
        })
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  async handleToggleVerified(e) {
    const { id, verified } = e.currentTarget.dataset
    const isVerified = verified === 1 || verified === '1' ? 0 : 1
    try {
      await adminApi.updateTeacher(id, { isVerified })
      wx.showToast({ title: isVerified ? '已认证' : '已取消认证', icon: 'success' })
      this.loadTeachers(this.data.page)
    } catch (e) {}
  },

  async handleDelete(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '确认删除',
      content: '确定删除该导师？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await adminApi.deleteTeacher(id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadTeachers(this.data.page)
          } catch (e) {}
        }
      }
    })
  },

  onReachBottom() {
    if (this.data.teachers.length < this.data.total) {
      this.loadTeachers(this.data.page + 1)
    }
  }
})
