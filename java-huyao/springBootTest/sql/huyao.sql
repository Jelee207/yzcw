-- =============================================================
-- 《狐妖小红娘》主题登录注册系统 数据库初始化脚本
-- 数据库：huyao
-- 适用：MySQL 8.0+
-- 执行：mysql -uroot -p123456 < huyao.sql
-- =============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS `huyao`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `huyao`;

-- 2. 创建用户表
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名（道号）',
    `password`    VARCHAR(64)  NOT NULL                COMMENT '密码（MD5 加盐加密后存储）',
    `race`        VARCHAR(10)  DEFAULT NULL            COMMENT '种族：人 / 妖',
    `gender`      VARCHAR(10)  DEFAULT NULL            COMMENT '性别：男 / 女',
    `avatar`      MEDIUMTEXT   DEFAULT NULL            COMMENT '头像（前端压缩后的 base64 data URL）',
    `permission`  TINYINT      DEFAULT 1               COMMENT '权限：1 普通用户 / 0 管理员',
    `gold`        INT          NOT NULL DEFAULT 0      COMMENT '金币数量：后台可管理，拈签消耗/领金增加',
    `create_time` DATETIME     DEFAULT NULL            COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '用户表';

-- 3. 苦情树 · 续缘相册表（后台管理系统可改 image_url 换图）
--    程序启动时也会 CREATE TABLE IF NOT EXISTS 兜底，并为已有数据库灌入种子数据
CREATE TABLE IF NOT EXISTS `tb_couple` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `slug`        VARCHAR(64)  NOT NULL                COMMENT '唯一标识 / 占位图文件名',
    `name`        VARCHAR(128) NOT NULL                COMMENT '情侣名',
    `mark`        VARCHAR(8)   DEFAULT NULL            COMMENT '印章单字',
    `chapter`     VARCHAR(64)  DEFAULT NULL            COMMENT '篇章',
    `era`         VARCHAR(64)  DEFAULT NULL            COMMENT '时代',
    `era_type`    VARCHAR(24)  DEFAULT NULL            COMMENT '分卷 past / now',
    `relic`       VARCHAR(128) DEFAULT NULL            COMMENT '续缘信物',
    `status`      VARCHAR(64)  DEFAULT NULL            COMMENT '缘分状态',
    `brief`       VARCHAR(1000) DEFAULT NULL           COMMENT '一句话小传',
    `story`       TEXT         DEFAULT NULL            COMMENT '前尘细说',
    `verse`       VARCHAR(500) DEFAULT NULL            COMMENT '题句',
    `tags`        VARCHAR(500) DEFAULT NULL            COMMENT '标签，逗号分隔',
    `image_url`   MEDIUMTEXT   DEFAULT NULL            COMMENT '图片位：后台改这里即可换图',
    `image_slot`  VARCHAR(255) DEFAULT NULL            COMMENT '建议的本地图片路径',
    `sort_no`     INT          DEFAULT 0               COMMENT '排序',
    `enabled`     TINYINT      DEFAULT 1               COMMENT '1 展示 / 0 隐藏',
    `create_time` DATETIME     DEFAULT NULL,
    `update_time` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_slug` (`slug`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '苦情树 · 续缘相册';

-- 4. 苦情树 · 我的小情侣（用户投稿，所有用户共享；程序启动时也会自动建表）
CREATE TABLE IF NOT EXISTS `tb_user_couple` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `slug`        VARCHAR(80)  NOT NULL                COMMENT '唯一标识',
    `name`        VARCHAR(128) NOT NULL                COMMENT '情侣名',
    `mark`        VARCHAR(8)   DEFAULT NULL            COMMENT '印章单字',
    `chapter`     VARCHAR(128) DEFAULT NULL            COMMENT '出处 / 篇章',
    `era`         VARCHAR(64)  DEFAULT NULL            COMMENT '时代描述',
    `relic`       VARCHAR(128) DEFAULT NULL            COMMENT '续缘信物',
    `status`      VARCHAR(64)  DEFAULT NULL            COMMENT '缘分状态',
    `brief`       VARCHAR(1000) DEFAULT NULL           COMMENT '一句话小传',
    `story`       TEXT         DEFAULT NULL            COMMENT '故事细说',
    `verse`       VARCHAR(500) DEFAULT NULL            COMMENT '题句',
    `tags`        VARCHAR(500) DEFAULT NULL            COMMENT '标签，逗号分隔',
    `image_url`   MEDIUMTEXT   DEFAULT NULL            COMMENT '图片位：路径 / 外链 / base64',
    `image_slot`  VARCHAR(255) DEFAULT NULL            COMMENT '建议的本地图片路径',
    `contributor` VARCHAR(64)  DEFAULT NULL            COMMENT '投稿人道号',
    `delete_token` VARCHAR(64) DEFAULT NULL            COMMENT '投稿人自助删除令牌',
    `enabled`     TINYINT      DEFAULT 1               COMMENT '1 展示 / 0 隐藏',
    `create_time` DATETIME     DEFAULT NULL,
    `update_time` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_slug` (`slug`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '苦情树 · 我的小情侣（用户共享投稿）';

-- 5. 预置账号（密码均为明文，无需加密）
--    管理账号：渔舟唱晚 / 111111 / 权限 0（可在首页看到并登录后台管理系统）
--    测试账号：admin / 123456 / 权限 1（普通用户）
--    注：admin 历史密码 originally 以 MD5("huyao"+明文) 存储；登录接口同时兼容明文与旧 MD5
INSERT INTO `tb_user` (`username`, `password`, `race`, `gender`, `permission`, `create_time`)
VALUES ('渔舟唱晚', '111111', '妖', '女', 0, NOW()),
       ('admin', '123456', '妖', '女', 1, NOW());
