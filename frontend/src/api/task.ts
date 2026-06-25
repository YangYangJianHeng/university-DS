import request from './request'

export function createTask(data: any) {
  return request.post('/task', data)
}

export function getTask(id: number) {
  return request.get(`/task/${id}`)
}

export function updateTask(id: number, data: any) {
  return request.put(`/task/${id}`, data)
}

export function updateTaskStatus(id: number, status: string) {
  return request.put(`/task/${id}/status`, { status })
}

export function listTasks(params: any) {
  return request.get('/task/list', { params })
}

export function getMyTasks() {
  return request.get('/task/my')
}
