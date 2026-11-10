<script setup>
/**
 * BackgroundFX.vue —— 全局背景动效（挂在 App.vue，路由切换时不重建）
 *   · 缓慢飘落的桃花瓣：远 / 中 / 近三层，各自带摆动、自转与描边
 *   · 流动的红绸：三条正弦波绸带缓慢起伏，一缕高光沿绸面游走
 *   · 鼠标视差：三层按深度以不同幅度跟随光标，带缓动回中
 *   · 静 / 动开关：右下角圆钮，状态写入 localStorage；系统「减少动态」时默认静
 * 单张 Canvas、单个 requestAnimationFrame，画布 pointer-events:none，不拦截任何交互。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
const STORE_KEY = 'huyao_bgfx'
const enabled = ref(true)

const toggleTitle = computed(() => enabled.value
  ? '背景动效运行中 · 点击暂停'
  : '背景动效已暂停 · 点击开启')

let ctx = null
let raf = 0
let W = 0, H = 0, dpr = 1
let last = 0, time = 0, motion = 1
let lastFx = -9, lastFy = -9
let petals = [], ribbons = []

/* 归一化光标位置（-1 ~ 1），x/y 为缓动后的实际值 */
const pointer = { tx: 0, ty: 0, x: 0, y: 0 }

/* 比壁纸里静止的花瓣更饱和，并带深色轮廓，避免动态粒子糊进背景 */
const PETAL_COLORS = [
  ['#ffe3e9', '#ef889c'],
  ['#ffd3e0', '#e36f90'],
  ['#fff0e8', '#eda79d'],
  ['#ffc9d6', '#d4516d'],
]

function rand(a, b) { return a + Math.random() * (b - a) }

/** 三层花瓣：深度同时决定大小、下落速度与视差幅度 */
function makePetals() {
  const count = W < 700 ? 28 : 54
  const depths = [0.42, 0.75, 1.25]
  const arr = []
  for (let i = 0; i < count; i++) {
    const depth = depths[i % 3]
    arr.push({
      x: rand(0, W),
      y: rand(-H * 0.2, H),
      size: rand(6, 12) * (0.7 + depth * 0.55),
      depth,
      vy: rand(8, 17) * (0.55 + depth * 0.5),
      swayAmp: rand(12, 38) * (0.6 + depth * 0.5),
      swayFreq: rand(0.25, 0.65),
      phase: rand(0, Math.PI * 2),
      angle: rand(0, Math.PI * 2),
      spin: rand(-0.5, 0.5),
      alpha: rand(0.45, 0.9) * (0.62 + depth * 0.34),
      color: PETAL_COLORS[i % PETAL_COLORS.length],
    })
  }
  return arr
}

/** 三条斜向红绸：不同的倾角、波频与相位，避免整齐划一 */
function makeRibbons() {
  return [
    { baseY: H * 0.18, angle: -0.16, a1: 28, a2: 13, f1: 0.0060, f2: 0.013,
      s1: 0.22, s2: 0.14, phase: 0,   width: 20, alpha: 0.20, depth: 0.5 },
    { baseY: H * 0.52, angle: 0.10,  a1: 36, a2: 17, f1: 0.0045, f2: 0.011,
      s1: 0.17, s2: 0.11, phase: 2.1, width: 28, alpha: 0.17, depth: 0.8 },
    { baseY: H * 0.84, angle: -0.09, a1: 24, a2: 11, f1: 0.0070, f2: 0.015,
      s1: 0.25, s2: 0.16, phase: 4.3, width: 17, alpha: 0.19, depth: 1.05 },
  ]
}

function resize() {
  const c = canvasRef.value
  if (!c) return
  W = window.innerWidth
  H = window.innerHeight
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  c.width = Math.round(W * dpr)
  c.height = Math.round(H * dpr)
  c.style.width = W + 'px'
  c.style.height = H + 'px'
  ctx = c.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  petals = makePetals()
  ribbons = makeRibbons()
  if (!enabled.value && ctx) ctx.clearRect(0, 0, W, H)
}

/** 一枚桃花瓣：两片贝塞尔勾出心形瓣尖，叠渐变、轮廓与细脉 */
function drawPetal(p, t, ox, oy) {
  const x = p.x + Math.sin(t * p.swayFreq + p.phase) * p.swayAmp + ox
  const y = p.y + oy
  ctx.save()
  ctx.translate(x, y)
  ctx.rotate(p.angle)
  ctx.scale(p.size, p.size)
  ctx.globalAlpha = Math.min(1, p.alpha)
  ctx.beginPath()
  ctx.moveTo(0, -1)
  ctx.bezierCurveTo(0.78, -0.92, 0.98, 0.22, 0, 1)
  ctx.bezierCurveTo(-0.98, 0.22, -0.78, -0.92, 0, -1)
  ctx.closePath()
  const g = ctx.createLinearGradient(-0.8, -1, 0.8, 1)
  g.addColorStop(0, p.color[0])
  g.addColorStop(1, p.color[1])
  ctx.fillStyle = g
  ctx.fill()
  ctx.globalAlpha = p.alpha * 0.55
  ctx.strokeStyle = '#c04a68'
  ctx.lineWidth = 0.07
  ctx.stroke()
  ctx.globalAlpha = p.alpha * 0.4
  ctx.strokeStyle = '#a83b58'
  ctx.lineWidth = 0.05
  ctx.beginPath()
  ctx.moveTo(0, -0.78)
  ctx.quadraticCurveTo(0.16, 0, 0, 0.82)
  ctx.stroke()
  ctx.restore()
}

/** 一条红绸：波浪带上下面闭合填充，再描一道游走的高光 */
function drawRibbon(rb, t, ox, oy) {
  ctx.save()
  ctx.translate(W / 2 + ox, H / 2 + oy)
  ctx.rotate(rb.angle)
  const span = Math.hypot(W, H) * 0.75
  const cy = rb.baseY - H / 2
  const pts = []
  for (let x = -span; x <= span; x += 22) {
    pts.push([x, cy
      + Math.sin(x * rb.f1 + t * rb.s1 + rb.phase) * rb.a1
      + Math.sin(x * rb.f2 - t * rb.s2) * rb.a2])
  }
  const hw = rb.width / 2
  const taper = (i) => 0.55 + 0.45 * Math.abs(Math.sin(i * 0.12 + rb.phase))

  ctx.beginPath()
  for (let i = 0; i < pts.length; i++) {
    const p = pts[i]
    if (i === 0) ctx.moveTo(p[0], p[1] - hw * taper(i))
    else ctx.lineTo(p[0], p[1] - hw * taper(i))
  }
  for (let i = pts.length - 1; i >= 0; i--) {
    const p = pts[i]
    ctx.lineTo(p[0], p[1] + hw * taper(i))
  }
  ctx.closePath()
  const g = ctx.createLinearGradient(0, cy - 40, 0, cy + 40)
  g.addColorStop(0,    'rgba(255,150,150,.06)')
  g.addColorStop(0.45, 'rgba(205,42,58,.98)')
  g.addColorStop(0.60, 'rgba(158,28,46,.88)')
  g.addColorStop(1,    'rgba(255,170,160,.06)')
  ctx.fillStyle = g
  ctx.globalAlpha = rb.alpha
  ctx.fill()

  ctx.beginPath()
  for (let i = 0; i < pts.length; i++) {
    const p = pts[i]
    if (i === 0) ctx.moveTo(p[0], p[1] - hw * 0.18)
    else ctx.lineTo(p[0], p[1] - hw * 0.18)
  }
  ctx.globalAlpha = Math.min(1, rb.alpha * 1.8)
  ctx.strokeStyle = 'rgba(255,232,210,.9)'
  ctx.lineWidth = 1.8
  ctx.setLineDash([40, 140])
  ctx.lineDashOffset = -t * 48
  ctx.stroke()
  ctx.setLineDash([])
  ctx.restore()
}

function frame(now) {
  raf = requestAnimationFrame(frame)
  if (!last) last = now
  let dt = (now - last) / 1000
  last = now
  if (dt > 0.05) dt = 0.05       // 切回标签页时不要瞬移
  time += dt * motion

  const k = Math.min(1, dt * 3.2)  // 光标缓动
  pointer.x += (pointer.tx - pointer.x) * k
  pointer.y += (pointer.ty - pointer.y) * k

  // 把缓动后的视差偏移暴露为 CSS 变量：既可供样式复用，也便于验证视差确实生效
  if (Math.abs(pointer.x - lastFx) > 0.002 || Math.abs(pointer.y - lastFy) > 0.002) {
    lastFx = pointer.x; lastFy = pointer.y
    const c = canvasRef.value
    if (c) {
      c.style.setProperty('--fx-x', pointer.x.toFixed(3))
      c.style.setProperty('--fx-y', pointer.y.toFixed(3))
    }
  }

  ctx.clearRect(0, 0, W, H)

  for (const rb of ribbons) {
    drawRibbon(rb, time, pointer.x * rb.depth * 32, pointer.y * rb.depth * 22)
  }
  for (const p of petals) {
    p.y += p.vy * dt * motion
    p.angle += p.spin * dt * motion
    if (p.y - p.size > H + 20) { p.y = -p.size * 2; p.x = rand(0, W) }
    drawPetal(p, time, pointer.x * p.depth * 28, pointer.y * p.depth * 18)
  }
}

function onPointerMove(e) {
  pointer.tx = (e.clientX / W - 0.5) * 2
  pointer.ty = (e.clientY / H - 0.5) * 2
}
function onPointerOut(e) {
  if (!e.relatedTarget) { pointer.tx = 0; pointer.ty = 0 }
}
function onVisibility() {
  if (document.hidden) {
    if (raf) { cancelAnimationFrame(raf); raf = 0 }
  } else applyState()
}

/** 统一的启停：关闭时停帧并清空画布，恢复纯壁纸 */
function applyState() {
  if (enabled.value) {
    if (!raf) { last = 0; raf = requestAnimationFrame(frame) }
  } else {
    if (raf) { cancelAnimationFrame(raf); raf = 0 }
    if (ctx) ctx.clearRect(0, 0, W, H)
    const c = canvasRef.value
    if (c) {
      c.style.setProperty('--fx-x', '0')
      c.style.setProperty('--fx-y', '0')
    }
    lastFx = -9; lastFy = -9
  }
}

function toggle() {
  enabled.value = !enabled.value
  try { localStorage.setItem(STORE_KEY, enabled.value ? '1' : '0') } catch (e) { /* 隐私模式下忽略 */ }
  applyState()
}

onMounted(() => {
  const mq = window.matchMedia('(prefers-reduced-motion: reduce)')
  motion = mq.matches ? 0.35 : 1
  let saved = null
  try { saved = localStorage.getItem(STORE_KEY) } catch (e) { /* 忽略 */ }
  enabled.value = saved === null ? !mq.matches : saved === '1'
  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener('mouseout', onPointerOut)
  window.addEventListener('blur', onPointerOut)
  document.addEventListener('visibilitychange', onVisibility)
  if (enabled.value) { last = 0; raf = requestAnimationFrame(frame) }
  else applyState()
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('mouseout', onPointerOut)
  window.removeEventListener('blur', onPointerOut)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<template>
  <canvas ref="canvasRef" class="bg-fx" aria-hidden="true"></canvas>
  <button class="fx-toggle" :class="{ 'is-off': !enabled }" type="button"
          :aria-pressed="enabled" :aria-label="toggleTitle" :title="toggleTitle"
          @click="toggle">{{ enabled ? '动' : '静' }}</button>
</template>

<style scoped>
.bg-fx {
  position: fixed;
  inset: 0;
  z-index: 0;
  display: block;
  pointer-events: none;
}

.fx-toggle {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 6;
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(142,44,44,.45);
  border-radius: 50%;
  background: rgba(250,245,234,.72);
  color: #8e2c2c;
  font-family: inherit;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  pointer-events: auto;
  box-shadow: 0 2px 10px rgba(90,40,40,.18);
  transition: transform .2s ease, background .2s ease, color .2s ease;
}
.fx-toggle:hover { transform: scale(1.08); background: rgba(250,245,234,.94); }
.fx-toggle:focus-visible { outline: 2px solid #a32d2d; outline-offset: 2px; }
.fx-toggle.is-off { color: #6b6560; border-color: rgba(107,101,96,.38); }

@media (max-width: 600px) {
  .fx-toggle { right: 12px; bottom: 12px; width: 38px; height: 38px; font-size: 14px; }
}
</style>
