// pages/ai-chat/index/index.js
const app = getApp()
const { aiApi } = require('../../../utils/api')

Page({
  data: {
    messages: [],
    inputValue: '',
    loading: false,
    scrollToView: '',
    userInitial: 'U',
    suggestions: [
      '如何快速组建竞赛团队？',
      '推荐适合计算机专业的竞赛',
      '如何提升项目管理的效率？',
      '组队时应该注意哪些问题？'
    ]
  },

  onLoad() {
    const userInfo = app.globalData.userInfo || {}
    this.setData({
      userInitial: (userInfo.username || 'U').charAt(0).toUpperCase()
    })
  },

  onInputChange(e) {
    this.setData({ inputValue: e.detail.value })
  },

  sendSuggestion(e) {
    const text = e.currentTarget.dataset.text
    this.data.inputValue = text
    this.handleSend()
  },

  async handleSend() {
    const text = this.data.inputValue.trim()
    if (!text || this.data.loading) return

    // 添加用户消息
    const userMsg = { role: 'user', content: text }
    const messages = [...this.data.messages, userMsg]
    this.setData({
      messages,
      inputValue: '',
      loading: true
    })

    // 滚动到底部
    this.scrollToBottom()

    try {
      // 构建发送给AI的历史记录
      const history = messages.slice(-10).map(m => ({
        role: m.role,
        content: m.content
      }))

      const res = await aiApi.sendMessage(text, history)

      // 添加AI回复
      const aiReply = res && res.reply ? res.reply : (typeof res === 'string' ? res : '抱歉，我暂时无法回答这个问题，请稍后再试。')
      const aiMsg = { role: 'assistant', content: aiReply }
      this.setData({
        messages: [...this.data.messages, aiMsg],
        loading: false
      })
      this.scrollToBottom()
    } catch (err) {
      console.error('AI请求失败:', err)
      const errorMsg = { role: 'assistant', content: '抱歉，服务器出错了，请稍后再试。' }
      this.setData({
        messages: [...this.data.messages, errorMsg],
        loading: false
      })
      this.scrollToBottom()
    }
  },

  scrollToBottom() {
    const len = this.data.messages.length
    this.setData({ scrollToView: `msg-${len - 1}` })
  }
})
