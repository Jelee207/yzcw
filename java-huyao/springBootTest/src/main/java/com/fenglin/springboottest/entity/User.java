package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体 —— 对应数据库表 tb_user
 * 字段：用户名（道号）、密码（MD5 加密）、种族（人/妖）、性别（男/女）、头像、注册时间
 */
@TableName("tb_user")
public class User {

    /** 主键，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（道号），唯一 */
    private String username;

    /** 密码（MD5 加盐加密后存储） */
    private String password;

    /** 种族：人 / 妖 */
    private String race;

    /** 性别：男 / 女 */
    private String gender;

    /** 头像：前端压缩成 256x256 JPEG 后的 base64 data URL */
    private String avatar;

    /** 注册时间 */
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
