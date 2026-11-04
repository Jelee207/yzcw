<script setup>
// 根组件：仅作为路由出口，页面切换带淡入过渡
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<style>
/* 页面切换淡入淡出过渡 */
.fade-enter-active,
.fade-leave-active { transition: opacity .4s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }
</style>
