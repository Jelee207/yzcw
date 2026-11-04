package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper —— 继承 BaseMapper 即可获得 CRUD 能力
 * 配合 MyBatis-Plus，无需编写 SQL 即可完成增删改查
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
