// app.js - 大学生竞赛组队匹配平台小程序
App({
  globalData: {
    // API基础地址（开发环境）
    baseUrl: 'http://localhost:8080/api',
    // 用户令牌
    token: '',
    // 用户信息
    userInfo: null,
    // 是否已登录
    isLoggedIn: false,
    // 未读通知数
    unreadCount: 0
  },

  onLaunch() {
    // 加载 TDesign 图标字体（使用 wx.loadFontFace 避免 CDN 加载失败导致的 ERR_CACHE_MISS 错误）
    wx.loadFontFace({
      family: 't',
      source: 'url("https://tdesign.gtimg.com/icon/0.4.2/fonts/t.woff")',
      global: true,
      success: () => {
        console.log('TDesign 图标字体加载成功')
      },
      fail: (err) => {
        console.warn('TDesign 图标字体加载失败，图标可能显示为方框:', err)
      }
    })

    // 尝试从本地存储恢复登录状态
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    if (token && userInfo) {
      this.globalData.token = token
      this.globalData.userInfo = userInfo
      this.globalData.isLoggedIn = true
      this.checkLoginStatus()
    }
  },

  onShow() {
    // 小程序显示时刷新未读数
    if (this.globalData.isLoggedIn) {
      this.fetchUnreadCount()
    }
  },

  // 检查登录状态是否有效
  async checkLoginStatus() {
    try {
      const res = await this.request({
        url: '/user/profile',
        method: 'GET'
      })
      if (res.code === 200) {
        this.globalData.userInfo = res.data
        this.globalData.isLoggedIn = true
        wx.setStorageSync('userInfo', res.data)
      }
    } catch (e) {
      // Token过期，清除登录状态
      this.globalData.token = ''
      this.globalData.userInfo = null
      this.globalData.isLoggedIn = false
      wx.removeStorageSync('token')
      wx.removeStorageSync('userInfo')
    }
  },

  // 获取未读通知数
  async fetchUnreadCount() {
    try {
      const res = await this.request({
        url: '/notification/unread-count',
        method: 'GET'
      })
      if (res.code === 200) {
        this.globalData.unreadCount = res.data || 0
        // 更新tabBar角标
        if (res.data > 0) {
          wx.setTabBarBadge({
            index: 3,
            text: res.data > 99 ? '99+' : String(res.data)
          })
        } else {
          wx.removeTabBarBadge({ index: 3 })
        }
      }
    } catch (e) {
      // ignore
    }
  },

  // 封装请求方法
  request(options) {
    return new Promise((resolve, reject) => {
      const { url, method = 'GET', data = {}, header = {} } = options

      // 合并认证头
      if (this.globalData.token) {
        header.Authorization = `Bearer ${this.globalData.token}`
      }

      wx.request({
        url: this.globalData.baseUrl + url,
        method,
        data,
        header: {
          'Content-Type': 'application/json',
          ...header
        },
        success: (res) => {
          if (res.statusCode === 200) {
            const result = res.data
            if (result.code === 200) {
              resolve(result)
            } else if (result.code === 401) {
              // Token失效
              this.globalData.token = ''
              this.globalData.userInfo = null
              this.globalData.isLoggedIn = false
              wx.removeStorageSync('token')
              wx.removeStorageSync('userInfo')
              wx.reLaunch({ url: '/pages/login/login' })
              reject(result)
            } else {
              wx.showToast({
                title: result.message || '请求失败',
                icon: 'none',
                duration: 2000
              })
              reject(result)
            }
          } else if (res.statusCode === 401) {
            this.globalData.token = ''
            this.globalData.userInfo = null
            this.globalData.isLoggedIn = false
            wx.removeStorageSync('token')
            wx.removeStorageSync('userInfo')
            wx.reLaunch({ url: '/pages/login/login' })
            reject(res)
          } else {
            wx.showToast({
              title: '网络错误',
              icon: 'none',
              duration: 2000
            })
            reject(res)
          }
        },
        fail: (err) => {
          wx.showToast({
            title: '网络连接失败',
            icon: 'none',
            duration: 2000
          })
          reject(err)
        }
      })
    })
  },

  // 登录成功后设置状态
  setLogin(token, userInfo) {
    this.globalData.token = token
    this.globalData.userInfo = userInfo
    this.globalData.isLoggedIn = true
    wx.setStorageSync('token', token)
    wx.setStorageSync('userInfo', userInfo)
  },

  // 退出登录
  logout() {
    this.globalData.token = ''
    this.globalData.userInfo = null
    this.globalData.isLoggedIn = false
    this.globalData.unreadCount = 0
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    wx.removeTabBarBadge({ index: 3 })
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
