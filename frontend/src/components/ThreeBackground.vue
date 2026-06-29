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

const ACCENT1 = '#5B6FF5'
const ACCENT2 = '#8B5CF6'
const ACCENT3 = '#6366f1'
const WHITE = '#f8f9fc'

function createCurvedTube(
  controlPoints: THREE.Vector3[],
  radius: number,
  tubularSegments: number,
  radialSegments: number,
  colorEdge: string,
  closed: boolean = false
): THREE.Group {
  const group = new THREE.Group()
  const curve = new THREE.CatmullRomCurve3(controlPoints, closed)

  const tubeGeo = new THREE.TubeGeometry(curve, tubularSegments, radius, radialSegments, closed)
  const tubeMat = new THREE.MeshPhysicalMaterial({
    color: new THREE.Color(WHITE),
    metalness: 0.05,
    roughness: 0.35,
    clearcoat: 0.05,
    clearcoatRoughness: 0.4,
  })
  const tubeMesh = new THREE.Mesh(tubeGeo, tubeMat)
  group.add(tubeMesh)

  const edgeGeo = new THREE.TubeGeometry(curve, tubularSegments, radius * 1.06, radialSegments, closed)
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

  if (!closed) {
    for (const pt of [controlPoints[0], controlPoints[controlPoints.length - 1]]) {
      const ringGeo = new THREE.TorusGeometry(radius * 1.08, 0.04, 16, 32)
      const ringMat = new THREE.MeshBasicMaterial({
        color: new THREE.Color(colorEdge),
        transparent: true,
        opacity: 0.45,
        depthWrite: false,
      })
      const ring = new THREE.Mesh(ringGeo, ringMat)
      ring.position.copy(pt)
      const tangent = curve.getTangent(pt.equals(controlPoints[0]) ? 0 : 1)
      ring.lookAt(pt.clone().add(tangent))
      ring.rotateX(Math.PI / 2)
      ring.renderOrder = 2
      group.add(ring)
    }
  }
  return group
}

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

  const edgeGeo = new THREE.TorusKnotGeometry(1.06, 0.19, 200, 24)
  const edgeMat = new THREE.MeshBasicMaterial({
    color: new THREE.Color(colorEdge),
    wireframe: true,
    transparent: true,
    opacity: 0.18,
    depthWrite: false,
  })
  const edgeMesh = new THREE.Mesh(edgeGeo, edgeMat)
  edgeMesh.renderOrder = 1
  group.add(edgeMesh)
  return group
}

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
    opacity: 0.2,
    depthWrite: false,
  })
  const edgeMesh = new THREE.Mesh(edgeGeo, edgeMat)
  edgeMesh.renderOrder = 1
  group.add(edgeMesh)
  return group
}

function createParticles(): THREE.Points {
  const count = 500
  const positions = new Float32Array(count * 3)
  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 18
    positions[i * 3 + 1] = (Math.random() - 0.5) * 12
    positions[i * 3 + 2] = (Math.random() - 0.5) * 10
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))

  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 32
  const ctx = canvas.getContext('2d')!
  const gradient = ctx.createRadialGradient(16, 16, 0, 16, 16, 16)
  gradient.addColorStop(0, 'rgba(91,111,245,0.8)')
  gradient.addColorStop(0.3, 'rgba(139,92,246,0.4)')
  gradient.addColorStop(1, 'rgba(139,92,246,0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 32, 32)

  const texture = new THREE.CanvasTexture(canvas)
  const mat = new THREE.PointsMaterial({
    map: texture,
    size: 0.07,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    transparent: true,
    opacity: 0.5,
  })
  return new THREE.Points(geo, mat)
}

function createLights(): void {
  scene.add(new THREE.AmbientLight(0xffffff, 0.7))
  const key = new THREE.DirectionalLight(0xffffff, 1.8)
  key.position.set(5, 8, 8)
  scene.add(key)
  const fill = new THREE.DirectionalLight(0xc8d6ff, 0.8)
  fill.position.set(-5, -1, -2)
  scene.add(fill)
  const rim = new THREE.DirectionalLight(new THREE.Color(ACCENT3), 0.6)
  rim.position.set(0, -3, -6)
  scene.add(rim)
  const topLight = new THREE.DirectionalLight(0xffffff, 0.5)
  topLight.position.set(0, 10, 0)
  scene.add(topLight)
}

function onMouseMove(e: MouseEvent) {
  targetMouseX = (e.clientX / window.innerWidth) * 2 - 1
  targetMouseY = -(e.clientY / window.innerHeight) * 2 + 1
}

function onResize() {
  if (!container.value) return
  const w = window.innerWidth
  const h = window.innerHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
}

function init() {
  if (!container.value) return

  scene = new THREE.Scene()
  scene.background = new THREE.Color('#F8F9FB')
  scene.fog = new THREE.Fog('#F8F9FB', 6, 20)

  const w = window.innerWidth
  const h = window.innerHeight
  camera = new THREE.PerspectiveCamera(42, w / h, 0.1, 50)
  camera.position.set(0, 1.5, 8)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.1
  container.value.appendChild(renderer.domElement)

  createLights()

  // Shape 1: Large curved tube
  const points1 = [
    new THREE.Vector3(-1.8, 1.2, -1.5), new THREE.Vector3(-0.9, 0.0, -0.5),
    new THREE.Vector3(0.0, -0.8, 0.2), new THREE.Vector3(1.2, -0.4, 0.0),
    new THREE.Vector3(2.0, 0.6, -0.8),
  ]
  const tube1 = createCurvedTube(points1, 0.14, 120, 20, ACCENT1)
  tube1.position.set(1.5, 0.8, 0); tube1.rotation.z = 0.2; tube1.rotation.y = -0.4
  tube1.userData = { speedX: 0.12, speedY: 0.08, speedZ: 0.06, amplitude: 0.5 }
  scene.add(tube1); shapes.push(tube1)

  // Shape 2: Curved tube
  const points2 = [
    new THREE.Vector3(2.0, -0.8, -0.5), new THREE.Vector3(1.0, -0.2, 0.3),
    new THREE.Vector3(0.0, 0.5, -0.3), new THREE.Vector3(-1.0, 0.8, 0.5),
    new THREE.Vector3(-2.2, 0.2, 0.0),
  ]
  const tube2 = createCurvedTube(points2, 0.12, 100, 18, ACCENT3)
  tube2.position.set(-1.2, -0.5, 0); tube2.rotation.y = 0.6; tube2.rotation.x = -0.15
  tube2.userData = { speedX: -0.1, speedY: 0.09, speedZ: 0.05, amplitude: 0.4 }
  scene.add(tube2); shapes.push(tube2)

  // Shape 3: Torus knot
  const knot1 = createTorusKnot(ACCENT1)
  knot1.position.set(0.3, 0.0, 0.5); knot1.scale.setScalar(0.55)
  knot1.userData = { speedX: 0.15, speedY: 0.1, speedZ: 0.08, amplitude: 0.6 }
  scene.add(knot1); shapes.push(knot1)

  // Shape 4: Another tube
  const points3 = [
    new THREE.Vector3(-1.5, 0.8, -2), new THREE.Vector3(-0.5, 0.3, -1),
    new THREE.Vector3(0.5, -0.5, -0.5), new THREE.Vector3(1.5, -0.2, -1),
  ]
  const tube3 = createCurvedTube(points3, 0.1, 80, 16, ACCENT2)
  tube3.position.set(0, -0.2, -1.5); tube3.rotation.y = 0.3
  tube3.userData = { speedX: 0.08, speedY: -0.06, speedZ: 0.07, amplitude: 0.35 }
  scene.add(tube3); shapes.push(tube3)

  // Shape 5: Torus ring
  const torus = createTorus(1.3, 0.06, ACCENT3)
  torus.position.set(-1.0, 0.3, -0.8); torus.rotation.x = Math.PI * 0.35; torus.rotation.y = Math.PI * 0.25
  torus.userData = { speedX: 0.06, speedY: 0.07, speedZ: 0.04, amplitude: 0.3 }
  scene.add(torus); shapes.push(torus)

  // Shape 6: Small knot
  const knot2 = createTorusKnot(ACCENT3)
  knot2.position.set(1.8, -0.6, -1.0); knot2.scale.setScalar(0.35)
  knot2.userData = { speedX: -0.13, speedY: 0.08, speedZ: 0.06, amplitude: 0.45 }
  scene.add(knot2); shapes.push(knot2)

  // Shape 7: Vertical tube
  const points4 = [
    new THREE.Vector3(0, 1.5, -1.5), new THREE.Vector3(0.3, 0.5, -0.8),
    new THREE.Vector3(-0.2, -0.5, -0.2), new THREE.Vector3(0.1, -1.2, -0.8),
  ]
  const tube4 = createCurvedTube(points4, 0.1, 80, 16, ACCENT2)
  tube4.position.set(1.0, 0, -1.5); tube4.rotation.z = -0.3
  tube4.userData = { speedX: 0.09, speedY: -0.07, speedZ: 0.05, amplitude: 0.4 }
  scene.add(tube4); shapes.push(tube4)

  // Shape 8: Small torus
  const torus2 = createTorus(0.9, 0.05, ACCENT1)
  torus2.position.set(-1.8, -0.8, -1.5); torus2.rotation.x = Math.PI * 0.45; torus2.rotation.z = Math.PI * 0.2
  torus2.userData = { speedX: -0.07, speedY: 0.06, speedZ: 0.05, amplitude: 0.3 }
  scene.add(torus2); shapes.push(torus2)

  particles = createParticles()
  scene.add(particles)

  clock = new THREE.Clock()
  window.addEventListener('mousemove', onMouseMove, { passive: true })
  window.addEventListener('resize', onResize)
  animate()
}

function animate() {
  animationId = requestAnimationFrame(animate)
  const dt = Math.min(clock.getDelta(), 0.1)
  const t = clock.getElapsedTime()

  mouseX += (targetMouseX - mouseX) * 0.02
  mouseY += (targetMouseY - mouseY) * 0.02
  camera.position.x += (mouseX * 0.6 - camera.position.x) * 0.03
  camera.position.y += (mouseY * 0.3 + 1.5 - camera.position.y) * 0.03
  camera.lookAt(0, 0, 0)

  for (const shape of shapes) {
    const ud = shape.userData as any
    shape.rotation.x += ud.speedX * dt
    shape.rotation.y += ud.speedY * dt
    shape.rotation.z += ud.speedZ * dt
    shape.position.y += Math.sin(t * ud.speedY * 10 + shape.position.x) * 0.002 * ud.amplitude
  }

  if (particles) {
    particles.rotation.y += dt * 0.025
    particles.rotation.x += dt * 0.012
  }

  renderer.render(scene, camera)
}

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

onMounted(() => { init() })
onBeforeUnmount(() => { dispose() })
</script>

<style scoped>
.three-bg {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 0;
  pointer-events: none;
}
.three-bg :deep(canvas) {
  display: block;
}
</style>
