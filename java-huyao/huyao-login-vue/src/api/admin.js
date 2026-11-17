/**
 * 后台管理系统接口封装。
 * 所有请求都带管理员会话令牌 X-Admin-Token；令牌存 localStorage，
 * 与前台登录用户隔离，退出后台时清除。
 */
import request from './request'

export const ADMIN_TOKEN_KEY = 'huyao_admin_token'

export function getAdminToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}
export function setAdminToken(token) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token || '')
}
export function clearAdminToken() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
}

/** 后台登录（无注册、无验证码）：仅管理员账号可用 */
export function adminLogin(username, password) {
  // 登录接口本身不需要令牌，单独用不带拦截头的请求
  return request.post('/admin/login', { username, password })
}

/** 校验当前令牌是否有效 */
export function adminSession() {
  return request.get('/admin/session', { headers: { 'X-Admin-Token': getAdminToken() } })
}

/** 退出后台 */
export function adminLogout() {
  return request.post('/admin/logout', {}, { headers: { 'X-Admin-Token': getAdminToken() } })
}

const adminHeaders = () => ({ headers: { 'X-Admin-Token': getAdminToken() } })

/* ---------- 续缘相册（官方相册 tb_couple，可增删改查+换图） ---------- */
export function adminCoupleList() {
  return request.get('/admin/couples', adminHeaders())
}
export function adminCoupleCreate(data) {
  return request.post('/admin/couples', data, adminHeaders())
}
export function adminCoupleUpdate(id, data) {
  return request.put(`/admin/couples/${id}`, data, adminHeaders())
}
export function adminCoupleImage(id, image) {
  return request.patch(`/admin/couples/${id}/image`, { image }, adminHeaders())
}
export function adminCoupleDelete(id) {
  return request.delete(`/admin/couples/${id}`, adminHeaders())
}

/* ---------- 用户管理：增删改查 + 权限 ---------- */
export function adminUserList() {
  return request.get('/admin/users', adminHeaders())
}
export function adminUserCreate(data) {
  return request.post('/admin/users', data, adminHeaders())
}
export function adminUserUpdate(id, data) {
  return request.put(`/admin/users/${id}`, data, adminHeaders())
}
export function adminUserDelete(id) {
  return request.delete(`/admin/users/${id}`, adminHeaders())
}

/* ---------- 法宝图鉴：增删改查 + 换图 ---------- */
export function adminTreasureList() {
  return request.get('/admin/treasures', adminHeaders())
}
export function adminTreasureCreate(data) {
  return request.post('/admin/treasures', data, adminHeaders())
}
export function adminTreasureUpdate(id, data) {
  return request.put(`/admin/treasures/${id}`, data, adminHeaders())
}
export function adminTreasureImage(id, image) {
  return request.patch(`/admin/treasures/${id}/image`, { image }, adminHeaders())
}
export function adminTreasureDelete(id) {
  return request.delete(`/admin/treasures/${id}`, adminHeaders())
}

/* ---------- 同心缘簿（用户投稿）：只提供列表与删除，没有任何修改接口 ---------- */
export function adminUserCoupleList() {
  return request.get('/admin/user-couples', adminHeaders())
}
export function adminUserCoupleDelete(id) {
  return request.delete(`/admin/user-couples/${id}`, adminHeaders())
}
