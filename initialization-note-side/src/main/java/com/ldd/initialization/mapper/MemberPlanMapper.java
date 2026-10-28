package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.MemberPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员套餐Mapper
 */
@Mapper
public interface MemberPlanMapper extends BaseMapper<MemberPlan> {
}
