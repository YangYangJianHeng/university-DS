// pages/admin/user-manage/user-manage.js
const { adminApi } = require('../../../utils/api')

Page({
  data: {
    users: [],
    loading: true,
    currentPage: 1,
    pageSize: 20,
    total: 0,
    keyword: '',
    showRolePicker: false,
    selectedUser: null,
    roleOptions: ['USER', 'TEACHER', 'ADMIN', 'REVIEWER']
  },

  onShow() { this.fetchUsers() },

  async fetchUsers() {
    this.setData({ loading: true })
    try {
      const res = await adminApi.getUsers({ page: this.data.currentPage, size: this.data.pageSize })
      const records = res && res.records ? res.records : (Array.isArray(res) ? res : [])
      const total = res && res.total ? res.total : records.length
      this.setData({ users: records, total })
    } catch (e) { console.error(e) }
    finally { this.setData({ loading: false }) }
  },

  onSearchInput(e) { this.setData({ keyword: e.detail.value }) },
  onSearch() { this.setData({ currentPage: 1 }); this.fetchUsers() },

  // 角色管理
  showRoleChange(e) {
    const { user } = e.currentTarget.dataset
    this.setData({ showRolePicker: true, selectedUser: user })
  },
  async onRoleChange(e) {
    const role = this.data.roleOptions[e.detail.value]
    try {
      await adminApi.updateUserRole(this.data.selectedUser.id, role)
      wx.showToast({ title: '角色更新成功', icon: 'success' })
      this.setData({ showRolePicker: false })
      this.fetchUsers()
    } catch (e) { /* 忽略 */ }
  },

  // 设为导师
  async setTeacher(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '设为导师',
      content: '确认将该用户设为导师？设置后将拥有导师权限。',
      success: async (res) => {
        if (res.confirm) {
          try {
            await adminApi.updateUserRole(id, 'TEACHER')
            wx.showToast({ title: '已设为导师', icon: 'success' })
            this.fetchUsers()
          } catch (e) { /* 忽略 */ }
        }
      }
    })
  },

  // 设为管理员
  async setAdmin(e) {
    const { id } = e.currentTarget.dataset
    try {
      await adminApi.updateUserRole(id, 'ADMIN')
      wx.showToast({ title: '已设为管理员', icon: 'success' })
      this.fetchUsers()
    } catch (e) { /* 忽略 */ }
  },

  // 状态切换
  async toggleStatus(e) {
    const { id, status } = e.currentTarget.dataset
    const newStatus = status === 1 ? 0 : 1
    try {
      await adminApi.updateUserStatus(id, newStatus)
      wx.showToast({ title: newStatus ? '已启用' : '已禁用', icon: 'success' })
      this.fetchUsers()
    } catch (e) { /* 忽略 */ }
  },

  roleLabel(r) { return { USER: '用户', TEACHER: '导师', ADMIN: '管理员', REVIEWER: '审核员' }[r] || r },
  roleTagClass(r) { return { ADMIN: 'tag-warning', TEACHER: 'tag-success', REVIEWER: 'tag-info', USER: '' }[r] || '' }
})
