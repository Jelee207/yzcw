<script setup>
/**
 * Register.vue —— 《狐妖小红娘》主题注册页
 * 风格与登录页一致：水墨背景 + 桃花花瓣 + 姻缘红线 + 云纹边框
 * 表单：种族（人/妖）、性别（男/女）、用户名、密码、确认密码、图像验证码
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, register } from '../api/user'

const router = useRouter()

/* ---------- 表单状态 ---------- */
const race     = ref('人')     // 种族：人 / 妖
const gender   = ref('')      // 性别：男 / 女
const username = ref('')      // 用户名（道号）
const password = ref('')      // 密码（信物）
const confirm  = ref('')      // 确认密码
const captcha  = ref('')      // 用户输入的验证码
const captchaId  = ref('')    // 验证码标识（后端下发）
const captchaImg = ref('')    // 验证码图片（base64）

/* ---------- 花瓣飘落 ---------- */
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

/* ---------- 图像验证码（后端下发 base64 图片） ---------- */
async function refreshCaptcha() {
  try {
    const data = await getCaptcha() // { captchaId, captchaImg }
    captchaId.value = data.captchaId
    captchaImg.value = data.captchaImg
    captcha.value = ''
  } catch (e) {
    // 错误提示已在拦截器统一处理
  }
}

/* ---------- 提交注册：调用后端接口 ---------- */
async function handleRegister() {
  if (!race.value)   return alert('请选择你的出身（人 / 妖）～')
  if (!gender.value) return alert('请选择你的性别（男 / 女）～')
  if (!username.value.trim())        return alert('道号（用户名）不可为空～')
  if (password.value.length < 6)     return alert('信物（密码）至少 6 位，才够牢靠～')
  if (password.value !== confirm.value) return alert('两次信物不一致，还请重新确认～')
  if (!captcha.value.trim())         return alert('请填写图形验证码～')
  try {
    await register({
      username: username.value,
      password: password.value,
      confirmPassword: confirm.value,
      race: race.value,
      gender: gender.value,
      captchaId: captchaId.value,
      captcha: captcha.value,
    })
    alert('缘已结下，愿君早日寻得良人。')
    router.push('/login')
  } catch (e) {
    // 失败提示已由拦截器处理，刷新验证码
    refreshCaptcha()
  }
}

onMounted(() => {
  petals.value = makePetals()
  refreshCaptcha()
})
</script>

<template>
  <div class="register-page">
    <!-- 飘落桃花花瓣层 -->
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

    <!-- ============ 注册卡片（云纹古风边框） ============ -->
    <div class="wrap">
      <div class="card">
        <span class="corner tl"/><span class="corner tr"/>
        <span class="corner br"/><span class="corner bl"/>

        <h1 class="title">狐缘阁</h1>
        <p class="subtitle">缔结新缘 · 许君一世相思</p>
        <div class="divider"><span>❀</span></div>

        <!-- 种族选择：人 / 妖 -->
        <div class="field">
          <label>出 身</label>
          <div class="seg">
            <span class="seg-item" :class="{ on: race === '人' }" @click="race = '人'">人</span>
            <span class="seg-item" :class="{ on: race === '妖' }" @click="race = '妖'">妖</span>
          </div>
        </div>

        <!-- 性别选择：男 / 女 -->
        <div class="field">
          <label>性 别</label>
          <div class="seg">
            <span class="seg-item" :class="{ on: gender === '男' }" @click="gender = '男'">男</span>
            <span class="seg-item" :class="{ on: gender === '女' }" @click="gender = '女'">女</span>
          </div>
        </div>

        <!-- 用户名 -->
        <div class="field">
          <label>道 号</label>
          <input v-model.trim="username" type="text" placeholder="请起一个名姓或道号"
                 autocomplete="username" />
        </div>

        <!-- 密码 -->
        <div class="field">
          <label>信 物</label>
          <input v-model="password" type="password" placeholder="请输入随身信物（至少 6 位）"
                 autocomplete="new-password" />
        </div>

        <!-- 确认密码 -->
        <div class="field">
          <label>再 验</label>
          <input v-model="confirm" type="password" placeholder="请再输入一次信物"
                 autocomplete="new-password" />
        </div>

        <!-- 图像验证码 -->
        <div class="field">
          <label>缘 印</label>
          <div class="captcha-row">
            <input v-model.trim="captcha" type="text" placeholder="请输入图形验证码"
                   @keyup.enter="handleRegister" />
            <img :src="captchaImg" class="captcha-img" @click="refreshCaptcha"
                 alt="验证码" title="点击刷新验证码" />
          </div>
        </div>

        <!-- 注册按钮 -->
        <button class="btn-register" @click="handleRegister">结 缘</button>

        <p class="footer">已有尘缘？<a @click="router.push('/login')">返回登录</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ============ 古风基调 ============ */
.register-page {
  min-height: 100vh; overflow: hidden; position: relative;
  font-family: "STXingkai", "华文行楷", "STKaiti", "楷体", "KaiTi", serif;
  color: #4a3b32;
  background:
    radial-gradient(ellipse 60% 45% at 18% 22%, rgba(127,154,153,.22), transparent 70%),
    radial-gradient(ellipse 50% 40% at 82% 75%, rgba(176,58,58,.14), transparent 70%),
    radial-gradient(ellipse 45% 35% at 70% 15%, rgba(184,146,74,.12), transparent 70%),
    linear-gradient(160deg, #f7f2e7 0%, #efe6d4 45%, #e8dcc8 100%);
}

/* ============ 花瓣 ============ */
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
.wrap { position: relative; z-index: 10; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px; }
.card {
  position: relative; width: 420px; padding: 46px 48px 40px;
  background: rgba(250, 245, 234, .82); backdrop-filter: blur(6px);
  border: 2px solid #b8924a; border-radius: 18px;
  box-shadow: 0 0 0 1px rgba(176,58,58,.25), 0 12px 40px rgba(74,59,50,.18),
              inset 0 0 60px rgba(245, 230, 210, .6);
}
.card::before { content: ""; position: absolute; inset: -9px; border: 1px dashed rgba(184,146,74,.55); border-radius: 24px; pointer-events: none; }
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
.subtitle { text-align: center; font-size: 14px; letter-spacing: 3px; color: #7f9a99; margin-bottom: 30px; }
.divider { display: flex; align-items: center; gap: 12px; margin: 0 auto 26px; width: 70%; }
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: linear-gradient(90deg, transparent, #b8924a, transparent); }
.divider span { color: #b03a3a; font-size: 15px; }

/* ============ 表单项 ============ */
.field { margin-bottom: 18px; }
.field label { display: block; font-size: 15px; letter-spacing: 2px; color: #8e2c2c; margin-bottom: 8px; }
.field input {
  width: 100%; height: 46px; padding: 0 16px;
  font-family: inherit; font-size: 16px; letter-spacing: 1px; color: #4a3b32;
  background: rgba(255,253,246,.75); border: 1px solid rgba(184,146,74,.6);
  border-radius: 10px; outline: none;
  transition: box-shadow .35s ease, border-color .35s ease;
}
.field input::placeholder { color: rgba(127,154,153,.75); }
.field input:focus {
  border-color: #b03a3a;
  box-shadow: 0 0 0 3px rgba(176,58,58,.12), 0 0 14px rgba(176,58,58,.18);
}

/* ============ 分段选择（人/妖、男/女） ============ */
.seg { display: flex; gap: 12px; }
.seg-item {
  flex: 1; height: 42px; display: flex; align-items: center; justify-content: center;
  font-size: 16px; letter-spacing: 2px; color: #7f9a99; cursor: pointer;
  background: rgba(255,253,246,.7); border: 1px solid rgba(184,146,74,.6);
  border-radius: 10px; user-select: none;
  transition: all .3s ease;
}
.seg-item:hover { color: #b03a3a; border-color: #b03a3a; }
.seg-item.on {
  color: #f5efe3; background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border-color: #8e2c2c;
  box-shadow: 0 0 12px rgba(176,58,58,.3);
}

/* ============ 验证码 ============ */
.captcha-row { display: flex; gap: 12px; align-items: stretch; }
.captcha-row input { flex: 1; }
.captcha-img {
  width: 120px; height: 46px; border-radius: 10px; cursor: pointer;
  border: 1px solid rgba(184,146,74,.6); flex-shrink: 0;
  object-fit: cover; display: block;
  transition: box-shadow .3s ease, border-color .3s ease;
}
.captcha-img:hover { border-color: #b03a3a; box-shadow: 0 0 12px rgba(176,58,58,.25); }

/* ============ 注册按钮 ============ */
.btn-register {
  width: 100%; height: 50px; margin-top: 4px;
  font-family: inherit; font-size: 20px; letter-spacing: 10px; text-indent: 10px;
  color: #f5efe3;
  background: linear-gradient(135deg, #b03a3a, #8e2c2c);
  border: 1px solid #b8924a; border-radius: 12px; cursor: pointer;
  transition: box-shadow .35s ease, transform .15s ease, filter .35s ease;
}
.btn-register:hover { box-shadow: 0 0 18px rgba(176,58,58,.4), 0 6px 18px rgba(142,44,44,.3); filter: brightness(1.06); }
.btn-register:active { transform: scale(.96); }

/* ============ 登录跳转 ============ */
.footer { margin-top: 22px; text-align: center; font-size: 14px; color: #7f9a99; }
.footer a { color: #b03a3a; text-decoration: none; cursor: pointer; }
.footer a:hover { text-decoration: underline; }
</style>
