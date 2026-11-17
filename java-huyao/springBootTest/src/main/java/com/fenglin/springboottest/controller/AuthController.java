package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.AdminSessionService;
import com.fenglin.springboottest.service.CaptchaService;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口（登录 / 注册 / 验证码），供前端 Vue 调用
 * 统一前缀 /api，返回 Result JSON
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private AdminSessionService adminSessionService;

    /**
     * 获取图像验证码
     * GET /api/captcha
     * 返回：{ code, message, data: { captchaId, captchaImg } }
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(captchaService.generate());
    }

    /**
     * 注册
     * POST /api/register
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest req) {
        // 1. 参数校验
        if (req.username() == null || req.username().isBlank()) {
            return Result.fail("道号（用户名）不可为空～");
        }
        if (req.password() == null || req.password().length() < 6) {
            return Result.fail("信物（密码）至少 6 位～");
        }
        if (req.confirmPassword() == null || !req.confirmPassword().equals(req.password())) {
            return Result.fail("两次信物不一致，请重新确认～");
        }
        if (req.race() == null || req.gender() == null) {
            return Result.fail("请选择出身与性别～");
        }
        // 2. 验证码校验
        if (req.captcha() == null || !captchaService.verify(req.captchaId(), req.captcha())) {
            return Result.fail("验证码有误或已过期，请重新输入～");
        }
        // 3. 注册入库
        String err = userService.register(req.username(), req.password(), req.race(), req.gender());
        if (err != null) {
            return Result.fail(err);
        }
        return Result.ok();
    }

    /**
     * 登录
     * POST /api/login
     * 成功返回用户信息（密码已置空）
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest req) {
        if (req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            return Result.fail("道号与信物不可为空～");
        }
        User user = userService.login(req.username(), req.password());
        if (user == null) {
            return Result.fail("道号或信物有误，请重新输入～");
        }
        // 不返回密码
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 后台登录：仅权限为 0 的管理员可用，无需验证码、无注册入口
     * POST /api/admin/login
     * 成功返回 { token, username }
     */
    @PostMapping("/admin/login")
    public Result<Map<String, String>> adminLogin(@RequestBody LoginRequest req) {
        if (req == null || req.username() == null || req.username().isBlank()
                || req.password() == null || req.password().isBlank()) {
            return Result.fail("道号与信物不可为空～");
        }
        User admin = userService.loginAdmin(req.username().trim(), req.password());
        if (admin == null) {
            return Result.fail(403, "无权进入后台：仅管理员可用管理账号登录～");
        }
        String token = adminSessionService.issue(admin.getUsername());
        return Result.ok(Map.of("token", token, "username", admin.getUsername()));
    }

    /** 后台会话校验：前端进入后台时确认 token 仍有效 */
    @GetMapping("/admin/session")
    public Result<Map<String, String>> adminSession(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        String username = adminSessionService.verify(token);
        if (username == null) return Result.fail(403, "登录已过期，请重新登录～");
        return Result.ok(Map.of("username", username));
    }

    /** 后台退出登录 */
    @PostMapping("/admin/logout")
    public Result<Void> adminLogout(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        adminSessionService.invalidate(token);
        return Result.ok();
    }
}
