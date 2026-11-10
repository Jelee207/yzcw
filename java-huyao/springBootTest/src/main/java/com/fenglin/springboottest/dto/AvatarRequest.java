package com.fenglin.springboottest.dto;

/**
 * 头像上传请求
 * username：当前登录的道号
 * image：前端压缩后的 base64 data URL（data:image/jpeg;base64,...）
 */
public record AvatarRequest(String username, String image) {
}
