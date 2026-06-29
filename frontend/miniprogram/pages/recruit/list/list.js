// pages/recruit/list/list.js
const app = getApp()
const { recruitApi } = require('../../../utils/api')
const { getStatusLabel, getStatusTagClass, formatDate, timeAgo } = require('../../../utils/util')

Page({
  data: {
    keyword: '',
    filterType: '',
    recruits: [],
    currentPage: 1,
    pageSize: 10,
    total: 0,
    hasMore: false,
    loading: true
  },

  onShow() {
    this.setData({ currentPage: 1, recruits: [] })
    this.fetchRecruits()
  },

  onPullDownRefresh() {
    this.setData({ currentPage: 1, recruits: [] })
    this.fetchRecruits().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore) {
      this.fetchRecruits()
    }
  },

  async fetchRecruits() {
    const { keyword, filterType, currentPage, pageSize, recruits } = this.data
    this.setData({ loading: currentPage === 1 })

    try {
      const params = {
        page: currentPage,
        size: pageSize
      }
      if (keyword) params.keyword = keyword
      if (filterType) params.status = filterType

      // request.js 已自动解包 Result.data，res 直接是 IPage 对象
      const res = await recruitApi.getList(params)
      const list = res.records || res.list || (Array.isArray(res) ? res : [])
      const total = res.total || list.length
      const newRecruits = currentPage === 1 ? list : [...recruits, ...list]

      this.setData({
        recruits: newRecruits,
        total,
        hasMore: newRecruits.length < total,
        currentPage: currentPage + 1
      })
    } catch (e) {
      console.error('获取招募列表失败:', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  onSearchChange(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.setData({ currentPage: 1, recruits: [] })
    this.fetchRecruits()
  },

  onSearchClear() {
    this.setData({ keyword: '', currentPage: 1, recruits: [] })
    this.fetchRecruits()
  },

  onFilterType(e) {
    const type = e.currentTarget.dataset.type
    this.setData({ filterType: type === this.data.filterType ? '' : type, currentPage: 1, recruits: [] })
    this.fetchRecruits()
  },

  statusLabel(status) {
    return getStatusLabel('recruit', status)
  },

  statusTheme(status) {
    const map = { OPEN: 'success', CLOSED: 'default', FULL: 'warning', AUDITING: 'warning', REJECTED: 'danger' }
    return map[status] || 'default'
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/recruit/detail/detail?id=${id}` })
  },

  goCreate() {
    if (!app.globalData.isLoggedIn) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    wx.navigateTo({ url: '/pages/recruit/create/create' })
  }
})
