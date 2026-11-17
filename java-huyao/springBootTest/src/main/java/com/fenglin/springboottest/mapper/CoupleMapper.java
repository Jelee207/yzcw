package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Couple;
import org.apache.ibatis.annotations.Mapper;

/**
 * 苦情树相册 Mapper —— 继承 BaseMapper 即可获得 CRUD 能力，
 * 供将来后台管理系统增删改情侣档案 / 更换图片使用。
 */
@Mapper
public interface CoupleMapper extends BaseMapper<Couple> {
}
