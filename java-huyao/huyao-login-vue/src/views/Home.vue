<script setup>
/**
 * Home.vue —— 续缘主页（登录成功后跳转）
 * 风格与登录页一致：水墨晕染背景 + 桃花花瓣 + 姻缘红线
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const petals = ref([])

// 生成飘落花瓣数据
function makePetals() {
  const arr = []
  for (let i = 0; i < 22; i++) {
    arr.push({
      left: Math.random() * 100,
      size: 8 + Math.random() * 10,
      duration: 9 + Math.random() * 12,
      delay: -Math.random() * 20,
      opacity: 0.4 + Math.random() * 0.4,
    })
  }
  return arr
}

// 退出登录，返回登录页
function handleLogout() {
  router.push('/login')
}

onMounted(() => { petals.value = makePetals() })
</script>

<template>
  <div class="home-page">
    <!-- 飘落花瓣 -->
    <div class="petal-layer">
      <span
        v-for="(p, i) in petals" :key="i" class="petal"
        :style="{ left: p.left + 'vw', width: p.size + 'px', height: p.size * 0.8 + 'px',
                  opacity: p.opacity, animationDuration: p.duration + 's', animationDelay: p.delay + 's' }"
      />
    </div>

    <!-- 姻缘红线：右下 -->
    <svg class="thread" width="240" height="180" viewBox="0 0 240 180">
      <path d="M250 190 C 180 150, 220 90, 150 70 S 60 30, 100 -10"
            fill="none" stroke="#b03a3a" stroke-width="2.5" stroke-linecap="round" opacity="0.7"/>
      <path d="M250 170 C 170 130, 210 70, 140 50 S 70 10, 110 -10"
            fill="none" stroke="#c9584f" stroke-width="1.5" opacity="0.55"/>
      <circle cx="150" cy="70" r="5" fill="none" stroke="#b03a3a" stroke-width="2" opacity="0.8"/>
    </svg>

    <!-- 主内容 -->
    <div class="content">
      <h1 class="title">缘 起 再 续</h1>
      <p class="subtitle">红线既牵 · 此生不负相思意</p>
      <div class="divider"><span>❀</span></div>

      <div class="cards">
        <div class="mini-card">
          <span class="icon">🏮</span>
          <h3>续缘簿</h3>
          <p>记下未了的尘缘，等待重逢之时</p>
        </div>
        <div class="mini-card">
          <span class="icon">🧵</span>
          <h3>红线谱</h3>
          <p>牵起月老手中的红线，寻回前世之人</p>
        </div>
        <div class="mini-card">
          <span class="icon">🌸</span>
          <h3>桃花酿</h3>
          <p>一盏桃花酒，敬岁月与相思</p>
        </div>
      </div>

      <button class="btn-logout" @click="handleLogout">忘却此缘 · 退出</button>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh; overflow: hidden; position: relative;
  display: flex; align-items: center; justify-content: center;
  background:
    radial-gradient(ellipse 60% 45% at 18% 22%, rgba(127,154,153,.22), transparent 70%),
    radial-gradient(ellipse 50% 40% at 82% 75%, rgba(176,58,58,.14), transparent 70%),
    radial-gradient(ellipse 45% 35% at 70% 15%, rgba(184,146,74,.12), transparent 70%),
    linear-gradient(160deg, #f7f2e7 0%, #efe6d4 45%, #e8dcc8 100%);
}

.petal-layer { position: fixed; inset: 0; pointer-events: none; z-index: 1; }
.petal {
  position: absolute; top: -4vh;
  background: linear-gradient(135deg, #f6c9c9, #e79a9a);
  border-radius: 80% 20% 70% 30% / 60% 40% 60% 40%;
  animation: fall linear infinite;
}
@keyframes fall {
  0%   { transform: translate(0, -6vh) rotate(0deg); }
  25%  { transform: translate(4vw, 24vh) rotate(90deg); }
  50%  { transform: translate(-3vw, 50vh) rotate(180deg); }
  75%  { transform: translate(5vw, 76vh) rotate(270deg); }
  100% { transform: translate(-2vw, 108vh) rotate(360deg); }
}

.thread { position: fixed; right: 0; bottom: 0; z-index: 2; pointer-events: none; animation: sway 9s ease-in-out infinite; }
@keyframes sway {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-6px) rotate(1.2deg); }
}

.content { position: relative; z-index: 10; text-align: center; padding: 40px; }
.title { font-size: 42px; letter-spacing: 12px; color: #8e2c2c; margin-bottom: 10px; }
.subtitle { font-size: 16px; letter-spacing: 4px; color: #7f9a99; margin-bottom: 24px; }
.divider { display: flex; align-items: center; gap: 12px; width: 240px; margin: 0 auto 40px; }
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: linear-gradient(90deg, transparent, #b8924a, transparent); }
.divider span { color: #b03a3a; font-size: 15px; }

.cards { display: flex; gap: 24px; justify-content: center; margin-bottom: 44px; flex-wrap: wrap; }
.mini-card {
  width: 200px; padding: 26px 20px;
  background: rgba(250, 245, 234, .85); border: 1.5px solid #b8924a; border-radius: 14px;
  box-shadow: 0 8px 24px rgba(74,59,50,.14);
  transition: transform .3s ease, box-shadow .3s ease;
}
.mini-card:hover { transform: translateY(-4px); box-shadow: 0 12px 28px rgba(176,58,58,.18); }
.mini-card .icon { font-size: 30px; }
.mini-card h3 { font-size: 19px; color: #8e2c2c; letter-spacing: 3px; margin: 10px 0 8px; }
.mini-card p { font-size: 13px; color: #7f9a99; line-height: 1.7; }

.btn-logout {
  height: 44px; padding: 0 36px;
  font-family: inherit; font-size: 16px; letter-spacing: 4px;
  color: #8e2c2c; background: transparent;
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .3s ease, transform .15s ease;
}
.btn-logout:hover { box-shadow: 0 0 14px rgba(176,58,58,.25); }
.btn-logout:active { transform: scale(.95); }
</style>
