package com.fenglin.springboottest.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 苦情树相册表初始化。
 *
 * 项目里已有的 huyao 库未必会重新执行 sql/huyao.sql，所以这里在启动时
 * 用 CREATE TABLE IF NOT EXISTS 兜底建表；表为空时再从
 * src/main/resources/data/couples.json（与前端预置数据同一份）灌入种子数据。
 * 之后一切以数据库为准，后台管理系统改的就是这张表。
 */
@Component
public class CoupleInitializer {

    private static final Logger log = LoggerFactory.getLogger(CoupleInitializer.class);

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS tb_couple (
              id          BIGINT       NOT NULL AUTO_INCREMENT,
              slug        VARCHAR(64)  NOT NULL COMMENT '唯一标识 / 占位图文件名',
              name        VARCHAR(128) NOT NULL COMMENT '情侣名',
              mark        VARCHAR(8)   DEFAULT NULL COMMENT '印章单字',
              chapter     VARCHAR(64)  DEFAULT NULL COMMENT '篇章',
              era         VARCHAR(64)  DEFAULT NULL COMMENT '时代',
              era_type    VARCHAR(24)  DEFAULT NULL COMMENT '分卷 past / now',
              relic       VARCHAR(128) DEFAULT NULL COMMENT '续缘信物',
              status      VARCHAR(64)  DEFAULT NULL COMMENT '缘分状态',
              brief       VARCHAR(1000) DEFAULT NULL COMMENT '一句话小传',
              story       TEXT         DEFAULT NULL COMMENT '前尘细说',
              verse       VARCHAR(500) DEFAULT NULL COMMENT '题句',
              tags        VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
              image_url   MEDIUMTEXT   DEFAULT NULL COMMENT '图片位：后台改这里即可换图',
              image_slot  VARCHAR(255) DEFAULT NULL COMMENT '建议的本地图片路径',
              sort_no     INT          DEFAULT 0    COMMENT '排序',
              enabled     TINYINT      DEFAULT 1    COMMENT '1 展示 / 0 隐藏',
              create_time DATETIME     DEFAULT NULL,
              update_time DATETIME     DEFAULT NULL,
              PRIMARY KEY (id),
              UNIQUE KEY uk_slug (slug)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='苦情树 · 续缘相册'
            """;

    /** 「我的小情侣」用户投稿表：所有用户共享，启动时自动建表，无需重建数据库 */
    private static final String USER_DDL = """
            CREATE TABLE IF NOT EXISTS tb_user_couple (
              id          BIGINT       NOT NULL AUTO_INCREMENT,
              slug        VARCHAR(80)  NOT NULL COMMENT '唯一标识 / 占位图文件名',
              name        VARCHAR(128) NOT NULL COMMENT '情侣名',
              mark        VARCHAR(8)   DEFAULT NULL COMMENT '印章单字',
              chapter     VARCHAR(128) DEFAULT NULL COMMENT '出处 / 篇章',
              era         VARCHAR(64)  DEFAULT NULL COMMENT '时代描述',
              relic       VARCHAR(128) DEFAULT NULL COMMENT '续缘信物',
              status      VARCHAR(64)  DEFAULT NULL COMMENT '缘分状态',
              brief       VARCHAR(1000) DEFAULT NULL COMMENT '一句话小传',
              story       TEXT         DEFAULT NULL COMMENT '故事细说',
              verse       VARCHAR(500) DEFAULT NULL COMMENT '题句',
              tags        VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
              image_url   MEDIUMTEXT   DEFAULT NULL COMMENT '图片位：路径 / 外链 / base64',
              image_slot  VARCHAR(255) DEFAULT NULL COMMENT '建议的本地图片路径',
              contributor VARCHAR(64)  DEFAULT NULL COMMENT '投稿人道号',
              delete_token VARCHAR(64) DEFAULT NULL COMMENT '投稿人自助删除令牌',
              enabled     TINYINT      DEFAULT 1    COMMENT '1 展示 / 0 隐藏',
              create_time DATETIME     DEFAULT NULL,
              update_time DATETIME     DEFAULT NULL,
              PRIMARY KEY (id),
              UNIQUE KEY uk_user_slug (slug)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='苦情树 · 我的小情侣（用户共享投稿）'
            """;

    private static final String SEED_SQL = """
            INSERT INTO tb_couple
              (slug, name, mark, chapter, era, era_type, relic, status, brief, story,
               verse, tags, image_url, image_slot, sort_no, enabled, create_time)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute(DDL);
            jdbcTemplate.execute(USER_DDL);
            ensureDeleteTokenColumn();
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_couple", Integer.class);
            if (count != null && count == 0) seed();
        } catch (Exception e) {
            log.warn("苦情树相册表初始化失败（数据库未就绪？）：{}", e.getMessage());
        }
    }

    /** 老库的 tb_user_couple 没有 delete_token 列，启动时补上，不重建表、不丢投稿 */
    private void ensureDeleteTokenColumn() {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = 'tb_user_couple' AND column_name = 'delete_token'",
                Integer.class);
        if (n != null && n == 0) {
            jdbcTemplate.execute("ALTER TABLE tb_user_couple "
                    + "ADD COLUMN delete_token VARCHAR(64) DEFAULT NULL COMMENT '投稿人自助删除令牌'");
            log.info("tb_user_couple 已补上 delete_token 列");
        }
    }

    /** 表为空时，把种子数据灌进去，保证新装好的后台一进来就有 10 对可管 */
    private void seed() throws Exception {
        try (InputStream in = new ClassPathResource("data/couples.json").getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(
                    in, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> m : list) {
                jdbcTemplate.update(SEED_SQL,
                        str(m, "slug"), str(m, "name"), str(m, "mark"), str(m, "chapter"),
                        str(m, "era"), str(m, "eraType"), str(m, "relic"), str(m, "status"),
                        str(m, "brief"), str(m, "story"), str(m, "verse"),
                        tags(m.get("tags")), str(m, "image"), str(m, "imageSlot"),
                        integer(m, "sortNo", 999), integer(m, "enabled", 1));
            }
            log.info("苦情树相册表已建好并灌入 {} 对情侣", list.size());
        }
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static int integer(Map<String, Object> m, String key, int fallback) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return fallback; }
    }

    /** 种子里的 tags 是数组，库里统一存逗号分隔 */
    private static String tags(Object v) {
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return v == null ? "" : String.valueOf(v);
    }
}
