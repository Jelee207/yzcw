<script setup>
/**
 * AdminLogin.vue —— 后台管理系统登录页
 * 只有登录功能：无注册入口、无验证码。仅权限为 0 的管理员账号可登录。
 * 登录成功把会话 token 存入 localStorage，跳 /admin。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin, setAdminToken } from '../api/admin'

const router = useRouter()
const account = ref('')
const password = ref('')
const busy = ref(false)
const tip = ref('')

async function handleLogin() {
  if (busy.value) return
  if (!account.value.trim() || !password.value.trim()) {
    tip.value = '请填写管理员道号与信物～'
    return
  }
  busy.value = true
  tip.value = '正在核验…'
  try {
    const data = await adminLogin(account.value.trim(), password.value)
    setAdminToken(data.token)
    localStorage.setItem('huyao_admin_user', data.username || account.value.trim())
    router.push('/admin')
  } catch (e) {
    tip.value = e?.message || '登录未成，请确认道号与信物～'
  } finally {
    busy.value = false
  }
}

function goHome() { router.push('/home') }
</script>

<template>
  <div class="admin-login-page">
    <svg class="thread thread-tl" width="260" height="200" viewBox="0 0 260 200">
      <path d="M-10 -10 C 60 30, 20 90, 90 110 S 190 150, 150 200" fill="none" stroke="#b03a3a" stroke-width="2.5" stroke-linecap="round" opacity="0.7"/>
    </svg>
    <svg class="tree" width="380" height="300" viewBox="0 0 380 300">
      <path d="M60 300 L70 180 Q72 150 60 120 Q90 140 100 110 Q120 150 140 100 Q150 160 170 120 Q190 170 200 130 Q215 190 230 300 Z" fill="#5a4a3c" opacity="0.35"/>
      <circle cx="130" cy="90" r="46" fill="#b03a3a" opacity="0.22"/>
    </svg>

    <div class="wrap">
      <div class="card">
        <span class="corner tl"/><span class="corner tr"/>
        <span class="corner br"/><span class="corner bl"/>

        <div class="admin-seal">管</div>
        <h1 class="title">后 台 管 理</h1>
        <p class="subtitle">狐缘阁 · 数据总枢</p>
        <div class="divider"><span>❀</span></div>

        <div class="field">
          <label>管理员道号</label>
          <input v-model.trim="account" type="text" placeholder="请输入管理账号"
                 autocomplete="username" @keyup.enter="handleLogin" />
        </div>
        <div class="field">
          <label>信 物</label>
          <input v-model="password" type="password" placeholder="请输入密码"
                 autocomplete="current-password" @keyup.enter="handleLogin" />
        </div>

        <p class="tip" :class="{ err: tip && !tip.includes('…') }">{{ tip || '仅管理员账号可进入后台' }}</p>

        <button class="btn-login" :disabled="busy" @click="handleLogin">
          {{ busy ? '核 验 中…' : '进 入 后 台' }}
        </button>
        <p class="footer"><a @click="goHome">← 返回前台首页</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-login-page {
  min-height: 100vh; position: relative; overflow: hidden;
  font-family: "STXingkai", "华文行楷", "STKaiti", "楷体", "KaiTi", serif;
  color: #4a3b32; background: none;
}
.thread { position: fixed; z-index: 2; pointer-events: none; }
.thread-tl { top: 0; left: 0; }
.tree { position: fixed; left: -40px; bottom: -30px; z-index: 1; pointer-events: none; opacity: .5; }
.wrap { position: relative; z-index: 10; height: 100vh; display: flex; align-items: center; justify-content: center; }
.card {
  position: relative; width: 420px; padding: 46px 48px 36px;
  background: rgba(250,245,234,.55); backdrop-filter: blur(3px);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 12px 40px rgba(74,59,50,.18),
              inset 0 0 60px rgba(245,230,210,.14);
  text-shadow: 0 0 5px rgba(255,253,246,1), 0 0 12px rgba(255,253,246,.9);
}
.card::before { content:""; position:absolute; inset:-9px; border:1px dashed rgba(184,146,74,.55); border-radius:24px; pointer-events:none; }
.corner { position:absolute; width:46px; height:46px; pointer-events:none;
  background: radial-gradient(circle at 30% 70%, transparent 6px, #b8924a 7px, #b8924a 9px, transparent 10px),
              radial-gradient(circle at 55% 45%, transparent 4px, #b8924a 5px, #b8924a 7px, transparent 8px); }
.corner.tl { top:-12px; left:-12px; }
.corner.tr { top:-12px; right:-12px; transform:rotate(90deg); }
.corner.br { bottom:-12px; right:-12px; transform:rotate(180deg); }
.corner.bl { bottom:-12px; left:-12px; transform:rotate(270deg); }
.admin-seal {
  width:64px; height:64px; margin:0 auto 14px; display:flex; align-items:center; justify-content:center;
  font-size:32px; color:#f5efe3; background:linear-gradient(135deg,#b03a3a,#8e2c2c);
  border:2px solid #b8924a; border-radius:12px; box-shadow:0 6px 18px rgba(142,44,44,.35);
}
.title { text-align:center; font-size:30px; letter-spacing:8px; color:#8e2c2c; margin-bottom:6px; }
.subtitle { text-align:center; font-size:13px; letter-spacing:3px; color:#33403e; margin-bottom:28px; }
.divider { display:flex; align-items:center; gap:12px; margin:0 auto 26px; width:70%; }
.divider::before,.divider::after { content:""; flex:1; height:1px; background:linear-gradient(90deg,transparent,#b8924a,transparent); }
.divider span { color:#b03a3a; font-size:15px; }
.field { margin-bottom:20px; }
.field label { display:block; font-size:15px; letter-spacing:2px; color:#8e2c2c; margin-bottom:8px; }
.field input {
  width:100%; height:46px; padding:0 16px; font-family:inherit; font-size:16px; letter-spacing:1px;
  color:#4a3b32; background:rgba(255,253,246,.8); border:1px solid rgba(184,146,74,.6);
  border-radius:10px; outline:none; transition:box-shadow .35s ease, border-color .35s ease;
}
.field input:focus { border-color:#b03a3a; box-shadow:0 0 0 3px rgba(176,58,58,.12), 0 0 14px rgba(176,58,58,.18); }
.tip { min-height:20px; margin:-4px 0 16px; text-align:center; font-size:12.5px; letter-spacing:1px; color:#8a6a20; }
.tip.err { color:#b03a3a; }
.btn-login {
  width:100%; height:50px; font-family:inherit; font-size:20px; letter-spacing:10px; text-indent:10px;
  color:#f5efe3; background:linear-gradient(135deg,#b03a3a,#8e2c2c);
  border:1px solid #b8924a; border-radius:12px; cursor:pointer;
  transition:box-shadow .35s ease, transform .15s ease, filter .35s ease;
}
.btn-login:hover:not(:disabled) { box-shadow:0 0 18px rgba(176,58,58,.4); filter:brightness(1.06); }
.btn-login:active:not(:disabled) { transform:scale(.96); }
.btn-login:disabled { opacity:.65; cursor:wait; }
.footer { margin-top:22px; text-align:center; font-size:14px; }
.footer a { color:#b03a3a; text-decoration:none; cursor:pointer; }
.footer a:hover { text-decoration:underline; }
</style>
