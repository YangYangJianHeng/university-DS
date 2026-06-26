// pages/admin/user-manage/user-manage.js
const app = getApp()
const { adminApi } = require('../../../utils/api')
const { getRoleLabel } = require('../../../utils/util')

Page({
  data: {
    users: [],
    filteredUsers: [],
    searchKey: '',
    currentFilter: 'all',
    totalUsers: 0,
    loading: true
  },

  onShow() {
    if (!app.globalData.isLoggedIn || app.globalData.userInfo?.role !== 'ADMIN') {
      wx.showToast({ title: '无管理员权限', icon: 'none' })
      setTimeout(() => wx.navigateBack(), 1500)
      return
    }
    this.loadUsers()
  },

  async loadUsers() {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getUsers({ page: 1, size: 100 })
      if (res.code === 200) {
        const data = res.data
        const users = data.records || data.list || data || []
        this.setData({
          users,
          totalUsers: users.length,
          loading: false
        })
        this.applyFilter()
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  applyFilter() {
    let list = [...this.data.users]
    const { searchKey, currentFilter } = this.data

    if (searchKey) {
      const kw = searchKey.toLowerCase()
      list = list.filter(u =>
        (u.username && u.username.toLowerCase().includes(kw)) ||
        (u.name && u.name.toLowerCase().includes(kw)) ||
        (u.major && u.major.toLowerCase().includes(kw))
      )
    }

    if (currentFilter === 'ACTIVE') {
      list = list.filter(u => u.status === 'ACTIVE' || u.status === 1 || u.status === '1')
    } else if (currentFilter === 'DISABLED') {
      list = list.filter(u => u.status === 'DISABLED' || u.status === 0 || u.status === '0')
    }

    this.setData({ filteredUsers: list })
  },

  onSearchChange(e) {
    this.setData({ searchKey: e.detail.value || '' })
    this.applyFilter()
  },

  onFilterChange(e) {
    const filter = e.currentTarget.dataset.filter
    this.setData({ currentFilter: filter })
    this.applyFilter()
  },

  async handleToggleStatus(e) {
    const { id, status } = e.currentTarget.dataset
    const isActive = status === 'ACTIVE' || status === 1 || status === '1'
    const newStatus = isActive ? 0 : 1

    try {
      await adminApi.updateUserStatus(id, newStatus)
      wx.showToast({ title: '已更新', icon: 'success' })
      this.loadUsers()
    } catch (e) {
      // ignore
    }
  },

  handleChangeRole(e) {
    const user = e.currentTarget.dataset.user
    const roles = ['STUDENT', 'USER', 'ADMIN', 'TEACHER', 'REVIEWER']
    const roleNames = ['学生', '普通用户', '管理员', '导师', '审核员']
    const currentRole = user.role || 'STUDENT'
    const currentIndex = roles.indexOf(currentRole)

    wx.showActionSheet({
      itemList: roleNames.map((name, i) => i === currentIndex ? `${name}(当前)` : name),
      success: async (res) => {
        const newRole = roles[res.tapIndex]
        if (newRole === currentRole) return

        try {
          await adminApi.updateUserRole(user.id, newRole)
          wx.showToast({ title: '角色已更新', icon: 'success' })
          this.loadUsers()
        } catch (e) {
          // ignore
        }
      }
    })
  },

  roleLabel(role) {
    return getRoleLabel(role)
  },

  roleTheme(role) {
    const map = {
      'ADMIN': 'danger',
      'TEACHER': 'warning',
      'REVIEWER': 'primary',
      'USER': 'default',
      'STUDENT': 'success'
    }
    return map[role] || 'default'
  }
})
