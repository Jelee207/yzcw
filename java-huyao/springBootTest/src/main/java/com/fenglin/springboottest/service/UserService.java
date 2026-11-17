package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户业务层：注册、登录、密码加密
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /** 密码加盐，增加安全性 */
    private static final String SALT = "huyao";

    /**
     * 密码加密：MD5(盐 + 明文)
     */
    public String encrypt(String rawPassword) {
        return DigestUtils.md5DigestAsHex((SALT + rawPassword).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 注册：校验用户名唯一，密码按明文入库（本项目无需加密），权限默认 1（普通用户）
     * @return 成功返回 null，失败返回错误信息
     */
    public String register(String username, String password, String race, String gender) {
        // 1. 校验用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return "此道号已被他人所用，还请另择名姓～";
        }
        // 2. 组装用户并入库：明文密码 + 默认权限 1
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRace(race);
        user.setGender(gender);
        user.setPermission(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return null; // 注册成功
    }

    /**
     * 登录：校验用户名与密码。
     * 密码按明文比对；为兼容早期 MD5 账号，明文不匹配时再用旧加密方式比对一次。
     * @return 成功返回用户对象，失败返回 null
     */
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = userMapper.selectOne(wrapper);
        if (user == null || user.getPassword() == null) return null;
        String stored = user.getPassword();
        if (stored.equals(password) || stored.equals(encrypt(password))) {
            return user;
        }
        return null;
    }

    /**
     * 后台登录：在普通登录校验之上，要求权限为 0（管理员）
     * @return 管理员用户对象，非管理员或密码错误返回 null
     */
    public User loginAdmin(String username, String password) {
        User user = login(username, password);
        if (user != null && Integer.valueOf(0).equals(user.getPermission())) {
            return user;
        }
        return null;
    }

    /** 按道号查询用户（含权限），不存在返回 null */
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).last("LIMIT 1");
        return userMapper.selectOne(wrapper);
    }
}
