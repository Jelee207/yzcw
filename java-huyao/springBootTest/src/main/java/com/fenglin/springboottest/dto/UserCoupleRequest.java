package com.fenglin.springboottest.dto;

/** 用户在「我的小情侣」页提交的数据：两人名字、出处、题句、图片地址（必填） */
public record UserCoupleRequest(
        String name,
        String chapter,
        String verse,
        String image,
        String contributor
) {
}
