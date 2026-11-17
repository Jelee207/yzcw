package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserCouple;
import org.apache.ibatis.annotations.Mapper;

/** 「我的小情侣」投稿 Mapper：所有用户共享同一份数据 */
@Mapper
public interface UserCoupleMapper extends BaseMapper<UserCouple> {
}
