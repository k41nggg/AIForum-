/** 帖子列表/推荐：作者展示（与帖子广场一致） */
export function authorLabel(post: { userNickname?: string; user_nickname?: string }): string {
  const name = (post.userNickname ?? post.user_nickname)?.trim()
  return name || '未知用户'
}

export function normalizePostAuthor<T extends { userNickname?: string; user_nickname?: string }>(
  post: T
): T & { userNickname?: string } {
  const nickname = (post.userNickname ?? post.user_nickname)?.trim()
  return { ...post, userNickname: nickname || undefined }
}

export function getCategoryName(
  categoryId: number,
  categories: { id: number; name: string }[]
): string {
  const cat = categories.find((c) => c.id === categoryId)
  return cat ? cat.name : '未知分类'
}
