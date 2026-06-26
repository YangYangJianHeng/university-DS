// pages/login/login.js
const app = getApp()
const { userApi } = require('../../utils/api')

Page({
  data: {
    form: {
      phone: '',
      password: ''
    },
    loading: false
  },

  onPhoneChange(e) {
    this.setData({ 'form.phone': e.detail.value })
  },

  onPasswordChange(e) {
    this.setData({ 'form.password': e.detail.value })
  },

  // 登录
  async handleLogin() {
    const { phone, password } = this.data.form

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

    this.setData({ loading: true })

    try {
      const res = await userApi.login({
        phone: phone.trim(),
        password: password.trim()
      })

      if (res.code === 200) {
        const { token, userInfo } = res.data
        app.setLogin(token, userInfo)

        wx.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          wx.switchTab({ url: '/pages/index/index' })
        }, 500)
      }
    } catch (err) {
      console.error('登录失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  // 跳转注册
  goRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  }
})
