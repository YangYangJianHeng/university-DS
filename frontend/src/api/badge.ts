import request from './request'

/** 管理员徽章管理 API */
export function adminListBadges(params: { page?: number; size?: number }) {
  return request.get('/admin/badges', { params })
}

export function adminCreateBadge(data: any) {
  return request.post('/admin/badges', data)
}

export function adminUpdateBadge(id: number, data: any) {
  return request.put(`/admin/badges/${id}`, data)
}

export function adminDeleteBadge(id: number) {
  return request.delete(`/admin/badges/${id}`)
}
