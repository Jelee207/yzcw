package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.AvatarRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 个人空间：头像读取 / 上传 / 移除
 * 头像由前端裁剪压缩成 256x256 的 JPEG data URL，存入 tb_user.avatar，
 * 随登录接口一并返回，换设备或换浏览器也能看到。
 */
@RestController
@RequestMapping("/api")
public class ProfileController {

    @Autowired
    private UserMapper userMapper;

    /** 允许的图片格式；限制 data URL 长度约 1MB，避免撑爆请求 */
    private static final Pattern IMAGE_DATA_URL = Pattern.compile(
            "^data:image/(png|jpeg|jpg|webp|gif);base64,([A-Za-z0-9+/=]+)$",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_IMAGE_CHARS = 1_400_000;

    /** GET /api/avatar?username=xxx —— 读取头像 */
    @GetMapping("/avatar")
    public Result<Map<String, String>> getAvatar(@RequestParam String username) {
        User u = findUser(username);
        if (u == null) return Result.fail("未找到此道号～");
        return Result.ok(Map.of("avatar", u.getAvatar() == null ? "" : u.getAvatar()));
    }

    /** POST /api/avatar —— 保存 / 更换头像 */
    @PostMapping("/avatar")
    public Result<Map<String, String>> saveAvatar(@RequestBody AvatarRequest req) {
        if (req == null || req.username() == null || req.username().isBlank()) {
            return Result.fail("请先登录后再设置头像～");
        }
        String image = req.image();
        if (image == null || !IMAGE_DATA_URL.matcher(image).matches()) {
            return Result.fail("头像格式有误，请上传 JPG / PNG / WebP 图片～");
        }
        if (image.length() > MAX_IMAGE_CHARS) {
            return Result.fail("图片过大，请换一张小一些的～");
        }
        Matcher m = IMAGE_DATA_URL.matcher(image);
        m.matches();
        try {
            Base64.getDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            return Result.fail("头像数据有误，请重新选择图片～");
        }
        if (findUser(req.username()) == null) return Result.fail("未找到此道号～");
        userMapper.update(null, new UpdateWrapper<User>()
                .eq("username", req.username())
                .set("avatar", image));
        return Result.ok(Map.of("avatar", image));
    }

    /** DELETE /api/avatar?username=xxx —— 移除头像 */
    @DeleteMapping("/avatar")
    public Result<Void> removeAvatar(@RequestParam String username) {
        if (findUser(username) == null) return Result.fail("未找到此道号～");
        // 用 Wrapper 直接 set 空串，避免 MyBatis-Plus 实体策略忽略 null 字段
        userMapper.update(null, new UpdateWrapper<User>()
                .eq("username", username)
                .set("avatar", ""));
        return Result.ok();
    }

    private User findUser(String username) {
        if (username == null || username.isBlank()) return null;
        QueryWrapper<User> q = new QueryWrapper<>();
        q.eq("username", username).last("LIMIT 1");
        return userMapper.selectOne(q);
    }
}
