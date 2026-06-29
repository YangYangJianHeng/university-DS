// pages/competition/list/list.js
const { competitionApi } = require('../../../utils/api')
const { getLevelLabel } = require('../../../utils/util')

Page({
  data: {
    keyword: '',
    filterLevel: '',
    competitions: [],
    currentPage: 1,
    hasMore: false,
    loading: true
  },

  onShow() {
    this.setData({ currentPage: 1, competitions: [] })
    this.fetchCompetitions()
  },

  onPullDownRefresh() {
    this.setData({ currentPage: 1, competitions: [] })
    this.fetchCompetitions().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore) this.fetchCompetitions()
  },

  async fetchCompetitions() {
    const { keyword, filterLevel, currentPage, competitions } = this.data
    this.setData({ loading: currentPage === 1 })

    try {
      const params = { page: currentPage, size: 10 }
      if (keyword) params.keyword = keyword
      if (filterLevel) params.level = filterLevel

      // request.js 已自动解包 Result.data，res 直接是 IPage 对象
      const res = await competitionApi.getList(params)
      const list = res.records || res.list || (Array.isArray(res) ? res : [])
      const total = res.total || list.length
      const newList = currentPage === 1 ? list : [...competitions, ...list]

      this.setData({
        competitions: newList,
        hasMore: newList.length < total,
        currentPage: currentPage + 1
      })
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value, currentPage: 1, competitions: [] })
    this.fetchCompetitions()
  },

  onSearchClear() {
    this.setData({ keyword: '', currentPage: 1, competitions: [] })
    this.fetchCompetitions()
  },

  onFilterLevel(e) {
    const level = e.currentTarget.dataset.level
    this.setData({ filterLevel: level === this.data.filterLevel ? '' : level, currentPage: 1, competitions: [] })
    this.fetchCompetitions()
  },

  levelLabel(level) {
    return getLevelLabel(level)
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/competition/detail/detail?id=${id}` })
  }
})
