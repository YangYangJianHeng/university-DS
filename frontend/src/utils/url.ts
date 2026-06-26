/**
 * 为 URL 附加 token 查询参数，用于浏览器直接加载的场景（img/iframe/video/audio/window.open）
 * axios 请求会自动带 Authorization header，但浏览器标签/媒体加载不会
 */
export function withToken(url: string): string {
  const token = localStorage.getItem('token')
  if (!token) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}token=${token}`
}
