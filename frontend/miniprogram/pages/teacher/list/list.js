// pages/teacher/list/list.js
const { teacherApi } = require('../../../utils/api')

Page({
  data: {
    keyword: '',
    department: '',
    teachers: [],
    loading: true
  },

  onShow() {
    this.fetchTeachers()
  },

  onPullDownRefresh() {
    this.fetchTeachers().finally(() => wx.stopPullDownRefresh())
  },

  async fetchTeachers() {
    this.setData({ loading: true })
    try {
      const params = {}
      if (this.data.keyword) params.keyword = this.data.keyword
      if (this.data.department) params.department = this.data.department

      const res = await teacherApi.getList(params)
      if (res.code === 200) {
        this.setData({ teachers: res.data || [] })
      }
    } catch (e) {
      this.setData({ teachers: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value })
    this.fetchTeachers()
  },

  onSearchClear() {
    this.setData({ keyword: '' })
    this.fetchTeachers()
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/teacher/detail/detail?id=${id}` })
  }
})
