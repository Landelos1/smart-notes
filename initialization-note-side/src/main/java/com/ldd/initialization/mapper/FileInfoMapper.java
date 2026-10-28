package com.ldd.initialization.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.FileUpInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
* @author Administrator
* @description 针对表【file_info】的数据库操作Mapper
* @createDate 2025-02-21 13:42:24
* @Entity com.ldd.hire.business.entity.FileInfo
*/
@Mapper
public interface FileInfoMapper extends BaseMapper<FileUpInfo> {

    @Select("""
        SELECT u.username AS username,
               f.original_filename AS target,
               f.create_time AS time
        FROM t_file_info f
        LEFT JOIN t_user u ON f.user_id = u.id
        ORDER BY f.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectRecentUploads(@Param("limit") Integer limit);

}




