package com.fenglin.springboottest.dto;

/**
 * 后台管理系统提交的情侣档案。
 * 所有字段均可为 null —— PATCH 式更新：只覆盖传了的字段，
 * 因此只改图片时，请求体里带一个 image 即可。
 */
public record CoupleRequest(
        String slug,
        String name,
        String mark,
        String chapter,
        String era,
        String eraType,
        String relic,
        String status,
        String brief,
        String story,
        String verse,
        String tags,
        String image,
        String imageSlot,
        Integer sortNo,
        Integer enabled
) {
}
