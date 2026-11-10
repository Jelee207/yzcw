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
    `create_time` DATETIME     DEFAULT NULL            COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '用户表';

-- 3. 预置测试账号
--    用户名：admin
--    密码明文：123456（加密方式 = MD5("huyao" + 明文)，即 MD5("huyao123456") = e2d9165158d1dca9530b0c5ff4981d4b）
INSERT INTO `tb_user` (`username`, `password`, `race`, `gender`, `create_time`)
VALUES ('admin', 'e2d9165158d1dca9530b0c5ff4981d4b', '妖', '女', NOW());
