import request from './request'

/** 检查并自动生成新证书 */
export function checkNewCertificates() {
  return request.get('/profile/certificates/check-new')
}

/** 获取用户所有证书 */
export function getUserCertificates() {
  return request.get('/profile/certificates')
}

/** 管理员同步奖状（为所有满足条件的用户生成奖状） */
export function adminSyncCertificates(badgeId?: number) {
  return request.post('/admin/certificates/sync', null, { params: badgeId ? { badgeId } : {} })
}

/** 管理员获取所有证书列表 */
export function adminListCertificates(params: { page?: number; size?: number }) {
  return request.get('/admin/certificates', { params })
}

/** 管理员删除证书 */
export function adminDeleteCertificate(id: number) {
  return request.delete(`/admin/certificates/${id}`)
}

/** 管理员重新生成全部证书（使用新模板） */
export function adminRegenerateCertificates() {
  return request.post('/admin/certificates/regenerate')
}
