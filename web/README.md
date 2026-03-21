# AIForum Web (Vue)

这是给后端 `AIForum` 项目配套的一个最小可用 Vue3 前端，用来快速测试：
- 注册 / 登录（JWT）
- 帖子列表（已发布）/ 发布帖子（默认进入待审核）/ 点赞
- 分类列表 / 创建分类 / 订阅分类

## 开发运行

1) 先启动后端（默认 `http://localhost:8080`）

2) 再启动前端（Vite 会代理 `/api` 到后端）

```powershell
cd "W:\java test\AIForum\web"
npm install
npm run dev
```

打开：`http://localhost:5173`

## 接口约定（按当前后端实现）

- `POST /api/auth/register`  { username, password, email?, nickname? }
- `POST /api/auth/login`     { username, password } -> { token }
- `GET  /api/user/me`
- `GET  /api/posts`          (只返回 status=2 的已发布)
- `POST /api/posts`          (需要登录，创建后 status=1 待审核)
- `POST /api/posts/{id}/like` (需要登录；同用户重复点赞返回“已点赞”) 
- `GET  /api/categories`
- `POST /api/categories`
- `POST /api/subscriptions`  { categoryId }

如果你的后端某个路径不同，我可以帮你把前端调用改成一致的。
