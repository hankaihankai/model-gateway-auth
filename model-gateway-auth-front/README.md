# Model Gateway Auth 前端

基于 Ant Design Pro V6 的后台管理系统，配套后端 `model-gateway-auth`。

## 目录结构要点

- `config/routes.ts`：写死的菜单与路由（修改菜单从这里改）
- `mock/user.ts`：用户列表本地 mock（生产构建会自动剥离）
- `src/services/`：所有 API 调用
- `src/app.tsx`：getInitialState、layout、request 三块运行时配置
- `src/access.ts`：菜单/路由权限判断（已登录即可见）

## 开发命令

```bash
pnpm install    # 装依赖
pnpm dev        # 开发模式，监听 8000 端口
pnpm build      # 产线构建到 dist/
pnpm preview    # 本地预览构建产物
```

## 联调

1. 启动后端：在 `model-gateway-auth` 目录下 `mvn spring-boot:run`，监听 8188
2. 启动前端：`pnpm dev`，监听 8000
3. 浏览器打开 `http://localhost:8000`
4. 登录账号由后端 `sys_user` 表维护

## 关键约束

- 业务码以 `code === 200` 为成功
- Token 存 `localStorage.access_token`，刷新页面保持登录
- 仅中文（zh-CN）
- 用户列表本期为前端 mock，**未对接真实后端列表接口**

## 设计与计划文档

- 设计：`docs/superpowers/specs/2026-05-04-mgw-auth-front-design.md`
- 实施：`docs/superpowers/plans/2026-05-04-mgw-auth-front-implementation.md`
