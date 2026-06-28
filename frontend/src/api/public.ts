import request from './request'

/**
 * Public API — 无需认证的公开接口，用于落地页等场景
 */
export interface PlatformStats {
  totalUsers: number
  totalTeams: number
  totalProjects: number
  totalCompetitions: number
}

export function getPlatformStats() {
  return request.get<PlatformStats>('/public/statistics')
}
