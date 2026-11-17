package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.UserCoupleRequest;
import com.fenglin.springboottest.entity.UserCouple;
import com.fenglin.springboottest.mapper.UserCoupleMapper;
import com.fenglin.springboottest.service.AdminSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 苦情树 · 「我的小情侣」共享相册接口。
 *
 * GET  /api/user-couples -> 所有人都能看到的投稿列表（按投稿时间倒序）
 * POST /api/user-couples -> 登录用户投稿，写入数据库后所有人可见
 * DELETE /api/user-couples/{id}?username=... -> 仅投稿人本人可删除自己的相片
 * DELETE /api/admin/user-couples/{id} -> 后台也仅管理员本人可删除自己上传的相片（需 X-Admin-Token）
 *
 * 投稿时记录投稿人道号（contributor）；删除时只认道号一致，其他人的相片一律删不了。
 * 早期投稿生成的删除令牌仅作兼容保留，不再作为删除凭证。
 *
 * 数据存在独立的 tb_user_couple 表，不与官方相册 tb_couple 混在一起。
 */
@RestController
@RequestMapping("/api")
public class UserCoupleController {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA);

    @Autowired
    private UserCoupleMapper userCoupleMapper;

    @Autowired
    private AdminSessionService adminSessionService;

    @Value("${huyao.admin.token:}")
    private String adminToken;

    /** 全员共享：按时间倒序展示所有投稿 */
    @GetMapping("/user-couples")
    public Result<List<Map<String, Object>>> list() {
        List<UserCouple> rows = userCoupleMapper.selectList(new QueryWrapper<UserCouple>()
                .eq("enabled", 1)
                .orderByDesc("create_time", "id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (UserCouple c : rows) data.add(toView(c));
        return Result.ok(data);
    }

    /** 后台：全部投稿列表（含隐藏），仅管理员可见 */
    @GetMapping("/admin/user-couples")
    public Result<List<Map<String, Object>>> adminList(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        List<UserCouple> rows = userCoupleMapper.selectList(new QueryWrapper<UserCouple>()
                .orderByDesc("create_time", "id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (UserCouple c : rows) data.add(toView(c));
        return Result.ok(data);
    }

    /** 投稿：两人名字与图片地址必填，出处、题句可留空 */
    @PostMapping("/user-couples")
    public Result<Map<String, Object>> create(@RequestBody UserCoupleRequest req) {
        if (req == null || isBlank(req.name())) {
            return Result.fail(400, "请先写下两个人的名字～");
        }
        String name = cut(req.name(), 128);
        String image = cut(req.image(), 2_000_000);
        if (image.isEmpty()) {
            return Result.fail(400, "请填上图片地址，相片不可留空～");
        }
        if (!isSafeImage(image)) {
            return Result.fail(400, "图片地址只支持 / 开头、http(s) 链接或 data:image 图片～");
        }
        String contributor = cut(req.contributor(), 128);
        if (contributor.isEmpty()) {
            return Result.fail(400, "请先登录，再题名留册～");
        }

        UserCouple c = new UserCouple();
        c.setSlug("u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        c.setDeleteToken(null);
        c.setName(name);
        c.setChapter(defaultIfBlank(req.chapter(), "我 心 中 的 故 事"));
        c.setVerse(cut(req.verse(), 500));
        c.setImageUrl(image);
        c.setContributor(contributor);
        c.setEnabled(1);
        c.setCreateTime(LocalDateTime.now());
        userCoupleMapper.insert(c);
        return Result.ok(toView(c));
    }

    /**
     * 投稿人自助删除：只认投稿时记录道号，当前道号不一致一律拒绝。
     */
    @DeleteMapping("/user-couples/{id}")
    public Result<Void> deleteMine(@PathVariable Long id,
                                   @RequestParam(value = "username", required = false) String username) {
        UserCouple c = userCoupleMapper.selectById(id);
        if (c == null) return Result.fail(404, "未寻得这一页～");
        if (isBlank(username)) return Result.fail(403, "请先登录，再删除自己题名的相片～");
        if (!username.equals(c.getContributor())) {
            return Result.fail(403, "仅可删除自己上传的相片，这一页不是你题名的～");
        }
        userCoupleMapper.deleteById(id);
        return Result.ok();
    }

    /** 后台同心缘簿：只读 + 无修改接口；管理员也只能删除自己上传的投稿 */
    @DeleteMapping("/admin/user-couples/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        String admin = adminSessionService.verify(token);
        if (admin == null) return Result.fail(403, deniedMessage());
        UserCouple c = userCoupleMapper.selectById(id);
        if (c == null) return Result.fail(404, "未寻得这一页～");
        if (!admin.equals(c.getContributor())) {
            return Result.fail(403, "后台亦仅可删除自己上传的相片～");
        }
        userCoupleMapper.deleteById(id);
        return Result.ok();
    }

    /* ---------------- 内部工具 ---------------- */

    private boolean adminAllowed(String token) {
        if (adminSessionService.verify(token) != null) return true;
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }

    private String deniedMessage() {
        return "无权访问后台，请先以管理员账号登录～";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String defaultIfBlank(String s, String fallback) {
        return isBlank(s) ? fallback : cut(s.trim(), 128);
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        String v = s.trim();
        return v.length() <= max ? v : v.substring(0, max);
    }

    /** 避免把 javascript: 之类危险地址塞进 img src */
    private static boolean isSafeImage(String image) {
        if (image.isEmpty()) return true;
        String s = image.toLowerCase(Locale.ROOT);
        return s.startsWith("/") || s.startsWith("http://") || s.startsWith("https://")
                || s.startsWith("data:image/");
    }

    /** 输出形状与官方相册一致，前端可复用同一套卡片；删除只认 contributor */
    private static Map<String, Object> toView(UserCouple c) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("slug", c.getSlug());
        m.put("name", c.getName());
        m.put("mark", c.getMark());
        m.put("chapter", c.getChapter());
        m.put("era", c.getEra());
        m.put("relic", c.getRelic());
        m.put("status", c.getStatus());
        m.put("brief", c.getBrief());
        m.put("story", c.getStory());
        m.put("verse", c.getVerse());
        m.put("tags", splitTags(c.getTags()));
        m.put("image", c.getImageUrl() == null ? "" : c.getImageUrl());
        m.put("imageSlot", c.getImageSlot());
        m.put("contributor", c.getContributor());
        m.put("source", "user");
        m.put("createTime", c.getCreateTime() == null ? "" : c.getCreateTime().format(TIME_FMT));
        return m;
    }

    private static List<String> splitTags(String tags) {
        if (isBlank(tags)) return List.of();
        List<String> out = new ArrayList<>();
        for (String s : tags.split("[,，、]")) {
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }
}
