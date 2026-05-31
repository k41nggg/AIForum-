export type PostStatKind = 'view' | 'like' | 'collect'

export const POST_STAT_META: Record<
  PostStatKind,
  { icon: string; iconActive: string; label: string; labelActive: string }
> = {
  view: { icon: '👁', iconActive: '👁', label: '浏览', labelActive: '浏览' },
  like: { icon: '♡', iconActive: '♥', label: '赞', labelActive: '已赞' },
  collect: { icon: '☆', iconActive: '★', label: '藏', labelActive: '已藏' }
}
