import request from './request'

export function listNotifications() {
  return request.get('/notification/list')
}

export function markAsRead(id: number) {
  return request.put(`/notification/${id}/read`)
}

export function getUnreadCount() {
  return request.get('/notification/unread-count')
}
