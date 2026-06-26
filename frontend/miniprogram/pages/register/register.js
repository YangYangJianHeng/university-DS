// pages/register/register.js
const app = getApp()
const { userApi } = require('../../utils/api')

Page({
  data: {
    form: {
      phone: '',
      username: '',
      password: '',
      confirmPassword: '',
      name: '',
      studentId: ''
    },
    loading: false
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  async handleRegister() {
    const { phone, username, password, confirmPassword, name, studentId } = this.data.form

    // 验证
    if (!phone.trim()) {
      wx.showToast({ title: '请输入手机号', icon: 'none' })
      return
    }
    if (phone.length !== 11) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!username.trim()) {
      wx.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }
    if (!password.trim()) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (password.length < 6 || password.length > 20) {
      wx.showToast({ title: '密码长度6-20位', icon: 'none' })
      return
    }
    if (password !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' })
      return
    }

    this.setData({ loading: true })

    try {
      const res = await userApi.register({
        phone: phone.trim(),
        username: username.trim(),
        password: password.trim(),
        name: name.trim(),
        studentId: studentId.trim()
      })

      if (res.code === 200) {
        wx.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 500)
      }
    } catch (err) {
      console.error('注册失败:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  goLogin() {
    wx.navigateBack()
  }
})
