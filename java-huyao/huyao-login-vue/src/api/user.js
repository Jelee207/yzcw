import request from './request'

/**
 * 用户相关接口
 */

// 获取图像验证码：返回 { captchaId, captchaImg }
export function getCaptcha() {
  return request.get('/captcha')
}

// 登录：返回用户信息 { id, username, race, gender, createTime }
export function login(data) {
  return request.post('/login', data)
}

// 注册：参数 { username, password, confirmPassword, race, gender, captchaId, captcha }
export function register(data) {
  return request.post('/register', data)
}

// 读取头像：返回 { avatar }（base64 data URL 或空串）
export function getAvatar(username) {
  return request.get('/avatar', { params: { username } })
}

// 上传 / 更换头像：参数 { username, image }，image 为压缩后的 base64 data URL
export function saveAvatar(data) {
  return request.post('/avatar', data)
}

// 移除头像
export function removeAvatar(username) {
  return request.delete('/avatar', { params: { username } })
}
