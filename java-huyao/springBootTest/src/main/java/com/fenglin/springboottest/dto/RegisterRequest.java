package com.fenglin.springboottest.dto;

/**
 * 注册请求参数
 * username 用户名，password 密码，confirmPassword 确认密码，
 * race 种族（人/妖），gender 性别（男/女），captchaId 验证码标识，captcha 验证码内容
 */
public record RegisterRequest(
        String username,
        String password,
        String confirmPassword,
        String race,
        String gender,
        String captchaId,
        String captcha
) {
}
