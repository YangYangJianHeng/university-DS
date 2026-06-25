import request from './request'

export function submitAchievement(data: any) {
  return request.post('/achievement', data)
}

export function listAchievements(params: any) {
  return request.get('/achievement/list', { params })
}

export function getAchievement(id: number) {
  return request.get(`/achievement/${id}`)
}

export function likeAchievement(id: number) {
  return request.post(`/achievement/${id}/like`)
}

export function getAchievementStatistics() {
  return request.get('/achievement/statistics')
}

// Admin achievement management
export function adminListAchievements(params: any) {
  return request.get('/admin/achievements', { params })
}

export function adminUpdateAchievement(id: number, data: any) {
  return request.put(`/admin/achievements/${id}`, data)
}

export function adminDeleteAchievement(id: number) {
  return request.delete(`/admin/achievements/${id}`)
}
