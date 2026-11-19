package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 法宝图鉴实体 —— 对应 tb_treasure。
 * 字段与前端 Home.vue 的法宝对象一一对应，image 支持本地路径 / 外链 / base64 data URL。
 */
@TableName("tb_treasure")
public class Treasure {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 唯一标识，同时用作默认占位图文件名 */
    private String slug;
    private String name;
    private String alias;
    private String mark;

    /** 分卷：tushan / daomen / xixiyu / nanguo */
    private String scroll;
    private String owner;
    private String chapter;
    private String kind;

    /** 灵力等级 1-5 */
    private Integer power;

    /** 法宝价格 1-40（按剧情价值随机定价：虚空之泪 40 最高、王权剑 39 第二） */
    private Integer price;
    private String brief;
    private String tale;
    private String verse;

    /** 标签，库中逗号分隔，接口返回数组 */
    private String tags;

    @TableField("image_url")
    private String imageUrl;

    @TableField("sort_no")
    private Integer sortNo;

    /** 1 展示 / 0 隐藏 */
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
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getMark() { return mark; }
    public void setMark(String mark) { this.mark = mark; }
    public String getScroll() { return scroll; }
    public void setScroll(String scroll) { this.scroll = scroll; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Integer getPower() { return power; }
    public void setPower(Integer power) { this.power = power; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public String getBrief() { return brief; }
    public void setBrief(String brief) { this.brief = brief; }
    public String getTale() { return tale; }
    public void setTale(String tale) { this.tale = tale; }
    public String getVerse() { return verse; }
    public void setVerse(String verse) { this.verse = verse; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
