package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TemplateComment;
import com.ldd.initialization.vo.TemplateCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板评论Mapper接口
 */
@Mapper
public interface TemplateCommentMapper extends BaseMapper<TemplateComment> {

    /**
     * 根据模板ID查询评论列表
     */
    @Select("SELECT * FROM t_template_comment WHERE template_id = #{templateId} ORDER BY created_at DESC")
    List<TemplateComment> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 获取模板的评论数量
     */
    @Select("SELECT COUNT(*) FROM t_template_comment WHERE template_id = #{templateId}")
    Integer countByTemplateId(@Param("templateId") Long templateId);

    /**
     * 管理员评论列表（模板评论）
     */
    @Select("<script>" +
            "SELECT c.id AS id, c.template_id AS targetId, t.title AS targetTitle, " +
            "c.content AS content, c.rating AS rating, c.likes AS likes, " +
            "c.created_at AS createdAt, u.username AS author, u.avatar_url AS avatar " +
            "FROM t_template_comment c " +
            "LEFT JOIN t_template t ON c.template_id = t.id " +
            "LEFT JOIN t_user u ON c.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='targetId != null'> AND c.template_id = #{targetId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> AND c.content LIKE CONCAT('%', #{keyword}, '%') </if> " +
            "ORDER BY c.created_at DESC" +
            "</script>")
    List<TemplateCommentVO> selectAdminComments(@Param("targetId") Long targetId,
                                               @Param("keyword") String keyword);

    /**
     * 管理员评论统计（模板评论）
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM t_template_comment c WHERE 1=1 " +
            "<if test='targetId != null'> AND c.template_id = #{targetId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> AND c.content LIKE CONCAT('%', #{keyword}, '%') </if> " +
            "</script>")
    Long countAdminComments(@Param("targetId") Long targetId,
                            @Param("keyword") String keyword);

    /**
     * 点赞评论
     */
    @Select("UPDATE t_template_comment SET likes = likes + 1 WHERE id = #{id}")
    void incrementLikes(@Param("id") Long id);

    /**
     * 取消点赞评论
     */
    @Select("UPDATE t_template_comment SET likes = likes - 1 WHERE id = #{id} AND likes > 0")
    void decrementLikes(@Param("id") Long id);
}
