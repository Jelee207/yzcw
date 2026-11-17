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
 * 法宝图鉴表初始化：启动时 CREATE TABLE IF NOT EXISTS，
 * 表为空则从 data/treasures.json 灌入 22 件种子法宝，之后一切以数据库为准。
 */
@Component
public class TreasureInitializer {

    private static final Logger log = LoggerFactory.getLogger(TreasureInitializer.class);

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS tb_treasure (
              id          BIGINT       NOT NULL AUTO_INCREMENT,
              slug        VARCHAR(64)  NOT NULL COMMENT '唯一标识 / 占位图文件名',
              name        VARCHAR(128) NOT NULL COMMENT '法宝名',
              alias       VARCHAR(128) DEFAULT NULL COMMENT '别称',
              mark        VARCHAR(8)   DEFAULT NULL COMMENT '印章单字',
              scroll      VARCHAR(32)  DEFAULT NULL COMMENT '分卷 tushan/daomen/xixiyu/nanguo',
              owner       VARCHAR(128) DEFAULT NULL COMMENT '持有者',
              chapter     VARCHAR(64)  DEFAULT NULL COMMENT '出处篇章',
              kind        VARCHAR(64)  DEFAULT NULL COMMENT '类别',
              power       INT          DEFAULT 3    COMMENT '灵力 1-5',
              price       INT          DEFAULT 5    COMMENT '法宝价格 2-25',
              brief       VARCHAR(2000) DEFAULT NULL COMMENT '器之用',
              tale        TEXT         DEFAULT NULL COMMENT '缘之由',
              verse       VARCHAR(500) DEFAULT NULL COMMENT '题句',
              tags        VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
              image_url   MEDIUMTEXT   DEFAULT NULL COMMENT '图片：路径 / 外链 / base64',
              sort_no     INT          DEFAULT 0    COMMENT '排序',
              enabled     TINYINT      DEFAULT 1    COMMENT '1 展示 / 0 隐藏',
              create_time DATETIME     DEFAULT NULL,
              update_time DATETIME     DEFAULT NULL,
              PRIMARY KEY (id),
              UNIQUE KEY uk_treasure_slug (slug)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法宝图鉴'
            """;

    private static final String SEED_SQL = """
            INSERT INTO tb_treasure
              (slug, name, alias, mark, scroll, owner, chapter, kind, power, price, brief, tale,
               verse, tags, image_url, sort_no, enabled, create_time)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute(DDL);
            upgradePriceColumn();
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tb_treasure", Integer.class);
            if (count != null && count == 0) seed();
        } catch (Exception e) {
            log.warn("法宝图鉴表初始化失败（数据库未就绪？）：{}", e.getMessage());
        }
    }

    /** 老库平滑补 price 列，并按种子 JSON 的 slug->price 回填（sort_no 顺序对应 2-25 档） */
    private void upgradePriceColumn() throws Exception {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = 'tb_treasure' AND column_name = 'price'",
                Integer.class);
        if (n != null && n > 0) return;   // 已有 price 列，不覆盖后台改过的价格
        jdbcTemplate.execute("ALTER TABLE tb_treasure "
                + "ADD COLUMN price INT DEFAULT 5 COMMENT '法宝价格 2-25'");
        try (InputStream in = new ClassPathResource("data/treasures.json").getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(
                    in, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> m : list) {
                jdbcTemplate.update("UPDATE tb_treasure SET price = ? WHERE slug = ?",
                        integer(m, "price", 5), str(m, "slug"));
            }
        }
        log.info("tb_treasure 已补上 price 列并回填种子价格");
    }

    private void seed() throws Exception {
        try (InputStream in = new ClassPathResource("data/treasures.json").getInputStream()) {
            List<Map<String, Object>> list = objectMapper.readValue(
                    in, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> m : list) {
                jdbcTemplate.update(SEED_SQL,
                        str(m, "slug"), str(m, "name"), str(m, "alias"), str(m, "mark"),
                        str(m, "scroll"), str(m, "owner"), str(m, "chapter"), str(m, "kind"),
                        integer(m, "power", 3), integer(m, "price", 5), str(m, "brief"), str(m, "tale"), str(m, "verse"),
                        tags(m.get("tags")), str(m, "image"), integer(m, "sortNo", 999),
                        integer(m, "enabled", 1));
            }
            log.info("法宝图鉴表已建好并灌入 {} 件法宝", list.size());
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

    private static String tags(Object v) {
        if (v instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return v == null ? "" : String.valueOf(v);
    }
}
