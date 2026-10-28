package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.UserMembership;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMembershipMapper extends BaseMapper<UserMembership> {
}
