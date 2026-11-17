package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 苦情树 · 续缘相册实体 —— 对应数据库表 tb_couple
 *
 * 一对情侣一条记录。imageUrl 是预留的「图片位」：
 * 后台管理系统只需改这个字段（图片路径 / 外链 URL / base64 data URL），
 * 前端 GET /api/couples 拿到后就会立刻换图，无需改任何前端代码。
 */
@TableName("tb_couple")
public class Couple {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 唯一标识，同时用作占位图文件名（public/couples/<slug>.svg） */
    private String slug;

    /** 情侣名，如「东方月初 × 涂山红红」 */
    private String name;

    /** 印章单字 */
    private String mark;

    /** 出自篇章 */
    private String chapter;

    /** 时代描述 */
    private String era;

    /** 分卷：past 前尘 / now 今生 */
    @TableField("era_type")
    private String eraType;

    /** 续缘信物 */
    private String relic;

    /** 缘分状态 */
    private String status;

    /** 一句话小传 */
    private String brief;

    /** 前尘细说 */
    private String story;

    /** 题句 */
    private String verse;

    /** 标签，库中以英文逗号分隔，接口返回时转成数组 */
    private String tags;

    /** 图片地址（预留图片位） */
    @TableField("image_url")
    private String imageUrl;

    /** 建议的本地图片位置，如 /couples/yuehong.jpg */
    @TableField("image_slot")
    private String imageSlot;

    /** 排序序号 */
    @TableField("sort_no")
    private Integer sortNo;

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

    public String getEraType() { return eraType; }
    public void setEraType(String eraType) { this.eraType = eraType; }

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

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
