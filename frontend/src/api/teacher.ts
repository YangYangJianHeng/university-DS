import request from './request'

export function registerTeacher(data: any) {
  return request.post('/teacher/register', data)
}

export function listTeachers(params: any) {
  return request.get('/teacher/list', { params })
}

export function getTeacher(id: number) {
  return request.get(`/teacher/${id}`)
}

export function inviteTeacher(teamId: number, teacherId: number, message?: string) {
  return request.post(`/team/${teamId}/teacher/invite`, { teacherId, message })
}

export function getMyTeams() {
  return request.get('/teacher/my-teams')
}

export function getPendingInvitations() {
  return request.get('/teacher/invitations/pending')
}

export function approveInvitation(id: number) {
  return request.put(`/teacher/invite/${id}/approve`)
}
