import request from './request'

export function getDashboard() {
  return request.get('/dashboard')
}

export function getExperience() {
  return request.get('/profile/experience')
}

export function getSkillRadar() {
  return request.get('/profile/skill-radar')
}

export function getStatistics() {
  return request.get('/profile/statistics')
}

export function getBadges() {
  return request.get('/profile/badges')
}
