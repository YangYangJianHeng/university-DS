import request from './request'

export function getProjectFiles(teamId: number, projectId: number) {
  return request.get('/file/list', { params: { teamId, projectId } })
}

export function getTeamFiles(teamId: number) {
  return request.get('/file/list', { params: { teamId } })
}

export function uploadFile(file: File, teamId: number, projectId?: number, parentId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('parentId', String(parentId ?? 0))
  if (teamId) formData.append('teamId', String(teamId))
  if (projectId) formData.append('projectId', String(projectId))
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function createFolder(teamId: number, name: string, projectId?: number, parentId?: number) {
  return request.post('/file/folder', { teamId, projectId, parentId, name })
}

export function deleteFile(id: number) {
  return request.delete(`/file/${id}`)
}

export function renameFile(id: number, name: string) {
  return request.put(`/file/${id}/rename`, { name })
}

export function getDownloadUrl(id: number) {
  return `/api/file/${id}/download`
}

export function getPreviewUrl(id: number) {
  return `/api/file/${id}/preview`
}
