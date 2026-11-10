<script setup>
// 根组件：全局背景动效 + 路由出口，页面切换带淡入过渡
import BackgroundFX from './components/BackgroundFX.vue'
</script>

<template>
  <!-- 全局背景动效：跨路由常驻，切换页面时动画不中断 -->
  <BackgroundFX />
  <div class="app-stage">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style>
/* 舞台置于背景动效之上，保证卡片与文字不被粒子覆盖 */
.app-stage { position: relative; z-index: 1; min-height: 100vh; }

/* 页面切换淡入淡出过渡 */
.fade-enter-active,
.fade-leave-active { transition: opacity .4s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }
</style>
