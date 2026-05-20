/** 从帖子/评论/用户对象中取出头像 URL（兼容多种字段名） */
export function pickUserAvatar(
  source?: { userAvatar?: string; user_avatar?: string; avatar?: string } | null
): string {
  if (!source) return ''
  const v = source.userAvatar ?? source.user_avatar ?? source.avatar
  return v?.trim() || ''
}

/** 将用户 avatar 字段转为可用的 img src */
export function resolveAvatarUrl(avatar?: string | null): string {
  const url = avatar?.trim()
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  return url.startsWith('/') ? url : `/${url}`
}

export function avatarInitial(name?: string | null): string {
  const n = name?.trim()
  if (!n) return '?'
  return n.slice(0, 1).toUpperCase()
}
