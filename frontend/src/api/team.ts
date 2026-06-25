import request from './request'

export function createTeam(data: any) {
  return request.post('/team', data)
}

export function getTeam(id: number) {
  return request.get(`/team/${id}`)
}

export function updateTeam(id: number, data: any) {
  return request.put(`/team/${id}`, data)
}

export function getMyTeams() {
  return request.get('/team/my')
}

export function getJoinedTeams() {
  return request.get('/team/joined')
}

export function joinTeam(id: number) {
  return request.post(`/team/${id}/join`)
}

export function approveMember(teamId: number, userId: number, approved: boolean) {
  return request.put(`/team/${teamId}/approve`, { userId, approved })
}

export function removeMember(teamId: number, userId: number) {
  return request.delete(`/team/${teamId}/member/${userId}`)
}

export function quitTeam(id: number) {
  return request.post(`/team/${id}/quit`)
}

export function getMembers(id: number) {
  return request.get(`/team/${id}/members`)
}

export function getTimeline(id: number) {
  return request.get(`/team/${id}/timeline`)
}
