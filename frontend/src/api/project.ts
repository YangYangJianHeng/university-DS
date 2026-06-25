import request from './request'

export function createProject(data: any) {
  return request.post('/project', data)
}

export function getProject(id: number) {
  return request.get(`/project/${id}`)
}

export function updateProject(id: number, data: any) {
  return request.put(`/project/${id}`, data)
}

export function addMilestone(projectId: number, data: any) {
  return request.post(`/project/${projectId}/milestone`, data)
}

export function archiveProject(id: number) {
  return request.post(`/project/${id}/archive`)
}

export function getGanttData(id: number) {
  return request.get(`/project/${id}/gantt`)
}

export function getRiskAssessment(id: number) {
  return request.get(`/project/${id}/risk`)
}

export function getMyProjects() {
  return request.get('/project/my')
}
