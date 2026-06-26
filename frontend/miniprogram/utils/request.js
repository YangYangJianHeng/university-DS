// utils/request.js - 统一请求封装
const app = getApp()

/**
 * 发起HTTP请求
 * @param {Object} options - 请求配置
 * @returns {Promise}
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const { url, method = 'GET', data = {}, header = {}, showLoading = false } = options

    if (showLoading) {
      wx.showLoading({ title: '加载中...', mask: true })
    }

    const finalHeader = { 'Content-Type': 'application/json', ...header }
    if (app.globalData.token) {
      finalHeader.Authorization = `Bearer ${app.globalData.token}`
    }

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header: finalHeader,
      success: (res) => {
        if (showLoading) wx.hideLoading()

        if (res.statusCode === 200) {
          const result = res.data
          if (result.code === 200) {
            resolve(result)
          } else if (result.code === 401) {
            handleUnauthorized()
            reject(result)
          } else {
            wx.showToast({ title: result.message || '请求失败', icon: 'none', duration: 2000 })
            reject(result)
          }
        } else if (res.statusCode === 401) {
          handleUnauthorized()
          reject(res)
        } else {
          wx.showToast({ title: '服务器错误', icon: 'none', duration: 2000 })
          reject(res)
        }
      },
      fail: (err) => {
        if (showLoading) wx.hideLoading()
        wx.showToast({ title: '网络连接失败', icon: 'none', duration: 2000 })
        reject(err)
      }
    })
  })
}

function handleUnauthorized() {
  app.globalData.token = ''
  app.globalData.userInfo = null
  app.globalData.isLoggedIn = false
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
  wx.reLaunch({ url: '/pages/login/login' })
}

// HTTP方法快捷函数
const http = {
  get: (url, data = {}, options = {}) => request({ url, method: 'GET', data, ...options }),
  post: (url, data = {}, options = {}) => request({ url, method: 'POST', data, ...options }),
  put: (url, data = {}, options = {}) => request({ url, method: 'PUT', data, ...options }),
  delete: (url, data = {}, options = {}) => request({ url, method: 'DELETE', data, ...options })
}

module.exports = { request, http }
