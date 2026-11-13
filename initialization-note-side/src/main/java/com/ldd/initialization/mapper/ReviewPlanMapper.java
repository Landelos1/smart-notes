package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.ReviewPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface ReviewPlanMapper extends BaseMapper<ReviewPlan> {
    @Select("SELECT * FROM t_review_plan WHERE user_id = #{userId} ORDER BY CASE WHEN status = 0 THEN 0 ELSE 1 END, start_date DESC, create_time DESC")
    List<ReviewPlan> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_review_plan WHERE id = #{id} AND user_id = #{userId}")
    ReviewPlan selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM t_review_plan WHERE note_id = #{noteId} AND user_id = #{userId} AND source_type = 'note-ai' ORDER BY create_time")
    List<ReviewPlan> selectGeneratedByNote(@Param("noteId") Long noteId, @Param("userId") Long userId);

    @Delete("DELETE FROM t_review_plan WHERE note_id = #{noteId} AND user_id = #{userId} AND source_type = 'note-ai'")
    int deleteGeneratedByNote(@Param("noteId") Long noteId, @Param("userId") Long userId);
}
