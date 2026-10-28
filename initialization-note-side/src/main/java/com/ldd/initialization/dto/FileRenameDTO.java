package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文件重命名请求DTO
 */
@Data
public class FileRenameDTO {
    
    /**
     * 新文件名
     */
    @NotBlank(message = "新文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String newName;
}
