// pages/notification/index/index.js
const app = getApp()
const { notificationApi } = require('../../../utils/api')
const { timeAgo } = require('../../../utils/util')

Page({
  data: {
    notifications: [],
    unreadCount: 0,
    loading: true
  },

  onShow() {
    this.fetchNotifications()
    this.fetchUnreadCount()
  },

  onPullDownRefresh() {
    this.fetchNotifications().finally(() => wx.stopPullDownRefresh())
  },

  async fetchNotifications() {
    if (!app.globalData.isLoggedIn) {
      this.setData({ notifications: [], loading: false })
      return
    }

    this.setData({ loading: true })
    try {
      const res = await notificationApi.getList()
      if (res.code === 200) {
        this.setData({ notifications: res.data || [] })
      }
    } catch (e) {
      this.setData({ notifications: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  async fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      if (res.code === 200) {
        const count = res.data || 0
        this.setData({ unreadCount: count })
        if (count > 0) {
          wx.setTabBarBadge({ index: 3, text: count > 99 ? '99+' : String(count) })
        } else {
          wx.removeTabBarBadge({ index: 3 })
        }
      }
    } catch (e) {
      // ignore
    }
  },

  async handleRead(e) {
    const id = e.currentTarget.dataset.id
    try {
      await notificationApi.markAsRead(id)
      const notifications = this.data.notifications.map(n => {
        if (n.id === id) n.isRead = 1
        return n
      })
      this.setData({ notifications, unreadCount: Math.max(0, this.data.unreadCount - 1) })
      this.fetchUnreadCount()
    } catch (e) {
      // ignore
    }
  },

  async markAllRead() {
    try {
      await notificationApi.markAllRead()
      const notifications = this.data.notifications.map(n => {
        n.isRead = 1
        return n
      })
      this.setData({ notifications, unreadCount: 0 })
      wx.removeTabBarBadge({ index: 3 })
    } catch (e) {
      // ignore
    }
  },

  formatTime(dateStr) {
    return timeAgo(dateStr)
  }
})
