// pages/admin/announcement/announcement.js
const app = getApp()
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    announcements: [],
    loading: true,
    showDialog: false,
    formTitle: '',
    formContent: '',
    formType: 'SYSTEM'
  },

  onShow() {
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadAnnouncements()
  },

  async loadAnnouncements() {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getAnnouncements()
      if (res.code === 200) {
        this.setData({
          announcements: (res.data || []).reverse(),
          loading: false
        })
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  showCreateDialog() {
    this.setData({
      showDialog: true,
      formTitle: '',
      formContent: '',
      formType: 'SYSTEM'
    })
  },

  onDialogClose() {
    this.setData({ showDialog: false })
  },

  onTitleChange(e) {
    this.setData({ formTitle: e.detail.value || '' })
  },

  onContentChange(e) {
    this.setData({ formContent: e.detail.value || '' })
  },

  onTypeChange(e) {
    this.setData({ formType: e.currentTarget.dataset.type })
  },

  async handleCreate() {
    const { formTitle, formContent, formType } = this.data
    if (!formTitle.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' })
      return
    }
    if (!formContent.trim()) {
      wx.showToast({ title: '请输入内容', icon: 'none' })
      return
    }

    try {
      await adminApi.createAnnouncement({
        title: formTitle.trim(),
        content: formContent.trim(),
        type: formType
      })
      wx.showToast({ title: '发布成功', icon: 'success' })
      this.setData({ showDialog: false })
      this.loadAnnouncements()
    } catch (e) {
      // ignore
    }
  },

  handleDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认删除',
      content: '确定要删除这条公告吗？',
      success: (res) => {
        if (res.confirm) {
          const list = this.data.announcements.filter(item => item.id !== id)
          this.setData({ announcements: list })
          wx.showToast({ title: '已删除（仅本地）', icon: 'none' })
        }
      }
    })
  },

  typeTheme(type) {
    const map = {
      'SYSTEM': 'primary',
      'COMPETITION': 'success',
      'NOTICE': 'warning'
    }
    return map[type] || 'default'
  },

  typeLabel(type) {
    const map = {
      'SYSTEM': '系统',
      'COMPETITION': '竞赛',
      'NOTICE': '通知'
    }
    return map[type] || type
  }
})
