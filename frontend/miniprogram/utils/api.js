// utils/api.js - 各模块API封装
const { http } = require('./request')

// ===== 用户模块 =====
const userApi = {
  login: (data) => http.post('/user/login', data),
  register: (data) => http.post('/user/register', data),
  getProfile: () => http.get('/user/profile'),
  updateProfile: (data) => http.put('/user/profile', data),
  getSkills: () => http.get('/user/skills'),
  addSkill: (data) => http.post('/user/skills', data),
  deleteSkill: (skillId) => http.delete(`/user/skills/${skillId}`),
  getUserProfile: (userId) => http.get(`/user/profile/${userId}`)
}

// ===== 招募模块 =====
const recruitApi = {
  getList: (params) => http.get('/recruit/list', params),
  getDetail: (id) => http.get(`/recruit/${id}`),
  create: (data) => http.post('/recruit', data),
  update: (id, data) => http.put(`/recruit/${id}`, data),
  delete: (id) => http.delete(`/recruit/${id}`),
  match: (recruitId) => http.get('/recruit/match', { recruitId }),
  apply: (id) => http.post(`/recruit/${id}/apply`),
  getMyRecruits: () => http.get('/recruit/my'),
  getApplications: (id) => http.get(`/recruit/${id}/applications`),
  approveApplication: (applicationId, approved) => http.put(`/recruit/applications/${applicationId}?approved=${approved}`)
}

// ===== 团队模块 =====
const teamApi = {
  getMyTeams: () => http.get('/team/my'),
  getJoinedTeams: () => http.get('/team/joined'),
  getDetail: (id) => http.get(`/team/${id}`),
  create: (data) => http.post('/team', data),
  update: (id, data) => http.put(`/team/${id}`, data),
  join: (id) => http.post(`/team/${id}/join`),
  quit: (id) => http.post(`/team/${id}/quit`),
  approveMember: (id, memberId, approved) => http.put(`/team/${id}/approve?memberId=${memberId}&approved=${approved}`),
  removeMember: (id, userId) => http.delete(`/team/${id}/member/${userId}`),
  getMembers: (id) => http.get(`/team/${id}/members`),
  disband: (id) => http.post(`/team/${id}/disband`),
  getTimeline: (id) => http.get(`/team/${id}/timeline`)
}

// ===== 项目模块 =====
const projectApi = {
  getMyProjects: () => http.get('/project/my'),
  getDetail: (id) => http.get(`/project/${id}`),
  create: (data) => http.post('/project', data),
  update: (id, data) => http.put(`/project/${id}`, data),
  addMilestone: (id, data) => http.post(`/project/${id}/milestone`, data),
  updateMilestone: (id, milestoneId, data) => http.put(`/project/${id}/milestone/${milestoneId}`, data),
  archive: (id) => http.post(`/project/${id}/archive`),
  getGantt: (id) => http.get(`/project/${id}/gantt`),
  getRisk: (id) => http.get(`/project/${id}/risk`)
}

// ===== 任务模块 =====
const taskApi = {
  getMyTasks: () => http.get('/task/my'),
  getList: (projectId) => http.get('/task/list', { projectId }),
  getDetail: (id) => http.get(`/task/${id}`),
  create: (data) => http.post('/task', data),
  update: (id, data) => http.put(`/task/${id}`, data),
  updateStatus: (id, status) => http.put(`/task/${id}/status`, { status }),
  createVote: (data) => http.post('/vote', data),
  castVote: (voteId, data) => http.post(`/vote/${voteId}/cast`, data),
  getWeeklyReport: (teamId) => http.get(`/team/${teamId}/weekly-report`)
}

// ===== 竞赛模块 =====
const competitionApi = {
  getList: (params) => http.get('/competition/list', params),
  getDetail: (id) => http.get(`/competition/${id}`),
  create: (data) => http.post('/competition', data),
  update: (id, data) => http.put(`/competition/${id}`, data),
  recommend: () => http.get('/competition/recommend'),
  toggleCollect: (id) => http.post(`/competition/${id}/collect`),
  getCalendar: (params) => http.get('/competition/calendar', params)
}

// ===== 导师模块 =====
const teacherApi = {
  getList: (params) => http.get('/teacher/list', params),
  getDetail: (id) => http.get(`/teacher/${id}`),
  register: (data) => http.post('/teacher/register', data),
  update: (id, data) => http.put(`/teacher/${id}`, data),
  invite: (teamId, teacherId) => http.post(`/team/${teamId}/teacher/invite`, { teacherId }),
  approveInvite: (id) => http.put(`/teacher/invite/${id}/approve`),
  getMyTeams: () => http.get('/teacher/my-teams'),
  getGuidanceLogs: () => http.get('/teacher/guidance-logs'),
  addGuidanceLog: (data) => http.post('/teacher/guidance-log', data)
}

// ===== 成果模块 =====
const achievementApi = {
  getList: (params) => http.get('/achievement/list', params),
  getDetail: (id) => http.get(`/achievement/${id}`),
  submit: (data) => http.post('/achievement', data),
  update: (id, data) => http.put(`/achievement/${id}`, data),
  toggleLike: (id) => http.post(`/achievement/${id}/like`),
  toggleCollect: (id) => http.post(`/achievement/${id}/collect`),
  getStatistics: () => http.get('/achievement/statistics')
}

// ===== 通知模块 =====
const notificationApi = {
  getList: () => http.get('/notification/list'),
  getUnreadCount: () => http.get('/notification/unread-count'),
  markAsRead: (id) => http.put(`/notification/${id}/read`),
  markAllRead: () => http.put('/notification/read-all')
}

// ===== 仪表盘模块 =====
const dashboardApi = {
  getDashboard: () => http.get('/dashboard'),
  getExperiences: () => http.get('/profile/experience'),
  getSkillRadar: () => http.get('/profile/skill-radar'),
  getStats: () => http.get('/profile/statistics'),
  getBadges: () => http.get('/profile/badges')
}

// ===== 日历模块 =====
const calendarApi = {
  getEvents: (params) => http.get('/calendar/events', params),
  checkConflict: (data) => http.post('/calendar/check-conflict', data),
  getReminders: () => http.get('/calendar/reminders'),
  addEvent: (data) => http.post('/calendar/event', data)
}

// ===== 文件模块 =====
const fileApi = {
  getList: (params) => http.get('/file/list', params),
  upload: (filePath, data = {}) => {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: getApp().globalData.baseUrl + '/file/upload',
        filePath,
        name: 'file',
        formData: data,
        header: {
          Authorization: `Bearer ${getApp().globalData.token}`
        },
        success: (res) => {
          try {
            const result = JSON.parse(res.data)
            if (result.code === 200) resolve(result)
            else reject(result)
          } catch (e) {
            reject(e)
          }
        },
        fail: reject
      })
    })
  },
  download: (id) => http.get(`/file/${id}/download`),
  delete: (id) => http.delete(`/file/${id}`),
  getVersions: (id) => http.get(`/file/${id}/versions`),
  rollback: (fileId, versionId) => http.put(`/file/${fileId}/rollback/${versionId}`)
}

// ===== 管理员模块 =====
const adminApi = {
  // 用户管理
  getUsers: (params) => http.get('/admin/users', params),
  updateUserStatus: (id, status) => http.put(`/admin/users/${id}/status?status=${status}`),
  updateUserRole: (id, role) => http.put(`/admin/users/${id}/role?role=${role}`),
  // 审核日志
  getAuditLogs: (params) => http.get('/admin/audit/list', params),
  processAudit: (id, action) => http.put(`/admin/audit/${id}?action=${action}`),
  // 招募审核
  getPendingRecruits: (params) => http.get('/admin/recruits/pending', params),
  auditRecruit: (id, action) => http.put(`/admin/recruits/${id}/audit?action=${action}`),
  // 统计与报表
  getStats: () => http.get('/admin/dashboard/stats'),
  getWeeklyReport: () => http.get('/admin/report/weekly'),
  // 公告管理
  getAnnouncements: () => http.get('/admin/announcements'),
  createAnnouncement: (data) => http.post('/admin/announcement', data),
  // 项目管理
  getProjects: (params) => http.get('/admin/projects', params),
  updateProject: (id, data) => http.put(`/admin/projects/${id}`, data),
  deleteProject: (id) => http.delete(`/admin/projects/${id}`),
  // 任务管理
  getTasks: (params) => http.get('/admin/tasks', params),
  updateTask: (id, data) => http.put(`/admin/tasks/${id}`, data),
  deleteTask: (id) => http.delete(`/admin/tasks/${id}`),
  // 团队管理
  getTeams: (params) => http.get('/admin/teams', params),
  updateTeam: (id, data) => http.put(`/admin/teams/${id}`, data),
  deleteTeam: (id) => http.delete(`/admin/teams/${id}`),
  // 导师管理
  getTeachers: (params) => http.get('/admin/teachers', params),
  updateTeacher: (id, data) => http.put(`/admin/teachers/${id}`, data),
  deleteTeacher: (id) => http.delete(`/admin/teachers/${id}`)
}

module.exports = {
  userApi,
  recruitApi,
  teamApi,
  projectApi,
  taskApi,
  competitionApi,
  teacherApi,
  achievementApi,
  notificationApi,
  dashboardApi,
  calendarApi,
  fileApi,
  adminApi
}
