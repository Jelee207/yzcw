package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.TreasureRequest;
import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.mapper.TreasureMapper;
import com.fenglin.springboottest.service.AdminSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 法宝图鉴接口。
 * 前台：GET /api/treasures -> 只返回 enabled=1
 * 后台：/api/admin/treasures/** 全套增删改查 + 换图（X-Admin-Token 鉴权）
 */
@RestController
@RequestMapping("/api")
public class TreasureController {

    @Autowired
    private TreasureMapper treasureMapper;

    @Autowired
    private AdminSessionService adminSessionService;

    @Value("${huyao.admin.token:}")
    private String adminToken;

    /** 前台：展示中的法宝 */
    @GetMapping("/treasures")
    public Result<List<Map<String, Object>>> list() {
        List<Treasure> rows = treasureMapper.selectList(new QueryWrapper<Treasure>()
                .eq("enabled", 1)
                .orderByAsc("sort_no", "id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (Treasure t : rows) data.add(toView(t));
        return Result.ok(data);
    }

    @GetMapping("/admin/treasures")
    public Result<List<Map<String, Object>>> adminList(
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        List<Treasure> rows = treasureMapper.selectList(new QueryWrapper<Treasure>()
                .orderByAsc("sort_no", "id"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (Treasure t : rows) data.add(toView(t));
        return Result.ok(data);
    }

    @PostMapping("/admin/treasures")
    public Result<Map<String, Object>> create(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody TreasureRequest req) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        if (req == null || isBlank(req.slug()) || isBlank(req.name())) {
            return Result.fail(400, "标识与法宝名不可为空～");
        }
        if (treasureMapper.selectOne(new QueryWrapper<Treasure>()
                .eq("slug", req.slug()).last("LIMIT 1")) != null) {
            return Result.fail(500, "此标识已存在，换一个罢～");
        }
        Treasure t = new Treasure();
        apply(t, req);
        if (t.getEnabled() == null) t.setEnabled(1);
        if (t.getSortNo() == null) t.setSortNo(999);
        if (t.getPower() == null) t.setPower(3);
        if (t.getPrice() == null) t.setPrice(5);
        t.setCreateTime(LocalDateTime.now());
        treasureMapper.insert(t);
        return Result.ok(toView(t));
    }

    @PutMapping("/admin/treasures/{id}")
    public Result<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody TreasureRequest req) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        Treasure t = treasureMapper.selectById(id);
        if (t == null) return Result.fail(404, "未寻得这一器～");
        apply(t, req);
        t.setUpdateTime(LocalDateTime.now());
        treasureMapper.updateById(t);
        return Result.ok(toView(t));
    }

    /** 只改图片：本地路径 / 外链 / base64 data URL */
    @PatchMapping("/admin/treasures/{id}/image")
    public Result<Map<String, Object>> updateImage(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody Map<String, String> body) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        Treasure t = treasureMapper.selectById(id);
        if (t == null) return Result.fail(404, "未寻得这一器～");
        String image = body == null ? null : body.get("image");
        if (image == null) return Result.fail(400, "请带上 image 字段～");
        t.setImageUrl(image.trim());
        t.setUpdateTime(LocalDateTime.now());
        treasureMapper.updateById(t);
        return Result.ok(toView(t));
    }

    @DeleteMapping("/admin/treasures/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAllowed(token)) return Result.fail(403, deniedMessage());
        if (treasureMapper.selectById(id) == null) return Result.fail(404, "未寻得这一器～");
        treasureMapper.deleteById(id);
        return Result.ok();
    }

    /* ---------------- 内部工具 ---------------- */
    private boolean adminAllowed(String token) {
        if (adminSessionService.verify(token) != null) return true;
        return adminToken != null && !adminToken.isBlank() && adminToken.equals(token);
    }
    private String deniedMessage() { return "无权访问后台，请先以管理员账号登录～"; }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private void apply(Treasure t, TreasureRequest r) {
        if (r == null) return;
        if (r.slug() != null) t.setSlug(r.slug());
        if (r.name() != null) t.setName(r.name());
        if (r.alias() != null) t.setAlias(r.alias());
        if (r.mark() != null) t.setMark(r.mark());
        if (r.scroll() != null) t.setScroll(r.scroll());
        if (r.owner() != null) t.setOwner(r.owner());
        if (r.chapter() != null) t.setChapter(r.chapter());
        if (r.kind() != null) t.setKind(r.kind());
        if (r.power() != null) t.setPower(r.power());
        if (r.price() != null) t.setPrice(Math.max(0, r.price()));
        if (r.brief() != null) t.setBrief(r.brief());
        if (r.tale() != null) t.setTale(r.tale());
        if (r.verse() != null) t.setVerse(r.verse());
        if (r.tags() != null) t.setTags(flattenTags(r.tags()));
        if (r.image() != null) t.setImageUrl(r.image());
        if (r.sortNo() != null) t.setSortNo(r.sortNo());
        if (r.enabled() != null) t.setEnabled(r.enabled());
    }

    private static String flattenTags(Object v) {
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(v);
    }

    /** 输出形状与前端法宝对象一致：tags 为数组，img 与 image 都给出 */
    private static Map<String, Object> toView(Treasure t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("slug", t.getSlug());
        m.put("name", t.getName());
        m.put("alias", nullToEmpty(t.getAlias()));
        m.put("mark", nullToEmpty(t.getMark()));
        m.put("scroll", nullToEmpty(t.getScroll()));
        m.put("owner", nullToEmpty(t.getOwner()));
        m.put("chapter", nullToEmpty(t.getChapter()));
        m.put("kind", nullToEmpty(t.getKind()));
        m.put("power", t.getPower() == null ? 3 : t.getPower());
        m.put("price", t.getPrice() == null ? 5 : t.getPrice());
        m.put("brief", nullToEmpty(t.getBrief()));
        m.put("tale", nullToEmpty(t.getTale()));
        m.put("verse", nullToEmpty(t.getVerse()));
        m.put("tags", splitTags(t.getTags()));
        String image = t.getImageUrl() == null ? "" : t.getImageUrl();
        m.put("image", image);
        // 前端直接使用 img；无自定义图时按 slug 回退到内置插画
        m.put("img", image.isEmpty() ? "/treasures/" + t.getSlug() + ".svg" : image);
        m.put("sortNo", t.getSortNo());
        m.put("enabled", t.getEnabled());
        return m;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split("[,，、]"))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
