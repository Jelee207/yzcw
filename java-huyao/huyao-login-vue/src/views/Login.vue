<script setup>
/**
 * Login.vue —— 《狐妖小红娘》主题登录页（Vue3 + script setup + CSS）
 * 用法：放入 src/views/，配合 vue-router 使用
 *   import Login from '@/views/Login.vue'
 *   { path: '/login', component: Login }
 * 登录成功后：router.push('/home')  → 跳转续缘主页
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/user'

const router = useRouter()

/* ---------- 响应式状态 ---------- */
const account  = ref('')     // 账号（道号）
const password = ref('')     // 密码（信物）
const remember = ref(false)  // 记住我（忆起前尘）
/* ---------- 登录：调用后端接口校验，成功后跳转续缘主页 ---------- */
async function handleLogin() {
  if (!account.value.trim() || !password.value.trim()) {
    alert('施主，缘深缘浅，还请留下名姓与信物（账号与密码）～')
    return
  }
  try {
    const user = await login({ username: account.value, password: password.value })
    // 登录成功，缓存用户信息并跳转
    localStorage.setItem('huyao_user', JSON.stringify(user))
    router.push('/home')
  } catch (e) {
    // 错误提示已在拦截器统一处理
  }
}

</script>

<template>
  <div class="login-page">
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

    <!-- ============ 登录卡片（云纹古风边框） ============ -->
    <div class="wrap">
      <div class="card">
        <span class="corner tl"/><span class="corner tr"/>
        <span class="corner br"/><span class="corner bl"/>

        <h1 class="title">狐缘阁</h1>
        <p class="subtitle">一纸相思 · 续前世之缘</p>
        <div class="divider"><span>❀</span></div>

        <!-- 账号输入框 -->
        <div class="field">
          <label>道 号</label>
          <input v-model.trim="account" type="text" placeholder="请输入名姓或道号"
                 autocomplete="username" @keyup.enter="handleLogin" />
        </div>

        <!-- 密码输入框 -->
        <div class="field">
          <label>信 物</label>
          <input v-model="password" type="password" placeholder="请输入随身信物"
                 autocomplete="current-password" @keyup.enter="handleLogin" />
        </div>

        <!-- 记住我 / 忘记密码 -->
        <div class="row">
          <span class="remember" @click="remember = !remember">
            <span class="checkbox" :class="{ on: remember }"/>忆起前尘
          </span>
          <span class="forget">遗落信物？</span>
        </div>

        <!-- 登录按钮 -->
        <button class="btn-login" @click="handleLogin">续 缘</button>

        <p class="footer">未结尘缘？<a @click="router.push('/register')">注册新缘</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ============ 古风基调 ============ */
.login-page {
  min-height: 100vh; overflow: hidden; position: relative;
  font-family: "STXingkai", "华文行楷", "STKaiti", "楷体", "KaiTi", serif;
  color: #4a3b32;
  /* 壁纸由 body 统一承载，页面完全透明，不再叠加任何洗白晕染 */
  background: none;
}

/* ============ 姻缘红线 / 苦情树 / 油纸伞 ============ */
.thread { position: fixed; z-index: 2; pointer-events: none; animation: sway 7s ease-in-out infinite; }
.thread-br { animation-duration: 9s; animation-direction: reverse; }
@keyframes sway {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50%      { transform: translateY(-6px) rotate(1.2deg); }
}
.tree { position: fixed; left: -40px; bottom: -30px; z-index: 1; pointer-events: none; opacity: .5; }
.umbrella { position: fixed; right: 6%; top: 10%; z-index: 1; pointer-events: none; opacity: .55; animation: drift 10s ease-in-out infinite; }
@keyframes drift {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50%      { transform: translateY(-12px) rotate(-1deg); }
}

/* ============ 居中卡片 ============ */
.wrap { position: relative; z-index: 10; height: 100vh; display: flex; align-items: center; justify-content: center; }
.card {
  position: relative; width: 420px; padding: 52px 48px 40px;
  background: rgba(250, 245, 234, .4); backdrop-filter: blur(2px);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 12px 40px rgba(74,59,50,.18),
              inset 0 0 60px rgba(245, 230, 210, .14);
}
/* 外圈云纹虚线 */
.card::before { content: ""; position: absolute; inset: -9px; border: 1px dashed rgba(184,146,74,.55); border-radius: 24px; pointer-events: none; }
/* 四角云头 */
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

/* ============ 标题与分隔 ============ */
.title { text-align: center; font-size: 34px; letter-spacing: 8px; color: #8e2c2c; margin-bottom: 6px; }
.subtitle { text-align: center; font-size: 14px; letter-spacing: 3px; color: #33403e; margin-bottom: 34px; }
.divider { display: flex; align-items: center; gap: 12px; margin: 0 auto 30px; width: 70%; }
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: linear-gradient(90deg, transparent, #b8924a, transparent); }
.divider span { color: #b03a3a; font-size: 15px; }

/* ============ 输入框（聚焦淡光晕） ============ */
.field { margin-bottom: 22px; }
.field label { display: block; font-size: 15px; letter-spacing: 2px; color: #8e2c2c; margin-bottom: 8px; }
.field input {
  width: 100%; height: 46px; padding: 0 16px;
  font-family: inherit; font-size: 16px; letter-spacing: 1px; color: #4a3b32;
  background: rgba(255,253,246,.75); border: 1px solid rgba(184,146,74,.6);
  border-radius: 10px; outline: none;
  transition: box-shadow .35s ease, border-color .35s ease;
}
.field input::placeholder { color: rgba(51,64,62,.95); }
.field input:focus {
  border-color: #b03a3a;
  box-shadow: 0 0 0 3px rgba(176,58,58,.12), 0 0 14px rgba(176,58,58,.18);
}

/* ============ 记住我 / 忘记密码 ============ */
.row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 26px; font-size: 14px; }
.remember { display: flex; align-items: center; gap: 7px; cursor: pointer; user-select: none; color: #33403e; }
.checkbox {
  width: 17px; height: 17px; border-radius: 4px;
  border: 1px solid #b8924a; background: rgba(255,253,246,.9);
  display: flex; align-items: center; justify-content: center;
  transition: background .25s ease;
}
.checkbox.on { background: #b03a3a; border-color: #8e2c2c; }
.checkbox.on::after { content: "✓"; color: #f5efe3; font-size: 12px; line-height: 1; }
.forget { color: #33403e; cursor: pointer; letter-spacing: 1px; transition: color .25s ease; }
.forget:hover { color: #b03a3a; text-decoration: underline dotted; }

/* ============ 登录按钮（hover 淡红光晕 / 点击缩放） ============ */
.btn-login {
  width: 100%; height: 50px;
  font-family: inherit; font-size: 20px; letter-spacing: 10px; text-indent: 10px;
  color: #f5efe3;
  background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 12px; cursor: pointer;
  transition: box-shadow .35s ease, transform .15s ease, filter .35s ease;
}
.btn-login:hover { box-shadow: 0 0 18px rgba(176,58,58,.4), 0 6px 18px rgba(142,44,44,.3); filter: brightness(1.06); }
.btn-login:active { transform: scale(.96); }

/* ============ 注册跳转 ============ */
.footer { margin-top: 24px; text-align: center; font-size: 14px; color: #33403e; }
.footer a { color: #b03a3a; text-decoration: none; cursor: pointer; }
.footer a:hover { text-decoration: underline; }

/* ===== 半透卡片文字：轻白柔光 ===== */
.card { text-shadow: 0 0 5px rgba(255,253,246,1), 0 0 12px rgba(255,253,246,.9); }
</style>
