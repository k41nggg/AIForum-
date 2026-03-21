<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="h1">分类</h1>
        <div class="subtle">管理话题分类与订阅你感兴趣的板块</div>
      </div>
      <div class="page-actions">
        <button class="btn" @click="load" :disabled="loading">刷新</button>
      </div>
    </div>

    <div class="grid-2">
      <section class="glass card">
        <div class="section-head">
          <h2 class="h2">创建分类</h2>
          <span class="pill">支持父子层级</span>
        </div>

        <div class="row">
          <div class="field">
            <label>名称</label>
            <input class="input" v-model.trim="createForm.name" placeholder="例如：技术专区" />
          </div>

          <div class="field">
            <label>描述（可选）</label>
            <input class="input" v-model.trim="createForm.description" placeholder="简单描述一下这个分类" />
          </div>

          <div class="field">
            <label>父分类（可不选）</label>
            <select class="select" v-model="createForm.parentId">
              <option value="">无（顶级分类）</option>
              <option v-for="c in flatParents" :key="c.id" :value="String(c.id)">
                {{ c.name }} (#{{ c.id }})
              </option>
            </select>
            <div class="subtle">不选择则创建为顶级分类；选择后将挂到对应父分类下。</div>
          </div>

          <button class="btn btn-primary" @click="create" :disabled="creating">
            {{ creating ? '提交中...' : '创建' }}
          </button>

          <div class="subtle">创建分类通常需要管理员权限（由后端决定）。</div>
        </div>
      </section>

      <section class="glass card">
        <div class="section-head">
          <h2 class="h2">分类列表</h2>
          <span class="pill">共 {{ categories.length }} 个</span>
        </div>

        <div v-if="loading" class="subtle" style="padding: 10px 0">加载中...</div>
        <div v-else-if="categories.length === 0" class="empty">暂无分类</div>

        <div v-else class="tree">
          <div class="cat" v-for="c in treeList" :key="c.id" :style="{ paddingLeft: (12 + c.depth * 18) + 'px' }">
            <div class="cat-head">
              <div class="cat-title">
                <span class="pill">#{{ c.id }}</span>
                <span class="name">{{ c.name }}</span>
              </div>
              <div class="cat-actions">
                <button class="btn" @click="subscribe(c.id)">订阅</button>
              </div>
            </div>

            <div class="cat-meta">
              <span class="pill">parent {{ c.parentId }}</span>
              <span class="subtle" v-if="c.description">{{ c.description }}</span>
              <span class="subtle" v-else>暂无描述</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { apiGet, apiPost, type ApiResult } from '../lib/api'
import { showToast } from '../lib/toast'

type Category = {
  id: number
  name: string
  description?: string
  parentId: number
}

type TreeItem = Category & { depth: number }

const loading = ref(false)
const creating = ref(false)
const categories = ref<Category[]>([])

const createForm = reactive({
  name: '',
  description: '',
  parentId: '' // string: '' 表示无父分类
})

const flatParents = computed(() => {
  // 作为父分类候选：这里直接给出所有分类（也可以只给顶级）；目前按 name 排序
  return [...categories.value].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

const treeList = computed<TreeItem[]>(() => {
  const list = categories.value || []
  const byParent = new Map<number, Category[]>()

  for (const c of list) {
    const pid = c.parentId ?? 0
    if (!byParent.has(pid)) byParent.set(pid, [])
    byParent.get(pid)!.push(c)
  }

  // 每个父节点下按 sort key（这里用 name）排序，让展示稳定
  for (const [, arr] of byParent) {
    arr.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
  }

  const result: TreeItem[] = []
  const walk = (parentId: number, depth: number) => {
    const children = byParent.get(parentId) || []
    for (const child of children) {
      result.push({ ...child, depth })
      walk(child.id, depth + 1)
    }
  }
  walk(0, 0)

  // 有些数据可能 parentId 不存在（脏数据），把它们也兜底展示
  const included = new Set(result.map((x) => x.id))
  for (const c of list) {
    if (!included.has(c.id)) result.push({ ...c, depth: 0 })
  }

  return result
})

async function load() {
  loading.value = true

  const res = await apiGet<ApiResult<Category[]>>('/categories/tree')
  loading.value = false

  if (!res) {
    showToast('error', '请求失败', '后端无响应（可能是权限拦截或服务未启动）')
    categories.value = []
    return
  }
  if (res.code !== 200) {
    if (res.message === '未登录' || res.message === '无权访问' || res.message === '无权操作') {
      showToast('error', '需要登录', '请先登录后再访问该页面')
    } else {
      showToast('error', '加载失败', res.message || '加载失败')
    }
    categories.value = []
    return
  }

  categories.value = res.data || []
}

async function create() {
  if (!createForm.name) {
    showToast('error', '创建失败', '分类名称不能为空')
    return
  }

  creating.value = true

  const payload: any = {
    name: createForm.name,
    description: createForm.description || null
  }
  if (createForm.parentId !== '') payload.parentId = Number(createForm.parentId)

  const res = await apiPost<ApiResult<Category>>('/categories', payload)
  creating.value = false

  if (!res) {
    showToast('error', '创建失败', '无法连接后端服务')
    return
  }
  if (res.code !== 200) {
    showToast('error', '创建失败', res.message || '创建失败')
    return
  }

  showToast('success', '创建成功', `categoryId=${(res.data as any).id}`)
  createForm.name = ''
  createForm.description = ''
  createForm.parentId = ''
  await load()
}

async function subscribe(categoryId: number) {
  const res = await apiPost<ApiResult<null>>('/subscriptions', { categoryId })
  if (!res) {
    showToast('error', '订阅失败', '无法连接后端服务')
    return
  }

  if (res.code !== 200) {
    // 后端幂等：重复订阅会返回 message=已订阅
    if (res.message === '已订阅') {
      showToast('success', '已订阅', '你已关注该分类')
      return
    }

    showToast('error', '订阅失败', res.message || '订阅失败')
    return
  }

  showToast('success', '订阅成功', '已加入你的关注列表')
}

onMounted(load)
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.page-head { display:flex; align-items:flex-end; justify-content:space-between; gap: 12px; }
.page-actions { display:flex; gap: 10px; }
.section-head { display:flex; align-items:center; justify-content:space-between; gap: 10px; margin-bottom: 12px; }
.empty { color: var(--muted); padding: 10px 0; }

.tree { display: grid; }

.cat {
  padding: 14px 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
  margin-bottom: 10px;
}

.cat:first-of-type { border-top: none; }
.cat-head { display:flex; align-items:center; justify-content:space-between; gap: 12px; }
.cat-title { display:flex; align-items:center; gap: 10px; }
.name { font-weight: 750; letter-spacing: 0.2px; }
.cat-meta { display:flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-top: 10px; }
.cat-actions { display:flex; gap: 10px; }
</style>
