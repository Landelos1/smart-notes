package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.Trash;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站Mapper接口
 */
@Mapper
public interface TrashMapper extends BaseMapper<Trash> {

    /**
     * 分页查询回收站列表
     * @param page 分页对象
     * @param userId 用户ID
     * @param trashType 回收站类型（可选）
     * @param keyword 搜索关键词（可选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT id, trash_type, source_id, user_id, delete_time, restore_deadline, " +
            "note_title, notebook_id, note_content_md, note_create_time, note_update_time, " +
            "file_original_filename, file_extension, file_size, file_type, file_create_time " +
            "FROM t_trash " +
            "WHERE user_id = #{userId} " +
            "<if test='trashType != null'> AND trash_type = #{trashType} </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (note_title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR file_original_filename LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY delete_time DESC" +
            "</script>")
    IPage<Trash> selectTrashListByPage(Page<Trash> page, 
                                       @Param("userId") Long userId, 
                                       @Param("trashType") Integer trashType,
                                       @Param("keyword") String keyword);

    /**
     * 查询已过期的回收站记录
     * @param deadline 截止时间
     * @return 过期记录列表
     */
    @Select("SELECT * FROM t_trash WHERE restore_deadline < #{deadline}")
    List<Trash> selectExpiredTrash(@Param("deadline") LocalDateTime deadline);

    /**
     * 删除已过期的回收站记录
     * @param deadline 截止时间
     * @return 删除的记录数
     */
    @Delete("DELETE FROM t_trash WHERE restore_deadline < #{deadline}")
    int deleteExpiredTrash(@Param("deadline") LocalDateTime deadline);

    /**
     * 根据ID和用户ID查询回收站记录
     * @param id 回收站记录ID
     * @param userId 用户ID
     * @return 回收站记录
     */
    @Select("SELECT * FROM t_trash WHERE id = #{id} AND user_id = #{userId}")
    Trash selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据用户ID删除所有回收站记录
     * @param userId 用户ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM t_trash WHERE user_id = #{userId}")
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 统计用户回收站记录数量
     * @param userId 用户ID
     * @param trashType 类型（可选）
     * @return 记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM t_trash WHERE user_id = #{userId} " +
            "<if test='trashType != null'> AND trash_type = #{trashType} </if>" +
            "</script>")
    Long countByUserId(@Param("userId") Long userId, @Param("trashType") Integer trashType);
}
