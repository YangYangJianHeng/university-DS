---
name: physlab-3d-modeling
description: >-
  Builds high-fidelity procedural Three.js 3D models for PhysLab physics
  experiments including lab bench, bulbs, rheostats, power supplies, wires,
  and current flow effects. Use when creating or improving 3D simulation
  scenes, equipment models, or visual effects for the experiment workspace.
---

# PhysLab 3D 建模规范

## 原则

1. **禁止** 用单一 Sphere/Box 代替器材；每个器材必须是 `THREE.Group` 组合件。
2. **参考真实实验台**：绿色防静电垫 / 木质台面、金属支架、红/黑香蕉插头导线。
3. **材质分层**：金属（MeshStandardMaterial, metalness 0.8）、玻璃（MeshPhysicalMaterial, transmission）、陶瓷（roughness 0.9）。
4. **物理联动**：电流越大 → 灯丝 emissive 越强；短路 → 红色脉冲 + 烟雾粒子。
5. **性能**：场景只初始化一次，props 变化走 `update()` 而非重建 `useEffect`。

## 目录结构

```
frontend/src/modules/simulation/scene/
├── CircuitLabScene.ts      # 场景主控：相机、灯光、动画循环
├── types.ts                # SceneState 接口
├── materials.ts            # 共享材质工厂
├── models/
│   ├── LabBench.ts
│   ├── BulbModel.ts
│   ├── RheostatModel.ts
│   ├── PowerSupplyModel.ts
│   ├── AmmeterModel.ts
│   └── WireSystem.ts       # 导线管 + 电流粒子
└── effects/
    └── ShortCircuitEffect.ts
```

## 器材建模要点

| 器材 | 几何组合 | 材质/特效 |
|------|----------|-----------|
| 小灯泡 | 玻璃泡 + 螺旋灯丝 + E27 金属灯头 | 玻璃 transmission 0.9；灯丝 emissive 随 I 变化 |
| 滑动变阻器 | 陶瓷管 + 镍铬线圈 + 滑块手柄 + 接线柱 | 线圈 emissive 微光；滑块位置绑定参数 |
| 直流电源 | 金属外壳 + 前面板 + 数码管读数 + 红/黑接线柱 | 显示屏 CanvasTexture 实时电压 |
| 导线 | CatmullRomCurve3 + TubeGeometry | 红正黑负；电流粒子沿曲线流动 |
| 实验台 | 台面 + 防静电垫 + 金属腿 | 垫面 #2d5a3d，台面 wood tone |

## 灯光

- HemisphereLight（环境）+ DirectionalLight（主光，阴影）+ PointLight（灯丝点光）
- `renderer.toneMapping = ACESFilmicToneMapping`
- `renderer.shadowMap.enabled = true`

## 交互

- OrbitControls：限制 polar angle，默认 45° 俯视角
- 相机动画：短路时轻微 shake

## SceneState

```typescript
interface SceneState {
  current: number
  voltage: number
  shortCircuit: boolean
  rheostatValue: number
  powerConnected: boolean
  ammeterParallel: boolean
}
```

## 开发检查清单

- [ ] 器材可识别（非 primitive 占位）
- [ ] 导线为 3D 管状体，非 LineBasicMaterial
- [ ] 电流粒子方向与速度 ∝ |I|
- [ ] 场景 update 不触发 React 重挂载
- [ ] dispose 清理 geometry/material/texture

## 参考

- 前端 UI 布局见 [physlab-frontend-ui](../physlab-frontend-ui/SKILL.md)
- 仿真状态见 [physlab-simulation](../physlab-simulation/SKILL.md)
