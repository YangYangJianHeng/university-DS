// pages/achievement/list/list.js
const { achievementApi } = require('../../../utils/api')

Page({
  data: {
    keyword: '',
    achievements: [],
    currentPage: 1,
    hasMore: false,
    loading: true
  },

  onShow() {
    this.setData({ currentPage: 1, achievements: [] })
    this.fetchAchievements()
  },

  onPullDownRefresh() {
    this.setData({ currentPage: 1, achievements: [] })
    this.fetchAchievements().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore) this.fetchAchievements()
  },

  async fetchAchievements() {
    const { keyword, currentPage, achievements } = this.data
    this.setData({ loading: currentPage === 1 })

    try {
      const params = { page: currentPage, size: 10 }
      if (keyword) params.keyword = keyword

      const res = await achievementApi.getList(params)
      if (res.code === 200) {
        const data = res.data
        const list = data.records || data.list || data || []
        const total = data.total || list.length
        const newList = currentPage === 1 ? list : [...achievements, ...list]

        this.setData({
          achievements: newList,
          hasMore: newList.length < total,
          currentPage: currentPage + 1
        })
      }
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  onSearch(e) {
    this.setData({ keyword: e.detail.value, currentPage: 1, achievements: [] })
    this.fetchAchievements()
  },

  onSearchClear() {
    this.setData({ keyword: '', currentPage: 1, achievements: [] })
    this.fetchAchievements()
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/achievement/detail/detail?id=${id}` })
  }
})
