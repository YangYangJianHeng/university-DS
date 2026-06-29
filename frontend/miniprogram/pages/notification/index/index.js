// pages/notification/index/index.js
const app = getApp()
const { notificationApi } = require('../../../utils/api')

Page({
  data: {
    notifications: [],
    loading: true,
    currentPage: 1,
    pageSize: 20,
    hasMore: false
  },

  onShow() {
    this.setData({ currentPage: 1, notifications: [] })
    this.fetchNotifications()
  },

  onPullDownRefresh() {
    this.setData({ currentPage: 1, notifications: [] })
    this.fetchNotifications().finally(() => wx.stopPullDownRefresh())
  },

  async fetchNotifications() {
    this.setData({ loading: true })
    try {
      const res = await notificationApi.getList({
        page: this.data.currentPage,
        size: this.data.pageSize
      })

      const records = res && res.records ? res.records : (Array.isArray(res) ? res : [])
      this.setData({
        notifications: this.data.currentPage === 1 ? records : [...this.data.notifications, ...records],
        hasMore: records.length >= this.data.pageSize
      })
    } catch (e) {
      console.error('获取通知失败:', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadMore() {
    this.setData({ currentPage: this.data.currentPage + 1 })
    await this.fetchNotifications()
  },

  async handleRead(e) {
    const { id, index } = e.currentTarget.dataset
    try {
      await notificationApi.markRead(id)
      const notifications = [...this.data.notifications]
      if (notifications[index]) {
        notifications[index].read = true
        this.setData({ notifications })
      }
      // 更新未读数
      app.fetchUnreadCount()
    } catch (e) {
      // 忽略
    }
  },

  async markAllRead() {
    try {
      await notificationApi.markAllRead()
      const notifications = this.data.notifications.map(n => ({ ...n, read: true }))
      this.setData({ notifications })
      app.fetchUnreadCount()
      wx.showToast({ title: '已全部标记为已读', icon: 'success' })
    } catch (e) {
      // 忽略
    }
  },

  iconClass(type) {
    const map = { SYSTEM: 'icon-system', TEAM: 'icon-team', RECRUIT: 'icon-recruit', TASK: 'icon-task', PROJECT: 'icon-project', MESSAGE: 'icon-message' }
    return map[type] || 'icon-system'
  },

  iconEmoji(type) {
    const map = { SYSTEM: '📢', TEAM: '👥', RECRUIT: '📋', TASK: '✅', PROJECT: '📁', MESSAGE: '💬' }
    return map[type] || '🔔'
  },

  formatTime(time) {
    if (!time) return ''
    const d = new Date(time)
    const now = new Date()
    const diff = now - d
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
})
