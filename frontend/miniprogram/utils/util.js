// utils/util.js - 通用工具函数

/**
 * 格式化日期
 */
function formatDate(dateStr, fmt = 'yyyy-MM-dd HH:mm') {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const o = {
    'M+': d.getMonth() + 1,
    'd+': d.getDate(),
    'H+': d.getHours(),
    'm+': d.getMinutes(),
    's+': d.getSeconds(),
    'q+': Math.floor((d.getMonth() + 3) / 3),
    'S': d.getMilliseconds()
  }
  if (/(y+)/.test(fmt)) {
    fmt = fmt.replace(RegExp.$1, (d.getFullYear() + '').substr(4 - RegExp.$1.length))
  }
  for (const k in o) {
    if (new RegExp('(' + k + ')').test(fmt)) {
      fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)))
    }
  }
  return fmt
}

/**
 * 相对时间
 */
function timeAgo(dateStr) {
  if (!dateStr) return ''
  const now = Date.now()
  const past = new Date(dateStr).getTime()
  const diff = now - past
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < 30 * day) return Math.floor(diff / day) + '天前'
  return formatDate(dateStr)
}

/**
 * 状态映射
 */
const statusMap = {
  recruit: {
    OPEN: '招募中',
    CLOSED: '已结束',
    FULL: '已满员',
    AUDITING: '审核中',
    REJECTED: '已驳回'
  },
  task: {
    TODO: '待开始',
    IN_PROGRESS: '进行中',
    DONE: '已完成',
    OVERDUE: '已逾期'
  },
  application: {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  },
  competition: {
    UPCOMING: '即将开始',
    REGISTERING: '报名中',
    ONGOING: '进行中',
    FINISHED: '已结束'
  }
}

function getStatusLabel(type, status) {
  return (statusMap[type] && statusMap[type][status]) || status || '未知'
}

function getStatusTagClass(type, status) {
  const classMap = {
    OPEN: 'tag-success',
    CLOSED: 'tag-info',
    FULL: 'tag-warning',
    AUDITING: 'tag-warning',
    REJECTED: 'tag-danger',
    TODO: 'tag-info',
    IN_PROGRESS: 'tag-primary',
    DONE: 'tag-success',
    OVERDUE: 'tag-danger',
    PENDING: 'tag-warning',
    APPROVED: 'tag-success',
    UPCOMING: 'tag-info',
    REGISTERING: 'tag-primary',
    ONGOING: 'tag-success',
    FINISHED: 'tag-info'
  }
  return classMap[status] || 'tag-info'
}

/**
 * 竞赛级别
 */
function getLevelLabel(level) {
  const map = {
    NATIONAL: '国家级',
    PROVINCIAL: '省级',
    SCHOOL: '校级',
    national: '国家级',
    provincial: '省级',
    school: '校级'
  }
  return map[level] || level || '校级'
}

/**
 * 角色映射
 */
function getRoleLabel(role) {
  const map = {
    ADMIN: '管理员',
    TEACHER: '导师',
    REVIEWER: '审核员',
    STUDENT: '学生',
    USER: '普通用户'
  }
  return map[role] || '学生'
}

/**
 * 招募状态颜色
 */
function getRecruitStatusColor(status) {
  const map = {
    OPEN: '#52c41a',
    CLOSED: '#8c8c8c',
    FULL: '#fa8c16',
    AUDITING: '#fa8c16',
    REJECTED: '#f5222d'
  }
  return map[status] || '#8c8c8c'
}

/**
 * 防抖
 */
function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}

/**
 * 节流
 */
function throttle(fn, delay = 300) {
  let last = 0
  return function (...args) {
    const now = Date.now()
    if (now - last >= delay) {
      last = now
      fn.apply(this, args)
    }
  }
}

module.exports = {
  formatDate,
  timeAgo,
  getStatusLabel,
  getStatusTagClass,
  getLevelLabel,
  getRoleLabel,
  getRecruitStatusColor,
  debounce,
  throttle
}
