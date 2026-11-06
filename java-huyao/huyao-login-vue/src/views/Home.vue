<script setup>
/**
 * Home.vue —— 狐缘阁首页（登录成功后进入）
 * 风格与登录页一致：水墨晕染背景 + 桃花花瓣 + 姻缘红线 + 苦情树 + 油纸伞 + 云纹卡片
 * 单页双栏目：顶部导航栏「首页 / 求签」，点击切换内容，不新增路由
 *   首页栏目：签文搜索栏 + 前往求签的引导入口
 *   求签栏目：签筒摇签 + 签文结果（原首页主体里的求签模块已移到这里）
 * 说明：原首页三张卡片（续缘簿 / 红线谱 / 桃花酿）已删除
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

/* ---------- 顶部导航：当前栏目 ---------- */
const activeTab = ref('home')   // 'home' 首页 | 'fortune' 求签
function switchTab(tab) {
  activeTab.value = tab
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/* ---------- 飘落花瓣数据 ---------- */
const petals = ref([])
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

/* ---------- 签文库：求签与搜索共用 ---------- */
const signs = [
  { level: '上上签', poem: '红线牵来前世约，桃花开处故人归', note: '所盼之事水到渠成，宜主动相寻，莫负良辰。', tag: '姻缘 重逢 吉' },
  { level: '上上签', poem: '月老笑点鸳鸯谱，一纸盟约两心知', note: '心意相通，所求皆顺，可放心前行。', tag: '姻缘 顺遂 吉' },
  { level: '上签', poem: '清风拂面旧梦回，一纸相思有处寄', note: '旧缘未断，静待佳音，不必急于追问。', tag: '旧缘 等待 吉' },
  { level: '上签', poem: '苦情树下花又发，前尘往事皆可期', note: '久候之事渐有转机，再坚持一时。', tag: '转机 守候 吉' },
  { level: '上签', poem: '签筒一摇缘分到，千里相逢不负卿', note: '有意料之外的相逢，敞开心扉即可。', tag: '相逢 惊喜 吉' },
  { level: '中签', poem: '云遮月色路漫漫，且把心事付流年', note: '时机未至，宜守不宜进，先安己心。', tag: '等待 平稳 平' },
  { level: '中签', poem: '一程山水一程缘，莫问归期问此心', note: '答案不在远方，先看清自己所愿。', tag: '自省 平静 平' },
  { level: '中签', poem: '桃花随水去无意，缘来缘去自有时', note: '顺其自然，勿强留，亦勿轻弃。', tag: '随缘 平常 平' },
  { level: '下签', poem: '落花有意水无情，莫向旧梦问归期', note: '强求无益，宜暂且放下，休养生息。', tag: '放下 阻滞 凶' },
  { level: '下签', poem: '雾锁重楼人不见，孤灯一盏夜更长', note: '眼下诸事不明，不宜妄动，静候天明。', tag: '阻滞 静守 凶' },
]

/* ---------- 求签 ---------- */
const current = ref(null)
const drawing = ref(false)
function drawSign() {
  if (drawing.value) return
  drawing.value = true
  current.value = null
  setTimeout(() => {
    current.value = signs[Math.floor(Math.random() * signs.length)]
    drawing.value = false
  }, 700)
}

/* ---------- 搜索栏 ---------- */
const keyword = ref('')
const searched = ref(false)
const results = computed(() => {
  const k = keyword.value.trim()
  if (!k) return []
  return signs.filter((s) => (s.level + s.poem + s.note + s.tag).includes(k))
})
function doSearch() {
  if (!keyword.value.trim()) {
    searched.value = false
    return
  }
  searched.value = true
}
function clearSearch() {
  keyword.value = ''
  searched.value = false
}

/* ---------- 退出 ---------- */
function handleLogout() {
  localStorage.removeItem('huyao_user')
  router.push('/login')
}

onMounted(() => { petals.value = makePetals() })
</script>

<template>
  <div class="home-page">
    <!-- 飘落桃花花瓣 -->
    <div class="petal-layer">
      <span
        v-for="(p, i) in petals" :key="i" class="petal"
        :style="{ left: p.left + 'vw', width: p.size + 'px', height: p.size * 0.8 + 'px',
                  opacity: p.opacity, animationDuration: p.duration + 's', animationDelay: p.delay + 's' }"
      />
    </div>

    <!-- 角落姻缘红线：左上 -->
    <svg class="thread thread-tl" width="260" height="200" viewBox="0 0 260 200">
      <path d="M-10 -10 C 60 30, 20 90, 90 110 S 190 150, 150 200"
            fill="none" stroke="#b03a3a" stroke-width="2.5" stroke-linecap="round" opacity="0.7"/>
      <path d="M-10 10 C 80 50, 40 110, 110 130 S 200 170, 170 200"
            fill="none" stroke="#c9584f" stroke-width="1.5" opacity="0.55"/>
      <circle cx="90" cy="110" r="5" fill="none" stroke="#b03a3a" stroke-width="2" opacity="0.8"/>
    </svg>
    <!-- 角落姻缘红线：右下 -->
    <svg class="thread thread-br" width="240" height="180" viewBox="0 0 240 180">
      <path d="M250 190 C 180 150, 220 90, 150 70 S 60 30, 100 -10"
            fill="none" stroke="#b03a3a" stroke-width="2.5" stroke-linecap="round" opacity="0.7"/>
      <path d="M250 170 C 170 130, 210 70, 140 50 S 70 10, 110 -10"
            fill="none" stroke="#c9584f" stroke-width="1.5" opacity="0.55"/>
      <circle cx="150" cy="70" r="5" fill="none" stroke="#b03a3a" stroke-width="2" opacity="0.8"/>
    </svg>

    <!-- 左下苦情树剪影 -->
    <svg class="tree" width="380" height="300" viewBox="0 0 380 300">
      <path d="M60 300 L70 180 Q72 150 60 120 Q90 140 100 110 Q120 150 140 100 Q150 160 170 120 Q190 170 200 130 Q215 190 230 300 Z"
            fill="#5a4a3c" opacity="0.35"/>
      <circle cx="130" cy="90" r="46" fill="#b03a3a" opacity="0.22"/>
      <circle cx="180" cy="70" r="34" fill="#b03a3a" opacity="0.18"/>
      <circle cx="95" cy="110" r="28" fill="#c9584f" opacity="0.16"/>
    </svg>

    <!-- 右上油纸伞 -->
    <svg class="umbrella" width="130" height="110" viewBox="0 0 130 110">
      <path d="M65 8 Q118 30 125 62 Q95 50 65 62 Q35 50 5 62 Q12 30 65 8 Z" fill="#c95b52" opacity="0.75"/>
      <path d="M65 8 Q90 30 95 60 M65 8 Q40 30 35 60 M65 8 V62" stroke="#f5efe3" stroke-width="1.4" fill="none" opacity="0.8"/>
      <line x1="65" y1="62" x2="65" y2="100" stroke="#5a4a3c" stroke-width="2.5"/>
      <path d="M60 100 Q65 106 70 100" stroke="#5a4a3c" stroke-width="2.5" fill="none"/>
    </svg>

    <!-- ============ 顶部导航栏（仅"首页"） ============ -->
    <header class="navbar">
      <div class="nav-inner">
        <div class="brand">
          <span class="brand-name">狐缘阁</span>
          <span class="brand-sub">一纸相思 · 续前世之缘</span>
        </div>
        <nav class="nav-links">
          <a class="nav-link active" @click="goHome">首 页</a>
        </nav>
        <div class="nav-seal" title="狐缘阁">缘</div>
      </div>
    </header>

    <!-- ============ 主内容 ============ -->
    <main class="content">
      <h1 class="title">缘 起 再 续</h1>
      <p class="subtitle">一签知前路 · 一缘定此生</p>
      <div class="divider"><span>❀</span></div>

      <!-- 搜索栏 -->
      <section class="search-bar">
        <span class="search-mark">寻</span>
        <input v-model="keyword" type="text" placeholder="输入心愿或签文关键字，寻一支签"
               @keyup.enter="doSearch" />
        <button v-if="keyword" class="btn-clear" title="清空" @click="clearSearch">×</button>
        <button class="btn-search" @click="doSearch">寻 签</button>
      </section>
      <p class="search-hint">可搜签文、解签或吉凶，如「姻缘」「上上签」「等待」</p>

      <!-- 搜索结果 -->
      <section v-if="searched && keyword.trim()" class="search-result">
        <template v-if="results.length">
          <p class="result-head">寻得 {{ results.length }} 支相关签文</p>
          <div class="result-list">
            <div v-for="(s, i) in results" :key="i" class="result-item">
              <span class="result-level">{{ s.level }}</span>
              <div>
                <p class="result-poem">{{ s.poem }}</p>
                <p class="result-note">{{ s.note }}</p>
              </div>
            </div>
          </div>
        </template>
        <p v-else class="result-empty">未寻得相关签文，不妨先诚心求一支签～</p>
      </section>

      <!-- 求签卡片（云纹古风边框，替换原三张卡片） -->
      <section class="card qiuqian-card">
        <span class="corner tl"/><span class="corner tr"/>
        <span class="corner br"/><span class="corner bl"/>

        <h2 class="card-title">求 签</h2>
        <p class="card-sub">静心一念 · 摇签问前尘</p>
        <div class="divider small"><span>❀</span></div>

        <transition name="sign" mode="out-in">
          <!-- 签筒 / 摇签中 -->
          <div v-if="!current" key="tube" class="tube-area">
            <div class="tube" :class="{ shaking: drawing }">
              <svg width="130" height="150" viewBox="0 0 130 150" aria-hidden="true">
                <g stroke="#b03a3a" stroke-width="3" stroke-linecap="round" opacity="0.85">
                  <line x1="44" y1="12" x2="52" y2="78" />
                  <line x1="58" y1="5" x2="60" y2="78" />
                  <line x1="72" y1="6" x2="68" y2="78" />
                  <line x1="86" y1="13" x2="78" y2="78" />
                </g>
                <g fill="#f7f2e7" stroke="#b8924a" stroke-width="1">
                  <rect x="40" y="8" width="8" height="13" rx="2" />
                  <rect x="54" y="2" width="8" height="13" rx="2" />
                  <rect x="68" y="3" width="8" height="13" rx="2" />
                  <rect x="82" y="9" width="8" height="13" rx="2" />
                </g>
                <rect x="30" y="72" width="70" height="12" rx="4" fill="#8e2c2c" stroke="#b8924a" stroke-width="1.5" />
                <path d="M34 84 L40 140 Q65 148 90 140 L96 84 Z" fill="url(#tubeGrad)" stroke="#b8924a" stroke-width="2" />
                <defs>
                  <linearGradient id="tubeGrad" x1="0" y1="0" x2="1" y2="0">
                    <stop offset="0" stop-color="#a8403a" />
                    <stop offset="0.5" stop-color="#c25b50" />
                    <stop offset="1" stop-color="#8e2c2c" />
                  </linearGradient>
                </defs>
                <text x="65" y="120" text-anchor="middle" font-size="20" fill="#f7f2e7" font-family="serif">缘</text>
              </svg>
            </div>
            <p class="tube-tip">{{ drawing ? '签筒轻摇，缘分将至…' : '心中默念所问之事，再按下求签' }}</p>
          </div>

          <!-- 签文结果 -->
          <div v-else key="result" class="sign-result">
            <span class="sign-level">{{ current.level }}</span>
            <p class="sign-poem">{{ current.poem }}</p>
            <p class="sign-note">解曰：{{ current.note }}</p>
          </div>
        </transition>

        <button class="btn-draw" :disabled="drawing" @click="drawSign">
          {{ drawing ? '摇 签 中…' : (current ? '再 求 一 签' : '诚 心 求 签') }}
        </button>
      </section>

      <button class="btn-logout" @click="handleLogout">忘却此缘 · 退出</button>
    </main>
  </div>
</template>

<style scoped>
/* ============ 古风基调（与登录页一致） ============ */
.home-page {
  min-height: 100vh; position: relative;
  font-family: "STXingkai", "华文行楷", "STKaiti", "楷体", "KaiTi", serif;
  color: #4a3b32;
  background:
    radial-gradient(ellipse 60% 45% at 18% 22%, rgba(127,154,153,.22), transparent 70%),
    radial-gradient(ellipse 50% 40% at 82% 75%, rgba(176,58,58,.14), transparent 70%),
    radial-gradient(ellipse 45% 35% at 70% 15%, rgba(184,146,74,.12), transparent 70%),
    linear-gradient(160deg, #f7f2e7 0%, #efe6d4 45%, #e8dcc8 100%);
}

/* ============ 花瓣 ============ */
.petal-layer { position: fixed; inset: 0; overflow: hidden; pointer-events: none; z-index: 1; }
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

/* ============ 姻缘红线 / 苦情树 / 油纸伞 ============ */
.thread { position: fixed; z-index: 2; pointer-events: none; animation: sway 7s ease-in-out infinite; }
.thread-tl { left: 0; top: 0; }
.thread-br { right: 0; bottom: 0; animation-duration: 9s; animation-direction: reverse; }
@keyframes sway {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-6px) rotate(1.2deg); }
}
.tree { position: fixed; left: -40px; bottom: -30px; z-index: 1; pointer-events: none; opacity: .5; }
.umbrella { position: fixed; right: 6%; top: 16%; z-index: 1; pointer-events: none; opacity: .55; animation: drift 10s ease-in-out infinite; }
@keyframes drift {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50%      { transform: translateY(-12px) rotate(-1deg); }
}

/* ============ 顶部导航栏（仅"首页"） ============ */
.navbar {
  position: sticky; top: 0; z-index: 50;
  background: rgba(250, 245, 234, .86); backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(184,146,74,.45);
  box-shadow: 0 2px 14px rgba(74,59,50,.08);
}
.nav-inner {
  max-width: 1080px; margin: 0 auto; height: 74px; padding: 0 28px;
  display: flex; align-items: center; gap: 36px;
}
.brand { display: flex; flex-direction: column; gap: 2px; flex: none; }
.brand-name { font-size: 24px; letter-spacing: 6px; color: #8e2c2c; }
.brand-sub { font-size: 11px; letter-spacing: 2px; color: #7f9a99; }
.nav-links { display: flex; align-items: center; flex: 1; }
.nav-link {
  position: relative; display: inline-block; padding: 8px 20px;
  font-size: 17px; letter-spacing: 4px; color: #7f9a99; cursor: pointer;
  transition: color .25s ease;
}
.nav-link:hover { color: #b03a3a; }
.nav-link.active { color: #8e2c2c; }
.nav-link.active::after {
  content: ""; position: absolute; left: 50%; bottom: -3px; transform: translateX(-50%);
  width: 30px; height: 2px; background: #b03a3a; border-radius: 2px;
}
.nav-seal {
  width: 34px; height: 34px; flex: none;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #f5efe3; background: #b03a3a;
  border-radius: 6px; box-shadow: 0 2px 8px rgba(176,58,58,.3);
}

/* ============ 主内容 ============ */
.content {
  position: relative; z-index: 10;
  max-width: 760px; margin: 0 auto; padding: 44px 24px 64px; text-align: center;
}
.title { font-size: 40px; letter-spacing: 12px; color: #8e2c2c; }
.subtitle { margin-top: 8px; font-size: 15px; letter-spacing: 4px; color: #7f9a99; }
.divider { display: flex; align-items: center; gap: 12px; width: 260px; margin: 22px auto 32px; }
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: linear-gradient(90deg, transparent, #b8924a, transparent); }
.divider span { color: #b03a3a; font-size: 15px; }

/* ============ 搜索栏 ============ */
.search-bar {
  display: flex; align-items: center; gap: 10px;
  max-width: 560px; margin: 0 auto; padding: 8px 8px 8px 18px;
  background: rgba(250, 245, 234, .9);
  border: 1.5px solid rgba(184,146,74,.65); border-radius: 14px;
  box-shadow: 0 6px 20px rgba(74,59,50,.12);
  transition: box-shadow .35s ease, border-color .35s ease;
}
.search-bar:focus-within {
  border-color: #b03a3a;
  box-shadow: 0 0 0 3px rgba(176,58,58,.12), 0 8px 24px rgba(176,58,58,.18);
}
.search-mark { flex: none; font-size: 17px; color: #b8924a; }
.search-bar input {
  flex: 1; min-width: 0; height: 42px; border: none; outline: none; background: transparent;
  font-family: inherit; font-size: 16px; letter-spacing: 1px; color: #4a3b32;
}
.search-bar input::placeholder { color: rgba(127,154,153,.8); }
.btn-clear {
  flex: none; width: 26px; height: 26px; line-height: 1;
  font-family: inherit; font-size: 18px; color: #7f9a99;
  background: transparent; border: none; cursor: pointer;
}
.btn-clear:hover { color: #b03a3a; }
.btn-search {
  flex: none; height: 42px; padding: 0 26px;
  font-family: inherit; font-size: 16px; letter-spacing: 4px; text-indent: 4px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .35s ease, transform .15s ease, filter .35s ease;
}
.btn-search:hover { box-shadow: 0 0 14px rgba(176,58,58,.4); filter: brightness(1.06); }
.btn-search:active { transform: scale(.96); }
.search-hint { margin-top: 12px; font-size: 12px; letter-spacing: 2px; color: rgba(127,154,153,.95); }

/* ============ 搜索结果 ============ */
.search-result { max-width: 560px; margin: 22px auto 0; text-align: left; }
.result-head { margin-bottom: 10px; font-size: 13px; letter-spacing: 2px; color: #8e2c2c; }
.result-list { display: flex; flex-direction: column; gap: 10px; max-height: 250px; overflow-y: auto; }
.result-item {
  display: flex; gap: 14px; align-items: flex-start; padding: 12px 16px;
  background: rgba(250, 245, 234, .8);
  border: 1px solid rgba(184,146,74,.45); border-radius: 10px;
}
.result-level {
  flex: none; padding: 3px 9px; font-size: 12px; letter-spacing: 1px;
  color: #f5efe3; background: #b03a3a; border-radius: 6px;
}
.result-poem { font-size: 15px; letter-spacing: 1px; color: #8e2c2c; }
.result-note { margin-top: 4px; font-size: 12px; line-height: 1.6; color: #7f9a99; }
.result-empty { padding: 14px; text-align: center; font-size: 14px; letter-spacing: 2px; color: #7f9a99; }

/* ============ 求签卡片（云纹古风边框，同登录页） ============ */
.qiuqian-card {
  position: relative; max-width: 520px; margin: 34px auto 0; padding: 32px 40px 36px;
  background: rgba(250, 245, 234, .82); backdrop-filter: blur(6px);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 12px 40px rgba(74,59,50,.18),
              inset 0 0 60px rgba(245, 230, 210, .6);
}
.qiuqian-card::before {
  content: ""; position: absolute; inset: -9px;
  border: 1px dashed rgba(184,146,74,.55); border-radius: 24px; pointer-events: none;
}
.corner {
  position: absolute; width: 46px; height: 46px; pointer-events: none;
  background:
    radial-gradient(circle at 30% 70%, transparent 6px, #b8924a 7px, #b8924a 9px, transparent 10px),
    radial-gradient(circle at 55% 45%, transparent 4px, #b8924a 5px, #b8924a 7px, transparent 8px);
}
.corner.tl { top: -12px; left: -12px; }
.corner.tr { top: -12px; right: -12px; transform: rotate(90deg); }
.corner.br { bottom: -12px; right: -12px; transform: rotate(180deg); }
.corner.bl { bottom: -12px; left: -12px; transform: rotate(270deg); }

.card-title { text-align: center; font-size: 28px; letter-spacing: 10px; text-indent: 10px; color: #8e2c2c; }
.card-sub { margin-top: 6px; text-align: center; font-size: 13px; letter-spacing: 3px; color: #7f9a99; }
.divider.small { width: 60%; margin: 18px auto 24px; }

/* 签筒 / 结果 */
.tube-area, .sign-result { min-height: 176px; }
.tube-area { display: flex; flex-direction: column; justify-content: center; }
.tube { display: flex; justify-content: center; }
.tube svg { transform-origin: 50% 82%; }
.tube.shaking svg { animation: shake .35s ease-in-out infinite; }
@keyframes shake {
  0%, 100% { transform: rotate(-6deg); }
  50%      { transform: rotate(6deg); }
}
.tube-tip { margin-top: 10px; font-size: 13px; letter-spacing: 2px; color: #7f9a99; }
.sign-result { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; }
.sign-level {
  padding: 5px 18px; font-size: 15px; letter-spacing: 4px; text-indent: 4px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 8px;
}
.sign-poem { font-size: 21px; letter-spacing: 3px; line-height: 1.8; color: #8e2c2c; }
.sign-note { font-size: 14px; letter-spacing: 1px; line-height: 1.8; color: #7f9a99; }
.sign-enter-active, .sign-leave-active { transition: opacity .35s ease, transform .35s ease; }
.sign-enter-from { opacity: 0; transform: translateY(12px); }
.sign-leave-to { opacity: 0; transform: translateY(-12px); }

/* 求签按钮 */
.btn-draw {
  width: 100%; height: 50px; margin-top: 4px;
  font-family: inherit; font-size: 19px; letter-spacing: 8px; text-indent: 8px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 12px; cursor: pointer;
  transition: box-shadow .35s ease, transform .15s ease, filter .35s ease;
}
.btn-draw:hover:not(:disabled) { box-shadow: 0 0 18px rgba(176,58,58,.4), 0 6px 18px rgba(142,44,44,.3); filter: brightness(1.06); }
.btn-draw:active:not(:disabled) { transform: scale(.96); }
.btn-draw:disabled { opacity: .65; cursor: wait; }

/* 退出 */
.btn-logout {
  height: 44px; margin-top: 28px; padding: 0 36px;
  font-family: inherit; font-size: 16px; letter-spacing: 4px;
  color: #8e2c2c; background: transparent;
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .3s ease, transform .15s ease;
}
.btn-logout:hover { box-shadow: 0 0 14px rgba(176,58,58,.25); }
.btn-logout:active { transform: scale(.95); }

/* ============ 窄屏适配 ============ */
@media (max-width: 640px) {
  .nav-inner { height: 64px; padding: 0 16px; gap: 16px; }
  .brand-name { font-size: 20px; letter-spacing: 4px; }
  .brand-sub { display: none; }
  .nav-link { padding: 8px 12px; font-size: 16px; }
  .content { padding: 30px 16px 48px; }
  .title { font-size: 30px; letter-spacing: 8px; }
  .search-bar { padding-left: 12px; }
  .btn-search { padding: 0 16px; letter-spacing: 2px; text-indent: 2px; }
  .qiuqian-card { padding: 28px 20px 30px; }
  .sign-poem { font-size: 18px; letter-spacing: 2px; }
  .thread-tl, .umbrella { opacity: .35; }
}
</style>
