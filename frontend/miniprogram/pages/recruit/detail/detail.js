// pages/recruit/detail/detail.js
const app = getApp()
const { recruitApi } = require('../../../utils/api')
const { getStatusLabel, formatDate } = require('../../../utils/util')

Page({
  data: {
    id: '',
    recruit: {},
    skills: [],
    matchResults: [],
    loading: true,
    applying: false,
    // 申请审核相关
    applications: [],
    isPublisher: false,
    loadingApplications: false
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
      // request.js 已自动解包 Result.data，res 直接是 RecruitVO 对象
      const recruit = await recruitApi.getDetail(this.data.id)
      if (!recruit || !recruit.id) {
        wx.showToast({ title: '招募信息不存在', icon: 'none' })
        return
      }
      // 提取技能标签
      const skills = recruit.skills || recruit.requiredSkills || []
      this.setData({
        recruit,
        skills: Array.isArray(skills) ? skills : (skills.split ? skills.split(',') : [])
      })

      // 判断当前用户是否是发布者或管理员
      const currentUserId = app.globalData.userInfo?.id
      const currentUserRole = app.globalData.userInfo?.role
      if (currentUserId && (recruit.publisherId === currentUserId || currentUserRole === 'ADMIN')) {
        this.setData({ isPublisher: true })
        this.fetchApplications()
      }
      // 异步加载智能匹配
      this.fetchMatches()
    } catch (e) {
      console.error('获取招募详情失败:', e)
    } finally {
      this.setData({ loading: false })
    }
  },

  async fetchMatches() {
    try {
      // request.js 已自动解包，res 直接是 List<Map>
      const res = await recruitApi.match(this.data.id)
      this.setData({ matchResults: res || [] })
    } catch (e) {
      // ignore
    }
  },

  // 加载申请列表（仅发布者）
  async fetchApplications() {
    this.setData({ loadingApplications: true })
    try {
      const res = await recruitApi.getApplications(this.data.id)
      this.setData({ applications: res || [] })
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loadingApplications: false })
    }
  },

  // 通过申请
  async handleApprove(e) {
    const applicationId = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认操作',
      content: '确定要通过该申请吗？',
      success: async (modalRes) => {
        if (!modalRes.confirm) return
        try {
          await recruitApi.approveApplication(applicationId, true)
          wx.showToast({ title: '已通过', icon: 'success' })
          this.fetchDetail()
          this.fetchApplications()
        } catch (e) {
          // ignore
        }
      }
    })
  },

  // 拒绝申请
  async handleReject(e) {
    const applicationId = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认操作',
      content: '确定要拒绝该申请吗？',
      success: async (modalRes) => {
        if (!modalRes.confirm) return
        try {
          await recruitApi.approveApplication(applicationId, false)
          wx.showToast({ title: '已拒绝', icon: 'success' })
          this.fetchApplications()
        } catch (e) {
          // ignore
        }
      }
    })
  },

  statusLabel(status) {
    return getStatusLabel('recruit', status)
  },

  statusTheme(status) {
    const map = { OPEN: 'success', CLOSED: 'default', FULL: 'warning', AUDITING: 'warning', REJECTED: 'danger' }
    return map[status] || 'default'
  },

  applicationStatusLabel(status) {
    const map = {
      'PENDING': '待审核',
      'APPROVED': '已通过',
      'REJECTED': '已拒绝'
    }
    return map[status] || status || '未知'
  },

  applicationStatusTheme(status) {
    const map = {
      'PENDING': 'warning',
      'APPROVED': 'success',
      'REJECTED': 'danger'
    }
    return map[status] || 'default'
  },

  async handleApply() {
    if (!app.globalData.isLoggedIn) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }

    wx.showModal({
      title: '确认申请',
      content: '确定要申请加入该招募吗？',
      success: async (modalRes) => {
        if (!modalRes.confirm) return

        this.setData({ applying: true })
        try {
          await recruitApi.apply(this.data.id)
          wx.showToast({ title: '申请成功', icon: 'success' })
          this.fetchDetail()
        } catch (e) {
          // ignore
        } finally {
          this.setData({ applying: false })
        }
      }
    })
  }
})
