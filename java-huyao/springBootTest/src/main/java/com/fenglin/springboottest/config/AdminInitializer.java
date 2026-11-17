package com.fenglin.springboottest.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 后台权限体系初始化。
 *
 * 已有数据库不会重新执行 sql/huyao.sql，所以这里在启动时做两件事：
 *   1. 给 tb_user 补上 permission 列（默认 1 普通用户），老账号全部置为 1；
 *   2. 给 tb_user 补上 gold 列（默认 0），供后台管理用户金币；
 *   3. 确保管理账号「渔舟唱晚」存在，权限 0、密码明文 111111。
 * 这样老库无需删表重建即可平滑升级。
 */
@Component
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    /** 管理账号与明文密码 */
    public static final String ADMIN_USERNAME = "渔舟唱晚";
    public static final String ADMIN_PASSWORD = "111111";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            ensurePermissionColumn();
            ensureGoldColumn();
            ensureAdminAccount();
        } catch (Exception e) {
            log.warn("后台权限初始化失败（数据库未就绪？）：{}", e.getMessage());
        }
    }

    /** tb_user 无 permission 列时补上，并把空值统一为 1 */
    private void ensurePermissionColumn() {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = 'tb_user' AND column_name = 'permission'",
                Integer.class);
        if (n != null && n == 0) {
            jdbcTemplate.execute("ALTER TABLE tb_user "
                    + "ADD COLUMN permission TINYINT DEFAULT 1 COMMENT '权限：1 普通用户 / 0 管理员'");
            log.info("tb_user 已补上 permission 列");
        }
        jdbcTemplate.execute("UPDATE tb_user SET permission = 1 WHERE permission IS NULL");
    }

    /** tb_user 无 gold 列时补上，并把空值统一为 0 */
    private void ensureGoldColumn() {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                        + "AND table_name = 'tb_user' AND column_name = 'gold'",
                Integer.class);
        if (n != null && n == 0) {
            jdbcTemplate.execute("ALTER TABLE tb_user "
                    + "ADD COLUMN gold INT NOT NULL DEFAULT 0 COMMENT '金币数量：拈签消耗/领金增加，后台可管理'");
            log.info("tb_user 已补上 gold 列");
        }
        jdbcTemplate.execute("UPDATE tb_user SET gold = 0 WHERE gold IS NULL");
    }

    /** 管理账号不存在则插入，存在则确保权限 0、密码为明文 111111 */
    private void ensureAdminAccount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_user WHERE username = ?", Integer.class, ADMIN_USERNAME);
        if (count != null && count > 0) {
            jdbcTemplate.update("UPDATE tb_user SET permission = 0, password = ? WHERE username = ?",
                    ADMIN_PASSWORD, ADMIN_USERNAME);
            log.info("管理账号「{}」已更新为权限 0 / 明文密码", ADMIN_USERNAME);
        } else {
            jdbcTemplate.update("INSERT INTO tb_user (username, password, race, gender, permission, create_time) "
                            + "VALUES (?, ?, '妖', '女', 0, NOW())",
                    ADMIN_USERNAME, ADMIN_PASSWORD);
            log.info("管理账号「{}」已创建", ADMIN_USERNAME);
        }
    }
}
