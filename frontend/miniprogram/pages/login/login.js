// pages/login/login.js
const app = getApp()
const { userApi } = require('../../utils/api')
const { getToken } = require('../../utils/request')

Page({
  data: {
    form: {
      phone: '',
      password: '',
      captchaKey: '',
      captchaCode: ''
    },
    captchaSrc: '',
    showPassword: false,
    loading: false
  },

  onLoad() {
    // 如果已登录，直接跳转首页
    if (getToken()) {
      wx.switchTab({ url: '/pages/index/index' })
      return
    }
    this.refreshCaptcha()
  },

  onPhoneChange(e) {
    this.setData({ 'form.phone': e.detail.value })
  },

  onPasswordChange(e) {
    this.setData({ 'form.password': e.detail.value })
  },

  onCaptchaChange(e) {
    this.setData({ 'form.captchaCode': e.detail.value })
  },

  togglePassword() {
    this.setData({ showPassword: !this.data.showPassword })
  },

  // 刷新验证码
  async refreshCaptcha() {
    try {
      const res = await userApi.getCaptcha()
      if (res && res.captchaImage) {
        this.setData({
          captchaSrc: res.captchaImage,
          'form.captchaKey': res.captchaKey,
          'form.captchaCode': ''
        })
      }
    } catch (e) {
      console.error('获取验证码失败:', e)
    }
  },

  // 登录
  async handleLogin() {
    const { phone, password, captchaKey, captchaCode } = this.data.form

    if (!phone.trim()) {
      wx.showToast({ title: '请输入手机号', icon: 'none' })
      return
    }
    if (!password.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (phone.length !== 11) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (password.length < 6) {
      wx.showToast({ title: '密码至少6位', icon: 'none' })
      return
    }
    if (!captchaCode.trim()) {
      wx.showToast({ title: '请输入验证码', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    wx.showLoading({ title: '登录中...', mask: true })

    try {
      const res = await userApi.login({
        phone: phone.trim(),
        password: password.trim(),
        captchaKey: captchaKey,
        captchaCode: captchaCode
      })

      wx.hideLoading()

      // res 直接是后端 data（含 token 和 userInfo）
      if (res && res.token) {
        const { setToken } = require('../../utils/request')
        setToken(res.token)
        app.globalData.token = res.token
        app.globalData.isLoggedIn = true

        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/index/index' })
          // 登录后获取用户信息
          app.fetchUserInfo()
        }, 800)
      } else {
        wx.showToast({ title: '登录失败', icon: 'none' })
      }
    } catch (err) {
      wx.hideLoading()
      this.refreshCaptcha()
    } finally {
      this.setData({ loading: false })
    }
  },

  goRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  }
})
