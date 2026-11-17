package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AdminSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台 · 用户管理。
 * 列表 / 新增 / 编辑（资料、权限、密码）/ 删除，均需管理员令牌。
 * 安全约束：不能删除当前登录的管理员，也不能把当前管理员降为普通用户。
 * 密码按明文存储（与前台注册一致）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminSessionService adminSessionService;

    @Value("${huyao.admin.token:}")
    private String adminToken;

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> list(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        List<User> rows = userMapper.selectList(new QueryWrapper<User>().orderByAsc("id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (User u : rows) data.add(toView(u));
        return Result.ok(data);
    }

    /** 新增用户：道号唯一；密码明文；权限默认 1 */
    @PostMapping("/users")
    public Result<Map<String, Object>> create(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        String username = str(body.get("username")).trim();
        if (username.isEmpty()) return Result.fail(400, "道号不可为空～");
        if (userMapper.selectCount(new QueryWrapper<User>().eq("username", username)) > 0) {
            return Result.fail(500, "此道号已存在～");
        }
        String password = str(body.get("password"));
        if (password.isEmpty()) return Result.fail(400, "密码不可为空～");
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);
        u.setRace(blankToNull(str(body.get("race"))));
        u.setGender(blankToNull(str(body.get("gender"))));
        Integer perm = intOf(body.get("permission"), 1);
        u.setPermission(perm == 0 ? 0 : 1);
        Integer gold = intOf(body.get("gold"), 0);
        u.setGold(Math.max(0, gold));
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);
        return Result.ok(toView(u));
    }

    /** 编辑用户：只覆盖传了的字段；password 传了才改 */
    @PutMapping("/users/{id}")
    public Result<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail(404, "未寻得此用户～");
        String currentAdmin = adminSessionService.verify(token);
        // 不允许把当前登录的管理员降权，避免把自己锁在后台外
        if (u.getUsername().equals(currentAdmin) && body.containsKey("permission")
                && intOf(body.get("permission"), 1) != 0) {
            return Result.fail(403, "不可将当前管理员降为普通用户～");
        }
        if (body.containsKey("username")) {
            String name = str(body.get("username")).trim();
            if (name.isEmpty()) return Result.fail(400, "道号不可为空～");
            Long dup = userMapper.selectCount(new QueryWrapper<User>()
                    .eq("username", name).ne("id", id));
            if (dup != null && dup > 0) return Result.fail(500, "此道号已存在～");
            u.setUsername(name);
        }
        if (body.containsKey("password")) {
            String pwd = str(body.get("password"));
            if (!pwd.isEmpty()) u.setPassword(pwd);
        }
        if (body.containsKey("race")) u.setRace(blankToNull(str(body.get("race"))));
        if (body.containsKey("gender")) u.setGender(blankToNull(str(body.get("gender"))));
        if (body.containsKey("permission")) {
            Integer perm = intOf(body.get("permission"), 1);
            u.setPermission(perm == 0 ? 0 : 1);
        }
        if (body.containsKey("gold")) {
            Integer gold = intOf(body.get("gold"), 0);
            u.setGold(Math.max(0, gold));
        }
        userMapper.updateById(u);
        return Result.ok(toView(u));
    }

    /** 删除用户：不能删除当前登录的管理员 */
    @DeleteMapping("/users/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        User u = userMapper.selectById(id);
        if (u == null) return Result.fail(404, "未寻得此用户～");
        String currentAdmin = adminSessionService.verify(token);
        if (u.getUsername().equals(currentAdmin)) {
            return Result.fail(403, "不可删除当前登录的管理员账号～");
        }
        // 头像随用户清理
        userMapper.update(null, new UpdateWrapper<User>().eq("id", id).set("avatar", ""));
        userMapper.deleteById(id);
        return Result.ok();
    }

    private boolean adminAllowed(String token) {
        if (adminSessionService.verify(token) != null) return true;
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }
    private String deniedMessage() { return "无权访问后台，请先以管理员账号登录～"; }

    private static Map<String, Object> toView(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("race", u.getRace() == null ? "" : u.getRace());
        m.put("gender", u.getGender() == null ? "" : u.getGender());
        m.put("permission", u.getPermission() == null ? 1 : u.getPermission());
        m.put("gold", u.getGold() == null ? 0 : u.getGold());
        m.put("hasAvatar", u.getAvatar() != null && !u.getAvatar().isEmpty());
        m.put("createTime", u.getCreateTime() == null ? "" : u.getCreateTime().toString());
        return m;
    }

    private static String str(Object v) { return v == null ? "" : String.valueOf(v); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
    private static int intOf(Object v, int fallback) {
        if (v == null) return fallback;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }
}
