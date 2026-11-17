package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 苦情树 · 「我的小情侣」用户投稿实体 —— 对应 tb_user_couple。
 *
 * 与官方相册 tb_couple 完全隔离：用户自由添加，所有登录用户均可查看，
 * contributor 记录投稿人的道号；imageUrl 同样是预留图片位。
 */
@TableName("tb_user_couple")
public class UserCouple {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 唯一标识，同时用作占位图文件名 */
    private String slug;

    /** 情侣名，如「张三 × 李四」 */
    private String name;

    /** 印章单字 */
    private String mark;

    /** 出处 / 篇章 */
    private String chapter;

    /** 时代描述 */
    private String era;

    /** 续缘信物 */
    private String relic;

    /** 缘分状态 */
    private String status;

    /** 一句话小传 */
    private String brief;

    /** 故事细说 */
    private String story;

    /** 题句 */
    private String verse;

    /** 标签，库中以逗号分隔 */
    private String tags;

    /** 图片地址（预留图片位） */
    @TableField("image_url")
    private String imageUrl;

    /** 建议的本地图片位置 */
    @TableField("image_slot")
    private String imageSlot;

    /** 投稿人道号 */
    private String contributor;

    /** 投稿人自助删除令牌：只有投稿时拿到它的人才能删除这一页 */
    @TableField("delete_token")
    private String deleteToken;

    /** 是否展示：1 展示 / 0 隐藏 */
    private Integer enabled;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }

    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }

    public String getEra() { return era; }
    public void setEra(String era) { this.era = era; }

    public String getRelic() { return relic; }
    public void setRelic(String relic) { this.relic = relic; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBrief() { return brief; }
    public void setBrief(String brief) { this.brief = brief; }

    public String getStory() { return story; }
    public void setStory(String story) { this.story = story; }

    public String getVerse() { return verse; }
    public void setVerse(String verse) { this.verse = verse; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageSlot() { return imageSlot; }
    public void setImageSlot(String imageSlot) { this.imageSlot = imageSlot; }

    public String getContributor() { return contributor; }
    public void setContributor(String contributor) { this.contributor = contributor; }

    public String getDeleteToken() { return deleteToken; }
    public void setDeleteToken(String deleteToken) { this.deleteToken = deleteToken; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
