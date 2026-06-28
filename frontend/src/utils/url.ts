/**
 * 文件安全访问工具 - 通过签名URL替代JWT token在URL参数中传递
 * 
 * 使用方式：
 * 1. 在组件 setup 中调用 prefetchFileUrls([fileIds...]) 预加载文件签名URL
 * 2. 调用 getSignedFileUrl(fileId, type) 获取安全的签名URL
 * 
 * 旧的 withToken() 已废弃，不再将JWT token附加到URL参数中
 */
import request from '@/api/request'

const signedUrlCache: Record<string, string> = {}
const SKIP_TOAST = { skipErrorToast: true }

/**
 * 预加载文件签名URL到缓存（异步，在组件setup中调用）
 * 使用 Promise.allSettled 确保单个文件失败不影响其他文件
 */
export async function prefetchFileUrls(fileIds: number[]) {
  const valid = fileIds.filter(id => typeof id === 'number' && !isNaN(id))
  if (valid.length === 0) return
  const uncached = valid.filter(id => !signedUrlCache[`${id}:preview`])
  if (uncached.length === 0) return

  try {
    const results = await Promise.allSettled(
      uncached.map(id => request.get(`/file/${id}/signed-url`, SKIP_TOAST))
    )
    results.forEach((result, i) => {
      if (result.status === 'fulfilled' && result.value?.data) {
        signedUrlCache[`${uncached[i]}:preview`] = result.value.data.previewUrl
        signedUrlCache[`${uncached[i]}:download`] = result.value.data.downloadUrl
      }
    })
  } catch (e) {
    console.warn('[prefetchFileUrls] Failed to fetch signed URLs:', e)
  }
}

/**
 * 获取单个文件的签名URL（同步等待，用于缓存未命中时兜底）
 * @returns 签名URL，失败时返回null
 */
export async function fetchSingleSignedUrl(fileId: number, type: 'preview' | 'download'): Promise<string | null> {
  if (typeof fileId !== 'number' || isNaN(fileId)) return null
  try {
    const res = await request.get(`/file/${fileId}/signed-url`, SKIP_TOAST)
    if (res?.data) {
      signedUrlCache[`${fileId}:preview`] = res.data.previewUrl
      signedUrlCache[`${fileId}:download`] = res.data.downloadUrl
      return res.data[type === 'preview' ? 'previewUrl' : 'downloadUrl'] || null
    }
  } catch (_) { /* 静默失败 */ }
  return null
}

/**
 * 获取文件的签名访问URL（同步，需先调用prefetchFileUrls预加载）
 * 如果缓存未命中，返回的fallback URL仅适用于ajax请求（带Authorization header），
 * 浏览器标签（img/iframe/video等）建议使用 fetchSingleSignedUrl 异步获取签名URL
 */
export function getSignedFileUrl(fileId: number, type: 'preview' | 'download'): string {
  if (typeof fileId !== 'number' || isNaN(fileId)) {
    console.warn('[getSignedFileUrl] Invalid fileId:', fileId)
    return '' // 无效ID不发起请求，避免后端报错
  }
  const cached = signedUrlCache[`${fileId}:${type}`]
  if (cached) return cached
  // 缓存未命中时触发异步加载，下次渲染会使用签名URL
  prefetchFileUrls([fileId])
  return `/api/file/${fileId}/${type}` // fallback（需要JWT Authorization header）
}

/**
 * @deprecated 使用 getSignedFileUrl() + prefetchFileUrls() 替代
 * 不再将JWT token附加到URL，避免token在浏览器历史、日志中泄露
 */
export function withToken(url: string): string {
  return url
}
