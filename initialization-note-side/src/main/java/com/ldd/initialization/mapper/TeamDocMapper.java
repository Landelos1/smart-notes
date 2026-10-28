package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.TeamDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 团队文档Mapper接口
 */
@Mapper
public interface TeamDocMapper extends BaseMapper<TeamDoc> {

    /**
     * 分页查询团队文档列表（带更新人信息）
     */
    @Select("<script>" +
            "SELECT d.*, u.username as updated_by_name, u.avatar_url as updated_by_avatar " +
            "FROM t_team_doc d " +
            "LEFT JOIN \"t_user\" u ON d.updated_by = u.id " +
            "WHERE d.team_id = #{teamId} AND d.status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (d.title LIKE CONCAT('%', #{keyword}, '%') OR d.content_md LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "ORDER BY d.is_pinned DESC, d.update_time DESC" +
            "</script>")
    IPage<TeamDoc> selectDocListByPage(Page<TeamDoc> page, 
                                        @Param("teamId") Long teamId,
                                        @Param("keyword") String keyword);

    /**
     * 获取文档详情（带用户信息）
     */
    @Select("SELECT d.*, " +
            "       u1.username as created_by_name, u1.avatar_url as created_by_avatar, " +
            "       u2.username as updated_by_name, u2.avatar_url as updated_by_avatar, " +
            "       u3.username as locked_by_name, u3.avatar_url as locked_by_avatar " +
            "FROM t_team_doc d " +
            "LEFT JOIN \"t_user\" u1 ON d.created_by = u1.id " +
            "LEFT JOIN \"t_user\" u2 ON d.updated_by = u2.id " +
            "LEFT JOIN \"t_user\" u3 ON d.locked_by = u3.id " +
            "WHERE d.id = #{docId} AND d.status = 1")
    TeamDoc selectDocWithUserInfo(@Param("docId") Long docId);

    /**
     * 清理过期锁
     */
    @Update("UPDATE t_team_doc SET locked_by = NULL, lock_token = NULL, lock_time = NULL, lock_expire_time = NULL " +
            "WHERE id = #{docId} AND lock_expire_time < NOW()")
    int clearExpiredLock(@Param("docId") Long docId);

    /**
     * 获取锁（仅当未被锁定或锁已过期）
     */
    @Update("UPDATE t_team_doc SET locked_by = #{userId}, lock_token = #{token}, " +
            "lock_time = NOW(), lock_expire_time = NOW() + INTERVAL '5 minutes' " +
            "WHERE id = #{docId} AND status = 1 " +
            "AND (locked_by IS NULL OR lock_expire_time < NOW() OR locked_by = #{userId})")
    int acquireLock(@Param("docId") Long docId, @Param("userId") Long userId, @Param("token") String token);

    /**
     * 释放锁
     */
    @Update("UPDATE t_team_doc SET locked_by = NULL, lock_token = NULL, lock_time = NULL, lock_expire_time = NULL " +
            "WHERE id = #{docId} AND (locked_by = #{userId} OR lock_token = #{token})")
    int releaseLock(@Param("docId") Long docId, @Param("userId") Long userId, @Param("token") String token);
}
