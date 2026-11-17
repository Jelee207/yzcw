<script setup>
/**
 * Admin.vue —— 后台管理系统（仅管理员可进入）
 *   · 续缘相册：官方相册 tb_couple 的增删改查 + 换图（可上传图片）+ 显示/隐藏 + 排序
 *   · 同心缘簿：用户投稿，只提供「删除」——不提供任何修改/编辑入口，
 *               无论前台还是后台，都只能删除自己上传的相片（道号一致）
 * 进入时校验管理员会话令牌，失效则退回登录页。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAdminToken, clearAdminToken, adminSession, adminLogout,
  adminCoupleList, adminCoupleCreate, adminCoupleUpdate, adminCoupleImage, adminCoupleDelete,
  adminUserList, adminUserCreate, adminUserUpdate, adminUserDelete,
  adminTreasureList, adminTreasureCreate, adminTreasureUpdate, adminTreasureImage, adminTreasureDelete,
  adminUserCoupleList, adminUserCoupleDelete,
} from '../api/admin'

const router = useRouter()
const adminUser = ref(localStorage.getItem('huyao_admin_user') || '管理员')
const activeTab = ref('couples')          // couples | userCouples
const loading = ref(false)
const tip = ref('')

/* ---------------- 会话校验 ---------------- */
onMounted(async () => {
  if (!getAdminToken()) { router.replace('/admin/login'); return }
  try {
    const s = await adminSession()
    adminUser.value = s.username || adminUser.value
  } catch (e) {
    clearAdminToken()
    router.replace('/admin/login')
  }
  loadCouples()
  loadUsers()
  loadTreasures()
})

function doingLogout() {
  adminLogout().catch(() => {})
  clearAdminToken()
  router.replace('/admin/login')
}
function goHome() { router.push('/home') }
function notify(msg) { tip.value = msg; if (msg) setTimeout(() => { if (tip.value === msg) tip.value = '' }, 2600) }

/* ---------------- 续缘相册（官方） ---------------- */
const couples = ref([])
const coupleKeyword = ref('')
const filteredCouples = computed(() => {
  const k = coupleKeyword.value.trim()
  if (!k) return couples.value
  return couples.value.filter(c => [c.name, c.slug, c.chapter, c.era, c.relic, c.status]
    .join('').includes(k))
})

async function loadCouples() {
  loading.value = true
  try { couples.value = await adminCoupleList() }
  catch (e) { notify('相册加载失败，请检查后端服务～') }
  finally { loading.value = false }
}

const emptyForm = () => ({
  slug: '', name: '', mark: '', chapter: '', era: '', eraType: 'past',
  relic: '', status: '', brief: '', story: '', verse: '', tags: '',
  image: '', imageSlot: '', sortNo: 999, enabled: 1,
})
const form = ref(emptyForm())
const editingId = ref(null)
const formBusy = ref(false)
const showForm = ref(false)

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  showForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function openEdit(c) {
  editingId.value = c.id
  form.value = {
    ...emptyForm(),
    ...c,
    tags: Array.isArray(c.tags) ? c.tags.join(',') : (c.tags || ''),
    eraType: c.eraType || 'past',
    enabled: c.enabled == null ? 1 : c.enabled,
  }
  showForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function closeForm() { showForm.value = false }

async function saveCouple() {
  if (formBusy.value) return
  const f = form.value
  if (!f.slug.trim() || !f.name.trim()) { notify('标识（slug）与情侣名必填～'); return }
  formBusy.value = true
  try {
    const payload = { ...f, tags: f.tags, sortNo: Number(f.sortNo) || 0, enabled: Number(f.enabled) }
    if (editingId.value) {
      await adminCoupleUpdate(editingId.value, payload)
      notify('已保存修改～')
    } else {
      await adminCoupleCreate(payload)
      notify('已新增一对续缘～')
    }
    closeForm()
    await loadCouples()
  } catch (e) { notify('保存未成，请检查填写内容～') }
  finally { formBusy.value = false }
}

async function toggleEnabled(c) {
  try {
    await adminCoupleUpdate(c.id, { enabled: c.enabled ? 0 : 1 })
    c.enabled = c.enabled ? 0 : 1
    notify(c.enabled ? '已设为展示' : '已隐藏')
  } catch (e) { notify('操作失败～') }
}

async function removeCouple(c) {
  if (!window.confirm(`确定删除「${c.name}」这一页吗？不可恢复。`)) return
  try {
    await adminCoupleDelete(c.id)
    couples.value = couples.value.filter(x => x.id !== c.id)
    notify('已删除～')
  } catch (e) { notify('删除失败～') }
}

/* ---------------- 图片上传：本地文件转压缩后的 base64 data URL ---------------- */
const imageInput = ref(null)
function pickImage() { imageInput.value?.click() }
function onImageFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { notify('请选择图片文件～'); return }
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      form.value.image = compressImage(img, 900, 0.82)
      notify('图片已就绪，记得点「保存」～')
    }
    img.onerror = () => notify('图片解析失败，换一张试试～')
    img.src = reader.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}
function compressImage(img, maxW, quality) {
  const scale = Math.min(1, maxW / img.width)
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(img.width * scale)
  canvas.height = Math.round(img.height * scale)
  canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height)
  return canvas.toDataURL('image/jpeg', quality)
}
function coupleImg(c) {
  if (c.image) return c.image
  return `/couples/${c.slug}.svg`
}
function onImgError(e) {
  if (!e.target.src.endsWith('/couples/placeholder.svg')) e.target.src = '/couples/placeholder.svg'
}

/* ---------------- 同心缘簿（用户投稿）：仅列表 + 删除本人投稿 ---------------- */
const myCouples = ref([])
const myKeyword = ref('')
const filteredMyCouples = computed(() => {
  const k = myKeyword.value.trim()
  if (!k) return myCouples.value
  return myCouples.value.filter(c => [c.name, c.chapter, c.contributor, c.verse]
    .join('').includes(k))
})
/** 后台也只能删自己上传的：以登录管理员道号与投稿道号一致为准 */
function canDeleteMyCouple(c) {
  return !!c && !!adminUser.value && adminUser.value !== '管理员' && adminUser.value === c.contributor
}
async function loadMyCouples() {
  loading.value = true
  try { myCouples.value = await adminUserCoupleList() }
  catch (e) { notify('同心缘簿加载失败～') }
  finally { loading.value = false }
}
async function removeMyCouple(c) {
  if (!canDeleteMyCouple(c)) { notify('仅可删除自己上传的相片～'); return }
  if (!window.confirm(`确定删除投稿「${c.name}」（题名：${c.contributor || '有缘人'}）吗？不可恢复。`)) return
  try {
    await adminUserCoupleDelete(c.id)
    myCouples.value = myCouples.value.filter(x => x.id !== c.id)
    notify('投稿已移除～')
  } catch (e) { notify(e?.message || '删除失败～') }
}
/* ---------------- 用户管理 ---------------- */
const users = ref([])
const userKeyword = ref('')
const filteredUsers = computed(() => {
  const k = userKeyword.value.trim()
  if (!k) return users.value
  return users.value.filter(u => [u.username, u.race, u.gender].join('').includes(k))
})
async function loadUsers() {
  loading.value = true
  try { users.value = await adminUserList() }
  catch (e) { notify('用户列表加载失败～') }
  finally { loading.value = false }
}
const emptyUserForm = () => ({ username: '', password: '', race: '人', gender: '女', permission: 1, gold: 0 })
const userForm = ref(emptyUserForm())
const editingUserId = ref(null)
const showUserForm = ref(false)
function openUserCreate() {
  editingUserId.value = null
  userForm.value = emptyUserForm()
  showUserForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function openUserEdit(u) {
  editingUserId.value = u.id
  userForm.value = { username: u.username, password: '', race: u.race || '人',
    gender: u.gender || '女', permission: Number(u.permission), gold: Number(u.gold) || 0 }
  showUserForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
async function saveUser() {
  if (formBusy.value) return
  if (!userForm.value.username.trim()) { notify('道号不可为空～'); return }
  if (!editingUserId.value && !userForm.value.password) { notify('密码不可为空～'); return }
  if (userForm.value.gold === '' || userForm.value.gold == null || Number(userForm.value.gold) < 0) {
    notify('金币数量须为不小于 0 的整数～'); return
  }
  formBusy.value = true
  try {
    const payload = { ...userForm.value, gold: Math.max(0, Math.floor(Number(userForm.value.gold) || 0)) }
    if (editingUserId.value && !payload.password) delete payload.password  // 编辑时不改密码则不带
    if (editingUserId.value) {
      await adminUserUpdate(editingUserId.value, payload)
      notify('用户资料已保存～')
    } else {
      await adminUserCreate(payload)
      notify('用户已创建～')
    }
    showUserForm.value = false
    await loadUsers()
  } catch (e) { notify('保存失败，请检查道号是否重复～') }
  finally { formBusy.value = false }
}
async function removeUser(u) {
  if (!window.confirm(`确定删除用户「${u.username}」吗？不可恢复。`)) return
  try {
    await adminUserDelete(u.id)
    users.value = users.value.filter(x => x.id !== u.id)
    notify('用户已删除～')
  } catch (e) { /* 拦截器已提示（如不可删自己） */ }
}

/* ---------------- 法宝管理 ---------------- */
const treasures = ref([])
const treasureKeyword = ref('')
const scrollOptions = [
  { key: 'tushan', label: '涂山卷' }, { key: 'daomen', label: '道门卷' },
  { key: 'xixiyu', label: '西西域卷' }, { key: 'nanguo', label: '南国卷' },
]
const filteredTreasures = computed(() => {
  const k = treasureKeyword.value.trim()
  if (!k) return treasures.value
  return treasures.value.filter(t => [t.name, t.alias, t.owner, t.chapter, t.kind]
    .join('').includes(k))
})
async function loadTreasures() {
  loading.value = true
  try { treasures.value = await adminTreasureList() }
  catch (e) { notify('法宝列表加载失败～') }
  finally { loading.value = false }
}
const emptyTreasureForm = () => ({
  slug: '', name: '', alias: '', mark: '', scroll: 'tushan', owner: '', chapter: '', kind: '',
  power: 3, price: 5, brief: '', tale: '', verse: '', tags: '', image: '', sortNo: 999, enabled: 1,
})
const treasureForm = ref(emptyTreasureForm())
const editingTreasureId = ref(null)
const showTreasureForm = ref(false)
function openTreasureCreate() {
  editingTreasureId.value = null
  treasureForm.value = emptyTreasureForm()
  showTreasureForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function openTreasureEdit(t) {
  editingTreasureId.value = t.id
  treasureForm.value = {
    ...emptyTreasureForm(), ...t,
    tags: Array.isArray(t.tags) ? t.tags.join(',') : (t.tags || ''),
    image: t.image || '',
  }
  showTreasureForm.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
async function saveTreasure() {
  if (formBusy.value) return
  const f = treasureForm.value
  if (!f.slug.trim() || !f.name.trim()) { notify('标识（slug）与法宝名必填～'); return }
  formBusy.value = true
  try {
    const payload = { ...f, power: Number(f.power) || 3, price: Math.max(0, Number(f.price) || 5),
      sortNo: Number(f.sortNo) || 0, enabled: Number(f.enabled) }
    if (editingTreasureId.value) {
      await adminTreasureUpdate(editingTreasureId.value, payload)
      notify('法宝已保存～')
    } else {
      await adminTreasureCreate(payload)
      notify('法宝已新增～')
    }
    showTreasureForm.value = false
    await loadTreasures()
  } catch (e) { notify('保存失败，请检查标识是否重复～') }
  finally { formBusy.value = false }
}
async function toggleTreasureEnabled(t) {
  try {
    await adminTreasureUpdate(t.id, { enabled: t.enabled ? 0 : 1 })
    t.enabled = t.enabled ? 0 : 1
    notify(t.enabled ? '已设为展示' : '已隐藏')
  } catch (e) { notify('操作失败～') }
}
async function removeTreasure(t) {
  if (!window.confirm(`确定删除法宝「${t.name}」吗？不可恢复。`)) return
  try {
    await adminTreasureDelete(t.id)
    treasures.value = treasures.value.filter(x => x.id !== t.id)
    notify('法宝已删除～')
  } catch (e) { notify('删除失败～') }
}

/* 法宝图片上传：复用相册的压缩函数 */
const treasureImageInput = ref(null)
function pickTreasureImage() { treasureImageInput.value?.click() }
function onTreasureImageFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { notify('请选择图片文件～'); return }
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => { treasureForm.value.image = compressImage(img, 900, 0.82); notify('图片已就绪，记得保存～') }
    img.onerror = () => notify('图片解析失败～')
    img.src = reader.result
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}
function treasureImg(t) {
  return t.image || t.img || `/treasures/${t.slug}.svg`
}

function switchTab(tab) {
  activeTab.value = tab
  tip.value = ''
  if (tab === 'userCouples' && !myCouples.value.length) loadMyCouples()
  if (tab === 'couples' && !couples.value.length) loadCouples()
  if (tab === 'users' && !users.value.length) loadUsers()
  if (tab === 'treasures' && !treasures.value.length) loadTreasures()
}
</script>

<template>
  <div class="admin-page">
    <!-- 顶部栏 -->
    <header class="admin-top">
      <div class="top-left">
        <span class="top-seal">管</span>
        <span class="top-title">狐缘阁 · 后台管理系统</span>
      </div>
      <div class="top-right">
        <span class="top-user">{{ adminUser }}</span>
        <button type="button" class="top-btn" @click="goHome">返回前台</button>
        <button type="button" class="top-btn danger" @click="doingLogout">退出后台</button>
      </div>
    </header>

    <main class="admin-main">
      <transition name="toast"><div v-if="tip" class="admin-toast">{{ tip }}</div></transition>

      <!-- 模块切换 -->
      <nav class="admin-tabs">
        <button type="button" :class="{ on: activeTab === 'users' }" @click="switchTab('users')">用户管理</button>
        <button type="button" :class="{ on: activeTab === 'treasures' }" @click="switchTab('treasures')">法宝管理</button>
        <button type="button" :class="{ on: activeTab === 'couples' }" @click="switchTab('couples')">续缘相册管理</button>
        <button type="button" :class="{ on: activeTab === 'userCouples' }" @click="switchTab('userCouples')">同心缘簿管理</button>
      </nav>

      <!-- ============ 用户管理 ============ -->
      <section v-if="activeTab === 'users'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>用 户 管 理</h2>
            <p>管理全部用户 · 可新增、改资料、改权限（1 普通 / 0 管理员）与密码、删除</p>
          </div>
          <button type="button" class="btn-primary" @click="openUserCreate">＋ 新 增 用 户</button>
        </div>

        <transition name="expand">
          <form v-if="showUserForm" class="couple-form" @submit.prevent="saveUser">
            <div class="form-title">{{ editingUserId ? '编辑用户' : '新增用户' }}</div>
            <div class="form-grid">
              <label><span>道号 <i>*</i></span><input v-model.trim="userForm.username" placeholder="用户名（唯一）" /></label>
              <label><span>密码 <i v-if="!editingUserId">*</i><em v-else>留空不改</em></span>
                <input v-model="userForm.password" type="text" :placeholder="editingUserId ? '留空表示不修改密码' : '明文密码'" />
              </label>
              <label><span>出身</span>
                <select v-model="userForm.race"><option>人</option><option>妖</option></select>
              </label>
              <label><span>性别</span>
                <select v-model="userForm.gender"><option>女</option><option>男</option></select>
              </label>
              <label><span>权限</span>
                <select v-model.number="userForm.permission"><option :value="1">1 · 普通用户</option><option :value="0">0 · 管理员</option></select>
              </label>
              <label><span>金币数量</span>
                <input v-model.number="userForm.gold" type="number" min="0" step="1" placeholder="0" />
              </label>
            </div>
            <p class="image-hint" style="margin-top:12px">密码明文存储；设为管理员后该用户可在首页看到「后台」入口并登录后台。金币数量可直接修改，前台拈签/领金会在此基础上增减并写回。</p>
            <div class="form-foot">
              <button type="button" class="btn-ghost" @click="showUserForm = false">取 消</button>
              <button type="submit" class="btn-primary" :disabled="formBusy">{{ formBusy ? '保 存 中…' : '确 认 保 存' }}</button>
            </div>
          </form>
        </transition>

        <div class="toolbar">
          <input v-model.trim="userKeyword" class="search" placeholder="检索道号 / 出身 / 性别…" />
          <span class="count">共 {{ users.length }} 人</span>
        </div>
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="filteredUsers.length" class="user-table-wrap">
          <table class="user-table">
            <thead><tr><th>ID</th><th>道号</th><th>出身</th><th>性别</th><th>权限</th><th>金币</th><th>头像</th><th>注册时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="u in filteredUsers" :key="u.id">
                <td>{{ u.id }}</td>
                <td class="uname">{{ u.username }}</td>
                <td>{{ u.race || '—' }}</td>
                <td>{{ u.gender || '—' }}</td>
                <td><span class="perm-tag" :class="Number(u.permission) === 0 ? 'admin' : ''">{{ Number(u.permission) === 0 ? '管理员' : '普通' }}</span></td>
                <td><span class="gold-cell">✦ {{ u.gold }}</span></td>
                <td>{{ u.hasAvatar ? '有' : '—' }}</td>
                <td class="muted">{{ u.createTime }}</td>
                <td>
                  <button type="button" @click="openUserEdit(u)">编辑</button>
                  <button type="button" class="danger" @click="removeUser(u)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="empty">没有匹配的用户～</p>
      </section>

      <!-- ============ 法宝管理 ============ -->
      <section v-else-if="activeTab === 'treasures'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>法 宝 图 鉴</h2>
            <p>管理首页法宝图鉴 · 可新增、编辑、上传图片、隐藏与排序，改动即时同步前台</p>
          </div>
          <button type="button" class="btn-primary" @click="openTreasureCreate">＋ 新 增 法 宝</button>
        </div>

        <transition name="expand">
          <form v-if="showTreasureForm" class="couple-form" @submit.prevent="saveTreasure">
            <div class="form-title">{{ editingTreasureId ? '编辑法宝' : '新增法宝' }}</div>
            <div class="form-grid">
              <label><span>标识 slug <i>*</i></span><input v-model.trim="treasureForm.slug" placeholder="如 xukong-zhilei（唯一）" :disabled="!!editingTreasureId" /></label>
              <label><span>法宝名 <i>*</i></span><input v-model.trim="treasureForm.name" /></label>
              <label><span>别称</span><input v-model.trim="treasureForm.alias" /></label>
              <label><span>印章</span><input v-model.trim="treasureForm.mark" maxlength="4" /></label>
              <label><span>分卷</span>
                <select v-model="treasureForm.scroll"><option v-for="s in scrollOptions" :key="s.key" :value="s.key">{{ s.label }}</option></select>
              </label>
              <label><span>持有者</span><input v-model.trim="treasureForm.owner" /></label>
              <label><span>出处</span><input v-model.trim="treasureForm.chapter" /></label>
              <label><span>类别</span><input v-model.trim="treasureForm.kind" /></label>
              <label><span>灵力 1-5</span><input v-model.number="treasureForm.power" type="number" min="1" max="5" /></label>
              <label><span>价格 2-25 金</span><input v-model.number="treasureForm.price" type="number" min="0" /></label>
              <label><span>排序</span><input v-model.number="treasureForm.sortNo" type="number" /></label>
              <label><span>标签（逗号分隔）</span><input v-model.trim="treasureForm.tags" /></label>
              <label><span>是否展示</span>
                <select v-model.number="treasureForm.enabled"><option :value="1">展示</option><option :value="0">隐藏</option></select>
              </label>
              <label class="wide"><span>器之用（简介）</span><input v-model.trim="treasureForm.brief" /></label>
              <label class="wide"><span>题句</span><input v-model.trim="treasureForm.verse" /></label>
              <label class="wide"><span>缘之由</span><textarea v-model.trim="treasureForm.tale" rows="3"></textarea></label>
            </div>

            <div class="image-editor">
              <div class="image-preview">
                <img v-if="treasureForm.image" :src="treasureForm.image" alt="图片预览" @error="onImgError" />
                <span v-else class="image-placeholder">尚 无 图 片</span>
              </div>
              <div class="image-side">
                <p class="image-label">法 宝 图</p>
                <button type="button" class="btn-ghost" @click="pickTreasureImage">上传本地图片</button>
                <input ref="treasureImageInput" class="hidden-file" type="file" accept="image/*" @change="onTreasureImageFile" />
                <input v-model.trim="treasureForm.image" class="image-url" placeholder="或填图片地址 / 外链 URL" />
                <button v-if="treasureForm.image" type="button" class="btn-text" @click="treasureForm.image = ''">移除图片</button>
                <p class="image-hint">上传后自动压缩为 JPEG 内嵌；留空则前台展示内置插画</p>
              </div>
            </div>

            <div class="form-foot">
              <button type="button" class="btn-ghost" @click="showTreasureForm = false">取 消</button>
              <button type="submit" class="btn-primary" :disabled="formBusy">{{ formBusy ? '保 存 中…' : (editingTreasureId ? '保 存 修 改' : '确 认 新 增') }}</button>
            </div>
          </form>
        </transition>

        <div class="toolbar">
          <input v-model.trim="treasureKeyword" class="search" placeholder="检索法宝名 / 持有者 / 类别…" />
          <span class="count">共 {{ treasures.length }} 器</span>
        </div>
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="filteredTreasures.length" class="grid">
          <article v-for="t in filteredTreasures" :key="t.id" class="item-card" :class="{ hidden: !t.enabled }">
            <div class="item-photo">
              <img :src="treasureImg(t)" :alt="t.name" loading="lazy" @error="onImgError" />
              <span v-if="!t.enabled" class="flag-hidden">已隐藏</span>
            </div>
            <div class="item-body">
              <h3>{{ t.name }} <span class="item-price">✦{{ t.price }}</span></h3>
              <p class="item-meta">{{ t.alias || '—' }} · {{ scrollOptions.find(s => s.key === t.scroll)?.label || '—' }}</p>
              <p class="item-relic">持有：{{ t.owner || '—' }}</p>
              <div class="item-actions">
                <button type="button" @click="openTreasureEdit(t)">编辑</button>
                <button type="button" @click="toggleTreasureEnabled(t)">{{ t.enabled ? '隐藏' : '展示' }}</button>
                <button type="button" class="danger" @click="removeTreasure(t)">删除</button>
              </div>
            </div>
          </article>
        </div>
        <p v-else class="empty">没有匹配的法宝～</p>
      </section>

      <!-- ============ 续缘相册 ============ -->
      <section v-else-if="activeTab === 'couples'" class="admin-section">
        <div class="section-head">
          <div>
            <h2>续 缘 相 册</h2>
            <p>官方相册数据 · 可新增、编辑、换图、隐藏与排序，改动即时同步前台</p>
          </div>
          <button type="button" class="btn-primary" @click="openCreate">＋ 新 增 一 对</button>
        </div>

        <!-- 新增/编辑表单 -->
        <transition name="expand">
          <form v-if="showForm" class="couple-form" @submit.prevent="saveCouple">
            <div class="form-title">{{ editingId ? '编辑续缘档案' : '新增续缘档案' }}</div>
            <div class="form-grid">
              <label><span>标识 slug <i>*</i></span><input v-model.trim="form.slug" placeholder="如 yuehong（唯一）" :disabled="!!editingId" /></label>
              <label><span>情侣名 <i>*</i></span><input v-model.trim="form.name" placeholder="东方月初 × 涂山红红" /></label>
              <label><span>印章</span><input v-model.trim="form.mark" maxlength="4" placeholder="单字，如 缘" /></label>
              <label><span>排序</span><input v-model.number="form.sortNo" type="number" /></label>
              <label><span>篇章</span><input v-model.trim="form.chapter" /></label>
              <label><span>时代</span><input v-model.trim="form.era" /></label>
              <label><span>分卷</span>
                <select v-model="form.eraType"><option value="past">前尘 past</option><option value="now">今生 now</option></select>
              </label>
              <label><span>续缘信物</span><input v-model.trim="form.relic" /></label>
              <label><span>缘分状态</span><input v-model.trim="form.status" /></label>
              <label><span>标签（逗号分隔）</span><input v-model.trim="form.tags" placeholder="转世,苦情树,吉" /></label>
              <label class="wide"><span>小传</span><input v-model.trim="form.brief" /></label>
              <label class="wide"><span>题句</span><input v-model.trim="form.verse" /></label>
              <label class="wide"><span>前尘细说</span><textarea v-model.trim="form.story" rows="2"></textarea></label>
              <label class="wide"><span>建议图位路径</span><input v-model.trim="form.imageSlot" placeholder="/couples/xxx.jpg" /></label>
              <label><span>是否展示</span>
                <select v-model.number="form.enabled"><option :value="1">展示</option><option :value="0">隐藏</option></select>
              </label>
            </div>

            <div class="image-editor">
              <div class="image-preview">
                <img v-if="form.image" :src="form.image" alt="图片预览" @error="onImgError" />
                <span v-else class="image-placeholder">尚 无 图 片</span>
              </div>
              <div class="image-side">
                <p class="image-label">相 片</p>
                <button type="button" class="btn-ghost" @click="pickImage">上传本地图片</button>
                <input ref="imageInput" class="hidden-file" type="file" accept="image/*" @change="onImageFile" />
                <input v-model.trim="form.image" class="image-url" placeholder="或填图片地址 / 外链 URL" />
                <button v-if="form.image" type="button" class="btn-text" @click="form.image = ''">移除图片</button>
                <p class="image-hint">上传后自动压缩为 JPEG 内嵌；留空则前台展示占位插画</p>
              </div>
            </div>

            <div class="form-foot">
              <button type="button" class="btn-ghost" @click="closeForm">取 消</button>
              <button type="submit" class="btn-primary" :disabled="formBusy">{{ formBusy ? '保 存 中…' : (editingId ? '保 存 修 改' : '确 认 新 增') }}</button>
            </div>
          </form>
        </transition>

        <div class="toolbar">
          <input v-model.trim="coupleKeyword" class="search" placeholder="检索情侣名 / 篇章 / 信物…" />
          <span class="count">共 {{ couples.length }} 对</span>
        </div>

        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="filteredCouples.length" class="grid">
          <article v-for="c in filteredCouples" :key="c.id" class="item-card" :class="{ hidden: !c.enabled }">
            <div class="item-photo">
              <img :src="coupleImg(c)" :alt="c.name" loading="lazy" @error="onImgError" />
              <span v-if="!c.enabled" class="flag-hidden">已隐藏</span>
              <span v-else-if="!c.image" class="flag-slot">占位图</span>
            </div>
            <div class="item-body">
              <h3>{{ c.name }}</h3>
              <p class="item-meta">{{ c.chapter || '—' }} · {{ c.era || '—' }}</p>
              <p class="item-relic">信物：{{ c.relic || '—' }}</p>
              <div class="item-actions">
                <button type="button" @click="openEdit(c)">编辑</button>
                <button type="button" @click="toggleEnabled(c)">{{ c.enabled ? '隐藏' : '展示' }}</button>
                <button type="button" class="danger" @click="removeCouple(c)">删除</button>
              </div>
            </div>
          </article>
        </div>
        <p v-else class="empty">没有匹配的续缘档案～</p>
      </section>

      <!-- ============ 同心缘簿：只删除，不可更改 ============ -->
      <section v-else class="admin-section">
        <div class="section-head">
          <div>
            <h2>同 心 缘 簿</h2>
            <p>用户共享投稿 · 后台仅可删除自己上传的相片，不提供任何修改功能</p>
          </div>
          <span class="readonly-tag">只读 + 删除</span>
        </div>
        <div class="toolbar">
          <input v-model.trim="myKeyword" class="search" placeholder="检索 cp 名 / 出处 / 题名道号…" />
          <span class="count">共 {{ myCouples.length }} 页</span>
        </div>

        <div v-if="loading" class="empty">加载中…</div>
        <div v-else-if="filteredMyCouples.length" class="grid">
          <article v-for="c in filteredMyCouples" :key="c.id" class="item-card">
            <div class="item-photo">
              <img :src="c.image || '/couples/placeholder.svg'" :alt="c.name" loading="lazy" @error="onImgError" />
            </div>
            <div class="item-body">
              <h3>{{ c.name }}</h3>
              <p class="item-meta">{{ c.chapter || '—' }}</p>
              <p class="item-relic">题句：{{ c.verse || '—' }}</p>
              <p class="item-relic">题名：{{ c.contributor || '有缘人' }} · {{ c.createTime || '' }}</p>
              <div class="item-actions">
                <button v-if="canDeleteMyCouple(c)" type="button" class="danger" @click="removeMyCouple(c)">删除此页</button>
              </div>
            </div>
          </article>
        </div>
        <p v-else class="empty">同心缘簿暂无投稿～</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.admin-page { min-height:100vh; font-family:"STKaiti","楷体","KaiTi",serif; color:#4a3b32;
  background:linear-gradient(160deg,#f7f1e4,#efe4cf); }
.admin-top { position:sticky; top:0; z-index:20; display:flex; align-items:center; justify-content:space-between;
  padding:12px 26px; background:rgba(142,44,44,.96); border-bottom:2px solid #b8924a; color:#f5efe3;
  box-shadow:0 3px 14px rgba(74,59,50,.25); }
.top-left,.top-right { display:flex; align-items:center; gap:14px; }
.top-seal { width:36px; height:36px; display:flex; align-items:center; justify-content:center;
  font-size:20px; color:#8e2c2c; background:#f5efe3; border:1px solid #b8924a; border-radius:8px; }
.top-title { font-size:20px; letter-spacing:3px; }
.top-user { font-size:14px; letter-spacing:1px; opacity:.9; }
.top-btn { font-family:inherit; font-size:13px; letter-spacing:1px; padding:6px 14px; cursor:pointer;
  color:#f5efe3; background:rgba(255,255,255,.1); border:1px solid rgba(245,239,227,.6); border-radius:7px; }
.top-btn:hover { background:rgba(255,255,255,.22); }
.top-btn.danger { border-color:#ffb3a7; color:#ffe0d8; }
.admin-main { max-width:1080px; margin:0 auto; padding:26px 22px 60px; }
.admin-toast { position:fixed; top:74px; left:50%; transform:translateX(-50%); z-index:50;
  padding:10px 22px; font-size:14px; color:#f5efe3; background:rgba(142,44,44,.94);
  border:1px solid #b8924a; border-radius:10px; box-shadow:0 6px 20px rgba(0,0,0,.18); }
.toast-enter-active,.toast-leave-active { transition:opacity .3s ease, transform .3s ease; }
.toast-enter-from,.toast-leave-to { opacity:0; transform:translateX(-50%) translateY(-8px); }
.admin-tabs { display:flex; gap:10px; margin-bottom:24px; border-bottom:1px solid rgba(184,146,74,.6); }
.admin-tabs button { font-family:inherit; font-size:16px; letter-spacing:2px; padding:10px 22px; cursor:pointer;
  color:#8e2c2c; background:none; border:none; border-bottom:3px solid transparent; }
.admin-tabs button.on { border-color:#b03a3a; font-weight:bold; }
.section-head { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; margin-bottom:18px; }
.section-head h2 { font-size:22px; letter-spacing:4px; color:#8e2c2c; margin:0 0 6px; }
.section-head p { font-size:13px; color:#6a5647; margin:0; }
.readonly-tag { flex:none; font-size:13px; letter-spacing:1px; color:#8a6a20;
  padding:6px 14px; border:1px dashed #b8924a; border-radius:8px; }
.btn-primary { font-family:inherit; font-size:15px; letter-spacing:2px; padding:9px 20px; cursor:pointer;
  color:#f5efe3; background:linear-gradient(135deg,#b03a3a,#8e2c2c); border:1px solid #8e2c2c; border-radius:9px; }
.btn-primary:hover:not(:disabled) { filter:brightness(1.1); }
.btn-primary:disabled { opacity:.6; cursor:wait; }
.btn-ghost { font-family:inherit; font-size:14px; letter-spacing:1px; padding:8px 16px; cursor:pointer;
  color:#8e2c2c; background:rgba(255,253,246,.7); border:1px solid #b8924a; border-radius:8px; }
.btn-ghost:hover { background:#fff8ec; }
.btn-text { font-family:inherit; font-size:12px; color:#b03a3a; background:none; border:none; cursor:pointer; text-decoration:underline; }

/* 表单 */
.couple-form { margin-bottom:24px; padding:22px; background:rgba(255,253,246,.85);
  border:1px solid #b8924a; border-radius:14px; box-shadow:0 6px 20px rgba(74,59,50,.1); }
.expand-enter-active,.expand-leave-active { transition:opacity .25s ease; }
.expand-enter-from,.expand-leave-to { opacity:0; }
.form-title { font-size:18px; letter-spacing:3px; color:#8e2c2c; margin-bottom:16px; }
.form-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:12px; }
.form-grid label { display:flex; flex-direction:column; gap:5px; font-size:12px; color:#6a5647; }
.form-grid label.wide { grid-column:span 2; }
.form-grid label i { color:#b03a3a; font-style:normal; }
.form-grid input,.form-grid select,.form-grid textarea { font-family:inherit; font-size:14px; color:#4a3b32;
  padding:7px 10px; background:#fffdf6; border:1px solid rgba(184,146,74,.6); border-radius:7px; outline:none; }
.form-grid input:focus,.form-grid textarea:focus { border-color:#b03a3a; }
.form-grid textarea { resize:vertical; }
.image-editor { display:flex; gap:20px; margin-top:18px; align-items:flex-start; }
.image-preview { width:200px; height:140px; flex:none; display:flex; align-items:center; justify-content:center;
  background:#efe4cf; border:1px solid #b8924a; border-radius:10px; overflow:hidden; }
.image-preview img { width:100%; height:100%; object-fit:cover; }
.image-placeholder { font-size:13px; letter-spacing:2px; color:#a08a6a; }
.image-side { flex:1; display:flex; flex-direction:column; gap:9px; align-items:flex-start; }
.image-label { font-size:14px; letter-spacing:2px; color:#8e2c2c; margin:0; }
.hidden-file { display:none; }
.image-url { width:100%; font-family:inherit; font-size:13px; padding:7px 10px; color:#4a3b32;
  background:#fffdf6; border:1px solid rgba(184,146,74,.6); border-radius:7px; outline:none; }
.image-hint { font-size:12px; color:#8a7458; margin:0; }
.form-foot { display:flex; justify-content:flex-end; gap:12px; margin-top:20px; }

/* 工具条与卡片 */
.toolbar { display:flex; align-items:center; gap:14px; margin-bottom:18px; }
.search { flex:1; max-width:420px; font-family:inherit; font-size:14px; padding:8px 14px; color:#4a3b32;
  background:#fffdf6; border:1px solid #b8924a; border-radius:9px; outline:none; }
.count { font-size:13px; letter-spacing:1px; color:#8e2c2c; }
.grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(230px,1fr)); gap:18px; }
.item-card { background:rgba(255,253,246,.9); border:1px solid rgba(184,146,74,.7); border-radius:12px;
  overflow:hidden; box-shadow:0 4px 14px rgba(74,59,50,.1); display:flex; flex-direction:column; }
.item-card.hidden { opacity:.55; }
.item-photo { position:relative; height:130px; background:#efe4cf; overflow:hidden; }
.item-photo img { width:100%; height:100%; object-fit:cover; }
.flag-hidden,.flag-slot { position:absolute; top:8px; left:8px; font-size:11px; padding:3px 8px; border-radius:6px;
  color:#f5efe3; background:rgba(142,44,44,.85); }
.flag-slot { background:rgba(138,106,32,.85); }
.item-body { padding:12px 14px 14px; display:flex; flex-direction:column; flex:1; }
.item-body h3 { font-size:16px; letter-spacing:1px; color:#8e2c2c; margin:0 0 6px; }
.item-price { float:right; font-size:12px; color:#8a6a20; font-weight:normal; }
.item-meta,.item-relic { font-size:12px; color:#6a5647; margin:0 0 4px; }
.item-actions { display:flex; gap:8px; margin-top:auto; padding-top:10px; flex-wrap:wrap; }
.item-actions button { font-family:inherit; font-size:12px; letter-spacing:1px; padding:5px 10px; cursor:pointer;
  color:#8e2c2c; background:rgba(184,146,74,.12); border:1px solid #b8924a; border-radius:6px; }
.item-actions button:hover { background:rgba(184,146,74,.3); }
.item-actions button.danger { color:#fff; background:#b03a3a; border-color:#8e2c2c; }
.item-actions button.danger:hover { background:#8e2c2c; }
.empty { text-align:center; padding:50px 0; font-size:15px; color:#8a7458; }

/* 用户表格 */
.user-table-wrap { overflow-x:auto; background:rgba(255,253,246,.9); border:1px solid rgba(184,146,74,.7);
  border-radius:12px; box-shadow:0 4px 14px rgba(74,59,50,.1); }
.user-table { width:100%; border-collapse:collapse; font-size:14px; }
.user-table th { text-align:left; padding:11px 14px; letter-spacing:1px; color:#f5efe3;
  background:rgba(142,44,44,.9); white-space:nowrap; }
.user-table td { padding:10px 14px; border-bottom:1px dashed rgba(184,146,74,.45); white-space:nowrap; }
.user-table tr:last-child td { border-bottom:none; }
.user-table td.uname { color:#8e2c2c; font-weight:bold; }
.user-table td.muted { color:#9a866a; font-size:12px; }
.user-table td button { font-family:inherit; font-size:12px; letter-spacing:1px; padding:5px 10px; margin-right:6px;
  cursor:pointer; color:#8e2c2c; background:rgba(184,146,74,.12); border:1px solid #b8924a; border-radius:6px; }
.user-table td button:hover { background:rgba(184,146,74,.3); }
.user-table td button.danger { color:#fff; background:#b03a3a; border-color:#8e2c2c; }
.perm-tag { display:inline-block; padding:3px 10px; border-radius:10px; font-size:12px;
  color:#8a6a20; background:rgba(184,146,74,.18); }
.perm-tag.admin { color:#fff; background:linear-gradient(135deg,#b03a3a,#8e2c2c); }
.gold-cell { color:#8a6a20; font-weight:bold; letter-spacing:.5px; }
.form-grid label em { font-style:normal; font-size:11px; color:#a08a6a; }

@media (max-width:760px) {
  .form-grid { grid-template-columns:repeat(2,1fr); }
  .top-title { font-size:16px; }
  .top-user { display:none; }
  .admin-main { padding:18px 12px 50px; }
  .section-head { flex-direction:column; }
}
</style>
