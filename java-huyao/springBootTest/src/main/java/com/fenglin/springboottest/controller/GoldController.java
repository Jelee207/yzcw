package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 前台金币同步接口。
 * 金币余额以数据库 tb_user.gold 为准：后台改了立即对前台生效；
 * 前台每日领金 / 抽签消耗后把最新余额写回。每日领取次数、冷却等规则状态仍在前端 localStorage。
 */
@RestController
@RequestMapping("/api/gold")
public class GoldController {

    @Autowired
    private UserMapper userMapper;

    /** 读取指定用户的金币余额 */
    @GetMapping
    public Result<Map<String, Object>> get(@RequestParam String username) {
        if (username == null || username.isBlank()) return Result.fail(400, "道号不可为空～");
        User u = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (u == null) return Result.fail(404, "未寻得此用户～");
        Map<String, Object> data = new HashMap<>();
        data.put("username", u.getUsername());
        data.put("coins", u.getGold() == null ? 0 : u.getGold());
        return Result.ok(data);
    }

    /** 同步金币余额（前台领金 / 抽签后写回，余额不小于 0） */
    @PostMapping
    public Result<Map<String, Object>> sync(@RequestBody Map<String, Object> body) {
        String username = body.get("username") == null ? "" : String.valueOf(body.get("username")).trim();
        if (username.isEmpty()) return Result.fail(400, "道号不可为空～");
        User u = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (u == null) return Result.fail(404, "未寻得此用户～");
        int coins = 0;
        try {
            Object c = body.get("coins");
            coins = c instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(c));
        } catch (Exception ignored) { }
        coins = Math.max(0, coins);
        userMapper.update(null, new UpdateWrapper<User>().eq("id", u.getId()).set("gold", coins));
        Map<String, Object> data = new HashMap<>();
        data.put("coins", coins);
        return Result.ok(data);
    }
}
