# 大学生竞赛组队平台 - 微信小程序

## 项目简介

基于 TDesign Miniprogram 组件库开发的微信小程序端，与 Web 端共享同一套 Spring Boot 后端 API。

## 技术栈

- **框架**: 微信小程序原生框架
- **UI组件**: TDesign Miniprogram v1.5+
- **API通信**: wx.request 封装
- **设计风格**: 渐变色主题 (#667eea → #764ba2)

## 快速开始

### 1. 安装依赖

```bash
cd frontend/miniprogram
npm install
```

### 2. 构建npm

在微信开发者工具中：
- 工具 → 构建 npm
- 确保 `project.config.json` 中 `packNpmManually` 为 `true`

### 3. 配置后端地址

修改 `app.js` 中的 `baseUrl`：

```javascript
baseUrl: 'http://你的后端地址:8080/api'
```

### 4. 添加 TabBar 图标

在 `images/` 目录下放置 TabBar 图标文件，参考 `images/README.md`。

### 5. 配置 AppID

修改 `project.config.json` 中的 `appid` 为你自己的小程序 AppID。

## 项目结构

```
miniprogram/
├── app.js              # 应用入口
├── app.json            # 应用配置
├── app.wxss            # 全局样式(Design System)
├── package.json        # 依赖配置
├── project.config.json # 项目配置
├── sitemap.json        # 站点地图
├── utils/
│   ├── request.js      # HTTP请求封装
│   ├── api.js          # 各模块API封装
│   └── util.js         # 工具函数
├── images/             # 图标资源
└── pages/
    ├── index/          # 工作台首页
    ├── login/          # 登录
    ├── register/       # 注册
    ├── profile/        # 个人中心
    ├── recruit/        # 招募模块
    │   ├── list/       # 招募列表
    │   ├── detail/     # 招募详情
    │   └── create/     # 发布招募
    ├── team/           # 团队模块
    │   ├── list/       # 团队列表
    │   └── detail/     # 团队详情
    ├── project/        # 项目模块
    │   ├── list/       # 项目列表
    │   └── detail/     # 项目详情
    ├── task/           # 任务模块
    │   ├── list/       # 任务列表
    │   └── detail/     # 任务详情
    ├── competition/    # 竞赛模块
    │   ├── list/       # 竞赛列表
    │   └── detail/     # 竞赛详情
    ├── teacher/        # 导师模块
    │   ├── list/       # 导师列表
    │   └── detail/     # 导师详情
    ├── achievement/    # 成果模块
    │   ├── list/       # 成果列表
    │   └── detail/     # 成果详情
    ├── notification/   # 通知消息
    └── admin/          # 管理后台
        └── dashboard/  # 管理仪表盘
```

## 功能模块

| 模块 | 功能 | 与Web端同步 |
|------|------|------------|
| 用户认证 | 登录/注册/JWT令牌 | ✅ |
| 工作台 | 统计卡片/快捷入口/最近动态 | ✅ |
| 招募匹配 | 列表/详情/发布/智能匹配/申请 | ✅ |
| 团队管理 | 列表/详情/成员管理/加入退出 | ✅ |
| 项目管理 | 列表/详情/里程碑/甘特图/风险评估 | ✅ |
| 任务协作 | 列表/详情/状态流转 | ✅ |
| 竞赛信息 | 列表/详情/收藏 | ✅ |
| 导师对接 | 列表/详情/邀请指导 | ✅ |
| 成果展厅 | 列表/详情/点赞收藏 | ✅ |
| 通知消息 | 列表/已读标记/全部已读 | ✅ |
| 管理后台 | 统计/审核/用户管理/公告 | ✅ |

## 设计规范

- 主题色: #667eea → #764ba2 渐变
- 卡片圆角: 12rpx
- 阴影层次: sm/md/lg 三级
- 字体: PingFang SC / Microsoft YaHei
- 组件: TDesign Miniprogram
