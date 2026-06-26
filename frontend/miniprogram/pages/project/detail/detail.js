// pages/project/detail/detail.js
const { projectApi } = require('../../../utils/api')

Page({
  data: {
    id: '',
    project: {},
    milestones: [],
    ganttData: [],
    riskInfo: null,
    riskColor: '#52c41a',
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
      const res = await projectApi.getDetail(this.data.id)
      if (res.code === 200) {
        const project = res.data
        this.setData({ project })

        // 解析里程碑
        if (project.milestones) {
          const milestones = typeof project.milestones === 'string'
            ? JSON.parse(project.milestones)
            : project.milestones
          this.setData({ milestones: Array.isArray(milestones) ? milestones : [] })
        }
      }
    } catch (e) {
      // ignore
    } finally {
      this.setData({ loading: false })
    }

    // 异步加载甘特图和风险
    this.fetchGantt()
    this.fetchRisk()
  },

  async fetchGantt() {
    try {
      const res = await projectApi.getGantt(this.data.id)
      if (res.code === 200) {
        const tasks = res.data || []
        if (tasks.length > 0) {
          const times = tasks.flatMap(t => [new Date(t.startDate).getTime(), new Date(t.endDate).getTime()])
          const minTime = Math.min(...times)
          const maxTime = Math.max(...times)
          const totalMs = maxTime - minTime || 1

          const colors = ['#4e6ef2', '#52c41a', '#fa8c16', '#8b5cf6', '#f5222d', '#13c2c2']
          const ganttData = tasks.map((t, i) => ({
            name: t.name || t.title,
            left: ((new Date(t.startDate).getTime() - minTime) / totalMs) * 100,
            width: Math.max(((new Date(t.endDate).getTime() - new Date(t.startDate).getTime()) / totalMs) * 100, 3),
            color: colors[i % colors.length]
          }))

          this.setData({ ganttData })
        }
      }
    } catch (e) {
      // ignore
    }
  },

  async fetchRisk() {
    try {
      const res = await projectApi.getRisk(this.data.id)
      if (res.code === 200 && res.data) {
        const risk = res.data
        const colorMap = { '低': '#52c41a', '中': '#fa8c16', '高': '#f5222d' }
        this.setData({
          riskInfo: risk,
          riskColor: colorMap[risk.level] || '#52c41a'
        })
      }
    } catch (e) {
      // ignore
    }
  }
})
