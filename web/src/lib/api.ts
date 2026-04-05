export type ApiResult<T> = {
  code: number
  message: string
  data: T
}

const TOKEN_KEY = 'jwt_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

function withAuthHeaders(init: RequestInit = {}): RequestInit {
  const token = getToken()
  const headers = new Headers(init.headers || {})

  // 只有在 body 存在时才设置 JSON，避免 GET 也强行带 Content-Type
  if (init.body != null) headers.set('Content-Type', 'application/json')

  if (token) headers.set('Authorization', `Bearer ${token}`)
  return { ...init, headers }
}

async function request<T>(url: string, init?: RequestInit): Promise<T | null> {
  try {
    const res = await fetch(`/api${url}`, withAuthHeaders(init))

    // 后端可能返回 403/401 的 HTML 或空响应，这里做更稳健的解析
    const text = await res.text()
    if (!text) return null

    // 针对 AI 接口可能直接返回文本的情况
    if (res.headers.get('Content-Type')?.includes('text/plain')) {
      return text as T
    }

    try {
      return JSON.parse(text) as T
    } catch {
      // 非 JSON：返回 null 让上层提示“服务异常/权限问题”
      console.error('Non-JSON response:', url, res.status, text.slice(0, 200))
      return null
    }
  } catch (e) {
    console.error('API request failed:', url, e)
    return null
  }
}

export function apiGet<T>(url: string) {
  return request<T>(url, { method: 'GET' })
}

export function apiPost<T>(url: string, body?: unknown) {
  return request<T>(url, { method: 'POST', body: body == null ? undefined : JSON.stringify(body) })
}

export function apiPut<T>(url: string, body?: unknown) {
  return request<T>(url, { method: 'PUT', body: body == null ? undefined : JSON.stringify(body) })
}

export function apiDelete<T>(url: string) {
  return request<T>(url, { method: 'DELETE' })
}

// Comment API
export const comment = {
  getComments: (postId: number) => apiGet<ApiResult<any[]>>(`/comments/post/${postId}`),
  addComment: (data: { postId: number; content: string; parentId?: number }) =>
    apiPost<ApiResult<any>>('/comments', data),
  likeComment: (commentId: number) => apiPost(`/comments/${commentId}/like`),
  unlikeComment: (commentId: number) => apiPost(`/comments/${commentId}/unlike`)
}
