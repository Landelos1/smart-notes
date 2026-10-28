package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TemplateCommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 模板评论点赞Mapper
 */
@Mapper
public interface TemplateCommentLikeMapper extends BaseMapper<TemplateCommentLike> {
    
    /**
     * 查询用户是否点赞了指定评论
     * @param userId 用户ID
     * @param commentId 评论ID
     * @return 点赞记录
     */
    @Select("SELECT * FROM t_template_comment_like WHERE user_id = #{userId} AND comment_id = #{commentId} LIMIT 1")
    TemplateCommentLike selectByUserIdAndCommentId(@Param("userId") Long userId, 
                                                    @Param("commentId") Long commentId);
}
