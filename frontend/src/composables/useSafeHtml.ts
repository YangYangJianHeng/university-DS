import DOMPurify from 'dompurify'

/**
 * 安全渲染HTML内容 - 使用DOMPurify过滤XSS攻击向量
 * 
 * 用法:
 *   import { sanitize } from '@/composables/useSafeHtml'
 *   const safe = sanitize(userInputHtml)
 * 
 * 或使用 Vue 指令:
 *   app.directive('safe-html', { ... })
 */

const ALLOWED_TAGS = ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'span', 'div', 'code', 'pre']
const ALLOWED_ATTR = ['href', 'target', 'rel', 'class']

export function sanitize(html: string): string {
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
  })
}

/**
 * 安全直接渲染 - 对已硬编码的可信HTML做直出
 * 仅当确认数据源为用户不可控时使用，否则使用 sanitize()
 */
export function trustStatic(html: string): string {
  if (process.env.NODE_ENV === 'development') {
    console.warn('[useSafeHtml] trustStatic() 仅应用于硬编码的静态HTML，确认数据源非用户输入')
  }
  return html
}
