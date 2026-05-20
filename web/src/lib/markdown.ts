import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ breaks: true, gfm: true })

export function renderMarkdown(raw: string): string {
  if (!raw) return ''
  const html = marked.parse(raw) as string
  return DOMPurify.sanitize(html, {
    ADD_ATTR: ['target'],
    ALLOWED_TAGS: [
      'p', 'br', 'strong', 'em', 'u', 's', 'code', 'pre', 'blockquote',
      'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'a', 'img', 'hr', 'span'
    ]
  })
}

/** 解析 Markdown 图片地址（列表预览用） */
export function extractImageUrls(raw: string, max = 3): string[] {
  if (!raw) return []
  const urls: string[] = []
  const re = /!\[[^\]]*\]\(([^)]+)\)/g
  let m: RegExpExecArray | null
  while ((m = re.exec(raw)) !== null && urls.length < max) {
    const url = m[1]?.trim()
    if (url && !urls.includes(url)) urls.push(url)
  }
  return urls
}

export function resolveMediaUrl(url: string): string {
  const u = url.trim()
  if (!u) return ''
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('data:')) return u
  return u.startsWith('/') ? u : `/${u}`
}

/** 列表摘要：去掉图片与 Markdown 符号，只留纯文本 */
export function contentExcerpt(raw: string, maxLen = 100): string {
  if (!raw) return ''
  const plain = raw
    .replace(/!\[[^\]]*\]\([^)]+\)/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[#*_`>~-]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return plain.length > maxLen ? plain.slice(0, maxLen) + '...' : plain
}
