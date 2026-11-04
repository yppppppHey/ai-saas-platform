# AI SaaS Frontend

AI SaaS 平台前端（Vue3 单页应用）。本仓库为前后端一体 Monorepo，后端各微服务说明见根目录 [README](../README.md)。

## 技术栈

Vue 3 · TypeScript · Vite · Element Plus · Pinia · Vue Router · Axios · ECharts

## 开发

```bash
npm install
npm run dev      # http://localhost:5173
```

dev 模式下 `/api` 由 Vite 代理至网关 `localhost:8080`（见 `vite.config.ts`），无需额外跨域配置。

## 构建与预览

```bash
npm run build    # vue-tsc 类型检查 + vite build，产物在 dist/
npm run preview  # 本地预览构建产物
```

生产部署：`dist/` 交由 nginx 等静态服务托管，并将 `/api` 反代至网关 8080。

## 目录结构

```
src/
├── api/         # 后端接口封装（axios）：auth / user / chat / task / billing / rag / request
├── assets/      # 静态资源
├── layouts/     # 布局组件（DefaultLayout）
├── router/      # 路由
├── stores/      # Pinia 状态（app / user）
├── views/       # 页面：dashboard、chat、knowledge、tasks、billing、settings、login、error
├── App.vue
└── main.ts
```

## 环境变量

`.env.development` / `.env.production`：

| 变量 | 说明 |
|---|---|
| `VITE_API_BASE_URL` | 接口前缀，默认 `/api` |
| `VITE_APP_TITLE` | 站点标题 |
