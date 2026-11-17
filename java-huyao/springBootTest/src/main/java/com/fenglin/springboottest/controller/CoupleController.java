package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.CoupleRequest;
import com.fenglin.springboottest.entity.Couple;
import com.fenglin.springboottest.mapper.CoupleMapper;
import com.fenglin.springboottest.service.AdminSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 苦情树 · 续缘相册接口
 *
 * 前台只读：
 *   GET    /api/couples                     -> 相册列表（只返回 enabled=1）
 *
 * 后台管理（未来的后台管理系统接这里，请求头带 X-Admin-Token）：
 *   GET    /api/admin/couples              -> 全部记录（含隐藏）
 *   POST   /api/admin/couples              -> 新增一对情侣
 *   PUT    /api/admin/couples/{id}         -> 修改档案（只覆盖传了的字段）
 *   PATCH  /api/admin/couples/{id}/image   -> 只改图片 {"image":"/couples/xxx.jpg"}
 *   DELETE /api/admin/couples/{id}         -> 删除
 *
 * 令牌由 application-dev.properties 的 huyao.admin.token 配置；
 * 未配置时所有后台写接口一律拒绝，避免相册数据被随意改动。
 */
@RestController
@RequestMapping("/api")
public class CoupleController {

    @Autowired
    private CoupleMapper coupleMapper;

    @Autowired
    private AdminSessionService adminSessionService;

    @Value("${huyao.admin.token:}")
    private String adminToken;

    /** 前台相册：给 Home.vue 用的展示数据 */
    @GetMapping("/couples")
    public Result<List<Map<String, Object>>> list() {
        List<Couple> rows = coupleMapper.selectList(new QueryWrapper<Couple>()
                .eq("enabled", 1)
                .orderByAsc("sort_no", "id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (Couple c : rows) data.add(toView(c));
        return Result.ok(data);
    }

    /** 后台：全部记录 */
    @GetMapping("/admin/couples")
    public Result<List<Map<String, Object>>> adminList(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        List<Couple> rows = coupleMapper.selectList(new QueryWrapper<Couple>()
                .orderByAsc("sort_no", "id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (Couple c : rows) data.add(toView(c));
        return Result.ok(data);
    }

    /** 后台：新增一对情侣 */
    @PostMapping("/admin/couples")
    public Result<Map<String, Object>> create(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody CoupleRequest req) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        if (req == null || isBlank(req.slug()) || isBlank(req.name())) {
            return Result.fail(400, "标识与情侣名不可为空～");
        }
        Couple exists = coupleMapper.selectOne(new QueryWrapper<Couple>()
                .eq("slug", req.slug()).last("LIMIT 1"));
        if (exists != null) return Result.fail(500, "此标识已存在，换一个罢～");

        Couple c = new Couple();
        apply(c, req);
        if (c.getEnabled() == null) c.setEnabled(1);
        if (c.getSortNo() == null) c.setSortNo(999);
        c.setCreateTime(LocalDateTime.now());
        coupleMapper.insert(c);
        return Result.ok(toView(c));
    }

    /** 后台：修改档案（PATCH 式，只覆盖非 null 字段） */
    @PutMapping("/admin/couples/{id}")
    public Result<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody CoupleRequest req) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        Couple c = coupleMapper.selectById(id);
        if (c == null) return Result.fail(404, "未寻得这一页～");
        apply(c, req);
        c.setUpdateTime(LocalDateTime.now());
        coupleMapper.updateById(c);
        return Result.ok(toView(c));
    }

    /** 后台：只改图片。image 可填 /couples/xxx.jpg、外链 URL 或 base64 data URL */
    @PatchMapping("/admin/couples/{id}/image")
    public Result<Map<String, Object>> updateImage(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody Map<String, String> body) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        Couple c = coupleMapper.selectById(id);
        if (c == null) return Result.fail(404, "未寻得这一页～");
        String image = body == null ? null : body.get("image");
        if (image == null) return Result.fail(400, "请带上 image 字段～");
        c.setImageUrl(image.trim());
        c.setUpdateTime(LocalDateTime.now());
        coupleMapper.updateById(c);
        return Result.ok(toView(c));
    }

    /** 后台：删除 */
    @DeleteMapping("/admin/couples/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        if (coupleMapper.selectById(id) == null) return Result.fail(404, "未寻得这一页～");
        coupleMapper.deleteById(id);
        return Result.ok();
    }

    /* ---------------- 内部工具 ---------------- */

    private boolean adminAllowed(String token) {
        // 管理员登录后签发的会话 token，或配置文件里的静态令牌，二者任一有效即可
        if (adminSessionService.verify(token) != null) return true;
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }

    private String deniedMessage() {
        return "无权访问后台，请先以管理员账号登录～";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 把请求体里非 null 的字段写进实体；null 表示「不改」 */
    private void apply(Couple c, CoupleRequest r) {
        if (r == null) return;
        if (r.slug() != null) c.setSlug(r.slug());
        if (r.name() != null) c.setName(r.name());
        if (r.mark() != null) c.setMark(r.mark());
        if (r.chapter() != null) c.setChapter(r.chapter());
        if (r.era() != null) c.setEra(r.era());
        if (r.eraType() != null) c.setEraType(r.eraType());
        if (r.relic() != null) c.setRelic(r.relic());
        if (r.status() != null) c.setStatus(r.status());
        if (r.brief() != null) c.setBrief(r.brief());
        if (r.story() != null) c.setStory(r.story());
        if (r.verse() != null) c.setVerse(r.verse());
        if (r.tags() != null) c.setTags(flattenTags(r.tags()));
        if (r.image() != null) c.setImageUrl(r.image());
        if (r.imageSlot() != null) c.setImageSlot(r.imageSlot());
        if (r.sortNo() != null) c.setSortNo(r.sortNo());
        if (r.enabled() != null) c.setEnabled(r.enabled());
    }

    /** 标签兼容 ["a","b"] 与 "a,b" 两种写法，库中统一存逗号分隔 */
    private static String flattenTags(Object v) {
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(v);
    }

    /** 统一输出形状，与 public/data/couples.json 的字段保持一致 */
    private static Map<String, Object> toView(Couple c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("slug", c.getSlug());
        m.put("name", c.getName());
        m.put("mark", c.getMark());
        m.put("chapter", c.getChapter());
        m.put("era", c.getEra());
        m.put("eraType", c.getEraType());
        m.put("relic", c.getRelic());
        m.put("status", c.getStatus());
        m.put("brief", c.getBrief());
        m.put("story", c.getStory());
        m.put("verse", c.getVerse());
        m.put("tags", splitTags(c.getTags()));
        m.put("image", c.getImageUrl() == null ? "" : c.getImageUrl());
        m.put("imageSlot", c.getImageSlot());
        m.put("sortNo", c.getSortNo());
        m.put("enabled", c.getEnabled());
        return m;
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split("[,，、]"))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
