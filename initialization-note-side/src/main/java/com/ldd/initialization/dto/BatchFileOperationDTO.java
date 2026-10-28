package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量文件操作请求DTO
 */
@Data
public class BatchFileOperationDTO {
    
    /**
     * 文件ID列表
     */
    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> fileIds;
}
