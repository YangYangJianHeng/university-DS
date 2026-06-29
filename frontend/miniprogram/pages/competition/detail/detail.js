// pages/competition/detail/detail.js
const { competitionApi } = require('../../../utils/api')
const { getLevelLabel } = require('../../../utils/util')

Page({
  data: {
    id: '',
    competition: {},
    isFavorited: false,
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id })
      this.fetchDetail()
    }
  },

  async fetchDetail() {
    this.setData({ loading: true })
    try {
      const res = await competitionApi.getDetail(this.data.id)
      this.setData({
        competition: res,
        isFavorited: res.isFavorited || false
      })
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  levelLabel(level) {
    return getLevelLabel(level)
  },

  async handleFavorite() {
    try {
      await competitionApi.toggleCollect(this.data.id)
      const newFav = !this.data.isFavorited
      this.setData({ isFavorited: newFav })
      wx.showToast({ title: newFav ? '已收藏' : '已取消收藏', icon: 'success' })
    } catch (e) {
      // ignore
    }
  },
})
