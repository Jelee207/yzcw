package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Treasure;
import org.apache.ibatis.annotations.Mapper;

/** 法宝 Mapper —— 继承 MyBatis-Plus BaseMapper 获得 CRUD 能力 */
@Mapper
public interface TreasureMapper extends BaseMapper<Treasure> {
}
