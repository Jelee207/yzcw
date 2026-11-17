import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import AdminLogin from '../views/AdminLogin.vue'
import Admin from '../views/Admin.vue'

// 路由配置：/login 登录页，/register 注册页，/home 续缘主页，/admin 后台管理系统
const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/home', name: 'Home', component: Home },
  { path: '/admin/login', name: 'AdminLogin', component: AdminLogin },
  { path: '/admin', name: 'Admin', component: Admin,
    // 后台守卫：无管理员令牌则退回后台登录页；令牌有效性由 Admin.vue 进入后再向后端校验
    beforeEnter: (to, from, next) => {
      if (localStorage.getItem('huyao_admin_token')) next()
      else next('/admin/login')
    }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
