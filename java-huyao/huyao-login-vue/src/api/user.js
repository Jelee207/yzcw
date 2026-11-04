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
