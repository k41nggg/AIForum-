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

/** 列表摘要：去掉图片与 Markdown 符号，只留纯文本 */
export function contentExcerpt(raw: string, maxLen = 100): string {
  if (!raw) return ''
  const plain = raw
    .replace(/!\[[^\]]*\]\([^)]+\)/g, '[图片]')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[#*_`>~-]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
  return plain.length > maxLen ? plain.slice(0, maxLen) + '...' : plain
}
