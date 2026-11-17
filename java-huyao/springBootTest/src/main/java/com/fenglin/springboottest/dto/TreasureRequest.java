package com.fenglin.springboottest.dto;

/** 后台提交法宝档案；字段可为 null —— PATCH 式更新只覆盖传了的字段 */
public record TreasureRequest(
        String slug, String name, String alias, String mark, String scroll,
        String owner, String chapter, String kind, Integer power, Integer price,
        String brief, String tale, String verse, Object tags, String image,
        Integer sortNo, Integer enabled
) {
}
