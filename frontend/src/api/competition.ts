import request from './request'

export function listCompetitions(params: any) {
  return request.get('/competition/list', { params })
}

export function getCompetition(id: number) {
  return request.get(`/competition/${id}`)
}

export function createCompetition(data: any) {
  return request.post('/competition', data)
}

export function updateCompetition(id: number, data: any) {
  return request.put(`/competition/${id}`, data)
}

export function getRecommendedCompetitions() {
  return request.get('/competition/recommend')
}

export function collectCompetition(id: number) {
  return request.post(`/competition/${id}/collect`)
}

export function getCompetitionCalendar() {
  return request.get('/competition/calendar')
}

// Admin competition management
export function adminListCompetitions(params: any) {
  return request.get('/admin/competitions', { params })
}

export function adminUpdateCompetition(id: number, data: any) {
  return request.put(`/admin/competitions/${id}`, data)
}

export function adminDeleteCompetition(id: number) {
  return request.delete(`/admin/competitions/${id}`)
}
