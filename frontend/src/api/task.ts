import request from './request'

// ========== 任务管理 ==========

export function createTask(data: any) {
  return request.post('/task', data)
}

export function getTaskDetail(id: number) {
  return request.get(`/task/${id}`)
}

export function updateTask(id: number, data: any) {
  return request.put(`/task/${id}`, data)
}

export function updateTaskStatus(id: number, status: string) {
  return request.put(`/task/${id}/status`, { status })
}

export function listTasks(params: any) {
  return request.get('/task/list', { params })
}

export function getMyTasks() {
  return request.get('/task/my')
}

export function getManagedTasks() {
  return request.get('/task/managed')
}

// ========== 人员指派 ==========

export function recommendAssignees(taskId: number) {
  return request.get(`/task/${taskId}/recommend`)
}

export function addAssignee(taskId: number, userId: number, assignmentNote?: string) {
  return request.post(`/task/${taskId}/assignee`, { userId, assignmentNote })
}

export function removeAssignee(taskId: number, userId: number) {
  return request.delete(`/task/${taskId}/assignee/${userId}`)
}

export function acceptTask(taskId: number, accept: boolean) {
  return request.put(`/task/${taskId}/accept`, { accept })
}

export function getAssignees(taskId: number) {
  return request.get(`/task/${taskId}/assignees`)
}

// ========== 成果提交 ==========

export function submitTaskResult(data: { taskId: number; title: string; description?: string; fileUrls?: string[] }) {
  return request.post('/task/submission', data)
}

export function getSubmissions(taskId: number) {
  return request.get(`/task/${taskId}/submissions`)
}

export function reviewSubmission(submissionId: number, action: string, comment?: string) {
  return request.put(`/task/submission/${submissionId}/review`, { action, comment })
}

// ========== 评论 ==========

export function addComment(taskId: number, content: string, mentionedUserIds?: number[]) {
  return request.post(`/task/${taskId}/comment`, { content, mentionedUserIds })
}

export function getComments(taskId: number) {
  return request.get(`/task/${taskId}/comments`)
}
