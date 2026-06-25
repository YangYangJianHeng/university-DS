---
name: physlab-simulation
description: >-
  Builds PhysLab 2D/3D physics simulation including circuit SPICE solver,
  mechanics, electromagnetism, optics, and thermal/modern physics engines.
  Use when implementing simulation logic, WASM physics, real-time state
  sync, instrument behavior, or experiment scene configuration.
---

# PhysLab 仿真引擎开发

## 设计原则

**「真」实验**：所有结果由物理模型实时计算生成，参数可调，禁止用预设动画替代计算结果。

## 引擎分层

```
┌─────────────────────────────────────┐
│  Scene Layer (Three.js/Babylon.js)  │  渲染、交互、动画表现
├─────────────────────────────────────┤
│  Simulation Core                      │  物理/电路数值求解
│  ├── Circuit Solver (Java SPICE)     │
│  ├── Mechanics (Cannon/Ammo WASM)    │
│  ├── Optics (ray tracing)            │
│  └── Thermo / Modern Physics         │
├─────────────────────────────────────┤
│  State Sync (WebSocket)              │  前后端状态同步
└─────────────────────────────────────┘
```

## 电路仿真

### 元件库

电阻、滑动变阻器、电容、电感、二极管、三极管、电源、开关、灯泡、电动机等。

### 求解器

- 自研轻量 SPICE，Java 后端主算，WebSocket 推送节点电压、支路电流。
- 前端负责：连线拓扑上报、可视化动画、参数展示。

### 交互规则

| 规则 | 行为 |
|------|------|
| 智能连线纠错 | 短路/量程错误 → 红色脉冲 + 器材损坏特效 + 锁定 |
| 操作顺序 | 考核/练习强制先搭建供电回路 |
| 隐性故障 | 教师可预设内部断路等，用于排故训练 |
| 器材磨损 | 长期使用精度下降（可选） |

### 可视化

- 电流：流动粒子或动态线条，方向速度 ∝ 电流大小。
- 节点电压、支路电流数字叠加显示。
- 示波器/曲线：探针拖拽，时间轴与 XY 图联动。

## 力学仿真

- 质点运动学/动力学：速度/加速度矢量、轨迹拖尾。
- 器材：气垫导轨、打点计时器、光电门、砝码、弹簧秤。
- 测量工具：机械秒表需启动-停止-归零完整流程。
- 碰撞形变动画；多视角相机。

**引擎**：Cannon.js 或 Ammo.js 编译为 WASM，可在 Web Worker 运行。

## 电磁学仿真

- 电场线 / 等势面 3D 可视化。
- 带电粒子轨迹；磁感线动态；法拉第电磁感应。
- 手摇发电机、变压器原理演示。

## 光学仿真

- 光线追迹：折射、反射、全反射。
- 干涉/衍射：双缝、光栅、薄膜，条纹随参数实时重算。
- 波长颜色模拟；屏幕可拖拽观察。

## 热学与近代物理

- 气体分子运动（温度可调）；P-V 图实时绘制。
- 光电效应；原子能级跃迁与光谱。

## 实验模式

| 模式 | 仿真行为 |
|------|----------|
| 演示 | 自动步骤 + 语音，参数预设路径 |
| 练习 | 自由操作 + 智能提示，可关闭 AI 引导 |
| 考核 | 无提示、限时、随机参数、禁用辅助功能 |

## 操作回放

- 全程录制操作序列与时间戳。
- 标记错误事件（短路、量程错误）。
- 支持 2x/慢放/关键帧跳转；语音标记数据同步回放。

## 状态同步协议

```json
{
  "sessionId": "uuid",
  "timestamp": 1719000000000,
  "type": "STATE_DELTA",
  "payload": {
    "nodes": [{"id": "n1", "voltage": 3.3}],
    "branches": [{"id": "b1", "current": 0.05}],
    "instruments": [{"id": "meter1", "reading": 1.23}]
  }
}
```

- 高频更新：增量 delta；周期性全量 snapshot 防漂移。
- 前端插值平滑动画，后端为权威数据源。

## 场景配置 Schema

每个实验 JSON/YAML 配置包含：

- `equipment[]`：模型路径、默认参数、热区、可损坏性
- `initialState`：初始连接与参数
- `constraints`：安全顺序、禁用操作列表
- `gradingRules`：考核扣分项
- `randomParams`：考核随机化范围

## 性能与扩展

- 仿真微服务按力/电/光拆分部署。
- WASM 物理放 Worker，避免阻塞 UI 线程。
- 复杂光学/干涉可 GPU shader 或预计算 lookup table。

## 开发检查清单

- [ ] 参数变化触发重算，非硬编码曲线
- [ ] 错误操作触发损坏/锁定逻辑
- [ ] 前后端状态一致，断线可恢复
- [ ] 考核随机参数可复现（seed）
- [ ] 回放事件与实时操作格式统一

## 参考

- 功能清单见 [开发文档.md](../../../开发文档.md) 第二节「2D/3D仿真引擎」
- 前端渲染见 [physlab-frontend-ui](../physlab-frontend-ui/SKILL.md)
- 后端计算服务见 [physlab-backend](../physlab-backend/SKILL.md)
