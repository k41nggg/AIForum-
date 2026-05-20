/** 帖子列表/推荐：作者展示（与帖子广场一致） */
export function authorLabel(post: { userNickname?: string }): string {
  const name = post.userNickname?.trim()
  return name || '未知用户'
}

export function getCategoryName(
  categoryId: number,
  categories: { id: number; name: string }[]
): string {
  const cat = categories.find((c) => c.id === categoryId)
  return cat ? cat.name : '未知分类'
}
