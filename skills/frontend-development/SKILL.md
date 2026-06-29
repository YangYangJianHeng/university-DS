---
name: frontend-development
description: >-
  「组队星」前端 UI 开发规范。基于 Vue 3 + TypeScript + Vite + Element Plus + Pinia，涵盖
  组件开发、状态管理、路由设计、API 请求封装、样式规范、响应式布局。
  Use when building pages, components, forms, tables, charts, responsive UI,
  or any frontend feature for the university team-building platform.
---

# 前端开发规范

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x (Composition API) | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 请求 |
| marked | latest | Markdown 渲染（AI 聊天） |
| DOMPurify | latest | XSS 防护 |

## 目录结构

```
frontend/
├── public/                    # 静态资源
├── src/
│   ├── api/                   # API 请求封装（按模块拆分）
│   │   ├── request.ts         # Axios 实例（拦截器、token 注入）
│   │   ├── ai.ts              # AI 对话 API
│   │   ├── team.ts            # 团队 API
│   │   ├── project.ts         # 项目 API
│   │   ├── task.ts            # 任务 API
│   │   ├── competition.ts     # 竞赛 API
│   │   ├── achievement.ts     # 成果 API
│   │   ├── badge.ts           # 徽章 API
│   │   └── certificate.ts     # 奖状 API
│   ├── components/            # 通用组件
│   │   ├── LoginDialog.vue    # 登录弹窗
│   │   └── ThreeBackground.vue # 3D 背景
│   ├── router/                # 路由配置
│   │   └── index.ts           # 路由守卫 + 动态路由
│   ├── stores/                # Pinia 状态
│   ├── views/                 # 页面组件
│   │   ├── LandingPage.vue    # 首页
│   │   ├── Login.vue          # 登录页
│   │   ├── Register.vue       # 注册页
│   │   ├── Dashboard.vue      # 工作台
│   │   ├── Profile.vue        # 个人中心
│   │   ├── AiChat.vue         # AI 智能助手
│   │   ├── TeamList.vue       # 团队列表
│   │   ├── TeamDetail.vue     # 团队详情
│   │   ├── ProjectList.vue    # 项目列表
│   │   ├── ProjectDetail.vue  # 项目详情
│   │   ├── TaskList.vue       # 任务看板
│   │   ├── RecruitList.vue    # 招募列表
│   │   ├── RecruitDetail.vue  # 招募详情
│   │   ├── CreateRecruit.vue  # 发布招募
│   │   ├── CompetitionList.vue
│   │   ├── CompetitionDetail.vue
│   │   ├── AchievementHall.vue
│   │   ├── TeacherList.vue    # 导师大厅
│   │   ├── admin/             # 管理后台页面
│   │   │   └── AdminDashboard.vue
│   │   └── layout/
│   │       └── Layout.vue     # 主布局（导航栏 + 侧边栏）
│   ├── App.vue                # 根组件
│   ├── main.ts                # 入口文件
│   └── env.d.ts               # 类型声明
├── package.json
├── vite.config.ts
├── tsconfig.json
└── index.html
```

## 组件开发规范

### 使用 Composition API + `<script setup>`

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

// Props
const props = defineProps<{ teamId: number }>()

// 响应式状态
const loading = ref(false)
const data = ref<any[]>([])

// 计算属性
const filteredData = computed(() => data.value.filter(...))

// 方法
async function fetchData() { ... }

// 生命周期
onMounted(() => fetchData())
</script>
```

### Element Plus 组件使用

- **表格**: `<el-table>` + `<el-table-column>`，搭配 `v-loading`、`stripe`、`size="small"`
- **表单**: `<el-form>` + `<el-form-item>` + `:rules` 校验
- **对话框**: `<el-dialog>` + `v-model` 双向绑定
- **消息提示**: `ElMessage.success()` / `.error()` / `.warning()`
- **确认弹窗**: `ElMessageBox.confirm()`
- **标签**: `<el-tag>` + `type` 属性实现彩色标签

### 图标

- 使用 `@element-plus/icons-vue`，按需导入
- 方式：`import { Plus, Delete, Edit } from '@element-plus/icons-vue'`

## API 请求规范

### request.ts 拦截器

```typescript
// 请求拦截：自动注入 Authorization token
service.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：统一错误处理、token 过期跳转登录
service.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) router.push('/login')
    return Promise.reject(error)
  }
)
```

### API 文件约定

每个模块一个 `api/*.ts` 文件，导出纯函数：

```typescript
// api/task.ts
import request from '@/api/request'

export function getTasks(projectId: number) {
  return request.get('/task/list', { params: { projectId } })
}

export function createTask(data: any) {
  return request.post('/task/create', data)
}
```

## 路由规范

- 主布局 `Layout.vue` 包裹需要登录的页面
- 路由守卫：未登录 → 跳转 `/login`
- 页面标题：`meta.title` 设置

```typescript
{
  path: '/',
  component: Layout,
  children: [
    { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },
  ]
}
```

## CSS 样式规范

- **Scoped**: 所有页面/组件样式必须 `<style scoped>`
- **变量**: 使用 CSS 变量统一主题色
  ```css
  --gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  ```
- **响应式**: 关键页面支持 `@media (max-width: 768px)` 移动端适配
- **颜色**: 主色 `#409eff`（Element Plus 默认蓝）
- **圆角**: 卡片 12px，按钮 6px，标签 4px
- **阴影**: `box-shadow: 0 2px 12px rgba(0,0,0,0.08)`

## 状态管理 (Pinia)

仅在跨组件共享时使用（如用户信息、全局设置），简单页面直接用 `ref()`。

## 流式 AI 对话

```typescript
// 通过 fetch + ReadableStream 实现 SSE 流式接收
const response = await fetch(`/api/ai/chat/stream`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  body: JSON.stringify({ sessionId, message })
})

const reader = response.body!.getReader()
const decoder = new TextDecoder()
while (true) {
  const { done, value } = await reader.read()
  if (done) break
  streamContent.value += decoder.decode(value, { stream: true })
}
```

## 开发检查清单

- [ ] 新页面是否使用 `<script setup lang="ts">`
- [ ] 样式是否 `<style scoped>`
- [ ] API 调用是否通过 `@/api/` 统一封装
- [ ] 错误处理是否有 ElMessage 提示
- [ ] 表格/列表是否有 loading 状态
- [ ] 空数据是否有 `<el-empty>` 占位
- [ ] 表单是否有输入校验规则
- [ ] 路径是否使用 `@/` 别名而非相对路径

## 参考

- 后端 API 见 [backend-development](../backend-development/SKILL.md)
- AI 聊天见 [ai-chat-assistant](../ai-chat-assistant/SKILL.md)
- 项目全貌见 [project-overview](../project-overview/SKILL.md)
