<template>
  <div ref="container" class="three-bg"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'

const container = ref<HTMLDivElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let animationId: number
let clock: THREE.Clock
let shapes: THREE.Group[] = []
let particles: THREE.Points | null = null
let mouseX = 0
let mouseY = 0
let targetMouseX = 0
let targetMouseY = 0

const ACCENT1 = '#7c3aed' // purple
const ACCENT2 = '#6366f1' // indigo
const ACCENT3 = '#06b6d4' // cyan
const WHITE = '#f8f9fc'
const DARK = '#0f0f1a'

// ── Create a curved tube mesh (the main 3D shape from the video) ──
function createCurvedTube(
  controlPoints: THREE.Vector3[],
  radius: number,
  tubularSegments: number,
  radialSegments: number,
  colorEdge: string,
  closed: boolean = false
): THREE.Group {
  const group = new THREE.Group()

  // Build the curve
  const curve = new THREE.CatmullRomCurve3(controlPoints, closed)

  // Base white geometry
  const tubeGeo = new THREE.TubeGeometry(curve, tubularSegments, radius, radialSegments, closed)
  const tubeMat = new THREE.MeshPhysicalMaterial({
    color: new THREE.Color(WHITE),
    metalness: 0.05,
    roughness: 0.35,
    clearcoat: 0.05,
    clearcoatRoughness: 0.4,
    reflectivity: 0.5,
  })
  const tubeMesh = new THREE.Mesh(tubeGeo, tubeMat)
  group.add(tubeMesh)

  // Edge glow: a slightly larger wireframe on top
  const edgeGeo = new THREE.TubeGeometry(curve, tubularSegments, radius * 1.06, radialSegments, closed)
  const edgeMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colorEdge),
    wireframe: true,
    transparent: true,
    opacity: 0.25,
    depthWrite: false,
  })
  const edgeMesh = new THREE.Mesh(edgeGeo, edgeMat)
  edgeMesh.renderOrder = 1
  edgeMesh.material.depthTest = true
  group.add(edgeMesh)

  // Cap glow rings at each end (for open tubes)
  if (!closed) {
    for (const pt of [controlPoints[0], controlPoints[controlPoints.length - 1]]) {
      const ringGeo = new THREE.TorusGeometry(radius * 1.08, 0.04, 16, 32)
      const ringMat = new THREE.MeshBasicMaterial({
        color: new THREE.Color(colorEdge),
        transparent: true,
        opacity: 0.5,
        depthWrite: false,
      })
      const ring = new THREE.Mesh(ringGeo, ringMat)
      ring.position.copy(pt)
      // Align ring to curve tangent
      const tangent = curve.getTangent(pt.equals(controlPoints[0]) ? 0 : 1)
      ring.lookAt(pt.clone().add(tangent))
      ring.rotateX(Math.PI / 2)
      ring.renderOrder = 2
      group.add(ring)
    }
  }

  return group
}

// ── Create a torus knot shape (like the video's central piece) ──
function createTorusKnot(colorEdge: string): THREE.Group {
  const group = new THREE.Group()

  const geo = new THREE.TorusKnotGeometry(1.0, 0.18, 200, 24)
  const mat = new THREE.MeshPhysicalMaterial({
    color: new THREE.Color(WHITE),
    metalness: 0.05,
    roughness: 0.3,
    clearcoat: 0.08,
    clearcoatRoughness: 0.35,
  })
  const mesh = new THREE.Mesh(geo, mat)
  group.add(mesh)

  // Edge wireframe
  const edgeGeo = new THREE.TorusKnotGeometry(1.06, 0.19, 200, 24)
  const edgeMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colorEdge),
    wireframe: true,
    transparent: true,
    opacity: 0.2,
    depthWrite: false,
  })
  const edgeMesh = new THREE.Mesh(edgeGeo, edgeMat)
  edgeMesh.renderOrder = 1
  group.add(edgeMesh)

  return group
}

// ── Create a simple torus (ring) shape ──
function createTorus(radius: number, tubeRadius: number, colorEdge: string): THREE.Group {
  const group = new THREE.Group()

  const geo = new THREE.TorusGeometry(radius, tubeRadius, 32, 100)
  const mat = new THREE.MeshPhysicalMaterial({
    color: new THREE.Color(WHITE),
    metalness: 0.05,
    roughness: 0.3,
  })
  const mesh = new THREE.Mesh(geo, mat)
  group.add(mesh)

  const edgeGeo = new THREE.TorusGeometry(radius * 1.03, tubeRadius * 1.08, 32, 100)
  const edgeMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colorEdge),
    wireframe: true,
    transparent: true,
    opacity: 0.22,
    depthWrite: false,
  })
  const edgeMesh = new THREE.Mesh(edgeGeo, edgeMat)
  edgeMesh.renderOrder = 1
  group.add(edgeMesh)

  return group
}

// ── Particle system (dust/stars like the video) ──
function createParticles(): THREE.Points {
  const count = 400
  const positions = new Float32Array(count * 3)
  const sizes = new Float32Array(count)

  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 16
    positions[i * 3 + 1] = (Math.random() - 0.5) * 10
    positions[i * 3 + 2] = (Math.random() - 0.5) * 8
    sizes[i] = Math.random() * 0.04 + 0.01
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('size', new THREE.BufferAttribute(sizes, 1))

  // Circle sprite texture via canvas
  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 32
  const ctx = canvas.getContext('2d')!
  const gradient = ctx.createRadialGradient(16, 16, 0, 16, 16, 16)
  gradient.addColorStop(0, 'rgba(255,255,255,0.9)')
  gradient.addColorStop(0.3, 'rgba(255,255,255,0.5)')
  gradient.addColorStop(1, 'rgba(255,255,255,0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 32, 32)

  const texture = new THREE.CanvasTexture(canvas)
  const mat = new THREE.PointsMaterial({
    map: texture,
    size: 0.08,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    transparent: true,
    opacity: 0.5,
  })

  return new THREE.Points(geo, mat)
}

// ── Lighting ──
function createLights(): void {
  // Soft ambient
  const ambient = new THREE.AmbientLight(0xffffff, 0.7)
  scene.add(ambient)

  // Key light (warm white)
  const key = new THREE.DirectionalLight(0xffffff, 1.8)
  key.position.set(5, 8, 8)
  scene.add(key)

  // Fill light (cool)
  const fill = new THREE.DirectionalLight(0xc8d6ff, 0.8)
  fill.position.set(-5, -1, -2)
  scene.add(fill)

  // Rim light (accent)
  const rim = new THREE.DirectionalLight(new THREE.Color(ACCENT3), 0.6)
  rim.position.set(0, -3, -6)
  scene.add(rim)

  // Top light
  const top = new THREE.DirectionalLight(0xffffff, 0.5)
  top.position.set(0, 10, 0)
  scene.add(top)
}

// ── Mouse tracking ──
function onMouseMove(e: MouseEvent) {
  targetMouseX = (e.clientX / window.innerWidth) * 2 - 1
  targetMouseY = -(e.clientY / window.innerHeight) * 2 + 1
}

// ── Handle resize ──
function onResize() {
  if (!container.value) return
  const w = window.innerWidth
  const h = window.innerHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
}

// ── Setup ──
function init() {
  if (!container.value) return

  // Scene
  scene = new THREE.Scene()
  scene.background = new THREE.Color('#f5f6fa')
  scene.fog = new THREE.Fog('#f5f6fa', 6, 20)

  // Camera
  const w = window.innerWidth
  const h = window.innerHeight
  camera = new THREE.PerspectiveCamera(42, w / h, 0.1, 50)
  camera.position.set(0, 1.5, 8)
  camera.lookAt(0, 0, 0)

  // Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.1
  container.value.appendChild(renderer.domElement)

  // Lights
  createLights()

  // ── Create the 3D tubular shapes ──

  // Shape 1: Large curved tube (sweeping C-curve) — purple edge
  const points1 = [
    new THREE.Vector3(-1.8, 1.2, -1.5),
    new THREE.Vector3(-0.9, 0.0, -0.5),
    new THREE.Vector3(0.0, -0.8, 0.2),
    new THREE.Vector3(1.2, -0.4, 0.0),
    new THREE.Vector3(2.0, 0.6, -0.8),
  ]
  const tube1 = createCurvedTube(points1, 0.14, 120, 20, ACCENT1)
  tube1.position.set(1.5, 0.8, 0)
  tube1.rotation.z = 0.2
  tube1.rotation.y = -0.4
  tube1.userData = { speedX: 0.12, speedY: 0.08, speedZ: 0.06, amplitude: 0.5 }
  scene.add(tube1)
  shapes.push(tube1)

  // Shape 2: Curved tube — cyan edge  
  const points2 = [
    new THREE.Vector3(2.0, -0.8, -0.5),
    new THREE.Vector3(1.0, -0.2, 0.3),
    new THREE.Vector3(0.0, 0.5, -0.3),
    new THREE.Vector3(-1.0, 0.8, 0.5),
    new THREE.Vector3(-2.2, 0.2, 0.0),
  ]
  const tube2 = createCurvedTube(points2, 0.12, 100, 18, ACCENT3)
  tube2.position.set(-1.2, -0.5, 0)
  tube2.rotation.y = 0.6
  tube2.rotation.x = -0.15
  tube2.userData = { speedX: -0.1, speedY: 0.09, speedZ: 0.05, amplitude: 0.4 }
  scene.add(tube2)
  shapes.push(tube2)

  // Shape 3: Torus knot — accent1 (purple edge, focal point)
  const knot1 = createTorusKnot(ACCENT1)
  knot1.position.set(0.3, 0.0, 0.5)
  knot1.scale.setScalar(0.55)
  knot1.userData = { speedX: 0.15, speedY: 0.1, speedZ: 0.08, amplitude: 0.6 }
  scene.add(knot1)
  shapes.push(knot1)

  // Shape 4: Another curved tube behind — indigo edge
  const points3 = [
    new THREE.Vector3(-1.5, 0.8, -2),
    new THREE.Vector3(-0.5, 0.3, -1),
    new THREE.Vector3(0.5, -0.5, -0.5),
    new THREE.Vector3(1.5, -0.2, -1),
  ]
  const tube3 = createCurvedTube(points3, 0.1, 80, 16, ACCENT2)
  tube3.position.set(0, -0.2, -1.5)
  tube3.rotation.y = 0.3
  tube3.userData = { speedX: 0.08, speedY: -0.06, speedZ: 0.07, amplitude: 0.35 }
  scene.add(tube3)
  shapes.push(tube3)

  // Shape 5: Large torus ring (like a halo)
  const torus = createTorus(1.3, 0.06, ACCENT3)
  torus.position.set(-1.0, 0.3, -0.8)
  torus.rotation.x = Math.PI * 0.35
  torus.rotation.y = Math.PI * 0.25
  torus.userData = { speedX: 0.06, speedY: 0.07, speedZ: 0.04, amplitude: 0.3 }
  scene.add(torus)
  shapes.push(torus)

  // Shape 6: Small torus knot — cyan edge
  const knot2 = createTorusKnot(ACCENT3)
  knot2.position.set(1.8, -0.6, -1.0)
  knot2.scale.setScalar(0.35)
  knot2.userData = { speedX: -0.13, speedY: 0.08, speedZ: 0.06, amplitude: 0.45 }
  scene.add(knot2)
  shapes.push(knot2)

  // Shape 7: Curved tube vertical sweep
  const points4 = [
    new THREE.Vector3(0, 1.5, -1.5),
    new THREE.Vector3(0.3, 0.5, -0.8),
    new THREE.Vector3(-0.2, -0.5, -0.2),
    new THREE.Vector3(0.1, -1.2, -0.8),
  ]
  const tube4 = createCurvedTube(points4, 0.1, 80, 16, ACCENT2)
  tube4.position.set(1.0, 0, -1.5)
  tube4.rotation.z = -0.3
  tube4.userData = { speedX: 0.09, speedY: -0.07, speedZ: 0.05, amplitude: 0.4 }
  scene.add(tube4)
  shapes.push(tube4)

  // Particles
  particles = createParticles()
  scene.add(particles)

  // Clock
  clock = new THREE.Clock()

  // Event listeners
  window.addEventListener('mousemove', onMouseMove, { passive: true })
  window.addEventListener('resize', onResize)

  // Start
  animate()
}

// ── Animation loop ──
function animate() {
  animationId = requestAnimationFrame(animate)

  const dt = Math.min(clock.getDelta(), 0.1)
  const t = clock.getElapsedTime()

  // Smooth mouse follow
  mouseX += (targetMouseX - mouseX) * 0.02
  mouseY += (targetMouseY - mouseY) * 0.02

  // Move camera slightly with mouse
  camera.position.x += (mouseX * 0.6 - camera.position.x) * 0.03
  camera.position.y += (mouseY * 0.3 + 1.5 - camera.position.y) * 0.03
  camera.lookAt(0, 0, 0)

  // Animate each shape
  for (const shape of shapes) {
    const ud = shape.userData as any
    shape.rotation.x += ud.speedX * dt
    shape.rotation.y += ud.speedY * dt
    shape.rotation.z += ud.speedZ * dt

    // Subtle floating
    shape.position.y += Math.sin(t * ud.speedY * 10 + shape.position.x) * 0.002 * ud.amplitude
  }

  // Rotate particles slowly
  if (particles) {
    particles.rotation.y += dt * 0.03
    particles.rotation.x += dt * 0.015
  }

  renderer.render(scene, camera)
}

// ── Cleanup ──
function dispose() {
  if (animationId) cancelAnimationFrame(animationId)

  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('resize', onResize)

  for (const shape of shapes) {
    shape.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.geometry.dispose()
        if (Array.isArray(child.material)) {
          child.material.forEach((m) => m.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
  }
  shapes = []

  if (particles) {
    particles.geometry.dispose()
    ;(particles.material as THREE.Material).dispose()
    particles = null
  }

  if (renderer) {
    renderer.dispose()
    if (container.value && renderer.domElement.parentElement === container.value) {
      container.value.removeChild(renderer.domElement)
    }
  }
}

// ── Lifecycle ──
let reducedMotion = false

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  init()
})

onBeforeUnmount(() => {
  dispose()
})
</script>

<style scoped>
.three-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.three-bg :deep(canvas) {
  display: block;
}
</style>
