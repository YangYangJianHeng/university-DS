import request from './request'

export function register(data: any) {
  return request.post('/user/register', data)
}

export function login(data: any) {
  return request.post('/user/login', data)
}

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data: any) {
  return request.put('/user/profile', data)
}

export function getUserProfile(userId: number) {
  return request.get(`/user/profile/${userId}`)
}

export function addSkill(skillName: string, level: string) {
  return request.post('/user/skills', { skillName, level })
}

export function deleteSkill(id: number) {
  return request.delete(`/user/skills/${id}`)
}

export function uploadAvatar(formData: FormData) {
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put('/user/password', data)
}
