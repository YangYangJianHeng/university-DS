// pages/recruit/create/create.js
const app = getApp()
const { recruitApi } = require('../../../utils/api')

Page({
  data: {
    form: {
      title: '',
      projectType: '',
      requiredCount: '',
      deadline: '',
      description: ''
    },
    skills: [],
    skillInput: '',
    dateVisible: false,
    submitting: false
  },

  onFieldChange(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onSkillInputChange(e) {
    this.setData({ skillInput: e.detail.value })
  },

  addSkill() {
    const skill = this.data.skillInput.trim()
    if (!skill) return
    if (this.data.skills.includes(skill)) {
      wx.showToast({ title: '技能已存在', icon: 'none' })
      return
    }
    this.setData({
      skills: [...this.data.skills, skill],
      skillInput: ''
    })
  },

  removeSkill(e) {
    const index = e.currentTarget.dataset.index
    const skills = this.data.skills.filter((_, i) => i !== index)
    this.setData({ skills })
  },

  showDatePicker() {
    this.setData({ dateVisible: true })
  },

  onDateConfirm(e) {
    this.setData({
      'form.deadline': e.detail.value,
      dateVisible: false
    })
  },

  onDateClose() {
    this.setData({ dateVisible: false })
  },

  async handleSubmit() {
    if (!app.globalData.isLoggedIn) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }

    const { form, skills } = this.data

    if (!form.title.trim()) {
      wx.showToast({ title: '请输入项目名称', icon: 'none' })
      return
    }
    if (!form.requiredCount || parseInt(form.requiredCount) < 1) {
      wx.showToast({ title: '请输入有效的招募人数', icon: 'none' })
      return
    }

    this.setData({ submitting: true })

    try {
      const data = {
        title: form.title.trim(),
        projectType: form.projectType.trim(),
        requiredCount: parseInt(form.requiredCount),
        deadline: form.deadline ? form.deadline + 'T23:59:59' : '',
        description: form.description.trim(),
        requiredSkills: skills
      }

      const res = await recruitApi.create(data)
      if (res.code === 200) {
        wx.showToast({ title: '发布成功', icon: 'success' })
        setTimeout(() => {
          wx.navigateBack()
        }, 500)
      }
    } catch (e) {
      // ignore
    } finally {
      this.setData({ submitting: false })
    }
  }
})
