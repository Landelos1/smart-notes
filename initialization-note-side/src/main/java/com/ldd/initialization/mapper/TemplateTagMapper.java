package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TemplateTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板标签Mapper接口
 */
@Mapper
public interface TemplateTagMapper extends BaseMapper<TemplateTag> {

    /**
     * 根据模板ID查询标签列表
     */
    @Select("SELECT tag_name FROM t_template_tag WHERE template_id = #{templateId}")
    List<String> selectTagsByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID列表批量查询标签
     */
    @Select("<script>" +
            "SELECT * FROM t_template_tag WHERE template_id IN " +
            "<foreach item='id' collection='templateIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<TemplateTag> selectTagsByTemplateIds(@Param("templateIds") List<Long> templateIds);
}
