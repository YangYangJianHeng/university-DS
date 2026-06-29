/**
 * 青创协同 API 封装
 * 全面对接后端所有API接口
 */
const { get, post, put, del, upload } = require('./request');

// ==================== 用户认证 ====================
const userApi = {
  getCaptcha() { return get('/captcha'); },
  register(data) { return post('/user/register', data); },
  login(data) { return post('/user/login', data); },
  logout() { return post('/user/logout'); },
  getProfile() { return get('/user/profile'); },
  updateProfile(data) { return put('/user/profile', data); },
  getUserProfile(userId) { return get(`/user/profile/${userId}`); },
  addSkill(skillName, level) { return post('/user/skills', { skillName, level }); },
  deleteSkill(id) { return del(`/user/skills/${id}`); },
  uploadAvatar(filePath) { return upload('/user/avatar', filePath); },
  changePassword(data) { return put('/user/password', data); },
  getSkills() { return get('/user/skills'); }
};

// ==================== 团队管理 ====================
const teamApi = {
  createTeam(data) { return post('/team', data); },
  getTeam(id) { return get(`/team/${id}`); },
  updateTeam(id, data) { return put(`/team/${id}`, data); },
  getMyTeams() { return get('/team/my'); },
  getJoinedTeams() { return get('/team/joined'); },
  joinTeam(id) { return post(`/team/${id}/join`); },
  approveMember(teamId, userId, approved) { return put(`/team/${teamId}/approve`, { userId, approved }); },
  removeMember(teamId, userId) { return del(`/team/${teamId}/member/${userId}`); },
  quitTeam(id) { return post(`/team/${id}/quit`); },
  getMembers(id) { return get(`/team/${id}/members`); },
  getTimeline(id) { return get(`/team/${id}/timeline`); }
};

// ==================== 招募管理 ====================
const recruitApi = {
  create(data) { return post('/recruit', data); },
  getDetail(id) { return get(`/recruit/${id}`); },
  getList(params) { return get('/recruit/list', params); },
  update(id, data) { return put(`/recruit/${id}`, data); },
  close(id) { return del(`/recruit/${id}`); },
  match(id) { return get(`/recruit/match`, { recruitId: id }); },
  apply(id) { return post(`/recruit/${id}/apply`); },
  getMyRecruits() { return get('/recruit/my'); },
  getApplications(id) { return get(`/recruit/${id}/applications`); },
  approveApplication(applicationId, approved) { return put(`/recruit/applications/${applicationId}`, null, { params: { approved } }); },
  convertToProject(id, data) { return post(`/recruit/${id}/convert-to-project`, data); },
  toggleContinueRecruit(id, continueRecruit) { return put(`/recruit/${id}/continue-recruit`, null, { params: { continueRecruit } }); }
};

// ==================== 项目管理 ====================
const projectApi = {
  create(data) { return post('/project', data); },
  getDetail(id) { return get(`/project/${id}`); },
  getList(params) { return get('/project/list', params); },
  update(id, data) { return put(`/project/${id}`, data); },
  delete(id) { return del(`/project/${id}`); },
  getMembers(id) { return get(`/project/${id}/members`); },
  updateStatus(id, status) { return put(`/project/${id}/status`, null, { params: { status } }); },
  getGantt(id) { return get(`/project/${id}/gantt`); },
  getRisk(id) { return get(`/project/${id}/risk`); },
  getMyProjects() { return get('/project/my'); }
};

// ==================== 任务管理 ====================
const taskApi = {
  create(data) { return post('/task', data); },
  getDetail(id) { return get(`/task/${id}`); },
  getList(params) { return get('/task/list', params); },
  update(id, data) { return put(`/task/${id}`, data); },
  delete(id) { return del(`/task/${id}`); },
  updateStatus(id, status) { return put(`/task/${id}/status`, null, { params: { status } }); },
  assignTask(id, userId) { return put(`/task/${id}/assign`, { userId }); }
};

// ==================== 竞赛中心 ====================
const competitionApi = {
  getList(params) { return get('/competition/list', params); },
  getDetail(id) { return get(`/competition/${id}`); },
  create(data) { return post('/competition', data); },
  getMyCompetitions() { return get('/competition/my'); },
  toggleCollect(id) { return post(`/competition/${id}/collect`); }
};

// ==================== 导师库 ====================
const teacherApi = {
  getList(params) { return get('/teacher/list', params); },
  getDetail(id) { return get(`/teacher/${id}`); },
  applyTeacher(data) { return post('/teacher/apply', data); },
  invite(teamId, teacherId) { return post(`/team/${teamId}/teacher/invite`, { teacherId }); },
  /** 更新当前登录导师的院系、职称、研究方向 */
  updateMyInfo(data) { return put('/user/teacher-profile', data); }
};

// ==================== 成果展厅 ====================
const achievementApi = {
  getList(params) { return get('/achievement/list', params); },
  getDetail(id) { return get(`/achievement/${id}`); },
  create(data) { return post('/achievement', data); },
  update(id, data) { return put(`/achievement/${id}`, data); },
  delete(id) { return del(`/achievement/${id}`); },
  toggleLike(id) { return post(`/achievement/${id}/like`); },
  toggleCollect(id) { return post(`/achievement/${id}/collect`); }
};

// ==================== 通知消息 ====================
const notificationApi = {
  getList(params) { return get('/notification/list', params); },
  getUnreadCount() { return get('/notification/unread-count'); },
  markRead(id) { return put(`/notification/${id}/read`); },
  markAllRead() { return put('/notification/read-all'); }
};

// ==================== AI助手 ====================
const aiApi = {
  sendMessage(message, history = []) { return post('/ai/chat', { message, history }); },
  streamChat(message, history = []) { return post('/ai/chat/stream', { message, history }); }
};

// ==================== 管理后台 ====================
const adminApi = {
  // 仪表盘
  getStats() { return get('/admin/dashboard/stats'); },
  getWeeklyReport() { return get('/admin/report/weekly'); },
  // 用户管理
  getUsers(params) { return get('/admin/users', params); },
  updateUserStatus(id, status) { return put(`/admin/users/${id}/status`, null, { params: { status } }); },
  updateUserRole(id, role) { return put(`/admin/users/${id}/role`, null, { params: { role } }); },
  // 审核管理
  getAuditList(params) { return get('/admin/audit/list', params); },
  handleAudit(id, action) { return put(`/admin/audit/${id}`, null, { params: { action } }); },
  getPendingRecruits(params) { return get('/admin/recruits/pending', params); },
  auditRecruit(id, action) { return put(`/admin/recruits/${id}/audit`, null, { params: { action } }); },
  // 公告管理
  createAnnouncement(data) { return post('/admin/announcement', data); },
  getAnnouncements() { return get('/admin/announcements'); },
  // 项目管理
  getAllProjects(params) { return get('/admin/projects', params); },
  updateProject(id, data) { return put(`/admin/projects/${id}`, data); },
  deleteProject(id) { return del(`/admin/projects/${id}`); },
  // 团队管理
  getAllTeams(params) { return get('/admin/teams', params); },
  updateTeam(id, data) { return put(`/admin/teams/${id}`, data); },
  deleteTeam(id) { return del(`/admin/teams/${id}`); },
  // 任务管理
  getAllTasks(params) { return get('/admin/tasks', params); },
  updateTask(id, data) { return put(`/admin/tasks/${id}`, data); },
  deleteTask(id) { return del(`/admin/tasks/${id}`); },
  // 导师管理
  getAllTeachers(params) { return get('/admin/teachers', params); },
  getPendingTeachers(params) { return get('/admin/teachers/pending', params); },
  approveTeacher(id) { return put(`/admin/teachers/${id}/approve`); },
  updateTeacherInfo(id, data) { return put(`/admin/teachers/${id}`, data); },
  deleteTeacher(id) { return del(`/admin/teachers/${id}`); }
};

module.exports = {
  userApi,
  teamApi,
  recruitApi,
  projectApi,
  taskApi,
  competitionApi,
  teacherApi,
  achievementApi,
  notificationApi,
  aiApi,
  adminApi
};
