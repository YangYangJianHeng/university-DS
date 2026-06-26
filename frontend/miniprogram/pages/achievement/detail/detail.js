// pages/achievement/detail/detail.js
const { achievementApi } = require('../../../utils/api')

Page({
  data: {
    id: '',
    achievement: {},
    isLiked: false,
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
      const res = await achievementApi.getDetail(this.data.id)
      if (res.code === 200) {
        this.setData({
          achievement: res.data,
          isLiked: res.data.isLiked || false,
          isFavorited: res.data.isFavorited || false
        })
      }
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleLike() {
    try {
      await achievementApi.toggleLike(this.data.id)
      this.setData({ isLiked: !this.data.isLiked })
      this.fetchDetail()
    } catch (e) {
      // ignore
    }
  },

  async handleFavorite() {
    try {
      await achievementApi.toggleCollect(this.data.id)
      this.setData({ isFavorited: !this.data.isFavorited })
      this.fetchDetail()
    } catch (e) {
      // ignore
    }
  }
})
