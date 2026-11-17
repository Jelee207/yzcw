<script setup>
/**
 * Home.vue —— 狐缘阁主页（登录成功后进入）
 * 风格与登录页一致：水墨晕染背景 + 桃花花瓣 + 姻缘红线 + 苦情树 + 油纸伞 + 云纹卡片
 * 顶部导航栏「首页 / 求签 / 签囊 / 金 / 法宝 / 苦情树」切换栏目（单页切换，不新增路由）：
 *   首页：签文搜索 +《狐妖小红娘》剧情故事线（按漫画原作篇章顺序，点击展卷看详情）
 *   求签：先择签种（姻缘签 / 福运签 / 前程签），签筒只从该签种签库中抽取
 *         签库只设「上上签 / 上签 / 中签」三级吉平签；每个签种 6 支、每个签级 6 支，共 18 支
 *   签囊：收藏签背包——所拈之签可「收入签囊」，按签种 / 签级筛选，已得与未得各有卡位
 *   金：领取金币页——每日登录 +20 金、每 3 分钟 +4 金（每日 10 次），拈签耗 DRAW_COST(2) 金、随缘请器耗 TREASURE_DRAW_COST(10) 金
 *   法宝：《狐妖小红娘》法宝图鉴，按涂山 / 道门 / 西西域 / 南国分卷，可检索、可展卷看详情；每件标 2-25 金价格；随缘请器随机得法宝入法囊，集齐一种签赐 3 件法宝
 *   苦情树：续缘相册，分两页——
 *           「涂山情谱」是原作 10 对（只读，图片位后台可换）；
 *           「同心缘簿」由用户自由投稿，POST /api/user-couples 写入 tb_user_couple，
 *           所有用户都能看到；仅投稿人本人（道号一致）可删除自己上传的相片。
 *   首页不再设置跳转求签栏目的引导入口，求签仅由顶部导航进入
 * 右上角「缘」章悬浮展开个人菜单：个人空间 / 退出（栏目切换，不新增路由）
 * 仅管理员（permission === 0）在导航最右可见「后台管理」按钮，跳 /admin 进入后台管理系统；
 * 普通用户（permission === 1）看不到该按钮，也无法登录后台。
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { saveAvatar, removeAvatar } from '../api/user'

const router = useRouter()

/* ---------- 顶部导航：当前栏目 ----------
 * 支持 ?tab=tree 之类的深链，方便把「苦情树」等栏目直接分享出去 */
const validTabs = ['home', 'fortune', 'bag', 'gold', 'treasure', 'tree', 'profile']
const initialTab = new URLSearchParams(window.location.search).get('tab')
const activeTab = ref(validTabs.includes(initialTab) ? initialTab : 'home')
function switchTab(tab) {
  activeTab.value = tab
  activeTreasure.value = null     // 离开栏目时收起法宝详情卷
  activeCouple.value = null       // 离开栏目时收起相册展卷
  if (tab === 'gold' || tab === 'fortune') syncGoldFromServer()  // 进入相关栏目刷新数据库余额
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
const pageTitle = computed(() => (
  activeTab.value === 'home' ? '缘 起 再 续' :
  activeTab.value === 'fortune' ? '拈 签 问 缘' :
  activeTab.value === 'bag' ? '签 囊 · 收 藏 背 包' :
  activeTab.value === 'gold' ? '金 囊 · 领 取 之 阁' :
  activeTab.value === 'treasure' ? '法 宝 图 鉴' :
  activeTab.value === 'tree' ? '苦 情 树' : '个 人 空 间'
))
const pageSub = computed(() => (
  activeTab.value === 'home' ? '一签知前路 · 一缘定此生' :
  activeTab.value === 'fortune' ? '一念起处 · 且听签语' :
  activeTab.value === 'bag' ? '所 得 之 签 · 尽 收 此 囊' :
  activeTab.value === 'gold' ? '日 有 金 禄 · 拈 签 不 忧' :
  activeTab.value === 'treasure' ? '一器一缘 · 皆系前尘' :
  activeTab.value === 'tree' ? '一树千缘 · 世世相寻' : '一纸名帖 · 记此间前缘'
))

/* ---------- 签种：选定后只从该签种的签库抽取 ---------- */
const signTypes = [
  { key: '姻缘签', icon: '缘', desc: '问情缘 · 觅故人' },
  { key: '福运签', icon: '福', desc: '问家宅 · 盼安康' },
  { key: '前程签', icon: '程', desc: '问学业 · 卜前程' },
]
/* 签级只留三级吉平签 */
const signLevels = ['上上签', '上签', '中签']

/* ---------- 签文库：3 签种 × 3 签级 × 2 支 = 18 支 ----------
 * 每个签种 6 支（上上 2 / 上 2 / 中 2），每个签级 6 支；
 * id 为稳定编号，签囊收藏按 id 记录，求签与搜索共用此库。
 */
const signs = [
  // 姻缘签 · 问情缘（6 支）
  { id: 'yy-01', type: '姻缘签', level: '上上签', poem: '红线牵来前世约，桃花开处故人归', note: '所盼之事水到渠成，宜主动相寻，莫负良辰。', tag: '姻缘 重逢 吉' },
  { id: 'yy-02', type: '姻缘签', level: '上上签', poem: '月老笑点鸳鸯谱，一纸盟约两心知', note: '心意相通，所求皆顺，可放心前行。', tag: '姻缘 顺遂 吉' },
  { id: 'yy-03', type: '姻缘签', level: '上签', poem: '清风拂面旧梦回，一纸相思有处寄', note: '旧缘未断，静待佳音，不必急于追问。', tag: '姻缘 旧缘 等待 吉' },
  { id: 'yy-04', type: '姻缘签', level: '上签', poem: '苦情树下花又发，前尘往事皆可期', note: '久候之事渐有转机，再坚持一时。', tag: '姻缘 转机 守候 吉' },
  { id: 'yy-05', type: '姻缘签', level: '中签', poem: '云遮月色路漫漫，且把心事付流年', note: '缘分尚在酝酿，从容相待，静候花开。', tag: '姻缘 从容 平稳 平' },
  { id: 'yy-06', type: '姻缘签', level: '中签', poem: '流水绕花终有遇，一程相伴一程春', note: '情缘缓缓而行，多些耐心，自会相逢于恰好之时。', tag: '姻缘 相守 渐进 平' },

  // 福运签 · 问家宅安康（6 支）
  { id: 'fy-01', type: '福运签', level: '上上签', poem: '福星高照满庭芳，一门和顺四时安', note: '家宅安宁，福气自来，宜广结善缘。', tag: '福运 家宅 平安 吉' },
  { id: 'fy-02', type: '福运签', level: '上上签', poem: '春风送暖入柴门，仓廪丰盈岁月长', note: '财禄渐丰，所求有得，宜惜福积善。', tag: '福运 财禄 丰盈 吉' },
  { id: 'fy-03', type: '福运签', level: '上签', poem: '灵鹊枝头报好音，旧疾新忧皆散尽', note: '身心渐安，烦忧自去，宜放宽心怀。', tag: '福运 安康 顺遂 吉' },
  { id: 'fy-04', type: '福运签', level: '上签', poem: '一盏清茶待客来，贵人暗助事竟成', note: '有贵人相助，凡事不必独自承担。', tag: '福运 贵人 相助 吉' },
  { id: 'fy-05', type: '福运签', level: '中签', poem: '云淡风轻寻常日，无灾无难即是福', note: '平淡日子里自有安稳，惜取眼前便是福气。', tag: '福运 平常 守成 平' },
  { id: 'fy-06', type: '福运签', level: '中签', poem: '庭前细雨润新苔，寻常日子亦生香', note: '福气藏在细处，安心度日，自有小喜临门。', tag: '福运 安宁 小喜 平' },

  // 前程签 · 问学业事业（6 支）
  { id: 'qc-01', type: '前程签', level: '上上签', poem: '长风破浪会有时，直挂云帆济沧海', note: '前程开阔，宜放手一搏，功名可期。', tag: '前程 功名 远行 吉' },
  { id: 'qc-02', type: '前程签', level: '上上签', poem: '十年磨剑今朝试，一举成名天下知', note: '平日积累已足，此番应试必有回响。', tag: '前程 学业 应试 吉' },
  { id: 'qc-03', type: '前程签', level: '上签', poem: '山重水复疑无路，柳暗花明又一村', note: '眼前困顿将解，转机在坚持之后。', tag: '前程 转机 事业 吉' },
  { id: 'qc-04', type: '前程签', level: '上签', poem: '鹏鸟振翅九万里，前路迢迢任君行', note: '宜远行、宜求变，新局胜于旧局。', tag: '前程 远行 变动 吉' },
  { id: 'qc-05', type: '前程签', level: '中签', poem: '一程山水一程缘，莫问归期问此心', note: '按自己的步调走，方向自会渐渐分明。', tag: '前程 自省 抉择 平' },
  { id: 'qc-06', type: '前程签', level: '中签', poem: '行到山腰且歇脚，蓄得清风再启程', note: '步子放稳，边走边学，前路自会渐次开阔。', tag: '前程 蓄力 渐进 平' },
]

/* ---------- 求签：先择签种，只从该签种签库抽取 ---------- */
const selectedType = ref('')
const current = ref(null)
const drawing = ref(false)
const repeatStreak = ref(0)   // 连续拈得「已入签囊」之签的次数，满 2 次下次保底出新签
const pool = computed(() => signs.filter((s) => s.type === selectedType.value))
const tubeChar = computed(() => signTypes.find((t) => t.key === selectedType.value)?.icon || '缘')

function selectType(key) {
  if (drawing.value) return          // 摇签过程中不允许换签种
  selectedType.value = key
  current.value = null               // 换签种即清空上一签，避免串签
}
function drawSign() {
  if (drawing.value || !selectedType.value) return
  if (gold.value < DRAW_COST) { showToast('金 币 不 足 · 先 领 金 再 拈 签'); return }
  drawing.value = true
  current.value = null
  const list = pool.value            // 抽签当下锁定所选签种的签池
  setTimeout(() => {
    // 保底：连续两次拈得旧签后，从本签种尚未入囊的签中必出一支
    const pity = repeatStreak.value >= 2
    const fresh = pity ? list.filter((s) => !isCollected(s)) : []
    let picked, pityHit = false
    if (pity && fresh.length) {
      picked = fresh[Math.floor(Math.random() * fresh.length)]
      repeatStreak.value = 0
      pityHit = true
    } else {
      picked = list[Math.floor(Math.random() * list.length)]
      repeatStreak.value = isCollected(picked) ? repeatStreak.value + 1 : 0
    }
    spendGold(DRAW_COST)
    current.value = picked
    collectOnDraw(picked)    // 拈得之签自动入囊，集齐 18 支更有收集感
    drawing.value = false
    if (pityHit) showToast('✦ 缘 至 · 必 得 一 支 未 曾 入 囊 之 签')
  }, 700)
}

/* ---------- 签囊：收藏签背包（按登录账号隔离，存于本地） ----------
 * 结构：{ [签 id]: 收藏时间戳 }；只认签库内现存的 id，签库改版不留脏数据。 */
function bagKey() {
  let name = 'guest'
  try { name = JSON.parse(localStorage.getItem('huyao_user') || 'null')?.username || 'guest' }
  catch (e) { /* 本地存档异常时按游客处理 */ }
  return 'huyao_sign_bag_' + name
}
function loadBag() {
  try {
    const raw = JSON.parse(localStorage.getItem(bagKey()) || '{}')
    const valid = new Set(signs.map((s) => s.id))
    return Object.fromEntries(Object.entries(raw).filter(([id]) => valid.has(id)))
  } catch (e) { return {} }
}
const bag = ref(loadBag())
function saveBag() { localStorage.setItem(bagKey(), JSON.stringify(bag.value)) }
function isCollected(s) { return !!bag.value?.[s.id] }
function collectedAt(s) {
  const t = bag.value?.[s.id]
  if (!t) return ''
  const d = new Date(t)
  return isNaN(d.getTime()) ? '' : `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}
const bagToast = ref('')
let bagToastTimer = null
function toggleCollect(s) {
  if (!s) return
  const next = { ...bag.value }
  if (next[s.id]) { delete next[s.id]; bagToast.value = '已 从 签 囊 取 出' }
  else { next[s.id] = Date.now(); bagToast.value = '✦ 已 收 入 签 囊' }
  bag.value = next
  saveBag()
  clearTimeout(bagToastTimer)
  bagToastTimer = setTimeout(() => { bagToast.value = '' }, 1800)
}
/** 求签所得自动入囊（已收过的不重复记录），并给一句提示 */
function collectOnDraw(s) {
  if (!s || isCollected(s)) return
  bag.value = { ...bag.value, [s.id]: Date.now() }
  saveBag()
  bagToast.value = '✦ 新 签 入 囊 · 可 往 签 囊 翻 看'
  clearTimeout(bagToastTimer)
  bagToastTimer = setTimeout(() => { bagToast.value = '' }, 1800)
  checkSignSetReward()    // 集齐某一签种时，自动赏 3 件随机法宝
}
const ownedTotal = computed(() => signs.filter(isCollected).length)
const bagPercent = computed(() => Math.round((ownedTotal.value / signs.length) * 100))
const typeStats = computed(() => signTypes.map((t) => ({
  name: t.key, icon: t.icon,
  got: signs.filter((s) => s.type === t.key && isCollected(s)).length,
  total: signs.filter((s) => s.type === t.key).length,
})))
const levelStats = computed(() => signLevels.map((name) => ({
  name,
  got: signs.filter((s) => s.level === name && isCollected(s)).length,
  total: signs.filter((s) => s.level === name).length,
})))

/* 背包页筛选：签种 / 签级 / 仅看已得 */
const bagType = ref('全部')
const bagLevel = ref('全部')
const bagOnlyOwned = ref(false)
const bagTypes = ['全部', ...signTypes.map((t) => t.key)]
const bagLevels = ['全部', ...signLevels]
const bagList = computed(() => signs.filter((s) =>
  (bagType.value === '全部' || s.type === bagType.value) &&
  (bagLevel.value === '全部' || s.level === bagLevel.value) &&
  (!bagOnlyOwned.value || isCollected(s))
))
const bagOwnedInView = computed(() => bagList.value.filter(isCollected).length)
/** 未得卡位上的「去求签」：带好签种直接回求签栏目 */
function goDrawFor(type) {
  selectedType.value = type
  current.value = null
  switchTab('fortune')
}
function setBagType(t) { bagType.value = t }
function setBagLevel(l) { bagLevel.value = l }
function toggleBagOwned() { bagOnlyOwned.value = !bagOnlyOwned.value }

/* ---------- 法囊：所得法宝收藏（按登录账号隔离，存于本地） ----------
 * 结构：{ [法宝 slug]: 收藏时间戳 }；随缘请器或集齐签种所得法宝皆入此囊。 */
const TREASURE_DRAW_COST = 10    // 随缘请器一次耗 10 金
const SIGN_SET_REWARD = 3        // 集齐一种签奖励 3 件随机法宝
function chestKey() { return 'huyao_treasure_chest_' + (currentUsername() || 'guest') }
function loadChest() {
  try {
    const raw = JSON.parse(localStorage.getItem(chestKey()) || '{}')
    return raw && typeof raw === 'object' && !Array.isArray(raw) ? raw : {}
  } catch (e) { return {} }
}
const chest = ref(loadChest())
function saveChest() { localStorage.setItem(chestKey(), JSON.stringify(chest.value)) }
function hasTreasure(t) { return !!(t && chest.value?.[t.slug]) }
function addTreasure(t) {
  if (!t || !t.slug || hasTreasure(t)) return false
  chest.value = { ...chest.value, [t.slug]: Date.now() }
  saveChest()
  return true
}
const chestCount = computed(() => Object.keys(chest.value).length)
const chestList = computed(() => treasures.filter(hasTreasure))

/* 集签奖励记录：{ [签种]: 已领取时间戳 }，集齐一种签只奖一次 */
function rewardKey() { return 'huyao_sign_reward_' + (currentUsername() || 'guest') }
function loadRewarded() {
  try {
    const raw = JSON.parse(localStorage.getItem(rewardKey()) || '{}')
    return raw && typeof raw === 'object' && !Array.isArray(raw) ? raw : {}
  } catch (e) { return {} }
}
const rewarded = ref(loadRewarded())
function saveRewarded() { localStorage.setItem(rewardKey(), JSON.stringify(rewarded.value)) }
function signSetDone(type) {
  return signs.filter((s) => s.type === type).every(isCollected)
}
/** 随机取 n 件展示中的法宝（避免重复），用于请器与集签奖励 */
function pickRandomTreasures(n) {
  const pool = treasures.filter((t) => t.enabled !== 0)
  const shuffled = pool.slice().sort(() => Math.random() - 0.5)
  return shuffled.slice(0, n)
}
/** 拈签入囊后调用：某签种刚集齐则奖 3 件法宝入法囊 */
function checkSignSetReward() {
  for (const t of signTypes) {
    if (rewarded.value[t.key] || !signSetDone(t.key)) continue
    rewarded.value = { ...rewarded.value, [t.key]: Date.now() }
    saveRewarded()
    const gains = pickRandomTreasures(SIGN_SET_REWARD)
    gains.forEach(addTreasure)
    showToast(`✦ 集齐「${t.key}」· 赐 ${gains.length} 件法宝入囊`)
    return
  }
}
/** 三级签各配一种描金色调 */
function levelClass(level) {
  return level === '上上签' ? 'lv-best' : level === '上签' ? 'lv-good' : 'lv-plain'
}

/* ---------- 金币：每日登录礼 + 每 3 分钟领 4 金（每日限 10 次） ----------
 * { date, coins, claimCount, lastClaimAt, loginDate } 按登录账号隔离；
 * 跨天保留余额、领取次数归零，登录礼可再领。 */
const GOLD_LOGIN = 20
const GOLD_TICK = 4
const GOLD_TICK_LIMIT = 10
const GOLD_CD_MS = 3 * 60 * 1000
const DRAW_COST = 2   // 拈签一次消耗 2 金
function dayKey(d = new Date()) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
function currentUsername() {
  try { return JSON.parse(localStorage.getItem('huyao_user') || 'null')?.username || '' }
  catch (e) { return '' }
}
function goldKey() {
  return 'huyao_gold_' + (currentUsername() || 'guest')
}
function loadGold() {
  let d = null
  try { d = JSON.parse(localStorage.getItem(goldKey()) || 'null') } catch (e) { d = null }
  if (!d || typeof d !== 'object') d = { coins: 0, claimCount: 0, lastClaimAt: 0, loginDate: '' }
  return { ...d, date: dayKey(), claimCount: d.date === dayKey() ? (d.claimCount || 0) : 0 }
}
const goldState = ref(loadGold())
const gold = computed(() => goldState.value.coins || 0)

/** 把当前余额写回数据库（后台管理的金币数量即时成为权威值） */
let goldWritePending = 0
function pushGoldToServer() {
  const name = currentUsername()
  if (!name) return
  goldWritePending++
  fetch('/api/gold', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: name, coins: gold.value }),
  }).catch(() => { /* 同步失败静默处理，不影响本地使用 */ })
    .finally(() => { goldWritePending = Math.max(0, goldWritePending - 1) })
}
function saveGold() {
  localStorage.setItem(goldKey(), JSON.stringify(goldState.value))
  pushGoldToServer()
}

/** 进入页面时以数据库余额为准；首次上线把本地历史金币迁移到数据库 */
async function syncGoldFromServer() {
  const name = currentUsername()
  if (!name || goldWritePending > 0) return
  try {
    const resp = await fetch('/api/gold?username=' + encodeURIComponent(name))
    const json = await resp.json()
    if (json.code !== 200 || !json.data || typeof json.data.coins !== 'number') return
    const serverCoins = Math.max(0, json.data.coins)
    const syncedKey = 'huyao_gold_synced_' + name
    const everSynced = localStorage.getItem(syncedKey) === '1'
    if (!everSynced && serverCoins === 0 && gold.value > 0) {
      // 旧版本金币只在本地：首次迁移，把历史余额写入数据库
      localStorage.setItem(syncedKey, '1')
      pushGoldToServer()
      return
    }
    localStorage.setItem(syncedKey, '1')
    goldState.value = { ...goldState.value, coins: serverCoins }
    localStorage.setItem(goldKey(), JSON.stringify(goldState.value))
  } catch (e) { /* 后端不可用则沿用本地缓存 */ }
}
const nowTs = ref(Date.now())
let goldClock = null

/** 统一提示条（签到、领金、抽签保底都用它） */
function showToast(msg) {
  bagToast.value = msg
  clearTimeout(bagToastTimer)
  bagToastTimer = setTimeout(() => { bagToast.value = '' }, 2200)
}
function grantLoginGold() {
  const today = dayKey()
  if (goldState.value.loginDate === today) return
  goldState.value = { ...goldState.value, date: today, coins: gold.value + GOLD_LOGIN, loginDate: today }
  saveGold()
  showToast('✦ 每 日 登 录 礼 · +20 金')
}
function refreshGoldDay() {
  const today = dayKey()
  if (goldState.value.date !== today) {
    goldState.value = { ...goldState.value, date: today, claimCount: 0 }
    saveGold()
    grantLoginGold()
  }
}
const goldCooldown = computed(() => Math.max(0, Math.ceil(((goldState.value.lastClaimAt || 0) + GOLD_CD_MS - nowTs.value) / 1000)))
function fmtCountdown(sec) {
  const s = Math.max(0, sec)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}
function claimTickGold() {
  if (goldCooldown.value > 0 || goldState.value.claimCount >= GOLD_TICK_LIMIT) return
  goldState.value = {
    ...goldState.value,
    coins: gold.value + GOLD_TICK,
    claimCount: (goldState.value.claimCount || 0) + 1,
    lastClaimAt: Date.now(),
  }
  saveGold()
  showToast(`✦ 福 袋 领 取 · +${GOLD_TICK} 金`)
}
function spendGold(n) {
  if (gold.value < n) return false
  goldState.value = { ...goldState.value, coins: gold.value - n }
  saveGold()
  return true
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

/* ---------- 首页剧情故事线：按《狐妖小红娘》漫画原作篇章顺序 ----------
 * 下沙 → 尾生 → 王权 → 月红（含北山）→ 千颜 → 南国 → 竹业 → 金晨曦 → 真龙 / 圈外
 * 时间跳跃以 era 标注（缘起 / 五百年前 / 今生），避免把叙事顺序误作编年顺序。
 */
const chapters = [
  {
    no: '序', mark: '树', name: '缘起 · 苦情树', alias: '再世续缘之始',
    era: '缘 起 · 世界观', eraType: 'origin',
    couple: '涂山狐妖 · 红线仙', relic: '苦情树 / 纯爱天篇',
    brief: '人与妖相恋，人一转世便忘尽前尘；苦情树下一纸誓约，把相爱的记忆与妖力封入法宝，一分为二，随灵魂轮回。',
    story: '远古圈外之患未平，傲来国三少划出「圈内」，涂山狐妖与苦情树共生，以再世续缘为业，替痴情之妖寻回转世的爱人。五百年后，失去全部妖力与记忆的涂山红红化作涂山苏苏，与东方月初的转世白月初结为搭档——一桩桩红线仙任务，就此铺开千年爱恨。',
    verse: '树不语，红线自会牵人来。',
    tags: ['再世续缘', '苦情树', '白月初 × 涂山苏苏'],
  },
  {
    no: '壹', mark: '沙', name: '下沙篇', alias: '千年御水珠篇',
    era: '今 生 · 第一桩续缘', eraType: 'now',
    couple: '梵云飞 × 厉雪扬', relic: '千年御水珠',
    brief: '西西域沙狐王子与冰将军，一珠分作两半；求婚九十九次之后，才等来第一百次的相认。',
    story: '梵云飞为救厉雪扬吞下专克沙狐的千年御水珠，从此成了不怕水的沙狐；二人相守八十年，军娘病逝后他以御水珠为祭发动转世续缘。转世的半颗珠子埋在厉雪扬体内成了胆结石，害她一直认不出那只「土狗」。苏苏与白月初接下的第一桩任务，便是替他们把这句「对不起我错了」说完。',
    verse: '求婚百次，只为换她一句记得。',
    tags: ['千年御水珠', '冰凌雪枪', '下沙篇'],
  },
  {
    no: '贰', mark: '木', name: '尾生篇', alias: '月啼篇',
    era: '今 生 · 抱柱之约', eraType: 'now',
    couple: '胡尾生 × 月啼暇', relic: '七宝妙树皮',
    brief: '一场大雨，一个「她没来」的误会；他抱树而死，却不知怀里那棵树正是她。',
    story: '月啼族族长月啼暇是森林的守护精灵，淋雨便会现出树形、动弹不得；与伐木工的前世相约私奔那日天降大雨，尾生苦等不至，抱树溺亡。七宝妙树皮作为续缘之匙长在转世尾生背后，使他一遇雨也化为树。待白月初与苏苏解开误会，二人才终成眷属，一起开了家植物店。',
    verse: '抱柱之约，原是抱住了她。',
    tags: ['七宝妙树皮', '月啼族', '尾生篇'],
  },
  {
    no: '叁', mark: '剑', name: '王权篇', alias: '权瞳篇',
    era: '前 尘 · 道门兵人', eraType: 'past',
    couple: '王权富贵 × 清瞳', relic: '王权剑',
    brief: '被当作除妖兵器养大的少年，为替一只蜘蛛精看一眼外面的世界，燃尽寿命唤出王权剑意。',
    story: '王权富贵持天地一剑，是王权家最锋利的「兵人」；清瞳以蛛丝织成画卷，替足不出户的他描摹道门之外的风景。为护清瞳离开家门，他几乎丧命，死前与她签下再世续缘。清瞳苦寻五百年，找到的转世王富贵早已忘了她，却仍痴心相随。',
    verse: '剑能斩妖，斩不断一双想看世界的眼。',
    tags: ['王权剑', '王权剑意', '清瞳'],
  },
  {
    no: '肆', mark: '泪', name: '月红篇', alias: '东方月初 × 涂山红红',
    era: '五 百 年 前 · 缘 之 始', eraType: 'past',
    couple: '东方月初 × 涂山红红', relic: '虚空之泪',
    brief: '苦情树下与黑狐决战，千古第一陨落；两颗虚空之泪，一颗被夺，一颗随他转世成了白月初的右眼。',
    story: '东方月初幼年为涂山红红所救，为她人妖和平的心愿离开涂山、扬名天下，用尽一生。八十岁那年大战黑狐后，二人在苦情树下立下再世续缘之约：以两眼虚空之泪为匙，以全部妖力和相识的一点一滴为代价。红红自此失去妖力与记忆，化作涂山苏苏。此篇亦夹叙北山妖帝石宽与御妖国公主因御妖子母符而错过的往事。',
    verse: '千古第一陨落之日，正是来生相见之始。',
    tags: ['虚空之泪', '纯爱天篇', '北山篇 · 御妖子母符'],
  },
  {
    no: '伍', mark: '颜', name: '千颜篇', alias: '颜如玉篇',
    era: '今 生 · 来世相见', eraType: 'now',
    couple: '颜如玉 × 律笺文', relic: '天罗地网',
    brief: '千面妖容的弟子与第一妖捕，一句「来世相见」，代价是四成妖力与全部记忆。',
    story: '空陷山牛鬼颜如玉习得涂山容容的易容术与望月掌，却栽在妖捕律笺文手里。她以卧底之身接近，用问话的方式与他定下续缘契约；续缘提前发动，四成妖力与记忆被黑狐夺去，他成了三尾黑魉，她忘了他。',
    verse: '我颜如玉，愿意使用四成妖力，让我们来世相见。',
    tags: ['望月掌', '天罗地网', '黑狐'],
  },
  {
    no: '陆', mark: '蛊', name: '南国篇', alias: '平兰篇',
    era: '前 尘 · 另 一 世', eraType: 'past',
    couple: '平丘月初 × 欢都落兰', relic: '情蛊 / 紫欢金铃',
    brief: '被当作「东方月初转世」养大的少年，只为证明自己是平丘月初，逃到南国，爱上一位公主。',
    story: '平丘月初拒绝入赘涂山，在傲来国三少相助下隐去法力出走，与南国公主欢都落兰相识相爱。落兰以心血饲养情蛊、以返雏毒针换回旧颜，只为让白月初想起那一世。最终在红红的宽容、东方月初的成全与容容的助力下，二人签下再世续缘。',
    verse: '我不是谁的转世，我只是喜欢你的平丘月初。',
    tags: ['情蛊', '紫欢金铃', '返雏毒针', '万毒之体'],
  },
  {
    no: '柒', mark: '竹', name: '竹业篇', alias: '东方淮竹篇',
    era: '前 尘 · 上 一 代', eraType: 'past',
    couple: '东方淮竹 × 王权霸业', relic: '王权剑 / 面具',
    brief: '面具组织出圈外一战，剑心尽失；上一代未能说尽的遗憾，成了王权富贵那一代的起点。',
    story: '王权霸业率「面具」众人踏出圈外，惨败而归、剑心尽碎；东方淮竹因金面火神之祸家破人亡，二人的结合始于一场身不由己。淮竹早逝，霸业终身未娶妻，把儿子王权富贵锻造成除妖机器，直到寿终时才抱着剑鞘与玉笛泪流满面。',
    verse: '淮竹，我终于可以怀着剑心去见你了。',
    tags: ['面具组织', '圈外', '王权剑意'],
  },
  {
    no: '捌', mark: '曦', name: '金晨曦篇', alias: '沐天城 · 两生花 · 边境',
    era: '今 生 · 边 境', eraType: 'now',
    couple: '白月初 × 涂山苏苏 / 木蔑 × 翠玉鸣鸾', relic: '金晨曦',
    brief: '边境沐天城风云再起，圈外生物「金晨曦」现世，黑狐暗线与白月初的身世一并浮出水面。',
    story: '金晨曦篇串起沐天城、两生花与边境诸线：木蔑与翠玉鸣鸾的前世今生、金人凤骗取蛭妖换血秘法的旧案、面具力量的传承，都在同一张大网里收束。苏苏与白月初一边接续缘任务，一边一步步逼近圈外的真相。',
    verse: '线越牵越长，尽头却是世界的边界。',
    tags: ['金晨曦', '沐天城', '翠玉鸣鸾', '圈外'],
  },
  {
    no: '玖', mark: '外', name: '真龙 · 圈外篇', alias: '无暮篇 · 镜花缘',
    era: '今 生 · 未 竟', eraType: 'now',
    couple: '白月初 × 涂山苏苏', relic: '虚空之泪',
    brief: '千年前王权无暮与梵云飞的旧谊、真龙之秘、圈外镜花缘……红线牵出圈外，故事仍未写完。',
    story: '无暮篇回溯千年前执掌王权剑的少年王权无暮与梵云飞的相遇；其后真龙、龙湾、镜花缘、东方灵族诸篇层层揭开东方血脉、黑狐与圈外生物的来历。从苦情树下的一纸誓约走到圈外，白月初与苏苏的红线，依旧没有尽头。',
    verse: '红线未断，故事仍在继续。',
    tags: ['无暮篇', '镜花缘', '东方灵族', '连载中'],
  },
]
const openChapter = ref(-1)
function toggleChapter(i) {
  openChapter.value = openChapter.value === i ? -1 : i
}

/* ---------- 法宝图鉴：《狐妖小红娘》设定，按四卷分门别类 ----------
 * 器物之外，亦收录绝缘之爪、纯质阳炎、苦情树等「非器而列名器谱」的设定，
 * 并在类别中标注「异能 / 神火 / 圣物」，以免与原作设定相混。
 */
const scrolls = [
  { key: 'all',     label: '全 部' },
  { key: 'tushan',  label: '涂 山 卷' },
  { key: 'daomen',  label: '道 门 卷' },
  { key: 'xixiyu',  label: '西 西 域 卷' },
  { key: 'nanguo',  label: '南 国 卷' },
]
function scrollLabel(key) {
  return scrolls.find((s) => s.key === key)?.label || '全 部'
}
function stars(power) {
  return '✦'.repeat(power) + '✧'.repeat(5 - power)
}

const treasures = [
  // ===== 涂山卷 · 续缘至宝 =====
  {
    name: '虚空之泪', alias: '天地间最强法宝', mark: '泪', scroll: 'tushan',
    owner: '东方月初 → 白月初（右眼）', chapter: '月红篇', kind: '续缘 · 空间', power: 5,
    brief: '源于至高法力，生于至诚至情。形与寻常眼球无异，却能穿破一切物体、将所触之物移入虚空之境，所到之处时空错乱。',
    tale: '五百年前东方月初垂死之际因悔恨炼出两颗：一颗被黑狐女王夺走，一颗作为与涂山红红再世续缘之匙，随转世化作白月初的右眼。',
    verse: '一眼成泪，虚空为此错乱。', tags: ['最强法宝', '续缘之匙', '时空'],
  },
  {
    name: '纯爱天篇', alias: '狐妖天书 · 红娘秘籍', mark: '书', scroll: 'tushan',
    owner: '涂山红红 → 涂山苏苏', chapter: '全篇', kind: '天书 · 辅佐', power: 4,
    brief: '苦情巨树的树浆化纸而成，与苦情树心灵相通，能感知树下许愿之人的相爱往事，显出关键提示；刀枪不入，是红线仙完成任务的不二帮手。',
    tale: '书名由东方月初亲手题给红红，已逾八百年。每位狐妖一生只有唯一一本天书，此书曾为虚空之泪所破，现归苏苏所有。',
    verse: '树浆为纸，写尽世间未了情。', tags: ['天书', '红线仙', '苦情树'],
  },
  {
    name: '忆梦锤', alias: '续缘三宝之一', mark: '锤', scroll: 'tushan',
    owner: '涂山红线仙', chapter: '全篇', kind: '续缘 · 记忆', power: 3,
    brief: '传为女娲补天炼石之柴火木所制，形似寻常木锤，锤身印涂山徽标。注入狐妖妖力敲击续缘二人头部，便可唤回前世相爱的记忆。',
    tale: '售价五十万，是转世续缘中唤醒记忆的必备道具。涂山容容曾以此敲回东方月初与王权富贵的前世记忆。',
    verse: '一锤轻落，前世如梦初醒。', tags: ['记忆', '续缘', '涂山道具'],
  },
  {
    name: '无尽酒葫', alias: '无尽酒壶', mark: '酒', scroll: 'tushan',
    owner: '涂山雅雅', chapter: '月红篇', kind: '空间 · 增力', power: 4,
    brief: '空间法宝，葫中酒液无穷无尽。雅雅饮下后妖力大增，九尾天狐那令群妖闻风丧胆的最强状态，正由饮酒而生。',
    tale: '雅雅常年将酒葫系于腰间，护涂山、战黑狐之际，往往先饮一口，再以绝对零域冻住对手。',
    verse: '一葫未尽，九尾已临。', tags: ['空间法宝', '妖力', '九尾天狐'],
  },
  {
    name: '九转玄阴水', alias: '涂山独门', mark: '阴', scroll: 'tushan',
    owner: '涂山狐妖', chapter: '全篇', kind: '独门 · 克制', power: 3,
    brief: '涂山狐妖独门法宝，至阴至寒，专克白月初的灭妖神火「纯质阳炎」——阴阳相对，正是涂山为制衡东方灵族所留。',
    tale: '此水不尚攻伐，唯以相克见长，常见于涂山与一气道盟相互牵制的旧事之中。',
    verse: '至阴一泓，可熄至阳之焰。', tags: ['相克', '纯质阳炎', '涂山'],
  },
  {
    name: '绝缘之爪', alias: '非法器 · 天赋异能', mark: '爪', scroll: 'tushan',
    owner: '涂山红红 / 涂山苏苏', chapter: '月红篇', kind: '异能 · 克器', power: 5,
    brief: '并非器物，而是与生俱来的天赋：双手免疫一切法宝，可徒手擒下虚空之泪、操控纯质阳炎，连没有实体的黑狐也能抓住。',
    tale: '红红以全部妖力和记忆为代价化作苏苏后，法力尽失，唯独这双手仍能接下天下法器。',
    verse: '万器皆可被执，唯情不可。', tags: ['异能', '克一切法宝', '涂山红红'],
  },
  {
    name: '苦情树', alias: '续缘之根 · 圣物', mark: '树', scroll: 'tushan',
    owner: '涂山', chapter: '全篇', kind: '圣物 · 续缘', power: 5,
    brief: '生长于涂山的巨树，花如蒲公英。人与妖在树下起誓，以相爱的记忆与部分妖力为祭封入法宝，再由狐妖一分为二，方得再世续缘。',
    tale: '它不是器物，而是天下续缘法宝的缘起；天书以它的树浆为纸，红线仙因它的许愿而奔走。',
    verse: '花开如雪，落处皆是来生。', tags: ['圣物', '再世续缘', '涂山'],
  },

  // ===== 道门卷 · 一气道盟与王权世家 =====
  {
    name: '王权剑', alias: '天地一剑', mark: '剑', scroll: 'daomen',
    owner: '王权富贵 → 白裘恩', chapter: '王权篇', kind: '攻伐 · 先天灵宝', power: 5,
    brief: '人类发现的第一件法宝，天生灵物，剑柄上方书「王权」二字，为天地一剑王权世家所有，众妖闻之畏惧。',
    tale: '王权富贵曾持此剑斩妖无数，后为护清瞳以自身寿命唤剑；此剑辗转之后，竟被白裘恩当作废铁按斤收着。',
    verse: '剑上二字，压尽天下妖名。', tags: ['先天灵宝', '王权世家', '兵人'],
  },
  {
    name: '纯质阳炎', alias: '灭妖神火', mark: '炎', scroll: 'daomen',
    owner: '东方灵族 / 东方月初 / 白月初', chapter: '月红篇', kind: '神火 · 灭妖', power: 5,
    brief: '东方灵族独门「灭妖神火」，专克妖怪，唯有天生灵力的东方血脉可以驱用。严格说是血脉绝技，因威名列于器谱。',
    tale: '东方月初曾在涂山废弃仓库留下一大团阳炎，只需简单召唤术便可取用；百年后其转世白月初同样运用自如。',
    verse: '火起东方，妖气尽退。', tags: ['神火', '东方灵族', '绝技'],
  },
  {
    name: '追踪符', alias: '瞬移符箓', mark: '符', scroll: 'daomen',
    owner: '一气道盟', chapter: '全篇', kind: '符箓 · 瞬移', power: 2,
    brief: '形如寻常符纸，注入法力便可瞬息抵达目标所在之处，但须持有目标的一件物品为引，且一张所费不菲。',
    tale: '红娘任务里赶赴续缘之地的常用之物，与忆梦锤同属「好用但很贵」的涂山与道门道具。',
    verse: '一符既焚，山海不隔。', tags: ['符箓', '瞬移', '道具'],
  },
  {
    name: '正神化身', alias: '道门降神之器', mark: '神', scroll: 'daomen',
    owner: '一气道盟道门', chapter: '全篇', kind: '召唤 · 攻伐', power: 3,
    brief: '道门法宝，可召唤出一尊体型巨大的神祇与敌对阵，威势浩大，正面交锋时尤为难挡。',
    tale: '与王权剑一脉的人间法器同源，皆是人类借法宝之力与妖相抗的凭据。',
    verse: '神影一现，天地为之俯首。', tags: ['召唤', '道门', '攻伐'],
  },
  {
    name: '无尽沙漏', alias: '袖里乾坤', mark: '沙', scroll: 'daomen',
    owner: '道门', chapter: '全篇', kind: '空间 · 收纳', power: 3,
    brief: '空间法宝，内藏约五公里的独立天地，可纳物、可藏身，威力强、用处极广。',
    tale: '与涂山无尽酒葫同为空间类法宝，一藏天地、一藏酒液，皆以「无尽」为名。',
    verse: '一粒沙中，另有一段光阴。', tags: ['空间法宝', '收纳', '道门'],
  },
  {
    name: '天罗地网', alias: '妖捕之器', mark: '网', scroll: 'daomen',
    owner: '律笺文', chapter: '颜如玉篇', kind: '擒缚 · 阵', power: 3,
    brief: '妖捕律笺文所持法宝，撒之成阵，专用于缉拿大妖，是人间捕妖一脉最倚重的束缚之器。',
    tale: '她以卧底之身接近颜如玉，最终却用智慧问出了那句续缘之约，此网终究没能网住自己的心。',
    verse: '网可缚妖，缚不住一眼动心。', tags: ['擒缚', '妖捕', '颜如玉篇'],
  },
  {
    name: '御妖子母符', alias: '御妖国禁制', mark: '御', scroll: 'daomen',
    owner: '御妖国', chapter: '北山篇', kind: '符箓 · 禁制', power: 3,
    brief: '御妖国先祖所创：子符令妖吞下，母符一毁则妖元尽毁；母符虽存，若不年年服解药亦痛楚难当。',
    tale: '正是这道符令万妖为奴，也酿成御妖国公主与北山妖帝石宽那段未能说尽的缘。',
    verse: '一符在手，万妖俯首；一心难求。', tags: ['禁制', '御妖国', '石宽'],
  },
  {
    name: '四象镜', alias: '式神之镜', mark: '镜', scroll: 'daomen',
    owner: '道门 / 金风岭', chapter: '全篇', kind: '召唤 · 式神', power: 3,
    brief: '可召唤朱雀、青龙等四象式神：朱雀擅火，青龙擅水，显现为性格鲜明的人形，召唤各有其代价。',
    tale: '式神出镜，代价却往往只是些出人意料的俗世琐事，是道门法宝里最不肃穆的一件。',
    verse: '镜开四象，各有其价。', tags: ['式神', '召唤', '道门'],
  },

  // ===== 西西域卷 · 下沙篇与尾生篇 =====
  {
    name: '千年御水珠', alias: '续缘之匙', mark: '珠', scroll: 'xixiyu',
    owner: '梵云飞 / 厉雪扬', chapter: '下沙篇', kind: '续缘 · 御水', power: 4,
    brief: '蓝色宝珠，存世千年以上，可在一定范围内御使水元素；本为专克沙狐一族的护身法宝。',
    tale: '梵云飞为救厉雪扬吞下此珠，从此成了不怕水的沙狐；后一分为二作二人再世续缘之匙，半颗竟化作转世军娘体内的胆结石。',
    verse: '一珠分两半，生死各执其一。', tags: ['续缘之匙', '御水', '下沙篇'],
  },
  {
    name: '冰凌雪枪', alias: '千年寒冰戈', mark: '冰', scroll: 'xixiyu',
    owner: '厉雪扬（冰将军）', chapter: '下沙篇', kind: '攻伐 · 冰', power: 4,
    brief: '冰将军厉雪扬的兵器，寒气极盛，遇水成冰，可冻住千年妖祸；漫画中称「千年寒冰戈」，动画作冰凌雪枪。',
    tale: '她曾持此枪孤身斩杀火妖，被梵云飞救下，才有了那第一百次求婚。',
    verse: '枪出成雪，一怒可封千里。', tags: ['冰系', '攻伐', '厉雪扬'],
  },
  {
    name: '七宝妙树皮', alias: '续缘之匙', mark: '木', scroll: 'xixiyu',
    owner: '月啼暇 / 胡尾生', chapter: '尾生篇', kind: '续缘 · 御木', power: 4,
    brief: '月啼族族长月啼暇的至宝，可操控一切植物；曾生长在胡尾生背后，使他一淋雨便化作树木。',
    tale: '尾生抱树而死的误会，正因月啼暇淋雨现出树形、不能移动；待二人误会解开、续缘功成，树皮方被收回。',
    verse: '抱柱之约，原是抱住了她。', tags: ['续缘之匙', '御木', '尾生篇'],
  },

  // ===== 南国卷 · 毒蛊之术 =====
  {
    name: '紫欢金铃', alias: '南国皇室至宝', mark: '铃', scroll: 'nanguo',
    owner: '欢都一族 / 欢都落兰', chapter: '南国篇', kind: '音波 · 毒', power: 4,
    brief: '南国皇室欢都一族的法宝，唯有炼成万毒之体者方可持有；配合音波毒功，可凝音波为震波针承载神经毒素，无形无质、百发百中。',
    tale: '后作为转世续缘的法宝，被见钱眼开的白裘恩顺手取走，成了南国篇里最令人哭笑不得的一笔。',
    verse: '铃响无声，毒已入神。', tags: ['万毒之体', '音波毒功', '续缘法宝'],
  },
  {
    name: '返雏毒针', alias: '返老还童之针', mark: '针', scroll: 'nanguo',
    owner: '欢都落兰', chapter: '南国篇', kind: '毒 · 返雏', power: 3,
    brief: '黑狐所予之毒与南国公主飞针相合而成。此针不向敌人，而是扎入自己体内，令毒液回流，使身体恢复至从前的状态。',
    tale: '以己身为炉，只为换回能与心上人相认的模样，是南国毒术里最温柔也最狠的一种。',
    verse: '针针向己，只为旧颜如初。', tags: ['毒针', '返雏', '落兰'],
  },
  {
    name: '情蛊', alias: '南国痴心蛊', mark: '蛊', scroll: 'nanguo',
    owner: '南国女子', chapter: '南国篇', kind: '蛊术 · 情', power: 3,
    brief: '南国姑娘求而不得时，择良地种下一枚蛊，日日以心血与妖气为饲，祈求能与心仪之人长相厮守。',
    tale: '落兰为平丘月初种下的，正是这一枚痴心；蛊由情生，也因情而难解。',
    verse: '心血饲蛊，只愿一人回头。', tags: ['蛊术', '痴情', '南国'],
  },
  {
    name: '紫星河水晶', alias: '南国洞顶之晶', mark: '晶', scroll: 'nanguo',
    owner: '南国', chapter: '南国篇', kind: '阵 · 阴气', power: 3,
    brief: '南国特产，体积庞大，镶嵌于洞穴顶部，通体透明泛紫。月光照射之下四周阴气大盛，配合黑狐之泪可强行抽出灵魂。',
    tale: '阴气与情蛊、毒功同为南国一脉的根基，也曾在平丘月初那一世搅动生死。',
    verse: '紫光满洞，月色亦可夺魂。', tags: ['阴气', '阵法', '黑狐之泪'],
  },
]

/* ---------- 每件法宝的专属插画 ----------
 * 图片为本地生成的古风矢量图，位于 public/treasures/<slug>.svg，
 * 无需外链、离线可用；新增法宝时在此补一条映射即可。
 */
const treasureImages = {
  '虚空之泪': 'xukong-zhilei', '纯爱天篇': 'chunai-tianpian', '忆梦锤': 'yimeng-chui',
  '无尽酒葫': 'wujin-jiuhu', '九转玄阴水': 'jiuzhuan-xuanyinshui', '绝缘之爪': 'jueyuan-zhizhua',
  '苦情树': 'kuqing-shu', '王权剑': 'wangquan-jian', '纯质阳炎': 'chunzhi-yangyan',
  '追踪符': 'zhuizong-fu', '正神化身': 'zhengshen-huashen', '无尽沙漏': 'wujin-shalou',
  '天罗地网': 'tianluo-diwang', '御妖子母符': 'yuyao-zimufu', '四象镜': 'sixiang-jing',
  '千年御水珠': 'qiannian-yushuizhu', '冰凌雪枪': 'bingling-xueqiang', '七宝妙树皮': 'qibao-miaoshupi',
  '紫欢金铃': 'zihuan-jinling', '返雏毒针': 'fanchu-duzhen', '情蛊': 'qinggu',
  '紫星河水晶': 'zixinghe-shuijing',
}
treasures.forEach((t) => { t.img = '/treasures/' + treasureImages[t.name] + '.svg' })
// 本地预置价格（与后端种子一致：22 件均匀铺在 2-25）
treasures.forEach((t, i) => { if (t.price == null) t.price = 2 + Math.round(i * 23 / 21) })

/** 法宝优先取数据库（后台可增删改+换图），后端不可用/为空时保留上面的本地预置 */
function normalizeTreasure(t) {
  return {
    ...t,
    tags: Array.isArray(t.tags) ? t.tags
      : String(t.tags || '').split(/[,，、]/).map((s) => s.trim()).filter(Boolean),
    power: Number(t.power) || 3,
    price: Number(t.price) || 5,
    img: t.img || t.image || `/treasures/${t.slug || 'placeholder'}.svg`,
  }
}
async function loadTreasures() {
  try {
    const r = await fetch('/api/treasures')
    const j = await r.json()
    if (j.code === 200 && Array.isArray(j.data) && j.data.length) {
      const rows = j.data.map(normalizeTreasure)
      // 原地替换，保持 treasures 的引用不变（模板与 computed 都直接引用它）
      treasures.splice(0, treasures.length, ...rows)
    }
  } catch (e) { /* 后端未启动：继续用本地预置数据 */ }
}

/* ---------- 法宝：分卷筛选 + 检索 + 展卷详情 ---------- */
const treasureScroll = ref('all')
const treasureKeyword = ref('')
const activeTreasure = ref(null)
const filteredTreasures = computed(() => {
  const k = treasureKeyword.value.trim()
  return treasures.filter((t) => {
    if (treasureScroll.value !== 'all' && t.scroll !== treasureScroll.value) return false
    if (!k) return true
    const hay = t.name + t.alias + t.owner + t.chapter + t.kind + t.brief + t.tale + t.verse +
                t.tags.join('') + scrollLabel(t.scroll)
    return hay.includes(k)
  })
})
function setScroll(key) { treasureScroll.value = key }
function openTreasure(t) { activeTreasure.value = t }
function closeTreasure() { activeTreasure.value = null }
function clearTreasureSearch() { treasureKeyword.value = '' }
function onEsc(e) {
  if (e.key === 'Escape') { closeTreasure(); closeCouple() }
}
/** 随缘请器：耗 10 金随机得一件展示中的法宝，收入法囊并展卷 */
function inviteTreasure() {
  if (gold.value < TREASURE_DRAW_COST) {
    showToast('金 币 不 足 · 请 先 领 金 再 请 器')
    return
  }
  const pool = treasures.filter((t) => t.enabled !== 0)
  if (!pool.length) { showToast('此 卷 暂 无 可 请 之 器'); return }
  if (!spendGold(TREASURE_DRAW_COST)) return
  const picked = pool[Math.floor(Math.random() * pool.length)]
  const isNew = addTreasure(picked)   // 已入囊的法宝不重复计数
  openTreasure(picked)
  showToast(isNew ? `✦ 请 得 · ${picked.name} 已 入 法 囊` : `✦ 再 遇 · ${picked.name}（法囊已有）`)
}

/* ---------- 苦情树 · 续缘相册 ----------
 * 每对情侣一条记录，image 为「预留图片位」：
 *   - 留空 → 展示 public/couples/<slug>.svg 的「图位以待」占位图
 *   - 后台把 image 改成 /couples/xxx.jpg、外链 URL 或 base64 data URL → 立刻换成真图
 *   - 图片加载失败 → 统一兜底到 /couples/placeholder.svg
 * 数据来源：优先 GET /api/couples（数据库 tb_couple，未来后台管理系统在此改图）；
 * 后端未启动 / 返回空 → 回退 public/data/couples.json，离线也能翻相册。
 */
const couples = ref([])
const coupleSource = ref('local')   // 'api' 数据库 | 'local' 本地预置
const coupleKeyword = ref('')
const activeCouple = ref(null)
const couplePage = ref(new URLSearchParams(window.location.search).get('page') === 'mine' ? 'mine' : 'fox')

/* ---------- 同心缘簿：用户投稿，全员共享 ---------- */
const myCouples = ref([])
const myCoupleForm = ref(emptyMyCoupleForm())
const myCoupleBusy = ref(false)
const myCoupleTip = ref('')
function emptyMyCoupleForm() {
  return { name: '', chapter: '', verse: '', image: '' }
}

/** 仅本人可删除自己上传的相片：投稿时记录的道号（用户名）一致才算本人 */
function canDeleteCouple(c) {
  const name = currentUsername()
  return !!c && !!name && name === c.contributor
}

/** 统一后端 / 本地两种数据形状，tags 兼容数组与逗号分隔字符串 */
function normalizeCouple(c) {
  return {
    ...c,
    id: c.id ?? c.slug,
    tags: Array.isArray(c.tags)
      ? c.tags
      : String(c.tags || '').split(/[,，、]/).map((s) => s.trim()).filter(Boolean),
    image: c.image || c.imageUrl || '',
  }
}

async function loadCouples() {
  try {
    const r = await fetch('/api/couples')
    const j = await r.json()
    if (j.code === 200 && Array.isArray(j.data) && j.data.length) {
      couples.value = j.data.map(normalizeCouple)
      coupleSource.value = 'api'
      return
    }
  } catch (e) { /* 后端未启动 / 表为空：走本地预置数据 */ }
  try {
    const r = await fetch('/data/couples.json')
    couples.value = (await r.json()).map(normalizeCouple)
    coupleSource.value = 'local'
  } catch (e) { couples.value = [] }
}

/** 用户投稿列表：存在数据库里，所有用户刷新后都能看到 */
async function loadMyCouples() {
  try {
    const r = await fetch('/api/user-couples')
    const j = await r.json()
    if (j.code === 200 && Array.isArray(j.data)) {
      myCouples.value = j.data.map(normalizeCouple)
      // 支持 ?couple=id 直接展开某一页，方便分享
      const cid = new URLSearchParams(window.location.search).get('couple')
      if (cid) {
        const hit = myCouples.value.find((c) => String(c.id) === cid)
        if (hit) activeCouple.value = hit
      }
    }
  } catch (e) { myCouples.value = [] }
}

/** 题名留册：提交一对自己喜爱的情侣，成功后立刻出现在共享相册 */
async function submitMyCouple() {
  if (myCoupleBusy.value) return
  const f = myCoupleForm.value
  if (!f.name.trim()) { myCoupleTip.value = '请先写下 cp 名～'; return }
  if (!f.image.trim()) { myCoupleTip.value = '请填上图片地址，相片不可留空～'; return }
  myCoupleBusy.value = true
  myCoupleTip.value = '正在题名留册…'
  try {
    const r = await fetch('/api/user-couples', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...f, contributor: displayName.value })
    })
    const j = await r.json()
    if (j.code === 200) {
      myCouples.value.unshift(normalizeCouple(j.data))
      myCoupleForm.value = emptyMyCoupleForm()
      myCoupleTip.value = '已留册，此后人人可见这一页～'
    } else {
      myCoupleTip.value = j.message || '留册未成，稍后再试罢～'
    }
  } catch (e) {
    myCoupleTip.value = '后台未启动，暂时无法留册～'
  } finally {
    myCoupleBusy.value = false
  }
}

/** 删掉自己题名的那一页：后端只认投稿道号，别人的相片删不了 */
async function removeMyCouple(c) {
  if (!canDeleteCouple(c)) return
  if (!window.confirm('确定移除「' + c.name + '」这一页吗？移除后所有人都看不到了。')) return
  try {
    const q = new URLSearchParams({ username: currentUsername() })
    const r = await fetch(`/api/user-couples/${encodeURIComponent(c.id)}?${q}`, { method: 'DELETE' })
    const j = await r.json()
    if (j.code === 200) {
      myCouples.value = myCouples.value.filter((x) => x.id !== c.id)
      if (activeCouple.value && String(activeCouple.value.id) === String(c.id)) activeCouple.value = null
      myCoupleTip.value = '已移除这一页～'
    } else {
      myCoupleTip.value = j.message || '移除未成，稍后再试罢～'
    }
  } catch (e) {
    myCoupleTip.value = '后台未启动，暂时无法移除～'
  }
}

const filteredCouples = computed(() => {
  const k = coupleKeyword.value.trim()
  return couples.value.filter((c) => {
    if (!k) return true
    const hay = [c.name, c.chapter, c.era, c.relic, c.status, c.brief, c.story, c.verse,
                 ...(c.tags || [])].join('')
    return hay.includes(k)
  })
})

/** 有图用图；官方无图用专属占位插画，用户投稿无图用通用占位插画 */
function coupleImage(c) {
  if (c.image) return c.image
  if (c.source === 'user') return '/couples/placeholder.svg'
  return `/couples/${c.slug}.svg`
}
function onCoupleImgError(e) {
  if (!e.target.src.endsWith('/couples/placeholder.svg')) {
    e.target.src = '/couples/placeholder.svg'
  }
}
function openCouple(c) { activeCouple.value = c }
function closeCouple() { activeCouple.value = null }
function clearCoupleSearch() { coupleKeyword.value = '' }

/* ---------- 个人空间：读取登录时存档的用户信息 ---------- */
const user = ref(null)
try { user.value = JSON.parse(localStorage.getItem('huyao_user') || 'null') } catch (e) { user.value = null }
/** 权限：0 管理员可见后台入口；其余（1 默认 / 老账号无此字段）均不可见 */
const isAdmin = computed(() => Number(user.value?.permission) === 0)
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

/* ---------- 后台管理入口：仅管理员可见，跳独立后台登录页 ---------- */
const ADMIN_TOKEN_KEY = 'huyao_admin_token'
function goAdmin() {
  if (!isAdmin.value) return
  router.push('/admin/login')
}

/* ---------- 退出 ---------- */
function handleLogout() {
  localStorage.removeItem('huyao_user')
  localStorage.removeItem(ADMIN_TOKEN_KEY)   // 同时清除后台会话
  router.push('/login')
}

onMounted(async () => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onEsc)   // Esc 收起法宝详情卷 / 相册展卷
  await syncGoldFromServer()                     // 先以数据库余额为准（后台改的金币即时生效）
  grantLoginGold()                               // 每日登录礼：当天首次进入自动 +20 金
  goldClock = setInterval(() => {                // 每秒刷新冷却倒计时，跨天自动重置并再发登录礼
    nowTs.value = Date.now()
    refreshGoldDay()
  }, 1000)
  syncAvatar()
  loadCouples()
  loadMyCouples()
  loadTreasures()                            // 法宝优先读数据库，后台改动即时生效
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onEsc)
  clearInterval(goldClock)
})
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

    <!-- ============ 顶部导航栏：首页 / 求签 / 签囊 / 法宝 / 苦情树 ============ -->
    <header class="navbar">
      <div class="nav-inner">
        <nav class="nav-links">
          <a class="nav-link" :class="{ active: activeTab === 'home' }"
             @click="switchTab('home')">首 页</a>
          <a class="nav-link" :class="{ active: activeTab === 'fortune' }"
             @click="switchTab('fortune')">求 签</a>
          <a class="nav-link" :class="{ active: activeTab === 'bag' }"
             @click="switchTab('bag')">签 囊<span v-if="ownedTotal" class="nav-badge">{{ ownedTotal }}</span></a>
          <a class="nav-link" :class="{ active: activeTab === 'treasure' }"
             @click="switchTab('treasure')">法 宝<span v-if="chestCount" class="nav-badge">{{ chestCount }}</span></a>
          <a class="nav-link" :class="{ active: activeTab === 'tree' }"
             @click="switchTab('tree')">苦 情 树</a>
          <button type="button" class="nav-gold" :class="{ active: activeTab === 'gold' }"
                  title="囊中金 · 点此进入领取金币页"
                  @click="switchTab('gold')">金 {{ gold }}</button>
          <button v-if="isAdmin" type="button" class="nav-admin"
                  title="后台管理系统 · 仅管理员可见"
                  @click="goAdmin">后 台</button>
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
            <div class="dropdown-gold">囊 中 金 <strong>{{ gold }}</strong></div>
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

      <!-- ============ 首页栏目：签文搜索 + 剧情故事线 ============ -->
      <!-- 搜索栏 -->
      <section v-if="activeTab === 'home'" class="search-bar">
        <span class="search-mark">寻</span>
        <input v-model="keyword" type="text" placeholder="输入心愿或签文关键字，寻一支签"
               @keyup.enter="doSearch" />
        <button v-if="keyword" class="btn-clear" title="清空" @click="clearSearch">×</button>
        <button class="btn-search" @click="doSearch">寻 签</button>
      </section>
      <p v-if="activeTab === 'home'" class="search-hint">可搜签种、签文、解签或签级，如「姻缘签」「上上签」「等待」</p>

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
        <p v-else class="result-empty">未寻得相关签文，换个字眼再寻寻罢～</p>
      </section>

      <!-- ============ 首页栏目：剧情故事线（取代原「前往求签」引导卡） ============ -->
      <section v-if="activeTab === 'home'" class="story-view">
        <div class="story-intro">
          <p class="story-intro-title">一 线 红 绳 · 串 起 前 尘</p>
          <p class="story-intro-text">自苦情树下的第一纸誓约，到牵出圈外的未竟之途。依《狐妖小红娘》漫画原作篇章顺序铺陈，点一处，便展开一段前尘。</p>
          <p class="story-stat">共 {{ chapters.length }} 章 · 缘起 / 下沙 / 尾生 / 王权 / 月红 / 千颜 / 南国 / 竹业 / 金晨曦 / 圈外</p>
          <div class="story-links">
            <button class="link-guide story-side-link" type="button"
                    @click="switchTab('treasure')">观 法 宝 图 鉴 →</button>
            <button class="link-guide story-side-link" type="button"
                    @click="switchTab('tree')">上 苦 情 树 翻 相 册 →</button>
          </div>
        </div>

        <div class="story-timeline">
          <div v-for="(c, i) in chapters" :key="c.no" class="story-node"
               :class="{ opened: openChapter === i }">
            <div class="story-dot">
              <span class="story-dot-mark">{{ c.mark }}</span>
              <span class="story-dot-no">{{ c.no }}</span>
            </div>
            <article class="story-card" tabindex="0" role="button"
                     :aria-expanded="openChapter === i" :aria-label="'展开' + c.name + '剧情'"
                     @click="toggleChapter(i)" @keyup.enter="toggleChapter(i)">
              <div class="story-head">
                <div class="story-heading">
                  <h3 class="story-name">{{ c.name }}</h3>
                  <span class="story-alias">{{ c.alias }}</span>
                </div>
                <span class="story-era" :class="'era-' + c.eraType">{{ c.era }}</span>
              </div>
              <p class="story-couple"><span>主</span>{{ c.couple }}</p>
              <p class="story-brief">{{ c.brief }}</p>

              <transition name="story">
                <div v-if="openChapter === i" class="story-detail">
                  <div class="divider small"><span>❀</span></div>
                  <h4>前 尘 细 说</h4>
                  <p class="story-text">{{ c.story }}</p>
                  <p class="story-verse">「{{ c.verse }}」</p>
                  <div class="story-tags">
                    <span class="story-relic">器 · {{ c.relic }}</span>
                    <span v-for="(tag, ti) in c.tags" :key="ti">{{ tag }}</span>
                  </div>
                </div>
              </transition>

              <div class="story-foot">
                <span class="story-relic-mini">器 · {{ c.relic }}</span>
                <span class="story-toggle">{{ openChapter === i ? '收 卷 ↑' : '展 卷 ↓' }}</span>
              </div>
            </article>
          </div>
          <p class="story-end">红 线 未 断 · 故 事 仍 在 继 续</p>
        </div>
      </section>

      <!-- ============ 求签栏目：签筒摇签 + 签文结果（由首页迁入） ============ -->
      <section v-if="activeTab === 'fortune'" class="card qiuqian-card">
        <span class="corner tl"/><span class="corner tr"/>
        <span class="corner br"/><span class="corner bl"/>

        <h2 class="card-title">求 签</h2>
        <p class="card-sub">择一类签 · 摇签问前尘 · 每签耗 {{ DRAW_COST }} 金</p>
        <div class="divider small"><span>❀</span></div>

        <p v-if="repeatStreak > 0" class="pity-tip">已 连 续 {{ repeatStreak }} / 2 次 拈 得 已 入 囊 之 签 · 满 两 次 则 下 一 签 必 得 未 得 之 签</p>

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
            <div class="sign-actions">
              <button type="button" class="btn-collect" :class="{ collected: isCollected(current) }"
                      @click="toggleCollect(current)">
                {{ isCollected(current) ? '✦ 已 入 签 囊 · 取 出' : '重 收 入 囊' }}
              </button>
              <button type="button" class="btn-collect ghost" @click="switchTab('bag')">前 往 签 囊 →</button>
            </div>
            <p class="sign-bag-tip">{{ isCollected(current) ? '此签已入囊 · 可前往签囊翻看，或点上方取出' : '取出后，可再点「重收入囊」放回' }}</p>
          </div>
        </transition>

        <button class="btn-draw" :disabled="drawing" @click="drawSign">
          {{ drawing ? '摇 签 中…' : (!selectedType ? '请 先 择 签 种' : (gold < DRAW_COST ? '金 不 足 · 领 金 后 再 拈' : (current ? '再 拈 一 签 · 耗 ' + DRAW_COST + ' 金' : '拈 签 问 缘 · 耗 ' + DRAW_COST + ' 金'))) }}
        </button>
      </section>

      <!-- ============ 金币栏目：领取金币页（由导航「金」进入） ============ -->
      <section v-if="activeTab === 'gold'" class="gold-view">
        <div class="gold-intro">
          <p class="gold-intro-title">金 囊 有 余 · 拈 签 不 忧</p>
          <p class="gold-intro-text">每日登录奉上 {{ GOLD_LOGIN }} 金；此后每隔 3 分钟可再领 {{ GOLD_TICK }} 金，每日至多 {{ GOLD_TICK_LIMIT }} 次。拈签一次耗金 {{ DRAW_COST }} 枚，签可重出，但连得两回旧签后，下一签必出新得。</p>
        </div>

        <div class="gold-balance">
          <div class="gold-balance-seal">金</div>
          <div class="gold-balance-main">
            <p class="gold-balance-label">囊 中 金</p>
            <strong class="gold-balance-num">{{ gold }}</strong>
            <p class="gold-balance-sub">足 供 {{ gold }} 次 拈 签</p>
          </div>
          <div class="gold-claim">
            <button type="button" class="btn-gold"
                    :disabled="goldCooldown > 0 || goldState.claimCount >= GOLD_TICK_LIMIT"
                    @click="claimTickGold">
              {{ goldCooldown > 0 ? '冷 却 中 · ' + fmtCountdown(goldCooldown) : (goldState.claimCount >= GOLD_TICK_LIMIT ? '今 日 已 领 满' : '领 取 +' + GOLD_TICK + ' 金') }}
            </button>
            <p class="gold-claim-tip">今 日 已 领 {{ goldState.claimCount }} / {{ GOLD_TICK_LIMIT }} 次 · 每 3 分 钟 一 次</p>
          </div>
        </div>

        <div class="gold-rule-grid">
          <article class="gold-rule-card">
            <span class="gold-rule-mark">日</span>
            <h3>每 日 登 录</h3>
            <p class="gold-rule-value">+{{ GOLD_LOGIN }} 金</p>
            <span class="gold-rule-state done">今 日 已 入 囊</span>
          </article>
          <article class="gold-rule-card">
            <span class="gold-rule-mark">时</span>
            <h3>每 3 分 钟</h3>
            <p class="gold-rule-value">+{{ GOLD_TICK }} 金</p>
            <span class="gold-rule-state" :class="{ done: goldState.claimCount >= GOLD_TICK_LIMIT }">今 日 {{ goldState.claimCount }} / {{ GOLD_TICK_LIMIT }} 次</span>
          </article>
          <article class="gold-rule-card">
            <span class="gold-rule-mark">签</span>
            <h3>拈 签 消 耗</h3>
            <p class="gold-rule-value">-{{ DRAW_COST }} 金 / 次</p>
            <span class="gold-rule-state">连 得 两 旧 签 · 下 签 必 新</span>
          </article>
          <article class="gold-rule-card">
            <span class="gold-rule-mark">器</span>
            <h3>随 缘 请 器</h3>
            <p class="gold-rule-value">-{{ TREASURE_DRAW_COST }} 金 / 次</p>
            <span class="gold-rule-state">集 齐 一 种 签 · 另 赐 三 器</span>
          </article>
        </div>

        <div class="gold-detail">
          <h2 class="gold-detail-title">领 金 章 程</h2>
          <ol>
            <li>每日首次进入本阁，自动奉上 {{ GOLD_LOGIN }} 金，当日不重复发放。</li>
            <li>每 3 分钟可领 +{{ GOLD_TICK }} 金，每日至多 {{ GOLD_TICK_LIMIT }} 次，冷却见上方按钮。</li>
            <li>跨天保留剩余金币，领取次数归零，并可再领当日登录礼。</li>
            <li>拈签消耗 {{ DRAW_COST }} 金；金币不足时不可拈签，先来此页领取即可。</li>
            <li>法宝页「随缘请器」耗 {{ TREASURE_DRAW_COST }} 金随机得一件法宝；集齐一种签的 {{ signs.filter(s => s.type === signTypes[0].key).length }} 支，另赐 {{ SIGN_SET_REWARD }} 件法宝，每种签只赐一次。</li>
          </ol>
        </div>

        <div class="bag-foot-actions">
          <button type="button" class="btn-back" @click="switchTab('fortune')">前 去 求 签</button>
          <button type="button" class="btn-back" @click="switchTab('home')">返 回 首 页</button>
        </div>
      </section>

      <!-- ============ 签囊栏目：收藏签背包 ============ -->
      <section v-if="activeTab === 'bag'" class="bag-view">
        <div class="bag-intro">
          <p class="bag-intro-title">一 囊 藏 签 · 尽 是 相 逢</p>
          <p class="bag-intro-text">拈得之签自动收入签囊，亦可取出。三类签各 6 支、三级签各 6 支，共 18 支；已得者展卷可阅，未得者留一席虚位，待来日再拈。</p>
          <p class="bag-intro-stat">签 囊 已 收 {{ ownedTotal }} / {{ signs.length }} 支 · 唯 上 上 签 / 上 签 / 中 签</p>
        </div>

        <!-- 收集进度：总数 + 按签种 / 按签级 -->
        <div class="bag-progress">
          <div class="bag-progress-head"><span>收 集 进 度</span><strong>{{ ownedTotal }} / {{ signs.length }}</strong></div>
          <div class="bag-progress-track"><div class="bag-progress-fill" :style="{ width: bagPercent + '%' }"></div></div>
          <div class="bag-stat-grid">
            <div class="bag-stat-box">
              <p class="bag-stat-label">按 签 种 · 每 类 6 支</p>
              <div v-for="t in typeStats" :key="t.name" class="bag-stat-row">
                <span class="bag-stat-icon">{{ t.icon }}</span>
                <span class="bag-stat-name">{{ t.name }}</span>
                <span class="bag-stat-num" :class="{ done: t.got === t.total }">{{ t.got }} / {{ t.total }}</span>
              </div>
            </div>
            <div class="bag-stat-box">
              <p class="bag-stat-label">按 签 级 · 每 级 6 支</p>
              <div v-for="l in levelStats" :key="l.name" class="bag-stat-row">
                <span class="bag-stat-icon">签</span>
                <span class="bag-stat-name">{{ l.name }}</span>
                <span class="bag-stat-num" :class="{ done: l.got === l.total }">{{ l.got }} / {{ l.total }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 筛选 -->
        <div class="bag-tools">
          <div class="bag-filter-row">
            <span class="bag-filter-label">签 种</span>
            <button v-for="t in bagTypes" :key="t" type="button"
                    class="bag-filter" :class="{ on: bagType === t }" @click="setBagType(t)">{{ t }}</button>
          </div>
          <div class="bag-filter-row">
            <span class="bag-filter-label">签 级</span>
            <button v-for="l in bagLevels" :key="l" type="button"
                    class="bag-filter" :class="{ on: bagLevel === l }" @click="setBagLevel(l)">{{ l }}</button>
            <button type="button" class="bag-filter toggle" :class="{ on: bagOnlyOwned }"
                    @click="toggleBagOwned">仅 看 已 得</button>
          </div>
          <p class="bag-count">此 筛 选 下 共 {{ bagList.length }} 席 · 已 得 {{ bagOwnedInView }} 支</p>
        </div>

        <!-- 18 席签位 -->
        <div v-if="bagList.length" class="bag-grid">
          <div v-for="s in bagList" :key="s.id" class="bag-card"
               :class="{ owned: isCollected(s), missing: !isCollected(s) }">
            <template v-if="isCollected(s)">
              <div class="bag-card-head">
                <span class="bag-card-type">{{ s.type }}</span>
                <span class="bag-card-level" :class="levelClass(s.level)">{{ s.level }}</span>
              </div>
              <p class="bag-card-poem">{{ s.poem }}</p>
              <p class="bag-card-note">解曰：{{ s.note }}</p>
              <div class="bag-card-foot">
                <span>得于 {{ collectedAt(s) }}</span>
                <button type="button" class="bag-card-btn" @click="toggleCollect(s)">取 出</button>
              </div>
            </template>
            <template v-else>
              <div class="bag-card-head">
                <span class="bag-card-type">{{ s.type }}</span>
                <span class="bag-card-level locked">{{ s.level }}</span>
              </div>
              <div class="bag-lock">未 得</div>
              <p class="bag-card-hint">此 席 尚 空 · 待 一 支 有 缘 之 签</p>
              <button type="button" class="bag-card-btn ghost" @click="goDrawFor(s.type)">去 求 {{ s.type }} →</button>
            </template>
          </div>
        </div>
        <p v-else class="bag-empty">此 筛 选 下 尚 无 已 得 之 签 · 且 去 求 签 栏 目 拈 一 支 罢</p>

        <div class="bag-foot-actions">
          <button type="button" class="btn-back" @click="switchTab('fortune')">前 去 求 签</button>
          <button type="button" class="btn-back" @click="switchTab('home')">返 回 首 页</button>
        </div>
      </section>

      <!-- ============ 法宝栏目：《狐妖小红娘》法宝图鉴 ============ -->
      <section v-if="activeTab === 'treasure'" class="treasure-view">
        <div class="treasure-intro">
          <p class="treasure-intro-title">万 器 有 灵 · 皆 因 情 起</p>
          <p class="treasure-intro-text">自苦情树下的一纸誓约，到南国洞中的一枚情蛊。所录皆出自《狐妖小红娘》原作设定，择器展卷，可窥其主、其篇、其缘。</p>
          <p class="treasure-stat">共录 {{ treasures.length }} 器 · 分 {{ scrolls.length - 1 }} 卷 · 涂山 / 道门 / 西西域 / 南国</p>
        </div>

        <!-- 检索：输入关键字 + 按卷筛选 -->
        <div class="treasure-tools">
          <div class="treasure-search">
            <span class="treasure-search-mark">觅</span>
            <input v-model="treasureKeyword" type="text" placeholder="寻器名、持有者、篇章或效用" />
            <button v-if="treasureKeyword" class="treasure-clear" type="button" title="清空"
                    @click="clearTreasureSearch">×</button>
          </div>
          <div class="scroll-tabs">
            <button v-for="s in scrolls" :key="s.key" type="button"
                    class="scroll-tab" :class="{ chosen: treasureScroll === s.key }"
                    @click="setScroll(s.key)">{{ s.label }}</button>
          </div>
          <div class="treasure-actions">
            <button class="btn-invite" type="button" @click="inviteTreasure"
                    :disabled="gold < TREASURE_DRAW_COST">
              {{ gold < TREASURE_DRAW_COST ? '金 不 足 · 领 金 后 请 器' : '随 缘 请 器 · ' + TREASURE_DRAW_COST + ' 金' }} <span>✦</span>
            </button>
            <span class="treasure-actions-tip">耗 {{ TREASURE_DRAW_COST }} 金随机得一件法宝 · 集齐一种签另赐三件</span>
          </div>
        </div>

        <!-- 法囊：所得法宝收藏 -->
        <div class="chest-panel">
          <div class="chest-head">
            <span class="chest-seal">囊</span>
            <span class="chest-title">我 的 法 囊</span>
            <strong class="chest-num">已 收 {{ chestCount }} / {{ treasures.length }} 器</strong>
          </div>
          <p class="chest-tip">随缘请器或集齐签种所得法宝，尽收此囊 · 集齐 {{ signTypes.length }} 种签各可得 {{ SIGN_SET_REWARD }} 件</p>
          <div v-if="chestList.length" class="chest-items">
            <button v-for="t in chestList" :key="t.slug" type="button" class="chest-item"
                    :title="t.name + ' · 价 ' + t.price + ' 金'" @click="openTreasure(t)">
              <img :src="t.img" :alt="t.name" />
              <span>{{ t.name }}</span>
            </button>
          </div>
          <p v-else class="chest-empty">法囊尚空 · 随缘请器，或集齐一种签，即可得法宝</p>
          <div class="chest-rewards">
            <span v-for="t in signTypes" :key="t.key" class="chest-reward-tag"
                  :class="{ done: !!rewarded[t.key] }">
              {{ t.icon }} {{ t.name }}{{ rewarded[t.key] ? ' · 已赐' : ' · 集齐赐' + SIGN_SET_REWARD + '器' }}
            </span>
          </div>
        </div>

        <p class="treasure-count">此卷共 {{ filteredTreasures.length }} 器</p>

        <!-- 法宝卡片：点击 / 回车展开详情卷 -->
        <div v-if="filteredTreasures.length" class="treasure-grid">
          <article v-for="t in filteredTreasures" :key="t.name" class="treasure-card"
                   tabindex="0" role="button" :aria-label="'查看' + t.name + '详情'"
                   @click="openTreasure(t)" @keyup.enter="openTreasure(t)">
            <img class="treasure-figure" :src="t.img" :alt="t.name + ' · 法宝图'" loading="lazy" />
            <div class="treasure-head">
              <span class="treasure-seal">{{ t.mark }}</span>
              <div class="treasure-namebox">
                <h3 class="treasure-name">{{ t.name }}</h3>
                <p class="treasure-alias">{{ t.alias }}</p>
              </div>
              <span class="treasure-stars">{{ stars(t.power) }}</span>
            </div>
            <p class="treasure-brief">{{ t.brief }}</p>
            <div class="treasure-meta">
              <span class="meta-scroll">{{ scrollLabel(t.scroll) }}</span>
              <span class="meta-chapter">{{ t.chapter }}</span>
              <span class="meta-kind">{{ t.kind }}</span>
            </div>
            <div class="treasure-foot">
              <span class="treasure-owner">主 · {{ t.owner }}</span>
              <span class="treasure-price">✦ {{ t.price }} 金</span>
              <span class="treasure-more">展 卷 →</span>
            </div>
          </article>
        </div>
        <p v-else class="treasure-empty">未寻得此器，换一卷或另觅他名罢～</p>
      </section>

      <!-- ============ 苦情树栏目：续缘相册（原作 / 用户投稿两页） ============ -->
      <section v-if="activeTab === 'tree'" class="tree-view">
        <div class="tree-intro">
          <p class="tree-intro-title">树 下 千 缘 · 一 册 相 思</p>
          <p class="tree-intro-text">人与妖相恋，人一转世便忘尽前尘。凡在苦情树下许过愿的，都在此册留了一格相片；你心上那一对，也可以在此题名留册。</p>
        </div>

        <!-- 两页切换：涂山情谱 / 同心缘簿（替代原随缘翻页） -->
        <div class="tree-tools">
          <div class="tree-pages">
            <button class="tree-page-btn" type="button"
                    :class="{ chosen: couplePage === 'fox' }"
                    @click="couplePage = 'fox'">涂 山 情 谱</button>
            <button class="tree-page-btn" type="button"
                    :class="{ chosen: couplePage === 'mine' }"
                    @click="couplePage = 'mine'">同 心 缘 簿</button>
          </div>
        </div>

        <!-- 第一页：原作中的小情侣 -->
        <template v-if="couplePage === 'fox'">
        <div class="tree-search tree-search-page">
          <span class="tree-search-mark">寻</span>
          <input v-model="coupleKeyword" type="text" placeholder="寻人名、篇章、信物或签语" />
          <button v-if="coupleKeyword" class="treasure-clear" type="button" title="清空"
                  @click="clearCoupleSearch">×</button>
        </div>

        <p class="tree-count">此册共 {{ filteredCouples.length }} 页 · 数据来源：{{ coupleSource === 'api' ? '后台数据库' : '本地预置' }}</p>

        <!-- 相册卡片：预留图片位；后台把 image 改成真图即自动替换 -->
        <div v-if="filteredCouples.length" class="tree-grid">
          <article v-for="c in filteredCouples" :key="c.id" class="tree-card"
                   tabindex="0" role="button" :aria-label="'翻看' + c.name + '这一页'"
                   @click="openCouple(c)" @keyup.enter="openCouple(c)">
            <span class="tree-tape"></span>
            <span class="tree-seal">{{ c.mark }}</span>
            <div class="tree-photo-wrap">
              <img class="tree-photo" :src="coupleImage(c)" :alt="c.name + ' · 相册图位'"
                   loading="lazy" @error="onCoupleImgError" />
              <span v-if="!c.image" class="tree-photo-tag">图 位 以 待</span>
            </div>
            <div class="tree-caption">
              <h3 class="tree-names">{{ c.name }}</h3>
              <p class="tree-chapter">{{ c.chapter }} · {{ c.era }}</p>
              <p class="tree-brief">{{ c.brief }}</p>
              <div class="tree-card-foot">
                <span class="tree-relic">器 · {{ c.relic }}</span>
                <span class="tree-more">展 卷 →</span>
              </div>
            </div>
          </article>
        </div>
        <p v-else class="tree-empty">此册未寻得这一页，换个字眼或另择一卷罢～</p>
        </template>

        <!-- 第二页：同心缘簿（用户投稿，全员共享） -->
        <template v-else>
          <div class="my-form-card">
            <p class="my-form-title">题 名 留 册</p>
            <p class="my-form-sub">不论出自哪部故事，只要你喜欢，便可在此留一格相片</p>
            <form @submit.prevent="submitMyCouple">
              <div class="my-form-grid">
                <label class="my-field">
                  <span>cp名 <i>*</i></span>
                  <input v-model="myCoupleForm.name" type="text" maxlength="128" required />
                </label>
                <label class="my-field">
                  <span>出 处</span>
                  <input v-model="myCoupleForm.chapter" type="text" maxlength="128"
                         placeholder="如：某某小说 / 自己的故事" />
                </label>
                <label class="my-field wide">
                  <span>题 句</span>
                  <input v-model="myCoupleForm.verse" type="text" maxlength="500"
                         placeholder="留一句话给他们" />
                </label>
                <label class="my-field wide">
                  <span>图 片 地 址 <i>*</i></span>
                  <input v-model="myCoupleForm.image" type="text" required
                         placeholder="/couples/xxx.jpg 或 http(s) 图片链接" />
                </label>
              </div>
              <div class="my-form-foot">
                <button class="btn-my-submit" type="submit" :disabled="myCoupleBusy">
                  {{ myCoupleBusy ? '留 册 中…' : '题 名 留 册' }}
                </button>
                <span class="my-form-tip">{{ myCoupleTip || '图片地址必填，留册后所有有缘人都能看到，仅自己可删' }}</span>
              </div>
            </form>
          </div>

          <p class="tree-count">此册共 {{ myCouples.length }} 页 · 来自各位有缘人的投稿</p>
          <div v-if="myCouples.length" class="tree-grid">
            <article v-for="c in myCouples" :key="c.id" class="tree-card"
                     tabindex="0" role="button" :aria-label="'翻看' + c.name + '这一页'"
                     @click="openCouple(c)" @keyup.enter="openCouple(c)">
              <span class="tree-tape"></span>
              <button v-if="canDeleteCouple(c)" class="btn-delete-couple" type="button"
                      title="删除这一页" :aria-label="'删除' + c.name"
                      @click.stop="removeMyCouple(c)">删</button>
              <div class="tree-photo-wrap">
                <img class="tree-photo" :src="coupleImage(c)" :alt="c.name + ' · 相册图位'"
                     loading="lazy" @error="onCoupleImgError" />
                <span v-if="!c.image" class="tree-photo-tag">图 位 以 待</span>
              </div>
              <div class="tree-caption">
                <h3 class="tree-names">{{ c.name }}</h3>
                <p class="tree-chapter">{{ c.chapter }} · {{ c.createTime }}</p>
                <div class="tree-card-foot">
                  <span></span>
                  <span class="tree-more">展 卷 →</span>
                </div>
                <p class="tree-contributor">题 名 · {{ c.contributor }}</p>
              </div>
            </article>
          </div>
          <p v-else class="tree-empty">此册还空着，第一对由你来题名罢～</p>
        </template>
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
          <p v-else class="profile-sign-empty">尚未求得签文，且回首页看看前尘故事罢～</p>
          <div class="profile-bag-line">
            <span>签 囊 已 收 {{ ownedTotal }} / {{ signs.length }} 支</span>
            <button type="button" class="link-guide" @click="switchTab('bag')">翻 看 签 囊 →</button>
          </div>
        </div>

        <button class="btn-back" type="button" @click="switchTab('home')">返 回 首 页</button>
      </section>
    </main>

    <!-- ============ 法宝详情：展卷（Esc / 点击遮罩 / 收卷按钮可关） ============ -->
    <transition name="dialog">
      <div v-if="activeTreasure" class="treasure-mask" @click.self="closeTreasure">
        <div class="treasure-dialog" role="dialog" aria-modal="true" :aria-label="activeTreasure.name">
          <button class="dialog-close" type="button" title="收卷" aria-label="收卷"
                  @click="closeTreasure">×</button>
          <img class="dialog-figure" :src="activeTreasure.img" :alt="activeTreasure.name + ' · 法宝图'" />
          <div class="dialog-head">
            <span class="dialog-seal">{{ activeTreasure.mark }}</span>
            <div class="dialog-namebox">
              <h2 class="dialog-name">{{ activeTreasure.name }}</h2>
              <p class="dialog-alias">{{ activeTreasure.alias }}</p>
            </div>
          </div>
          <div class="divider small"><span>❀</span></div>

          <div class="dialog-grid">
            <div class="dialog-field"><span>所 属</span><strong>{{ scrollLabel(activeTreasure.scroll) }}</strong></div>
            <div class="dialog-field"><span>持 有 者</span><strong>{{ activeTreasure.owner }}</strong></div>
            <div class="dialog-field"><span>出 处</span><strong>{{ activeTreasure.chapter }}</strong></div>
            <div class="dialog-field"><span>类 别</span><strong>{{ activeTreasure.kind }}</strong></div>
          </div>
          <p class="dialog-power">灵 力 <span>{{ stars(activeTreasure.power) }}</span>　·　估 价 <span>✦ {{ activeTreasure.price }} 金</span></p>

          <div class="dialog-section"><h4>器 之 用</h4><p>{{ activeTreasure.brief }}</p></div>
          <div class="dialog-section"><h4>缘 之 由</h4><p>{{ activeTreasure.tale }}</p></div>
          <p class="dialog-verse">「{{ activeTreasure.verse }}」</p>
          <div class="dialog-tags"><span v-for="(tag, i) in activeTreasure.tags" :key="i">{{ tag }}</span></div>
          <button class="dialog-back" type="button" @click="closeTreasure">收 卷</button>
        </div>
      </div>
    </transition>

    <!-- ============ 苦情树相册：展卷（Esc / 点击遮罩 / 收卷按钮可关） ============ -->
    <transition name="dialog">
      <div v-if="activeCouple" class="tree-mask" @click.self="closeCouple">
        <div class="tree-dialog" role="dialog" aria-modal="true" :aria-label="activeCouple.name">
          <button class="dialog-close" type="button" title="收卷" aria-label="收卷"
                  @click="closeCouple">×</button>
          <div class="tree-dialog-photo">
            <img :src="coupleImage(activeCouple)" :alt="activeCouple.name + ' · 相册图位'"
                 @error="onCoupleImgError" />
            <span v-if="!activeCouple.image" class="tree-dialog-photo-tip">此 间 留 白 · 待 补 丹 青</span>
          </div>
          <div class="dialog-head">
            <span v-if="activeCouple.source !== 'user'" class="dialog-seal">{{ activeCouple.mark }}</span>
            <div class="dialog-namebox">
              <h2 class="dialog-name">{{ activeCouple.name }}</h2>
              <p class="dialog-alias">
                {{ activeCouple.source === 'user' ? activeCouple.chapter : activeCouple.chapter + ' · ' + activeCouple.status }}
              </p>
            </div>
          </div>
          <div class="divider small"><span>❀</span></div>

          <template v-if="activeCouple.source !== 'user'">
            <div class="dialog-grid">
              <div class="dialog-field"><span>篇 章</span><strong>{{ activeCouple.chapter }}</strong></div>
              <div class="dialog-field"><span>时 代</span><strong>{{ activeCouple.era }}</strong></div>
              <div class="dialog-field"><span>续 缘 信 物</span><strong>{{ activeCouple.relic }}</strong></div>
              <div class="dialog-field"><span>缘 分</span><strong>{{ activeCouple.status }}</strong></div>
            </div>
            <div class="dialog-section"><h4>前 尘 细 说</h4><p>{{ activeCouple.story }}</p></div>
          </template>
          <p class="dialog-verse">「{{ activeCouple.verse }}」</p>
          <div v-if="activeCouple.tags && activeCouple.tags.length" class="dialog-tags">
            <span v-for="(tag, i) in activeCouple.tags" :key="i">{{ tag }}</span>
          </div>
          <p v-if="activeCouple.source === 'user'" class="tree-dialog-hint">
            这 一 页 由 {{ activeCouple.contributor }} 题 名 留 册 · {{ activeCouple.createTime }}
          </p>
          <p v-else class="tree-dialog-hint">预留图位 {{ activeCouple.imageSlot || '/couples/' + activeCouple.slug + '.jpg' }} · 后台改 image 即换图</p>
          <button v-if="canDeleteCouple(activeCouple)" class="btn-delete-couple dialog-delete" type="button"
                  @click="removeMyCouple(activeCouple)">删 除 这 一 页</button>
          <button class="dialog-back" type="button" @click="closeCouple">收 卷</button>
        </div>
      </div>
    </transition>
    <!-- 全局提示条：签到、领金、抽签保底等 -->
    <transition name="toast"><div v-if="bagToast" class="bag-toast">{{ bagToast }}</div></transition>
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

/* ============ 顶部导航栏：首页 / 求签 / 签囊 / 金 / 法宝 / 苦情树 ============ */
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

/* ============ 首页：剧情故事线（红线时间轴 + 可展卷卡片） ============ */
.story-view { max-width: 700px; margin: 34px auto 0; text-align: left; }
.story-intro {
  padding: 18px 24px 20px; text-align: center;
  background: rgba(250, 245, 234, .4); backdrop-filter: blur(2px);
  border: 1px dashed rgba(184,146,74,.7); border-radius: 14px;
}
.story-intro-title { font-size: 18px; letter-spacing: 4px; color: #8e2c2c; }
.story-intro-text { margin-top: 8px; font-size: 13px; letter-spacing: 1px; line-height: 1.9; color: #33403e; }
.story-stat { margin-top: 8px; font-size: 11.5px; letter-spacing: 1px; line-height: 1.8; color: #8e2c2c; }
.link-guide {
  display: inline-block; margin-left: 8px; padding: 0;
  font-family: inherit; font-size: 13px; letter-spacing: 1px;
  color: #8e2c2c; background: transparent; border: none;
  border-bottom: 1px solid #b03a3a; cursor: pointer;
}
.link-guide:hover { color: #b03a3a; }
.link-guide.story-side-link { margin: 10px 0 0; font-size: 12px; }

.story-timeline { position: relative; margin-top: 26px; padding-left: 46px; }
.story-timeline::before {
  content: ""; position: absolute; left: 18px; top: 6px; bottom: 24px; width: 2px;
  background: linear-gradient(180deg, rgba(176,58,58,0), #b03a3a 8%, #b03a3a 88%, rgba(176,58,58,0));
  opacity: .55;
}
.story-node { position: relative; margin-bottom: 16px; }
.story-dot {
  position: absolute; left: -46px; top: 14px; width: 38px; height: 38px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #f5efe3; background: #b03a3a;
  border: 1px solid #b8924a; border-radius: 50%;
  box-shadow: 0 0 0 4px rgba(250,245,234,.75), 0 3px 10px rgba(142,44,44,.28);
  transition: transform .25s ease, background .25s ease;
}
.story-dot-mark { font-size: 15px; line-height: 1.1; }
.story-dot-no { font-size: 9px; line-height: 1.2; opacity: .9; }
.story-node.opened .story-dot { background: #8e2c2c; transform: scale(1.08); }

.story-card {
  padding: 16px 20px 14px; cursor: pointer;
  background: rgba(250, 245, 234, .52); backdrop-filter: blur(2px);
  border: 1.5px solid rgba(184,146,74,.55); border-radius: 14px;
  box-shadow: 0 5px 18px rgba(74,59,50,.09);
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
}
.story-card:hover, .story-card:focus-visible {
  transform: translateX(3px); outline: none; border-color: #b03a3a;
  box-shadow: 0 10px 26px rgba(142,44,44,.16);
}
.story-node.opened .story-card { border-color: #b03a3a; box-shadow: 0 10px 30px rgba(142,44,44,.18); }
.story-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.story-heading { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.story-name { font-weight: normal; font-size: 20px; letter-spacing: 2px; color: #8e2c2c; }
.story-alias { font-size: 11px; letter-spacing: 1px; color: #33403e; }
.story-era { flex: none; padding: 3px 9px; font-size: 11px; letter-spacing: 1px; border-radius: 6px; }
.era-origin { color: #f5efe3; background: #b8924a; }
.era-past { color: #8e2c2c; background: rgba(184,146,74,.2); border: 1px solid rgba(184,146,74,.6); }
.era-now { color: #33403e; background: rgba(255,253,246,.7); border: 1px solid rgba(184,146,74,.45); }
.story-couple { margin-top: 8px; font-size: 12px; letter-spacing: 1px; color: #33403e; }
.story-couple span {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; margin-right: 8px;
  font-size: 11px; color: #f5efe3; background: #b03a3a; border-radius: 4px;
}
.story-brief { margin-top: 8px; font-size: 13px; line-height: 1.9; letter-spacing: .5px; color: #4a3b32; }
.story-detail { margin-top: 4px; }
.story-detail .divider { width: 180px; margin: 8px auto 12px; }
.story-detail h4 {
  font-weight: normal; font-size: 13px; letter-spacing: 3px;
  color: #8e2c2c; border-left: 3px solid #b03a3a; padding-left: 10px;
}
.story-text { margin-top: 10px; font-size: 13px; line-height: 2; letter-spacing: .5px; color: #4a3b32; }
.story-verse { margin-top: 14px; text-align: center; font-size: 15px; letter-spacing: 2px; color: #8e2c2c; }
.story-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 14px; }
.story-tags span {
  padding: 3px 9px; font-size: 11px; letter-spacing: .5px; color: #33403e;
  background: rgba(255,253,246,.65); border: 1px solid rgba(184,146,74,.45); border-radius: 12px;
}
.story-tags .story-relic { color: #8e2c2c; background: rgba(184,146,74,.16); border-color: rgba(184,146,74,.6); }
.story-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  margin-top: 12px; padding-top: 10px; border-top: 1px dashed rgba(184,146,74,.5);
}
.story-relic-mini { font-size: 11px; letter-spacing: .5px; color: #33403e; }
.story-toggle { flex: none; font-size: 11px; letter-spacing: 1px; color: #b03a3a; }
.story-end { margin-top: 18px; text-align: center; font-size: 13px; letter-spacing: 4px; color: #8e2c2c; }
.story-enter-active, .story-leave-active { transition: opacity .22s ease, transform .22s ease; }
.story-enter-from, .story-leave-to { opacity: 0; transform: translateY(-6px); }

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

/* ============ 金币：余额面板 / 领取 / 重复签保底 ============ */
.nav-gold {
  margin-left: 6px; padding: 5px 10px; white-space: nowrap;
  font-family: inherit; font-size: 13px; letter-spacing: 1px;
  color: #8a6a20; background: rgba(184,146,74,.16);
  border: 1px solid #b8924a; border-radius: 8px; cursor: pointer;
  transition: background .25s ease, transform .15s ease;
}
.nav-gold:hover { background: rgba(184,146,74,.32); }
.nav-gold.active { color: #f5efe3; background: linear-gradient(135deg, #c9a24a, #8a6a20); }
.nav-gold:active { transform: scale(.95); }
/* 后台管理入口：仅管理员可见，红色印章样式区别于普通导航 */
.nav-admin {
  margin-left: 6px; padding: 5px 10px; white-space: nowrap;
  font-family: inherit; font-size: 13px; letter-spacing: 1px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #8e2c2c; border-radius: 8px; cursor: pointer;
  box-shadow: 0 0 0 1px rgba(184,146,74,.5);
  transition: filter .25s ease, transform .15s ease;
}
.nav-admin:hover { filter: brightness(1.12); }
.nav-admin:active { transform: scale(.95); }
.dropdown-gold {
  padding: 8px 16px; border-bottom: 1px dashed rgba(184,146,74,.5);
  font-size: 13px; letter-spacing: 2px; color: #8e2c2c;
}
.dropdown-gold strong { font-size: 16px; font-weight: normal; color: #b03a3a; }
.gold-panel {
  margin-top: 4px; padding: 14px 16px; text-align: center;
  background: rgba(255,253,246,.5);
  border: 1px solid rgba(184,146,74,.7); border-radius: 12px;
}
.gold-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 8px; }
.gold-seal {
  width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center;
  font-size: 14px; color: #f5efe3;
  background: linear-gradient(135deg, #c9a24a, #8a6a20);
  border: 1px solid #b8924a; border-radius: 50%;
}
.gold-label { font-size: 13px; letter-spacing: 3px; color: #8e2c2c; }
.gold-amount { font-size: 22px; font-weight: normal; letter-spacing: 2px; color: #b03a3a; }
.gold-hint { font-size: 11px; letter-spacing: 1px; color: #6a5c4c; }
.gold-rule { margin-top: 8px; font-size: 11px; letter-spacing: .5px; line-height: 1.7; color: #4a3b32; }
.btn-gold {
  margin-top: 10px; padding: 7px 22px;
  font-family: inherit; font-size: 14px; letter-spacing: 3px;
  color: #f5efe3; background: linear-gradient(135deg, #c9a24a, #9a7528);
  border: 1px solid #b8924a; border-radius: 9px; cursor: pointer;
  transition: box-shadow .3s ease, transform .15s ease, filter .3s ease;
}
.btn-gold:hover:not(:disabled) { filter: brightness(1.06); box-shadow: 0 4px 14px rgba(154,117,40,.3); }
.btn-gold:active:not(:disabled) { transform: scale(.96); }
.btn-gold:disabled { opacity: .6; cursor: wait; }
.pity-tip { margin-top: 10px; text-align: center; font-size: 12px; letter-spacing: 1px; color: #8e2c2c; }

/* 领取金币页 */
.gold-view { max-width: 860px; margin: 26px auto 0; text-align: left; }
.gold-intro {
  max-width: 720px; margin: 0 auto 20px; padding: 18px 24px; text-align: center;
  background: rgba(250,245,234,.42); backdrop-filter: blur(2px);
  border: 1px dashed rgba(184,146,74,.7); border-radius: 14px;
}
.gold-intro-title { font-size: 18px; letter-spacing: 4px; color: #8e2c2c; }
.gold-intro-text { margin-top: 8px; font-size: 13px; letter-spacing: 1px; line-height: 1.9; color: #33403e; }
.gold-balance {
  display: flex; flex-wrap: wrap; align-items: center; gap: 18px; padding: 22px 26px;
  background: rgba(255,253,246,.55); border: 2px solid #b8924a; border-radius: 16px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.2), 0 10px 32px rgba(74,59,50,.16);
}
.gold-balance-seal {
  width: 62px; height: 62px; flex: none;
  display: flex; align-items: center; justify-content: center; font-size: 30px;
  color: #f5efe3; background: linear-gradient(135deg, #c9a24a, #8a6a20);
  border: 2px solid #b8924a; border-radius: 50%;
  box-shadow: 0 4px 14px rgba(138,106,32,.35);
}
.gold-balance-main { flex: 1; min-width: 140px; }
.gold-balance-label { font-size: 13px; letter-spacing: 3px; color: #8e2c2c; }
.gold-balance-num { display: block; margin: 2px 0; font-size: 40px; font-weight: normal; letter-spacing: 2px; line-height: 1.2; color: #b03a3a; }
.gold-balance-sub { font-size: 11px; letter-spacing: 2px; color: #6a5c4c; }
.gold-claim { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.gold-claim-tip { font-size: 11px; letter-spacing: 1px; color: #6a5c4c; }
.gold-rule-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-top: 18px; }
.gold-rule-card {
  padding: 18px 14px; text-align: center;
  background: rgba(250,245,234,.5); border: 1px dashed rgba(184,146,74,.7); border-radius: 14px;
}
.gold-rule-mark {
  width: 36px; height: 36px; display: inline-flex; align-items: center; justify-content: center;
  font-size: 18px; color: #8a6a20; background: rgba(255,253,246,.8);
  border: 1px solid #b8924a; border-radius: 50%;
}
.gold-rule-card h3 { margin-top: 10px; font-size: 15px; font-weight: normal; letter-spacing: 3px; color: #8e2c2c; }
.gold-rule-value { margin-top: 8px; font-size: 22px; letter-spacing: 2px; color: #b03a3a; }
.gold-rule-state {
  display: inline-block; margin-top: 8px; padding: 3px 10px;
  font-size: 11px; letter-spacing: 1px; color: #4a3b32;
  background: rgba(184,146,74,.14); border: 1px solid rgba(184,146,74,.5); border-radius: 6px;
}
.gold-rule-state.done { color: #8e2c2c; background: rgba(176,58,58,.12); border-color: rgba(176,58,58,.4); }
.gold-detail {
  margin-top: 18px; padding: 18px 22px;
  background: rgba(250,245,234,.44); border: 1px solid rgba(184,146,74,.55); border-radius: 14px;
}
.gold-detail-title { font-size: 16px; font-weight: normal; letter-spacing: 4px; text-align: center; color: #8e2c2c; }
.gold-detail ol { margin: 12px 0 0 20px; }
.gold-detail li { margin-bottom: 8px; font-size: 13px; letter-spacing: 1px; line-height: 1.8; color: #4a3b32; }

/* ============ 签囊：收藏签背包 ============ */
.nav-badge {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 17px; height: 17px; margin-left: 2px; padding: 0 4px;
  vertical-align: top; font-size: 11px; letter-spacing: 0; line-height: 1;
  color: #f5efe3; background: #b03a3a; border-radius: 9px;
}

/* 求签结果：收入签囊 */
.sign-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 2px; }
.btn-collect {
  padding: 8px 20px; font-family: inherit; font-size: 14px; letter-spacing: 3px;
  color: #8e2c2c; background: rgba(255,253,246,.6);
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .3s ease, transform .15s ease, background .3s ease;
}
.btn-collect:hover { box-shadow: 0 0 12px rgba(176,58,58,.28); background: rgba(255,253,246,.9); }
.btn-collect:active { transform: scale(.96); }
.btn-collect.collected { color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c); }
.btn-collect.ghost { color: #33403e; background: transparent; border-color: rgba(184,146,74,.7); }
.sign-bag-tip { font-size: 11px; letter-spacing: 1px; color: #6a5c4c; }
.bag-toast {
  position: fixed; left: 50%; bottom: 42px; z-index: 90;
  transform: translateX(-50%); padding: 10px 24px;
  font-size: 15px; letter-spacing: 3px; color: #f5efe3;
  background: rgba(142,44,44,.94); border: 1px solid #b8924a; border-radius: 10px;
  box-shadow: 0 6px 22px rgba(74,59,50,.32);
}
.toast-enter-active, .toast-leave-active { transition: opacity .3s ease, transform .3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 12px); }

/* 背包页卷首 */
.bag-view { max-width: 980px; margin: 26px auto 0; text-align: left; }
.bag-intro {
  max-width: 760px; margin: 0 auto 20px; padding: 18px 24px; text-align: center;
  background: rgba(250,245,234,.42); backdrop-filter: blur(2px);
  border: 1px dashed rgba(184,146,74,.7); border-radius: 14px;
}
.bag-intro-title { font-size: 18px; letter-spacing: 4px; color: #8e2c2c; }
.bag-intro-text { margin-top: 8px; font-size: 13px; letter-spacing: 1px; line-height: 1.9; color: #33403e; }
.bag-intro-stat { margin-top: 8px; font-size: 12px; letter-spacing: 2px; color: #8e2c2c; }

/* 收集进度 */
.bag-progress {
  padding: 18px 22px; background: rgba(250,245,234,.44);
  border: 1px solid rgba(184,146,74,.55); border-radius: 14px;
}
.bag-progress-head { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; letter-spacing: 3px; color: #8e2c2c; }
.bag-progress-head strong { font-size: 20px; font-weight: normal; letter-spacing: 2px; }
.bag-progress-track { height: 9px; margin-top: 10px; background: rgba(184,146,74,.2); border-radius: 6px; overflow: hidden; }
.bag-progress-fill { height: 100%; border-radius: 6px; background: linear-gradient(90deg, #c25b50, #8e2c2c); transition: width .5s ease; }
.bag-stat-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 18px; }
.bag-stat-box { padding: 12px 16px; background: rgba(255,253,246,.42); border: 1px dashed rgba(184,146,74,.5); border-radius: 10px; }
.bag-stat-label { font-size: 12px; letter-spacing: 2px; color: #8e2c2c; margin-bottom: 6px; }
.bag-stat-row { display: flex; align-items: center; gap: 10px; padding: 5px 0; border-bottom: 1px dashed rgba(184,146,74,.3); }
.bag-stat-row:last-child { border-bottom: none; }
.bag-stat-icon { width: 22px; height: 22px; flex: none; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #8e2c2c; background: rgba(245,239,227,.9); border: 1px solid #b8924a; border-radius: 50%; }
.bag-stat-name { flex: 1; font-size: 14px; letter-spacing: 2px; color: #4a3b32; }
.bag-stat-num { font-size: 14px; letter-spacing: 1px; color: #6a5c4c; }
.bag-stat-num.done { color: #b03a3a; }

/* 筛选 */
.bag-tools { margin: 20px 0 0; padding: 14px 16px; background: rgba(250,245,234,.4); border: 1px solid rgba(184,146,74,.5); border-radius: 12px; }
.bag-filter-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 10px; }
.bag-filter-row:last-of-type { margin-bottom: 0; }
.bag-filter-label { width: 44px; flex: none; font-size: 12px; letter-spacing: 2px; color: #8e2c2c; }
.bag-filter {
  padding: 5px 14px; font-family: inherit; font-size: 13px; letter-spacing: 2px;
  color: #4a3b32; background: rgba(255,253,246,.55);
  border: 1px solid rgba(184,146,74,.6); border-radius: 8px; cursor: pointer;
  transition: background .25s ease, color .25s ease, transform .15s ease;
}
.bag-filter:hover { border-color: #b03a3a; }
.bag-filter:active { transform: scale(.96); }
.bag-filter.on { color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c); border-color: #b8924a; }
.bag-filter.toggle { margin-left: auto; }
.bag-count { margin-top: 10px; font-size: 12px; letter-spacing: 2px; color: #33403e; }

/* 18 席签位 */
.bag-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 18px; }
.bag-card {
  min-height: 212px; padding: 16px 16px 14px;
  display: flex; flex-direction: column; gap: 10px;
  background: rgba(250,245,234,.52); backdrop-filter: blur(2px);
  border: 1px solid rgba(184,146,74,.6); border-radius: 14px;
  transition: transform .25s ease, box-shadow .25s ease;
}
.bag-card.owned { box-shadow: 0 4px 16px rgba(74,59,50,.14); }
.bag-card.owned:hover { transform: translateY(-3px); box-shadow: 0 8px 22px rgba(142,44,44,.22); }
.bag-card.missing { background: rgba(240,235,224,.3); border-style: dashed; }
.bag-card-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.bag-card-type { font-size: 12px; letter-spacing: 2px; color: #8e2c2c; }
.bag-card-level { padding: 3px 10px; font-size: 12px; letter-spacing: 2px; border-radius: 6px; white-space: nowrap; }
.lv-best { color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c); border: 1px solid #b8924a; }
.lv-good { color: #8e2c2c; background: rgba(184,146,74,.22); border: 1px solid #b8924a; }
.lv-plain { color: #40584c; background: rgba(110,145,120,.16); border: 1px solid rgba(110,145,120,.55); }
.bag-card-level.locked { color: #8a8175; background: transparent; border: 1px dashed rgba(120,110,95,.5); }
.bag-card-poem { font-size: 17px; letter-spacing: 2px; line-height: 1.8; color: #8e2c2c; }
.bag-card-note { flex: 1; font-size: 12px; letter-spacing: .5px; line-height: 1.8; color: #33403e; }
.bag-card-foot { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding-top: 8px; border-top: 1px dashed rgba(184,146,74,.45); font-size: 11px; color: #6a5c4c; }
.bag-card-btn {
  padding: 4px 12px; font-family: inherit; font-size: 12px; letter-spacing: 2px;
  color: #8e2c2c; background: rgba(255,253,246,.7);
  border: 1px solid rgba(184,146,74,.7); border-radius: 7px; cursor: pointer;
}
.bag-card-btn:hover { background: #fff; border-color: #b03a3a; }
.bag-card-btn.ghost { align-self: center; margin-top: auto; color: #33403e; }
.bag-lock { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 26px; letter-spacing: 8px; color: rgba(120,110,95,.5); }
.bag-card-hint { font-size: 11px; letter-spacing: 1px; text-align: center; color: #6a5c4c; }
.bag-empty { margin: 22px 0; text-align: center; font-size: 14px; letter-spacing: 2px; color: #33403e; }
.bag-foot-actions { display: flex; justify-content: center; gap: 16px; margin-top: 22px; }
.profile-bag-line {
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
  margin-top: 14px; padding-top: 10px; border-top: 1px dashed rgba(184,146,74,.5);
  font-size: 13px; letter-spacing: 2px; color: #8e2c2c;
}

/* ============ 法宝图鉴：卷首 / 检索 / 卡片 / 展卷 ============ */
.treasure-view { text-align: left; }
.treasure-intro {
  max-width: 700px; margin: 0 auto 22px; padding: 18px 24px; text-align: center;
  background: rgba(250,245,234,.4); backdrop-filter: blur(2px);
  border: 1px dashed rgba(184,146,74,.7); border-radius: 14px;
}
.treasure-intro-title { font-size: 18px; letter-spacing: 4px; color: #8e2c2c; }
.treasure-intro-text { margin-top: 8px; font-size: 13px; letter-spacing: 1px; line-height: 1.9; color: #33403e; }
.treasure-stat { margin-top: 8px; font-size: 12px; letter-spacing: 2px; color: #8e2c2c; }

.treasure-tools { max-width: 700px; margin: 0 auto; }
.treasure-search {
  display: flex; align-items: center; gap: 10px; padding: 6px 8px 6px 16px;
  background: rgba(250,245,234,.5);
  border: 1.5px solid rgba(184,146,74,.65); border-radius: 12px;
  transition: border-color .3s ease, box-shadow .3s ease;
}
.treasure-search:focus-within { border-color: #b03a3a; box-shadow: 0 0 0 3px rgba(176,58,58,.12); }
.treasure-search-mark { flex: none; font-size: 16px; color: #b8924a; }
.treasure-search input {
  flex: 1; min-width: 0; height: 36px; border: none; outline: none; background: transparent;
  font-family: inherit; font-size: 15px; letter-spacing: 1px; color: #4a3b32;
}
.treasure-search input::placeholder { color: rgba(51,64,62,.9); }
.treasure-clear {
  flex: none; width: 26px; height: 26px; border: none; background: transparent;
  font-family: inherit; font-size: 18px; color: #33403e; cursor: pointer;
}
.treasure-clear:hover { color: #b03a3a; }
.scroll-tabs { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 14px; }
.scroll-tab {
  padding: 7px 16px; font-family: inherit; font-size: 13px; letter-spacing: 2px;
  color: #8e2c2c; background: rgba(250,245,234,.55);
  border: 1px solid rgba(184,146,74,.6); border-radius: 20px; cursor: pointer;
  transition: background .25s ease, color .25s ease, border-color .25s ease, box-shadow .25s ease;
}
.scroll-tab:hover { border-color: #b03a3a; }
.scroll-tab.chosen {
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border-color: #b8924a; box-shadow: 0 4px 12px rgba(142,44,44,.25);
}
.treasure-actions { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 12px; margin-top: 14px; }
.btn-invite {
  height: 38px; padding: 0 22px; font-family: inherit; font-size: 14px;
  letter-spacing: 3px; text-indent: 3px; color: #f5efe3;
  background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
  transition: box-shadow .3s ease, transform .15s ease, filter .3s ease;
}
.btn-invite span { letter-spacing: 0; text-indent: 0; }
.btn-invite:hover { box-shadow: 0 0 14px rgba(176,58,58,.4); filter: brightness(1.06); }
.btn-invite:active { transform: scale(.96); }
.btn-invite:disabled { opacity: .6; cursor: not-allowed; box-shadow: none; filter: grayscale(.3); }
.treasure-actions-tip { font-size: 11px; letter-spacing: 1px; color: #33403e; }

/* ============ 法囊：所得法宝收藏 ============ */
.chest-panel { max-width: 700px; margin: 4px auto 20px; padding: 16px 20px 18px;
  background: rgba(255,253,246,.82); border: 1px solid rgba(184,146,74,.6); border-radius: 14px;
  box-shadow: 0 3px 16px rgba(142,44,44,.08); }
.chest-head { display: flex; align-items: center; gap: 10px; }
.chest-seal { display: inline-flex; align-items: center; justify-content: center; width: 28px; height: 28px;
  background: linear-gradient(135deg,#b03a3a,#8e2c2c); color: #f5efe3; font-size: 15px; border-radius: 6px; }
.chest-title { font-size: 15px; letter-spacing: 3px; color: #8e2c2c; }
.chest-num { margin-left: auto; font-size: 13px; letter-spacing: 1px; color: #8a6a20; font-weight: normal; }
.chest-tip { margin: 8px 0 12px; font-size: 11px; letter-spacing: 1px; color: #6d5a44; line-height: 1.7; }
.chest-items { display: flex; flex-wrap: wrap; gap: 8px; }
.chest-item { width: 62px; padding: 5px 3px 6px; font-family: inherit; cursor: pointer;
  background: rgba(184,146,74,.1); border: 1px solid rgba(184,146,74,.5); border-radius: 9px;
  transition: transform .15s ease, box-shadow .2s ease; }
.chest-item:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(142,44,44,.18); }
.chest-item img { display: block; width: 42px; height: 42px; margin: 0 auto 3px; object-fit: contain; }
.chest-item span { display: block; font-size: 10px; letter-spacing: .5px; color: #4a3b32;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chest-empty { text-align: center; font-size: 12px; letter-spacing: 1px; color: #9a866a; padding: 10px 0; }
.chest-rewards { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; padding-top: 12px;
  border-top: 1px dashed rgba(184,146,74,.5); }
.chest-reward-tag { padding: 4px 10px; font-size: 11px; letter-spacing: 1px; color: #8a6a20;
  background: rgba(184,146,74,.14); border-radius: 10px; }
.chest-reward-tag.done { color: #f5efe3; background: linear-gradient(135deg,#b03a3a,#8e2c2c); }
.treasure-count { max-width: 700px; margin: 16px auto 10px; font-size: 12px; letter-spacing: 2px; color: #8e2c2c; }
.treasure-empty { margin-top: 26px; text-align: center; font-size: 14px; letter-spacing: 2px; color: #33403e; }

.treasure-grid {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px;
  max-width: 700px; margin: 0 auto;
}
.treasure-card {
  display: flex; flex-direction: column; gap: 10px; min-height: 212px; padding: 16px 18px 14px;
  background: rgba(250,245,234,.5); backdrop-filter: blur(2px);
  border: 1.5px solid rgba(184,146,74,.55); border-radius: 14px; cursor: pointer;
  box-shadow: 0 5px 18px rgba(74,59,50,.09);
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
}
.treasure-card:hover, .treasure-card:focus-visible {
  transform: translateY(-3px); border-color: #b03a3a; outline: none;
  box-shadow: 0 10px 26px rgba(142,44,44,.2);
}
/* 每件法宝的专属插画：卡片取主体横幅，展卷取完整画幅 */
.treasure-figure {
  display: block; width: 100%; height: 140px;
  object-fit: cover; object-position: center 38%;
  background: #f4e9d2;
  border: 1px solid rgba(184,146,74,.5); border-radius: 10px;
}
.treasure-head { display: flex; align-items: center; gap: 10px; }
.treasure-seal {
  flex: none; display: flex; align-items: center; justify-content:center;
  width: 38px; height: 38px; font-size: 19px; color: #f5efe3; background: #b03a3a;
  border: 1px solid #b8924a; border-radius: 8px; box-shadow: 0 2px 8px rgba(176,58,58,.28);
}
.treasure-namebox { flex: 1; min-width: 0; }
.treasure-name { font-weight: normal; font-size: 19px; letter-spacing: 2px; color: #8e2c2c; line-height: 1.3; }
.treasure-alias { margin-top: 2px; font-size: 11px; letter-spacing: 1px; color: #33403e; }
.treasure-stars { flex: none; font-size: 12px; letter-spacing: 1px; color: #b8924a; }
.treasure-brief { flex: 1; font-size: 12.5px; line-height: 1.85; letter-spacing: .5px; color: #4a3b32; }
.treasure-meta { display: flex; flex-wrap: wrap; gap: 6px; }
.treasure-meta span { padding: 2px 8px; font-size: 11px; letter-spacing: 1px; border-radius: 5px; }
.meta-scroll { color: #8e2c2c; background: rgba(184,146,74,.18); border: 1px solid rgba(184,146,74,.55); }
.meta-chapter, .meta-kind { color: #33403e; background: rgba(255,253,246,.6); border: 1px solid rgba(184,146,74,.4); }
.treasure-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding-top: 8px; border-top: 1px dashed rgba(184,146,74,.5);
}
.treasure-owner { font-size: 11px; letter-spacing: .5px; color: #33403e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.treasure-price { flex: none; font-size: 11px; letter-spacing: .5px; color: #8a6a20; }
.treasure-more { flex: none; font-size: 11px; letter-spacing: 1px; color: #b03a3a; }

/* 法宝详情展卷：遮罩固定在视口，压过 sticky 导航 */
.treasure-mask {
  position: fixed; inset: 0; z-index: 200;
  display: flex; align-items: center; justify-content: center; padding: 20px;
  background: rgba(46,32,28,.5); backdrop-filter: blur(3px);
}
.treasure-dialog {
  position: relative; width: min(560px, 100%); max-height: 86vh; overflow-x: hidden; overflow-y: auto;
  padding: 30px 32px 28px; text-align: left;
  background: rgba(250,245,234,.97);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 20px 60px rgba(46,32,28,.4),
              inset 0 0 60px rgba(245,230,210,.25);
}
.treasure-dialog::before {
  content: ""; position: absolute; inset: -9px;
  border: 1px dashed rgba(184,146,74,.55); border-radius: 24px; pointer-events: none;
}
.dialog-close {
  position: absolute; top: 14px; right: 16px; width: 30px; height: 30px;
  border: none; background: transparent; font-family: inherit; font-size: 24px;
  line-height: 1; color: #8e2c2c; cursor: pointer;
}
.dialog-close:hover { color: #b03a3a; transform: scale(1.1); }
.dialog-figure {
  display: block; width: 100%; height: 190px;
  object-fit: contain; margin-bottom: 18px;
  background: #f4e9d2;
  border: 1px solid rgba(184,146,74,.6); border-radius: 12px;
}
.dialog-head { display: flex; align-items: center; gap: 14px; padding-right: 36px; }
.dialog-seal {
  flex: none; display: flex; align-items: center; justify-content: center;
  width: 56px; height: 56px; font-size: 28px; color: #f5efe3;
  background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 10px;
}
.dialog-name { font-weight: normal; font-size: 26px; letter-spacing: 4px; color: #8e2c2c; }
.dialog-alias { margin-top: 4px; font-size: 12px; letter-spacing: 2px; color: #33403e; }
.dialog-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.dialog-field { padding: 10px 12px; background: rgba(255,253,246,.7); border: 1px solid rgba(184,146,74,.45); border-radius: 10px; }
.dialog-field span { display: block; font-size: 11px; letter-spacing: 2px; color: #33403e; }
.dialog-field strong { display: block; margin-top: 4px; font-weight: normal; font-size: 14px; letter-spacing: 1px; color: #4a3b32; line-height: 1.5; }
.dialog-power { margin-top: 14px; font-size: 13px; letter-spacing: 2px; color: #8e2c2c; }
.dialog-power span { margin-left: 8px; font-size: 15px; letter-spacing: 2px; color: #b8924a; }
.dialog-section { margin-top: 16px; }
.dialog-section h4 { font-weight: normal; font-size: 14px; letter-spacing: 3px; color: #8e2c2c; border-left: 3px solid #b03a3a; padding-left: 10px; }
.dialog-section p { margin-top: 8px; font-size: 13.5px; line-height: 2; letter-spacing: .5px; color: #4a3b32; }
.dialog-verse { margin-top: 18px; text-align: center; font-size: 16px; letter-spacing: 2px; color: #8e2c2c; }
.dialog-tags { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 16px; }
.dialog-tags span { padding: 3px 10px; font-size: 11px; letter-spacing: 1px; color: #8e2c2c; background: rgba(184,146,74,.14); border: 1px solid rgba(184,146,74,.55); border-radius: 14px; }
.dialog-back {
  display: block; width: 160px; height: 40px; margin: 22px auto 0;
  font-family: inherit; font-size: 15px; letter-spacing: 5px; text-indent: 5px;
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 10px; cursor: pointer;
}
.dialog-back:hover { filter: brightness(1.07); }
.dialog-enter-active, .dialog-leave-active { transition: opacity .22s ease; }
.dialog-enter-active .treasure-dialog, .dialog-leave-active .treasure-dialog { transition: transform .22s ease; }
.dialog-enter-from, .dialog-leave-to { opacity: 0; }
.dialog-enter-from .treasure-dialog, .dialog-leave-to .treasure-dialog { transform: translateY(14px) scale(.97); }

/* ============ 苦情树 · 续缘相册：卷首 / 检索 / 相片 / 展卷 ============ */
.story-links { display: flex; flex-wrap: wrap; justify-content: center; gap: 0 18px; }

.tree-view { text-align: left; }
.tree-intro {
  max-width: 700px; margin: 0 auto 22px; padding: 18px 24px; text-align: center;
  background: rgba(250,245,234,.4); backdrop-filter: blur(2px);
  border: 1px dashed rgba(184,146,74,.7); border-radius: 14px;
}
.tree-intro-title { font-size: 18px; letter-spacing: 4px; color: #8e2c2c; }
.tree-intro-text { margin-top: 8px; font-size: 13px; letter-spacing: 1px; line-height: 1.9; color: #33403e; }

.tree-tools { max-width: 700px; margin: 0 auto; }
.tree-search {
  display: flex; align-items: center; gap: 10px; padding: 6px 8px 6px 16px;
  background: rgba(250,245,234,.5);
  border: 1.5px solid rgba(184,146,74,.65); border-radius: 12px;
  transition: border-color .3s ease, box-shadow .3s ease;
}
.tree-search:focus-within { border-color: #b03a3a; box-shadow: 0 0 0 3px rgba(176,58,58,.12); }
.tree-search-mark { flex: none; font-size: 16px; color: #b8924a; }
.tree-search input {
  flex: 1; min-width: 0; height: 36px; border: none; outline: none; background: transparent;
  font-family: inherit; font-size: 15px; letter-spacing: 1px; color: #4a3b32;
}
.tree-search input::placeholder { color: rgba(51,64,62,.9); }
.tree-search-page { max-width: 700px; margin: 0 auto; }
.tree-pages { display: flex; justify-content: center; flex-wrap: wrap; gap: 10px; }
.tree-page-btn {
  min-width: 154px; padding: 9px 18px; cursor: pointer;
  font-family: inherit; font-size: 14px; letter-spacing: 2px; color: #8e2c2c;
  background: rgba(250,245,234,.6);
  border: 1.5px solid rgba(184,146,74,.7); border-radius: 10px;
  box-shadow: 0 3px 10px rgba(74,59,50,.12);
  transition: transform .25s ease, border-color .25s ease, background .25s ease;
}
.tree-page-btn:hover { border-color: #b03a3a; transform: translateY(-2px); }
.tree-page-btn.chosen {
  color: #fdf6e8; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border-color: #b03a3a; box-shadow: 0 5px 16px rgba(142,44,44,.35);
}
.tree-count { max-width: 700px; margin: 16px auto 12px; font-size: 12px; letter-spacing: 2px; color: #8e2c2c; }
.tree-empty { margin-top: 26px; text-align: center; font-size: 14px; letter-spacing: 2px; color: #33403e; }

/* 同心缘簿：题名留册表单 */
.my-form-card {
  max-width: 700px; margin: 0 auto; padding: 20px 22px 18px;
  background: rgba(252,247,236,.94);
  border: 1px solid rgba(184,146,74,.75); border-radius: 4px;
  box-shadow: 0 8px 22px rgba(74,59,50,.18);
}
.my-form-title { text-align: center; font-size: 17px; letter-spacing: 4px; color: #8e2c2c; }
.my-form-sub { margin-top: 6px; text-align: center; font-size: 11px; letter-spacing: 1px; color: #33403e; }
.my-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 16px; }
.my-field { display: flex; flex-direction: column; gap: 5px; font-size: 12px; letter-spacing: 1px; color: #4a3b32; }
.my-field.wide { grid-column: 1 / -1; }
.my-field i { color: #b03a3a; font-style: normal; }
.my-field input, .my-field textarea {
  width: 100%; padding: 8px 10px; font-family: inherit; font-size: 13px; letter-spacing: .5px;
  color: #4a3b32; background: rgba(255,252,244,.85);
  border: 1px solid rgba(184,146,74,.6); border-radius: 6px; outline: none; resize: vertical;
}
.my-field input:focus, .my-field textarea:focus {
  border-color: #b03a3a; box-shadow: 0 0 0 3px rgba(176,58,58,.1);
}
.my-form-foot { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 12px; margin-top: 16px; }
.btn-my-submit {
  padding: 9px 24px; cursor: pointer; font-family: inherit; font-size: 14px; letter-spacing: 3px;
  color: #fdf6e8; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #8e2c2c; border-radius: 8px;
  box-shadow: 0 4px 12px rgba(142,44,44,.3); transition: transform .2s ease, box-shadow .2s ease;
}
.btn-my-submit:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(142,44,44,.4); }
.btn-my-submit:disabled { opacity: .6; cursor: not-allowed; }
.my-form-tip { font-size: 11px; letter-spacing: .5px; color: #33403e; }
.tree-contributor { margin-top: 6px; font-size: 10px; letter-spacing: 1px; color: #8e2c2c; }

/* 删除入口：只对自己题名的那一页出现 */
.btn-delete-couple {
  position: absolute; top: 12px; right: 12px; z-index: 4;
  width: 26px; height: 26px; padding: 0; cursor: pointer;
  font-family: inherit; font-size: 12px; line-height: 1;
  color: #fdf6e8; background: rgba(176,58,58,.9);
  border: 1px solid #b8924a; border-radius: 50%;
  box-shadow: 0 2px 6px rgba(142,44,44,.35);
  transition: transform .2s ease, background .2s ease;
}
.btn-delete-couple:hover { background: #8e2c2c; transform: scale(1.1); }
.btn-delete-couple.dialog-delete {
  position: static; display: block; width: auto; height: auto;
  margin: 16px auto 0; padding: 8px 18px;
  border-radius: 8px; font-size: 13px; letter-spacing: 2px;
}
.btn-delete-couple.dialog-delete:hover { transform: translateY(-2px); }

/* 相片卡：微微倾斜的宣纸相角，胶带上墙，像一册摊开的旧相册 */
.tree-grid {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 30px 24px; max-width: 700px; margin: 0 auto; padding-top: 6px;
}
.tree-card {
  position: relative; display: flex; flex-direction: column; padding: 12px 12px 14px;
  text-align: center; background: rgba(252,247,236,.94);
  border: 1px solid rgba(184,146,74,.75); border-radius: 3px;
  box-shadow: 0 8px 22px rgba(74,59,50,.18), inset 0 0 24px rgba(245,230,210,.5);
  cursor: pointer; transform: rotate(-1.1deg);
  transition: transform .28s ease, box-shadow .28s ease, border-color .28s ease;
}
.tree-card:nth-child(even) { transform: rotate(1.2deg); }
.tree-card:nth-child(3n) { transform: rotate(.6deg); }
.tree-card:hover, .tree-card:focus-visible {
  transform: rotate(0deg) translateY(-5px); border-color: #b03a3a; outline: none;
  box-shadow: 0 14px 32px rgba(142,44,44,.26);
}
.tree-tape {
  position: absolute; top: -11px; left: 50%; width: 76px; height: 22px;
  transform: translateX(-50%) rotate(-3deg);
  background: rgba(176,58,58,.42);
  border-left: 1px dashed rgba(255,255,255,.55); border-right: 1px dashed rgba(255,255,255,.55);
  box-shadow: 0 1px 3px rgba(74,59,50,.12);
}
.tree-seal {
  position: absolute; top: 18px; right: 18px; z-index: 2;
  display: flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; font-size: 16px; color: #f5efe3;
  background: #b03a3a; border: 1px solid #b8924a; border-radius: 5px;
  box-shadow: 0 2px 6px rgba(142,44,44,.35);
}
.tree-photo-wrap { position: relative; }
.tree-photo {
  display: block; width: 100%; aspect-ratio: 4 / 3; object-fit: cover;
  background: #f4e9d2; border: 5px solid #fbf6ea;
  box-shadow: 0 2px 8px rgba(74,59,50,.18);
}
.tree-photo-tag {
  position: absolute; bottom: 10px; left: 50%; transform: translateX(-50%);
  padding: 2px 10px; white-space: nowrap;
  font-size: 10px; letter-spacing: 2px; color: #8e2c2c;
  background: rgba(250,245,234,.88); border: 1px solid rgba(184,146,74,.6); border-radius: 12px;
}
.tree-caption { padding: 10px 4px 0; }
.tree-names { font-weight: normal; font-size: 18px; letter-spacing: 2px; color: #8e2c2c; line-height: 1.4; }
.tree-chapter { margin-top: 3px; font-size: 11px; letter-spacing: 1px; color: #33403e; }
.tree-brief { margin-top: 8px; min-height: 44px; font-size: 12px; line-height: 1.8; letter-spacing: .4px; color: #4a3b32; }
.tree-card-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  margin-top: 8px; padding-top: 8px; border-top: 1px dashed rgba(184,146,74,.6);
}
.tree-relic { font-size: 11px; color: #33403e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-more { flex: none; font-size: 11px; letter-spacing: 1px; color: #b03a3a; }

/* 相册展卷：与法宝展卷同制，顶部为放大后的相片位 */
.tree-mask {
  position: fixed; inset: 0; z-index: 200;
  display: flex; align-items: center; justify-content: center; padding: 20px;
  background: rgba(46,32,28,.5); backdrop-filter: blur(3px);
}
.tree-dialog {
  position: relative; width: min(560px, 100%); max-height: 86vh; overflow-x: hidden; overflow-y: auto;
  padding: 30px 32px 28px; text-align: left;
  background: rgba(250,245,234,.97);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 20px 60px rgba(46,32,28,.4),
              inset 0 0 60px rgba(245,230,210,.25);
}
.tree-dialog::before {
  content: ""; position: absolute; inset: -9px;
  border: 1px dashed rgba(184,146,74,.55); border-radius: 24px; pointer-events: none;
}
.tree-dialog-photo {
  margin-bottom: 18px; padding: 8px;
  background: #fbf6ea; border: 1px solid rgba(184,146,74,.6); border-radius: 12px;
  box-shadow: 0 4px 14px rgba(74,59,50,.15);
}
.tree-dialog-photo img {
  display: block; width: 100%; height: 230px; object-fit: contain;
  background: #f4e9d2; border-radius: 8px;
}
.tree-dialog-photo-tip {
  display: block; margin-top: 8px; text-align: center;
  font-size: 11px; letter-spacing: 2px; color: #8e2c2c; opacity: .75;
}
.tree-dialog-hint {
  margin-top: 16px; padding: 8px 10px; text-align: center;
  font-size: 11px; letter-spacing: .5px; color: #33403e;
  background: rgba(255,253,246,.6);
  border: 1px dashed rgba(184,146,74,.6); border-radius: 8px;
}

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
  .nav-links { left: 128px; gap: 0; }
  .nav-link { padding: 8px 9px; font-size: 15px; letter-spacing: 2px; }
  .user-menu { right: 12px; }
  .nav-gold { margin-left: 2px; padding: 4px 7px; font-size: 11px; }
  .gold-rule-grid { grid-template-columns: 1fr; }
  .gold-balance { padding: 16px; gap: 12px; }
  .gold-balance-num { font-size: 32px; }
  .gold-intro { padding: 14px 16px; }
  .nav-seal { width: 34px; height: 34px; font-size: 17px; }
  .user-dropdown { width: 164px; }
  .profile-card { padding: 28px 18px 26px; }
  .profile-avatar { width: 82px; height: 82px; }
  .avatar-fallback { font-size: 36px; }
  .profile-grid { gap: 8px; }
  .btn-back { padding: 0 16px; margin: 18px 4px 0; }
  .story-view { margin-top: 26px; }
  .story-intro { padding: 14px 16px 16px; }
  .story-timeline { padding-left: 38px; }
  .story-timeline::before { left: 15px; }
  .story-dot { left: -38px; width: 32px; height: 32px; }
  .story-dot-mark { font-size: 13px; }
  .story-card { padding: 14px 14px 12px; }
  .story-name { font-size: 18px; }
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
  .sign-actions { gap: 8px; }
  .bag-grid { grid-template-columns: 1fr; gap: 12px; }
  .bag-stat-grid { grid-template-columns: 1fr; }
  .bag-intro, .bag-progress, .bag-tools { padding: 14px 16px; }
  .bag-filter.toggle { margin-left: 0; }
  .bag-card { min-height: 0; }
  .bag-foot-actions { gap: 8px; }
  .thread-tl, .umbrella { opacity: .35; }
  .treasure-intro { padding: 14px 16px; }
  .treasure-grid { grid-template-columns: 1fr; }
  .treasure-card { min-height: 0; }
  .treasure-figure { height: 156px; }
  .dialog-figure { height: 150px; }
  .scroll-tab { padding: 6px 11px; font-size: 12px; letter-spacing: 1px; }
  .treasure-dialog { padding: 24px 18px 22px; }
  .dialog-grid { gap: 8px; }
  .dialog-seal { width: 46px; height: 46px; font-size: 23px; }
  .dialog-name { font-size: 22px; }
  .tree-grid { grid-template-columns: 1fr; gap: 24px; }
  .tree-card, .tree-card:nth-child(even), .tree-card:nth-child(3n) { transform: none; }
  .tree-intro { padding: 14px 16px; }
  .tree-names { font-size: 17px; }
  .tree-dialog { padding: 24px 18px 22px; }
  .tree-dialog-photo img { height: 170px; }
  .tree-pages { gap: 8px; }
  .tree-page-btn { min-width: 0; flex: 1; padding: 8px 6px; font-size: 12px; letter-spacing: 1px; }
  .my-form-card { padding: 16px 14px; }
  .my-form-grid { grid-template-columns: 1fr; }
  .my-field.wide { grid-column: auto; }
}

/* 更窄的手机：四个栏目 + 右侧「缘」章不打架 */
@media (max-width: 420px) {
  .brand-sub { display: none; }
  .nav-links { left: 76px; }
  .nav-link { padding: 8px 3px; font-size: 12px; letter-spacing: 1px; }
  .nav-gold { padding: 3px 5px; font-size: 10px; }
}

/* ===== 半透卡片文字：轻白柔光，保证花纹背景上仍清晰（不叠任何背景块） ===== */
.qiuqian-card, .profile-card, .search-bar, .result-item, .type-card, .profile-field, .profile-sign,
.treasure-intro, .treasure-search, .scroll-tab, .treasure-card, .treasure-count, .treasure-empty,
.treasure-actions-tip,
.tree-intro, .tree-search, .tree-card, .tree-count, .tree-empty,
.tree-page-btn, .my-form-card, .my-form-tip, .tree-contributor,
.tree-dialog-hint, .tree-photo-tag,
.story-intro, .story-card, .story-end, .story-side-link,
.bag-intro, .bag-progress, .bag-stat-box, .bag-tools, .bag-count, .bag-card,
.bag-empty, .profile-bag-line, .sign-bag-tip, .gold-panel, .pity-tip,
.gold-intro, .gold-balance, .gold-rule-card, .gold-detail {
  text-shadow: 0 0 5px rgba(255,253,246,1), 0 0 12px rgba(255,253,246,.9);
}
</style>
