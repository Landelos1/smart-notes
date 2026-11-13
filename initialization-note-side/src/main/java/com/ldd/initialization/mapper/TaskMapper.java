package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    @Select("SELECT * FROM t_task WHERE user_id = #{userId} ORDER BY CASE WHEN status = 2 THEN 1 ELSE 0 END, due_time NULLS LAST, create_time DESC")
    List<Task> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_task WHERE id = #{id} AND user_id = #{userId}")
    Task selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM t_task WHERE note_id = #{noteId} AND user_id = #{userId} AND source_type = 'note-ai' ORDER BY create_time")
    List<Task> selectGeneratedByNote(@Param("noteId") Long noteId, @Param("userId") Long userId);

    @Delete("DELETE FROM t_task WHERE note_id = #{noteId} AND user_id = #{userId} AND source_type = 'note-ai'")
    int deleteGeneratedByNote(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
