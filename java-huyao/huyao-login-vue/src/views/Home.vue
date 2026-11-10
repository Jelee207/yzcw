<script setup>
/**
 * Home.vue —— 狐缘阁主页（登录成功后进入）
 * 风格与登录页一致：水墨晕染背景 + 桃花花瓣 + 姻缘红线 + 苦情树 + 油纸伞 + 云纹卡片
 * 顶部导航栏「首页 / 求签」切换栏目（单页切换，不新增路由）：
 *   首页：签文搜索 + 前往求签的引导入口
 *   求签：先择签种（姻缘签 / 福运签 / 前程签），签筒只从该签种签库中抽取
 * 右上角「缘」章悬浮展开个人菜单：个人空间 / 退出（栏目切换，不新增路由）
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { saveAvatar, removeAvatar } from '../api/user'

const router = useRouter()

/* ---------- 顶部导航：当前栏目 ---------- */
const activeTab = ref('home')   // 'home' 首页 | 'fortune' 求签 | 'profile' 个人空间
function switchTab(tab) {
  activeTab.value = tab
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
const pageTitle = computed(() => (
  activeTab.value === 'home' ? '缘 起 再 续' :
  activeTab.value === 'fortune' ? '拈 签 问 缘' : '个 人 空 间'
))
const pageSub = computed(() => (
  activeTab.value === 'home' ? '一签知前路 · 一缘定此生' :
  activeTab.value === 'fortune' ? '一念起处 · 且听签语' : '一纸名帖 · 记此间前缘'
))

/* ---------- 签种：选定后只从该签种的签库抽取 ---------- */
const signTypes = [
  { key: '姻缘签', icon: '缘', desc: '问情缘 · 觅故人' },
  { key: '福运签', icon: '福', desc: '问家宅 · 盼安康' },
  { key: '前程签', icon: '程', desc: '问学业 · 卜前程' },
]

/* ---------- 签文库：按签种分组，求签与搜索共用 ---------- */
const signs = [
  // 姻缘签 · 问情缘
  { type: '姻缘签', level: '上上签', poem: '红线牵来前世约，桃花开处故人归', note: '所盼之事水到渠成，宜主动相寻，莫负良辰。', tag: '姻缘 重逢 吉' },
  { type: '姻缘签', level: '上上签', poem: '月老笑点鸳鸯谱，一纸盟约两心知', note: '心意相通，所求皆顺，可放心前行。', tag: '姻缘 顺遂 吉' },
  { type: '姻缘签', level: '上签', poem: '清风拂面旧梦回，一纸相思有处寄', note: '旧缘未断，静待佳音，不必急于追问。', tag: '姻缘 旧缘 等待 吉' },
  { type: '姻缘签', level: '上签', poem: '苦情树下花又发，前尘往事皆可期', note: '久候之事渐有转机，再坚持一时。', tag: '姻缘 转机 守候 吉' },
  { type: '姻缘签', level: '中签', poem: '云遮月色路漫漫，且把心事付流年', note: '时机未至，宜守不宜进，先安己心。', tag: '姻缘 等待 平稳 平' },
  { type: '姻缘签', level: '下签', poem: '落花有意水无情，莫向旧梦问归期', note: '强求无益，宜暂且放下，休养生息。', tag: '姻缘 放下 阻滞 凶' },

  // 福运签 · 问家宅安康
  { type: '福运签', level: '上上签', poem: '福星高照满庭芳，一门和顺四时安', note: '家宅安宁，福气自来，宜广结善缘。', tag: '福运 家宅 平安 吉' },
  { type: '福运签', level: '上上签', poem: '春风送暖入柴门，仓廪丰盈岁月长', note: '财禄渐丰，所求有得，宜惜福积善。', tag: '福运 财禄 丰盈 吉' },
  { type: '福运签', level: '上签', poem: '灵鹊枝头报好音，旧疾新忧皆散尽', note: '身心渐安，烦忧自去，宜放宽心怀。', tag: '福运 安康 顺遂 吉' },
  { type: '福运签', level: '上签', poem: '一盏清茶待客来，贵人暗助事竟成', note: '有贵人相助，凡事不必独自承担。', tag: '福运 贵人 相助 吉' },
  { type: '福运签', level: '中签', poem: '云淡风轻寻常日，无灾无难即是福', note: '平平淡淡最是真，莫贪非分之喜。', tag: '福运 平常 守成 平' },
  { type: '福运签', level: '下签', poem: '雾锁重楼人不见，孤灯一盏夜更长', note: '眼下诸事不明，宜静守，勿轻动财物。', tag: '福运 阻滞 静守 凶' },

  // 前程签 · 问学业事业
  { type: '前程签', level: '上上签', poem: '长风破浪会有时，直挂云帆济沧海', note: '前程开阔，宜放手一搏，功名可期。', tag: '前程 功名 远行 吉' },
  { type: '前程签', level: '上上签', poem: '十年磨剑今朝试，一举成名天下知', note: '平日积累已足，此番应试必有回响。', tag: '前程 学业 应试 吉' },
  { type: '前程签', level: '上签', poem: '山重水复疑无路，柳暗花明又一村', note: '眼前困顿将解，转机在坚持之后。', tag: '前程 转机 事业 吉' },
  { type: '前程签', level: '上签', poem: '鹏鸟振翅九万里，前路迢迢任君行', note: '宜远行、宜求变，新局胜于旧局。', tag: '前程 远行 变动 吉' },
  { type: '前程签', level: '中签', poem: '一程山水一程缘，莫问归期问此心', note: '答案不在远方，先看清自己所愿，再定去留。', tag: '前程 自省 抉择 平' },
  { type: '前程签', level: '下签', poem: '逆水行舟风正急，且收帆楫待潮平', note: '时机未至，不宜妄进，宜蓄力修己。', tag: '前程 阻滞 蓄力 凶' },
]

/* ---------- 求签：先择签种，只从该签种签库抽取 ---------- */
const selectedType = ref('')
const current = ref(null)
const drawing = ref(false)
const pool = computed(() => signs.filter((s) => s.type === selectedType.value))
const tubeChar = computed(() => signTypes.find((t) => t.key === selectedType.value)?.icon || '缘')

function selectType(key) {
  if (drawing.value) return          // 摇签过程中不允许换签种
  selectedType.value = key
  current.value = null               // 换签种即清空上一签，避免串签
}
function drawSign() {
  if (drawing.value || !selectedType.value) return
  drawing.value = true
  current.value = null
  const list = pool.value            // 抽签当下锁定所选签种的签池
  setTimeout(() => {
    current.value = list[Math.floor(Math.random() * list.length)]
    drawing.value = false
  }, 700)
}

/* ---------- 搜索栏 ---------- */
const keyword = ref('')
const searched = ref(false)
const results = computed(() => {
  const k = keyword.value.trim()
  if (!k) return []
  return signs.filter((s) => (s.type + s.level + s.poem + s.note + s.tag).includes(k))
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

/* ---------- 个人空间：读取登录时存档的用户信息 ---------- */
const user = ref(null)
try { user.value = JSON.parse(localStorage.getItem('huyao_user') || 'null') } catch (e) { user.value = null }
const displayName = computed(() => user.value?.username || '有缘人')
const userRace = computed(() => user.value?.race || '未记')
const userGender = computed(() => user.value?.gender || '未记')
const joinDate = computed(() => {
  const raw = user.value?.createTime
  if (!raw) return '未记'
  const d = new Date(raw)
  if (isNaN(d.getTime())) return String(raw).slice(0, 10)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

/* ---------- 个人空间：头像上传（裁剪压缩后存入数据库） ---------- */
const avatarUrl = ref(user.value?.avatar || '')
const avatarInput = ref(null)
const avatarBusy = ref(false)
const avatarTip = ref('')

/** 同步更新内存与本地存档，保证退出重进也能立即显示 */
function patchUser(patch) {
  const next = { ...(user.value || {}), ...patch }
  user.value = next
  localStorage.setItem('huyao_user', JSON.stringify(next))
}

/** 进入页面时向后端取一次最新头像；后端未启动时静默保留本地存档 */
async function syncAvatar() {
  const name = user.value?.username
  if (!name) return
  try {
    const r = await fetch('/api/avatar?username=' + encodeURIComponent(name))
    const j = await r.json()
    if (j.code === 200) {
      avatarUrl.value = j.data?.avatar || ''
      patchUser({ avatar: avatarUrl.value })
    }
  } catch (e) { /* 后端不可用时保留登录时缓存的头像 */ }
}

function openAvatarPicker() {
  if (avatarBusy.value) return
  avatarInput.value?.click()
}

/** 居中裁成正方形并压到 256x256 JPEG，避免大图撑爆请求与数据库 */
function compressImage(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(new Error('读取图片失败'))
    reader.onload = () => {
      const img = new Image()
      img.onerror = () => reject(new Error('图片解析失败'))
      img.onload = () => {
        const size = 256
        const canvas = document.createElement('canvas')
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')
        const side = Math.min(img.width, img.height)
        ctx.fillStyle = '#f5efe3'   // 透明 PNG 补一层宣纸底色
        ctx.fillRect(0, 0, size, size)
        ctx.drawImage(img, (img.width - side) / 2, (img.height - side) / 2,
                      side, side, 0, 0, size, size)
        resolve(canvas.toDataURL('image/jpeg', 0.85))
      }
      img.src = reader.result
    }
    reader.readAsDataURL(file)
  })
}

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''      // 允许连续选择同一张图片
  if (!file) return
  if (!file.type.startsWith('image/')) { avatarTip.value = '请选择图片文件～'; return }
  if (file.size > 8 * 1024 * 1024) { avatarTip.value = '图片请勿超过 8MB～'; return }
  if (!user.value?.username) { avatarTip.value = '请先登录后再设置头像～'; return }
  avatarBusy.value = true
  avatarTip.value = '正在描摹容颜…'
  try {
    const image = await compressImage(file)
    const res = await saveAvatar({ username: user.value.username, image })
    avatarUrl.value = res.avatar
    patchUser({ avatar: res.avatar })
    avatarTip.value = '头像已入名帖 ✦'
  } catch (err) {
    avatarTip.value = '头像未能上传，请稍后再试～'
  } finally {
    avatarBusy.value = false
  }
}

async function clearAvatar() {
  if (!user.value?.username || avatarBusy.value) return
  avatarBusy.value = true
  try {
    await removeAvatar(user.value.username)
    avatarUrl.value = ''
    patchUser({ avatar: '' })
    avatarTip.value = '已恢复默认名帖～'
  } catch (err) {
    avatarTip.value = '移除失败，请稍后再试～'
  } finally {
    avatarBusy.value = false
  }
}

/* ---------- 右上角「缘」章：悬浮展开菜单 ---------- */
const menuOpen = ref(false)
function openMenu() { menuOpen.value = true }
function closeMenu() { menuOpen.value = false }
function onDocClick(e) {
  if (e.target instanceof Element && !e.target.closest('.user-menu')) closeMenu()
}

/* ---------- 退出 ---------- */
function handleLogout() {
  localStorage.removeItem('huyao_user')
  router.push('/login')
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  syncAvatar()
})
onUnmounted(() => { document.removeEventListener('click', onDocClick) })
</script>

<template>
  <div class="home-page">
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

    <!-- ============ 顶部导航栏：首页 / 求签 ============ -->
    <header class="navbar">
      <div class="nav-inner">
        <nav class="nav-links">
          <a class="nav-link" :class="{ active: activeTab === 'home' }"
             @click="switchTab('home')">首 页</a>
          <a class="nav-link" :class="{ active: activeTab === 'fortune' }"
             @click="switchTab('fortune')">求 签</a>
        </nav>
      </div>

      <!-- 阁名与题句：与「缘」章同列视口最右 -->
      <div class="brand">
        <span class="brand-name">狐缘阁</span>
        <span class="brand-sub">一纸相思 · 续前世之缘</span>
      </div>

      <!-- 右上角个人菜单：贴视口最右，光标悬浮「缘」字展开 -->
      <div class="user-menu" :class="{ open: menuOpen }"
           @mouseenter="openMenu" @mouseleave="closeMenu">
        <button class="nav-seal" :class="{ 'has-avatar': avatarUrl }" type="button"
                :title="avatarUrl ? '我的头像 · 悬浮展开个人菜单' : '缘 · 悬浮展开个人菜单'"
                aria-label="个人菜单" :aria-expanded="menuOpen"
                @click="openMenu">
          <img v-if="avatarUrl" :src="avatarUrl" alt="我的头像" />
          <template v-else>缘</template>
        </button>
        <transition name="menu">
          <div v-if="menuOpen" class="user-dropdown" @click="closeMenu">
            <div class="dropdown-head">
              <img v-if="avatarUrl" class="dropdown-avatar" :src="avatarUrl" alt="" />
              <span v-else class="dropdown-avatar fallback">缘</span>
              <span class="dropdown-head-text">
                {{ displayName }}
                <small>狐缘阁 · 有缘人</small>
              </span>
            </div>
            <button class="dropdown-item" type="button" @click="switchTab('profile')">个 人 空 间</button>
            <button class="dropdown-item danger" type="button" @click="handleLogout">退 出</button>
          </div>
        </transition>
      </div>
    </header>

    <!-- ============ 主内容 ============ -->
    <main class="content">
      <h1 class="title">{{ pageTitle }}</h1>
      <p class="subtitle">{{ pageSub }}</p>
      <div class="divider"><span>❀</span></div>

      <!-- ============ 首页栏目：签文搜索 + 求签引导 ============ -->
      <!-- 搜索栏 -->
      <section v-if="activeTab === 'home'" class="search-bar">
        <span class="search-mark">寻</span>
        <input v-model="keyword" type="text" placeholder="输入心愿或签文关键字，寻一支签"
               @keyup.enter="doSearch" />
        <button v-if="keyword" class="btn-clear" title="清空" @click="clearSearch">×</button>
        <button class="btn-search" @click="doSearch">寻 签</button>
      </section>
      <p v-if="activeTab === 'home'" class="search-hint">可搜签种、签文、解签或吉凶，如「姻缘签」「上上签」「等待」</p>

      <!-- 搜索结果 -->
      <section v-if="activeTab === 'home' && searched && keyword.trim()" class="search-result">
        <template v-if="results.length">
          <p class="result-head">寻得 {{ results.length }} 支相关签文</p>
          <div class="result-list">
            <div v-for="(s, i) in results" :key="i" class="result-item">
              <div class="result-badges">
                <span class="result-type">{{ s.type }}</span>
                <span class="result-level">{{ s.level }}</span>
              </div>
              <div>
                <p class="result-poem">{{ s.poem }}</p>
                <p class="result-note">{{ s.note }}</p>
              </div>
            </div>
          </div>
        </template>
        <p v-else class="result-empty">
          未寻得相关签文，不妨拈一签问问缘分～
          <button class="link-guide" @click="switchTab('fortune')">前往求签 →</button>
        </p>
      </section>

      <!-- 首页栏目：前往求签的引导入口 -->
      <section v-if="activeTab === 'home'" class="guide-card">
        <span class="guide-mark">签</span>
        <p class="guide-title">心中有所问，何不摇一支签？</p>
        <p class="guide-text">静心默念所问之事，于签筒中求一支签，问一问前尘来路。</p>
        <button class="btn-guide" @click="switchTab('fortune')">前 往 求 签 <span>→</span></button>
      </section>

      <!-- ============ 求签栏目：签筒摇签 + 签文结果（由首页迁入） ============ -->
      <section v-if="activeTab === 'fortune'" class="card qiuqian-card">
        <span class="corner tl"/><span class="corner tr"/>
        <span class="corner br"/><span class="corner bl"/>

        <h2 class="card-title">求 签</h2>
        <p class="card-sub">择一类签 · 摇签问前尘</p>
        <div class="divider small"><span>❀</span></div>

        <!-- 签种选择：选定后签筒只从该签种签库抽取 -->
        <section class="type-picker">
          <p class="type-hint">先择一类签，所抽之签皆出于此类</p>
          <div class="type-list">
            <button v-for="t in signTypes" :key="t.key" type="button"
                    class="type-card" :class="{ chosen: selectedType === t.key }"
                    @click="selectType(t.key)">
              <span class="type-icon">{{ t.icon }}</span>
              <span class="type-name">{{ t.key }}</span>
              <span class="type-desc">{{ t.desc }}</span>
            </button>
          </div>
        </section>

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
                <text x="65" y="120" text-anchor="middle" font-size="20" fill="#f7f2e7" font-family="serif">{{ tubeChar }}</text>
              </svg>
            </div>
            <p class="tube-tip">{{ drawing ? '签筒轻摇，缘分将至…' : (selectedType ? selectedType + ' · 心中默念所问之事，再轻拈一签' : '请先择一类签，再拈签问缘') }}</p>
          </div>

          <!-- 签文结果 -->
          <div v-else key="result" class="sign-result">
            <div class="sign-head">
              <span class="sign-type">{{ current.type }}</span>
              <span class="sign-level">{{ current.level }}</span>
            </div>
            <p class="sign-poem">{{ current.poem }}</p>
            <p class="sign-note">解曰：{{ current.note }}</p>
          </div>
        </transition>

        <button class="btn-draw" :disabled="drawing || !selectedType" @click="drawSign">
          {{ drawing ? '摇 签 中…' : (!selectedType ? '请 先 择 签 种' : (current ? '再 拈 一 签' : '拈 签 问 缘')) }}
        </button>
      </section>

      <!-- ============ 个人空间栏目（由「缘」字悬浮菜单进入） ============ -->
      <section v-if="activeTab === 'profile'" class="profile-card">
        <!-- 头像：点击圆形小像或按钮上传，自动居中裁剪压缩 -->
        <div class="avatar-block">
          <button class="profile-avatar" type="button" :disabled="avatarBusy"
                  title="点击更换头像" aria-label="更换头像" @click="openAvatarPicker">
            <img v-if="avatarUrl" :src="avatarUrl" alt="我的头像" />
            <span v-else class="avatar-fallback">{{ displayName.slice(0, 1) }}</span>
            <span class="avatar-mask">换</span>
          </button>
          <input ref="avatarInput" class="avatar-file" type="file"
                 accept="image/*" @change="onAvatarChange" />
          <div class="avatar-actions">
            <button class="btn-avatar" type="button" :disabled="avatarBusy" @click="openAvatarPicker">更 换 头 像</button>
            <button v-if="avatarUrl" class="btn-avatar ghost" type="button" :disabled="avatarBusy" @click="clearAvatar">移 除 头 像</button>
          </div>
          <p class="avatar-tip">{{ avatarTip || '支持 JPG / PNG / WebP，自动裁成圆形小像' }}</p>
        </div>
        <h2 class="profile-name">{{ displayName }}</h2>
        <p class="profile-desc">狐缘阁 · 有缘人名帖</p>

        <div class="profile-grid">
          <div class="profile-field"><span>道 号</span><strong>{{ displayName }}</strong></div>
          <div class="profile-field"><span>出 身</span><strong>{{ userRace }}</strong></div>
          <div class="profile-field"><span>性 别</span><strong>{{ userGender }}</strong></div>
          <div class="profile-field"><span>结 缘</span><strong>{{ joinDate }}</strong></div>
        </div>

        <div class="profile-sign">
          <p class="profile-sign-label">近 期 所 求 之 签</p>
          <template v-if="current">
            <p class="profile-sign-meta">{{ current.type }} · {{ current.level }}</p>
            <p class="profile-sign-poem">{{ current.poem }}</p>
          </template>
          <p v-else class="profile-sign-empty">尚未求得签文，何不前往求签问一问前路？</p>
        </div>

        <button class="btn-back" type="button" @click="switchTab('fortune')">前 往 求 签</button>
        <button class="btn-back" type="button" @click="switchTab('home')">返 回 首 页</button>
      </section>
    </main>
  </div>
</template>

<style scoped>
/* ============ 古风基调（与登录页一致） ============ */
.home-page {
  min-height: 100vh; position: relative;
  font-family: "STXingkai", "华文行楷", "STKaiti", "楷体", "KaiTi", serif;
  color: #4a3b32;
  /* 壁纸由 body 统一承载，页面完全透明，不再叠加任何洗白晕染 */
  background: none;
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

/* ============ 顶部导航栏：首页 / 求签 ============ */
.navbar {
  position: sticky; top: 0; z-index: 50;
  background: rgba(250, 245, 234, .55); backdrop-filter: blur(3px);
  border-bottom: 1px solid rgba(184,146,74,.45);
  box-shadow: 0 2px 14px rgba(74,59,50,.08);
}
.nav-inner {
  max-width: 1080px; margin: 0 auto; height: 74px; padding: 0 28px;
  display: flex; align-items: center; gap: 36px;
}
/* 阁名与题句：脱离 1080px 内容区，贴视口最左 */
.brand {
  position: absolute; top: 50%; left: clamp(16px, 3vw, 40px);
  transform: translateY(-50%); z-index: 55;
  display: flex; flex-direction: column; align-items: flex-start; gap: 2px;
  text-align: left; pointer-events: none;
}
.brand-name { font-size: 24px; letter-spacing: 6px; color: #8e2c2c; }
.brand-sub { font-size: 11px; letter-spacing: 2px; color: #33403e; }
.brand-name, .brand-sub { text-shadow: 0 0 8px rgba(255,253,246,.9); }
.nav-links {
  position: absolute; top: 50%; left: calc(clamp(16px, 3vw, 40px) + 165px);
  transform: translateY(-50%);
  display: flex; align-items: center;
}
.nav-link {
  position: relative; display: inline-block; padding: 8px 20px;
  font-size: 17px; letter-spacing: 4px; color: #33403e; cursor: pointer;
  transition: color .25s ease;
}
.nav-link:hover { color: #b03a3a; }
.nav-link.active { color: #8e2c2c; }
.nav-link.active::after {
  content: ""; position: absolute; left: 50%; bottom: -3px; transform: translateX(-50%);
  width: 30px; height: 2px; background: #b03a3a; border-radius: 2px;
}
/* 右上角个人菜单：脱离 1080px 内容区，真正贴到视口最右 */
.user-menu {
  position: absolute; top: 50%; right: clamp(16px, 3vw, 40px);
  transform: translateY(-50%); z-index: 60;
}
.nav-seal {
  width: 38px; height: 38px; padding: 0; border: none;
  font-family: inherit; font-size: 19px;
  display: flex; align-items: center; justify-content: center;
  color: #f5efe3; background: #b03a3a; border-radius: 6px;
  box-shadow: 0 2px 8px rgba(176,58,58,.3); cursor: pointer;
  transition: transform .22s ease, box-shadow .22s ease, background .22s ease;
}
.user-menu:hover .nav-seal,
.user-menu.open .nav-seal,
.nav-seal:focus-visible {
  background: #8e2c2c; transform: scale(1.08);
  box-shadow: 0 4px 14px rgba(142,44,44,.45);
}
.nav-seal:focus-visible { outline: 2px solid #b8924a; outline-offset: 3px; }

/* 已设头像：右上角印章换成圆形小像，描金边呼应古风 */
.nav-seal.has-avatar {
  border-radius: 50%; overflow: hidden;
  background: rgba(250,245,234,.9);
  box-shadow: 0 0 0 2px #b8924a, 0 2px 10px rgba(142,44,44,.35);
}
.nav-seal img { display: block; width: 100%; height: 100%; object-fit: cover; }
.user-menu:hover .nav-seal.has-avatar,
.user-menu.open .nav-seal.has-avatar,
.nav-seal.has-avatar:focus-visible {
  background: rgba(250,245,234,.95);
  box-shadow: 0 0 0 2px #b8924a, 0 4px 16px rgba(142,44,44,.45);
}

.user-dropdown {
  position: absolute; top: calc(100% + 12px); right: 0;
  width: 178px; padding: 8px;
  background: rgba(250,245,234,.92); backdrop-filter: blur(4px);
  border: 1px solid rgba(184,146,74,.7); border-radius: 12px;
  box-shadow: 0 10px 30px rgba(74,59,50,.25), inset 0 0 30px rgba(245,230,210,.6);
}
/* 透明桥接：鼠标从印章移向菜单时不会触发 mouseleave */
.user-dropdown::before {
  content: ""; position: absolute; top: -14px; left: 0; right: 0; height: 14px;
}
.dropdown-head {
  display: flex; align-items: center; gap: 9px;
  padding: 8px 10px 10px; margin-bottom: 4px;
  font-size: 14px; letter-spacing: 1px; color: #8e2c2c;
  border-bottom: 1px dashed rgba(184,146,74,.55);
}
.dropdown-avatar {
  flex: 0 0 28px; width: 28px; height: 28px;
  border-radius: 50%; object-fit: cover;
  box-shadow: 0 0 0 1px rgba(184,146,74,.8);
}
.dropdown-avatar.fallback {
  display: flex; align-items: center; justify-content: center;
  background: #b03a3a; color: #f5efe3; font-size: 14px;
}
.dropdown-head-text { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dropdown-head small {
  display: block; margin-top: 3px;
  font-size: 11px; letter-spacing: 0; color: #33403e;
}
.dropdown-item {
  display: block; width: 100%; padding: 10px 12px;
  border: none; border-radius: 8px; background: transparent;
  font-family: inherit; font-size: 15px; letter-spacing: 2px;
  color: #4a3b32; text-align: left; cursor: pointer;
  transition: background .2s ease, color .2s ease;
}
.dropdown-item:hover { background: rgba(176,58,58,.1); color: #8e2c2c; }
.dropdown-item.danger {
  margin-top: 4px; color: #b03a3a;
  border-top: 1px solid rgba(184,146,74,.35); border-radius: 0 0 8px 8px;
}
.dropdown-item.danger:hover { background: rgba(176,58,58,.14); color: #8e2c2c; }
.menu-enter-active, .menu-leave-active { transition: opacity .18s ease, transform .18s ease; }
.menu-enter-from, .menu-leave-to { opacity: 0; transform: translateY(-8px); }

/* ============ 主内容 ============ */
.content {
  position: relative; z-index: 10;
  max-width: 760px; margin: 0 auto; padding: 44px 24px 64px; text-align: center;
  /* 不叠加任何背景图层，壁纸完整透出 */
}
/* 裸排文字：深色字 + 轻白柔光，不铺色块、不产生光斑 */
.title, .subtitle, .search-hint, .divider span, .type-hint, .tube-tip {
  text-shadow: 0 1px 3px rgba(255,250,240,.9), 0 0 7px rgba(255,250,240,.55);
}
.title { font-size: 40px; letter-spacing: 12px; color: #8e2c2c; }
.subtitle { margin-top: 8px; font-size: 15px; letter-spacing: 4px; color: #384846; }
.divider { display: flex; align-items: center; gap: 12px; width: 260px; margin: 22px auto 32px; }
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: linear-gradient(90deg, transparent, #b8924a, transparent); }
.divider span { color: #b03a3a; font-size: 15px; }

/* ============ 搜索栏 ============ */
.search-bar {
  display: flex; align-items: center; gap: 10px;
  max-width: 560px; margin: 0 auto; padding: 8px 8px 8px 18px;
  background: rgba(250, 245, 234, .5);
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
.search-bar input::placeholder { color: rgba(51,64,62,.95); }
.btn-clear {
  flex: none; width: 26px; height: 26px; line-height: 1;
  font-family: inherit; font-size: 18px; color: #33403e;
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
.search-hint { margin-top: 12px; font-size: 12px; letter-spacing: 2px; color: #384846; }

/* ============ 搜索结果 ============ */
.search-result { max-width: 560px; margin: 22px auto 0; text-align: left; }
.result-head { margin-bottom: 10px; font-size: 13px; letter-spacing: 2px; color: #8e2c2c; }
.result-list { display: flex; flex-direction: column; gap: 10px; max-height: 250px; overflow-y: auto; }
.result-item {
  display: flex; gap: 14px; align-items: flex-start; padding: 12px 16px;
  background: rgba(250, 245, 234, .48);
  border: 1px solid rgba(184,146,74,.45); border-radius: 10px;
}
.result-level {
  flex: none; padding: 3px 9px; font-size: 12px; letter-spacing: 1px;
  color: #f5efe3; background: #b03a3a; border-radius: 6px;
}
.result-poem { font-size: 15px; letter-spacing: 1px; color: #8e2c2c; }
.result-note { margin-top: 4px; font-size: 12px; line-height: 1.6; color: #33403e; }
.result-empty { padding: 14px; text-align: center; font-size: 14px; letter-spacing: 2px; color: #33403e; }

/* ============ 首页：前往求签引导 ============ */
.guide-card {
  max-width: 520px; margin: 34px auto 0; padding: 30px 32px 32px;
  text-align: center;
  background: rgba(250, 245, 234, .4); backdrop-filter: blur(2px);
  border: 1.5px dashed rgba(184,146,74,.7); border-radius: 16px;
  box-shadow: 0 8px 26px rgba(74,59,50,.1);
}
.guide-mark {
  display: inline-flex; align-items: center; justify-content: center;
  width: 44px; height: 44px; margin-bottom: 14px;
  font-size: 22px; color: #f5efe3; background: #b03a3a;
  border: 1px solid #b8924a; border-radius: 50%;
}
.guide-title { font-size: 20px; letter-spacing: 3px; color: #8e2c2c; }
.guide-text { margin-top: 10px; font-size: 13px; letter-spacing: 1px; line-height: 1.9; color: #33403e; }
.btn-guide {
  height: 46px; margin-top: 20px; padding: 0 34px;
  font-family: inherit; font-size: 17px; letter-spacing: 5px; text-indent: 5px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .35s ease, transform .15s ease, filter .35s ease;
}
.btn-guide span { letter-spacing: 0; text-indent: 0; }
.btn-guide:hover { box-shadow: 0 0 16px rgba(176,58,58,.38); filter: brightness(1.06); }
.btn-guide:active { transform: scale(.96); }
.link-guide {
  display: inline-block; margin-left: 8px; padding: 0;
  font-family: inherit; font-size: 13px; letter-spacing: 1px;
  color: #8e2c2c; background: transparent; border: none;
  border-bottom: 1px solid #b03a3a; cursor: pointer;
}
.link-guide:hover { color: #b03a3a; }

/* ============ 求签卡片（云纹古风边框，同登录页） ============ */
.qiuqian-card {
  position: relative; max-width: 520px; margin: 34px auto 0; padding: 32px 40px 36px;
  background: rgba(250, 245, 234, .38); backdrop-filter: blur(2px);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 12px 40px rgba(74,59,50,.18),
              inset 0 0 60px rgba(245, 230, 210, .14);
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
.card-sub { margin-top: 6px; text-align: center; font-size: 13px; letter-spacing: 3px; color: #33403e; }
.divider.small { width: 60%; margin: 18px auto 24px; }

/* ============ 签种三选一 ============ */
.type-picker { margin-bottom: 22px; }
.type-hint { font-size: 13px; letter-spacing: 2px; color: #33403e; }
.type-list { display: flex; justify-content: center; gap: 12px; margin-top: 14px; }
.type-card {
  flex: 1; max-width: 150px; padding: 16px 8px 14px;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  font-family: inherit; cursor: pointer; color: #4a3b32;
  background: rgba(250, 245, 234, .44);
  border: 1.5px solid rgba(184,146,74,.5); border-radius: 12px;
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease, background .25s ease;
}
.type-card:hover { border-color: #b03a3a; transform: translateY(-2px); }
.type-card.chosen {
  color: #f5efe3;
  background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border-color: #b8924a;
  box-shadow: 0 6px 18px rgba(142,44,44,.28);
}
.type-icon {
  display: flex; align-items: center; justify-content: center;
  width: 34px; height: 34px; font-size: 18px;
  color: #8e2c2c; background: rgba(245,239,227,.95);
  border: 1px solid #b8924a; border-radius: 50%;
}
.type-card.chosen .type-icon { color: #8e2c2c; background: #f5efe3; }
.type-name { font-size: 17px; letter-spacing: 3px; text-indent: 3px; }
.type-desc { font-size: 11px; letter-spacing: 1px; opacity: .85; }
.sign-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 10px; }
.sign-type {
  padding: 5px 14px; font-size: 14px; letter-spacing: 3px; text-indent: 3px;
  color: #8e2c2c; background: rgba(184,146,74,.16);
  border: 1px solid #b8924a; border-radius: 8px;
}
.result-badges { flex: none; display: flex; flex-direction: column; align-items: flex-start; gap: 5px; }
.result-type {
  padding: 2px 8px; font-size: 11px; letter-spacing: 1px;
  color: #8e2c2c; background: rgba(184,146,74,.16);
  border: 1px solid rgba(184,146,74,.55); border-radius: 5px;
}

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
.tube-tip { margin-top: 10px; font-size: 13px; letter-spacing: 2px; color: #33403e; }
.sign-result { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; }
.sign-level {
  padding: 5px 18px; font-size: 15px; letter-spacing: 4px; text-indent: 4px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 8px;
}
.sign-poem { font-size: 21px; letter-spacing: 3px; line-height: 1.8; color: #8e2c2c; }
.sign-note { font-size: 14px; letter-spacing: 1px; line-height: 1.8; color: #33403e; }
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

/* ============ 个人空间名帖 ============ */
.profile-card {
  position: relative; max-width: 520px; margin: 0 auto; padding: 36px 34px 32px;
  background: rgba(250,245,234,.4); backdrop-filter: blur(2px);
  border: 1.5px solid rgba(184,146,74,.7); border-radius: 16px;
  box-shadow: 0 10px 30px rgba(74,59,50,.18), inset 0 0 50px rgba(245,230,210,.14);
}
.avatar-block { display: flex; flex-direction: column; align-items: center; margin-bottom: 14px; }
.profile-avatar {
  position: relative; width: 96px; height: 96px; padding: 0; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  font-family: inherit; border: 2px solid #b8924a; border-radius: 50%;
  background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  box-shadow: 0 4px 16px rgba(142,44,44,.28); cursor: pointer;
  transition: transform .22s ease, box-shadow .22s ease;
}
.profile-avatar:hover:not(:disabled) { transform: scale(1.04); box-shadow: 0 6px 22px rgba(142,44,44,.42); }
.profile-avatar:disabled { opacity: .7; cursor: wait; }
.profile-avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar-fallback { font-size: 42px; line-height: 1; color: #f5efe3; }
.avatar-mask {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  font-size: 16px; letter-spacing: 2px; color: #f5efe3;
  background: rgba(74,43,43,.58); opacity: 0; transition: opacity .22s ease;
}
.profile-avatar:hover .avatar-mask { opacity: 1; }
.avatar-file { display: none; }
.avatar-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 12px; }
.btn-avatar {
  height: 34px; padding: 0 18px;
  font-family: inherit; font-size: 13px; letter-spacing: 2px;
  color: #f5efe3; background: #b03a3a;
  border: 1px solid #b8924a; border-radius: 8px; cursor: pointer;
  transition: filter .25s ease, transform .15s ease;
}
.btn-avatar:hover:not(:disabled) { filter: brightness(1.08); }
.btn-avatar:active:not(:disabled) { transform: scale(.96); }
.btn-avatar:disabled { opacity: .6; cursor: wait; }
.btn-avatar.ghost { color: #8e2c2c; background: transparent; }
.avatar-tip { margin-top: 8px; font-size: 12px; letter-spacing: 1px; color: #33403e; }
.profile-name { font-size: 26px; letter-spacing: 6px; color: #8e2c2c; font-weight: normal; }
.profile-desc { margin-top: 4px; font-size: 13px; letter-spacing: 2px; color: #33403e; }
.profile-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin: 26px 0 20px; }
.profile-field {
  padding: 12px 14px; text-align: left;
  background: rgba(255,253,246,.46);
  border: 1px solid rgba(184,146,74,.45); border-radius: 10px;
}
.profile-field span { display: block; font-size: 12px; letter-spacing: 2px; color: #33403e; }
.profile-field strong {
  display: block; margin-top: 4px; font-weight: normal;
  font-size: 18px; letter-spacing: 2px; color: #4a3b32;
}
.profile-sign {
  padding: 18px; border: 1px dashed rgba(184,146,74,.6); border-radius: 12px;
  background: rgba(255,253,246,.38);
}
.profile-sign-label { font-size: 13px; letter-spacing: 3px; color: #8e2c2c; margin-bottom: 8px; }
.profile-sign-meta { font-size: 14px; letter-spacing: 2px; color: #b03a3a; }
.profile-sign-poem { margin-top: 10px; font-size: 17px; letter-spacing: 2px; color: #4a3b32; }
.profile-sign-empty { font-size: 14px; letter-spacing: 1px; color: #33403e; }
.btn-back {
  height: 40px; margin: 22px 6px 0; padding: 0 26px;
  font-family: inherit; font-size: 15px; letter-spacing: 4px;
  color: #8e2c2c; background: transparent;
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .3s ease, transform .15s ease;
}
.btn-back:hover { box-shadow: 0 0 14px rgba(176,58,58,.25); }
.btn-back:active { transform: scale(.95); }

/* ============ 窄屏适配 ============ */
@media (max-width: 640px) {
  .nav-inner { height: 64px; padding: 0 16px; gap: 16px; }
  .brand { left: 12px; }
  .brand-name { font-size: 17px; letter-spacing: 2px; }
  .brand-sub { font-size: 9px; letter-spacing: 1px; }
  .nav-links { left: 138px; }
  .nav-links { gap: 2px; }
  .nav-link { padding: 8px 12px; font-size: 16px; }
  .user-menu { right: 12px; }
  .nav-seal { width: 34px; height: 34px; font-size: 17px; }
  .user-dropdown { width: 164px; }
  .profile-card { padding: 28px 18px 26px; }
  .profile-avatar { width: 82px; height: 82px; }
  .avatar-fallback { font-size: 36px; }
  .profile-grid { gap: 8px; }
  .btn-back { padding: 0 16px; margin: 18px 4px 0; }
  .guide-card { padding: 24px 18px 26px; }
  .content { padding: 30px 16px 48px; }
  .title { font-size: 30px; letter-spacing: 8px; }
  .search-bar { padding-left: 12px; }
  .btn-search { padding: 0 16px; letter-spacing: 2px; text-indent: 2px; }
  .qiuqian-card { padding: 28px 20px 30px; }
  .type-list { gap: 8px; }
  .type-card { padding: 12px 4px; }
  .type-name { font-size: 15px; letter-spacing: 1px; text-indent: 1px; }
  .type-desc { display: none; }
  .sign-type, .sign-level { font-size: 13px; }
  .sign-poem { font-size: 18px; letter-spacing: 2px; }
  .thread-tl, .umbrella { opacity: .35; }
}

/* ===== 半透卡片文字：轻白柔光，保证花纹背景上仍清晰（不叠任何背景块） ===== */
.qiuqian-card, .guide-card, .profile-card, .search-bar, .result-item, .type-card, .profile-field, .profile-sign {
  text-shadow: 0 0 5px rgba(255,253,246,1), 0 0 12px rgba(255,253,246,.9);
}
</style>
