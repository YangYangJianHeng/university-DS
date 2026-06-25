import request from './request'

export function createRecruit(data: any) {
  return request.post('/recruit', data)
}

export function getRecruit(id: number) {
  return request.get(`/recruit/${id}`)
}

export function listRecruits(params: any) {
  return request.get('/recruit/list', { params })
}

export function updateRecruit(id: number, data: any) {
  return request.put(`/recruit/${id}`, data)
}

export function closeRecruit(id: number) {
  return request.delete(`/recruit/${id}`)
}

export function matchRecruit(id: number) {
  return request.get('/recruit/match', { params: { recruitId: id } })
}

export function applyRecruit(id: number) {
  return request.post(`/recruit/${id}/apply`)
}

export function getMyRecruits() {
  return request.get('/recruit/my')
}

export function getApplications(recruitId: number) {
  return request.get(`/recruit/${recruitId}/applications`)
}

export function approveApplication(applicationId: number, approved: boolean) {
  return request.put(`/recruit/applications/${applicationId}`, null, { params: { approved } })
}

// 招募转项目
export function convertRecruitToProject(recruitId: number, data: {
  projectName?: string
  projectDescription?: string
  continueRecruit: number
  tasks?: Array<{
    title: string
    description?: string
    priority?: string
    deadline?: string
  }>
}) {
  return request.post(`/recruit/${recruitId}/convert-to-project`, data)
}
