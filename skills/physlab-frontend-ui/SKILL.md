---
name: physlab-frontend-ui
description: >-
  Develops PhysLab frontend UI including 3D scenes (Three.js/Babylon.js),
  data charts (ECharts/D3.js), Web/PWA, Electron desktop, and mobile touch
  layouts. Use when building pages, components, 3D experiment views, charts,
  responsive UI, or frontend interaction for the physics simulation platform.
---

# PhysLab 前端 UI 开发

## 项目定位

物理实验仿真与教学一体化平台前端：高保真 3D 仿真、实时电路可视化、数据采集图表、实验报告编辑、教学管理界面。支持 Web、移动端触屏、Electron 桌面、VR/AR 扩展。

## 技术栈

| 领域 | 选型 | 用途 |
|------|------|------|
| 3D 渲染 | Three.js / Babylon.js | 实验器材、力学/光学场景 |
| 图表 | ECharts / D3.js | 实时曲线、散点、拟合、误差棒 |
| 桌面 | Electron | 可选桌面打包 |
| 实时通信 | WebSocket + STOMP | 仿真状态、AI 流式对话 |
| 离线 | Service Worker + IndexedDB | 移动端资源缓存与操作同步 |
| 公式 | LaTeX 即时渲染 | 实验报告富文本 |

## 目录与模块约定

按业务域组织，而非按技术类型：

```
frontend/
├── apps/
│   ├── web/              # 主 Web 端（学生/教师）
│   ├── mobile/           # 移动端简化 UI
│   └── desktop/          # Electron 壳
├── modules/
│   ├── experiment/       # 实验列表、预习、模式切换
│   ├── simulation/       # 3D 场景、器材交互、连线
│   ├── instruments/      # 虚拟仪器 UI（万用表、示波器等）
│   ├── data-chart/       # 数据表、实时图表、拟合
│   ├── report/           # 报告编辑、模板、提交
│   ├── teaching/         # 班级、发布、学情仪表板
│   └── ai-assistant/     # 悬浮 AI 问答、操作引导
├── shared/
│   ├── ui/               # 通用组件（按钮、面板、对话框）
│   ├── hooks/            # WebSocket、仿真状态 hooks
│   └── utils/            # 坐标、单位、格式化
└── assets/               # 模型、贴图、音效
```

## UI 设计原则

1. **所见即所得**：仿真参数变化必须即时反映到 3D 场景与图表，禁止纯预设动画替代物理结果。
2. **分层模式 UI**：演示 / 练习 / 考核三种模式共用场景，通过提示密度、功能锁定、计时器区分。
3. **操作反馈明确**：错误操作（短路、量程错误）用红色脉冲、器材损坏特效、锁定重置；正确连线 90° 走线优化。
4. **测量工具深度模拟**：旋钮、秒表、档位切换需真实交互（点击启动-停止-归零），非直接填数。
5. **移动端简化**：触屏拖拽、大按钮、辅助屏模式；核心功能保留，管理端复杂表格可降级。

## 3D 场景开发

### 场景结构

- 每个实验对应一个 Scene Config（器材列表、初始参数、相机预设、热点定义）。
- 器材模型支持旋转缩放；内嵌热区（点击旋钮弹出说明、语音讲解）。
- 多视角：第一人称 / 上帝 / 特写，通过相机 preset 切换。

### 交互规范

- 导线拖拽：节点吸附、高亮、一键整理走线。
- 实时电流动画：粒子或动态线条，方向与速度随电流变化。
- 力学：速度/加速度矢量、轨迹拖尾、碰撞形变。
- 光学：光线追迹、可拖拽屏幕观察干涉/衍射条纹。

### 性能

- 模型 LOD、按需加载、WASM 物理引擎（Cannon.js/Ammo.js）放 Worker。
- 大场景分块加载；图表与 3D 渲染分离帧循环。

## 虚拟仪器 UI

| 仪器 | 交互要点 |
|------|----------|
| 电压/电流表 | 量程选择、读数面板 |
| 万用表 | 旋转档位旋钮 |
| 示波器 | 时间/电压分度调节、波形探针 |
| 信号发生器 | 频率/幅度/波形类型 |
| 滑动变阻器 | 滑片拖拽，联动亮度/电流动画 |

仪器读数通过「读取」按钮填入数据表，支持手动覆盖。

## 图表与数据 UI

- 折线图、散点图自动生成；支持自定义坐标轴。
- 拟合：线性、多项式、指数；显示误差棒与理论/班级平均对比曲线。
- 异常点：偏离趋势点黄色高亮，提示重测。
- 探针可拖拽，XY 图与时间轴波形同步更新。
- 导出：Excel/CSV 数据、图表 PNG。

## 实验报告编辑器

- 富文本 + LaTeX 公式即时渲染。
- 分层模板：高中填空式 / 大学学术规范；教师可自定义。
- 自动填充：原理、步骤、数据、图表、结论来自实验会话。
- 提交前自检 UI：数据点数、短路记录、异常点处理状态。

## WebSocket 集成

```typescript
// 仿真状态订阅（示意）
stompClient.subscribe(`/topic/simulation/${sessionId}`, (msg) => {
  applySimulationState(JSON.parse(msg.body));
});

// AI 流式对话
stompClient.subscribe(`/topic/ai/${sessionId}/stream`, (chunk) => {
  appendAssistantMessage(chunk.body);
});
```

前端发送操作事件与 AI 问题时必须携带 `sessionId` 和当前实验上下文摘要。

## 响应式与无障碍

- 断点：desktop ≥1024、tablet 768–1023、mobile <768。
- 触屏：拖拽阈值、双指缩放 3D 场景。
- 考核模式：禁用开发者工具提示入口；关键操作需确认。
- 语音：练习模式可选 TTS 讲解与语音提问。

## 开发检查清单

- [ ] 三种模式（演示/练习/考核）UI 状态正确切换
- [ ] 仿真状态与 WebSocket 推送同步，无卡顿堆积
- [ ] 错误操作有视觉反馈且可重置
- [ ] 仪器交互符合真实操作顺序
- [ ] 图表与 3D 参数联动
- [ ] 移动端核心流程可用
- [ ] 离线缓存与联网同步不丢数据

## 参考

- 业务与流程详见项目根目录 [开发文档.md](../../../开发文档.md)
- 仿真计算对接见 [physlab-simulation](../physlab-simulation/SKILL.md)
- 后端 API 约定见 [physlab-backend](../physlab-backend/SKILL.md)
