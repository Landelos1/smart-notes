package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TemplateFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 模板收藏Mapper
 */
@Mapper
public interface TemplateFavoriteMapper extends BaseMapper<TemplateFavorite> {
    
    /**
     * 查询用户是否收藏了指定模板
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 收藏记录
     */
    @Select("SELECT * FROM t_template_favorite WHERE user_id = #{userId} AND template_id = #{templateId} LIMIT 1")
    TemplateFavorite selectByUserIdAndTemplateId(@Param("userId") Long userId, 
                                                   @Param("templateId") Long templateId);
}
