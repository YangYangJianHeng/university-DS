import request from './request'

export function listCompetitions(params: any) {
  return request.get('/competition/list', { params })
}

export function getCompetition(id: number) {
  return request.get(`/competition/${id}`)
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
