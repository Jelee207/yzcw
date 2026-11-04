import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'

// 创建 Vue 应用，挂载路由并渲染
createApp(App).use(router).mount('#app')
