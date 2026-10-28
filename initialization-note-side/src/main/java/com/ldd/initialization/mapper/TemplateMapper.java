package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板Mapper接口
 */
@Mapper
public interface TemplateMapper extends BaseMapper<Template> {

    /**
     * 根据分类查询模板列表
     */
    @Select("SELECT * FROM t_template WHERE category = #{category} ORDER BY updated_at DESC")
    List<Template> selectByCategory(@Param("category") String category);

    /**
     * 获取热门模板（按下载量排序）
     */
    @Select("SELECT * FROM t_template ORDER BY downloads DESC LIMIT #{limit}")
    List<Template> selectHotTemplates(@Param("limit") Integer limit);

    /**
     * 获取相关模板（同分类，排除自己）
     */
    @Select("SELECT * FROM t_template WHERE category = #{category} AND id != #{excludeId} ORDER BY downloads DESC LIMIT #{limit}")
    List<Template> selectRelatedTemplates(@Param("category") String category, @Param("excludeId") Long excludeId, @Param("limit") Integer limit);

    /**
     * 增加下载次数
     */
    @Select("UPDATE t_template SET downloads = downloads + 1 WHERE id = #{id}")
    void incrementDownloads(@Param("id") Long id);

    /**
     * 增加浏览次数
     */
    @Select("UPDATE t_template SET views = views + 1 WHERE id = #{id}")
    void incrementViews(@Param("id") Long id);
}
