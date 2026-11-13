package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.dto.FileListRequestDTO;
import com.ldd.initialization.enums.FileSourceTypeEnum;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.FileInfoService;
import com.ldd.initialization.service.TrashService;
import com.ldd.initialization.service.ai.DocumentService;
import com.ldd.initialization.vo.FileUploadVO;
import com.ldd.initialization.vo.StructuredNoteVO;
import com.ldd.initialization.service.ai.StructuredNoteService;

import com.ldd.initialization.dto.BatchFileOperationDTO;
import com.ldd.initialization.dto.FileRenameDTO;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private TrashService trashService;

    @Autowired
    private StructuredNoteService structuredNoteService;

    /**
     * 将 PDF 或图片资料整理成结构化 Markdown 笔记。
     */
    @SaCheckLogin
    @PostMapping("/structure-note")
    public Result<StructuredNoteVO> structureNote(@RequestParam("file") MultipartFile file) {
        try {
            if (file.getSize() > 20 * 1024 * 1024) {
                return Result.error("资料文件不能超过20MB");
            }
            return Result.success(structuredNoteService.structure(file));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("结构化资料失败: {}", e.getMessage(), e);
            return Result.error("资料解析失败，请检查文件内容或模型配置");
        }
    }
    
    /**
     * 上传单个文档（自动获取当前用户）
     */
    /**
     * 上传单个文档（自动获取当前用户）
     * 支持 .md, .txt, .doc, .pdf 格式。
     * 非PDF格式文件将自动转换为PDF后进行处理和存储。
     */
    @SaCheckLogin
    @PostMapping("/upload")
    public Result<FileUploadVO> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("接收到文档上传请求: {}", file.getOriginalFilename());

        try {
            // 获取当前登录用户ID
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            // 参数校验
            if (file.isEmpty()) {
                return Result.error("上传的文件不能为空");
            }

            // 文件大小校验（10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.error("文件大小不能超过10MB");
            }

            // 1. 预先读取原始文件字节到内存
            byte[] originalFileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            String fileExtension = getFileExtension(originalFilename);
            switch (fileExtension.toLowerCase()) {
                case "md":
                case "txt":
                case "doc":
                case "docx":
                case "pdf":
                case "xls":
                case "xlsx":
                case "ppt":
                case "pptx":
                    break;
                default:
                    log.warn("不支持的文件类型上传: {}", fileExtension);
                    return Result.error("不支持的文件类型: " + fileExtension);
            }

            // 4. 保存原始文件信息到存储服务（不再将非PDF转换为PDF）
            log.info("开始保存文件到存储服务...");
            FileUpInfo fileInfo = fileInfoService.uploadFile(file, userId);
            log.info("文件保存成功，文件ID: {}", fileInfo.getId());

            // 5. 使用原始文件字节数组来处理文档并存储到向量数据库
            log.info("开始文档解析和向量化处理...");
            log.info("   - 文件名: {}, 大小: {} bytes", originalFilename, originalFileBytes.length);
            log.info("   - 用户ID: {}, 知识库类型: 个人, 文件ID: {}", userId, fileInfo.getId());
            
            Map<String, Object> processResult = documentService.uploadAndProcessDocumentFromBytes(
                    originalFileBytes,
                    originalFilename,
                    file.getContentType(),
                    userId,         // 个人知识库ID
                    1,              // 知识库类型: 1-个人
                    fileInfo.getId() // 关联文件存储服务中PDF文件的记录ID
            );

            // 6. 构建返回结果
            FileUploadVO result = new FileUploadVO();
            // 注意：这里复制的是 fileInfo 的属性，fileInfo 现在代表的是转换后的PDF文件信息
            BeanUtils.copyProperties(fileInfo, result);

            // 添加文档处理信息
            if ((Boolean) processResult.get("success")) {
                result.setDocumentCount((Integer) processResult.get("documentCount"));
                result.setProcessingStatus("SUCCESS");
                result.setProcessingMessage("文档上传并处理成功");
                log.info("文档向量化处理成功，分割为 {} 个片段", processResult.get("documentCount"));
            } else {
                result.setProcessingStatus("FAILED");
                String errorMsg = (String) processResult.get("message");
                result.setProcessingMessage(errorMsg);
                log.warn("文档向量化处理失败: {}", errorMsg);
                
                // 记录更详细的错误信息
                if (processResult.get("exceptionType") != null) {
                    log.warn("   - 异常类型: {}", processResult.get("exceptionType"));
                }
                if (processResult.get("stackTrace") != null) {
                    log.warn("   - 堆栈信息: {}", processResult.get("stackTrace"));
                }
            }

            log.info("用户 {} 上传文档完成: {}, 文件ID: {}, 处理状态: {}", userId, originalFilename, fileInfo.getId(), result.getProcessingStatus());
            return Result.success(result);

        } catch (IOException e) {
            log.error("读取文件字节失败: {}", e.getMessage(), e);
            return Result.error("文件读取失败: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("文档转换或处理失败: {}", e.getMessage(), e);
            log.error("   - 异常类型: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("   - 根因: {}", e.getCause().getMessage());
            }
            return Result.error("文档转换或处理失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文档上传处理发生未知错误: {}", e.getMessage(), e);
            log.error("   - 异常类型: {}", e.getClass().getName());
            return Result.error("文档上传失败: " + e.getMessage());
        }
    }



    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 获取不带扩展名的文件名
     */
    private String getFileNameWithoutExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }
    /**
     * 获取用户上传的文件列表（POST请求，使用JSON请求体）
     */
    @SaCheckLogin
    @PostMapping("/files/list")
    public Result<PageResult<FileUpInfo>> getUserFiles(@RequestBody FileListRequestDTO request) {
        try {
            // 获取当前登录用户ID
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());

            log.info("获取用户 {} 的文件列表，请求参数: {}", userId, request);

            // --- 1. 参数校验和默认值处理 (利用 DTO 的初始值和业务层的二次校验) ---
            int pageNum = request.getPage();
            int pageSize = request.getSize();

            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 10;
            }

            // 获取 DTO 中已经处理过默认值的排序字段和方向
            String finalSortBy = request.getSortBy();
            String finalSortOrder = request.getSortOrder(); // 已经转为小写

            // --- 2. 链式构建 QueryWrapper ---
            QueryWrapper<FileUpInfo> queryWrapper = new QueryWrapper<FileUpInfo>()
                    .eq("user_id", userId); // 用户ID是强制条件

            queryWrapper.and(wrapper -> wrapper
                    .isNull("source_type")
                    .or()
                    .ne("source_type", FileSourceTypeEnum.SHARED_KB_UPLOAD.getValue())
            );

            // 关键字查询：使用 and(condition, consumer) 方法，当 condition 为 true 时，才执行 consumer 里的逻辑
            queryWrapper.and(StringUtils.hasText(request.getKeyword()), wrapper -> wrapper
                    .like("original_filename", request.getKeyword())
                    .or()
                    .like("file_name", request.getKeyword())
            );

            // 文件类型查询：使用 eq(condition, column, value) 方法
            // todo 再次提醒：请确认 fileType 对应的是 file_extension 字段
            queryWrapper.eq(StringUtils.hasText(request.getFileType()), "file_extension", request.getFileType());

            // 动态排序：首先验证排序字段的安全性，然后使用 orderBy(condition, isAsc, columns) 方法
            if (!isValidSortField(finalSortBy)) {
                finalSortBy = "create_time"; // 如果 DTO 给出的字段无效，则强制使用安全的默认字段
                log.warn("用户 {} 提供的排序字段 {} 无效，已回退到默认字段: create_time", userId, request.getSortBy());
            }

            // isAsc 为 true 表示升序，false 表示降序
            boolean isAsc = "asc".equals(finalSortOrder);
            queryWrapper.orderBy(true, isAsc, finalSortBy); // 第一个 true 表示这个排序条件始终生效

            // --- 3. 分页查询 ---
            Page<FileUpInfo> filePage = new Page<>(pageNum, pageSize); // 使用校验后的页码和大小
            fileInfoService.page(filePage, queryWrapper);

            log.info("用户 {} 的文件列表查询完成，总数: {}", userId, filePage.getTotal());
            return Result.success(PageResult.convert(filePage));

        } catch (Exception e) {
            log.error("获取用户文件列表失败: {}", e.getMessage(), e);
            return Result.error("获取文件列表失败，请稍后重试。");
        }
    }
    /**
     * 删除用户上传的文件
     */
    @SaCheckLogin
    @DeleteMapping("/files/{fileId}")
    public Result<Void> deleteUserFile(@PathVariable @NotNull Long fileId) {
        try {
            // 获取当前登录用户ID
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            
            // 查询文件信息
            FileUpInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            
            // 验证文件所有者
            if (!userId.equals(fileInfo.getUserId())) {
                return Result.error("无权限删除该文件");
            }
            
            // 移入回收站
            trashService.moveFileToTrash(fileInfo, userId);
            
            // 1. 先删除向量数据库中的相关记录
            try {
                Map<String, Object> vectorDeleteResult = documentService.deleteDocumentVectors(fileId, userId.toString());
                if ((Boolean) vectorDeleteResult.get("success")) {
                    log.info("成功删除文件 {} 的向量数据，删除记录数: {}", 
                            fileId, vectorDeleteResult.get("deletedCount"));
                } else {
                    log.warn("删除文件 {} 的向量数据失败: {}", fileId, vectorDeleteResult.get("message"));
                }
            } catch (Exception e) {
                log.error("删除文件 {} 的向量数据时发生异常: {}", fileId, e.getMessage(), e);
                // 向量数据删除失败不影响文件删除，只记录日志
            }
            
            // 2. 删除文件记录
            fileInfoService.removeById(fileId);
            
            log.info("用户 {} 删除文件成功: {}", userId, fileInfo.getOriginalFilename());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            return Result.error("删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 完整的向量化诊断接口
     * 依次测试：数据库连接 -> EmbeddingModel -> 完整向量化流程
     */
    @SaIgnore
    @GetMapping("/test-vector")
    public Result<Map<String, Object>> testVectorConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 1. 测试数据库连接
            log.info("=== 开始向量化诊断 ===");
            log.info("1: 测试数据库连接...");
            try {
                Map<String, Object> stats = documentService.getDocumentStats();
                boolean connectionOk = stats.get("success") != null;
                result.put("step1_databaseConnection", connectionOk);
                result.put("step1_message", "数据库连接正常");
                log.info("数据库连接测试通过");
            } catch (Exception e) {
                result.put("step1_databaseConnection", false);
                result.put("step1_error", e.getMessage());
                log.error("数据库连接测试失败: {}", e.getMessage());
            }
            
            // 2. 测试 EmbeddingModel 是否正常注入和工作
            log.info("2: 测试EmbeddingModel...");
            try {
                String testEmbeddingResult = documentService.testEmbeddingModel();
                result.put("step2_embeddingModel", true);
                result.put("step2_message", testEmbeddingResult);
                log.info("EmbeddingModel测试通过: {}", testEmbeddingResult);
            } catch (Exception e) {
                result.put("step2_embeddingModel", false);
                result.put("step2_error", e.getMessage());
                result.put("step2_errorType", e.getClass().getName());
                log.error("EmbeddingModel测试失败: {}", e.getMessage(), e);
            }
            
            // 3. 测试完整向量插入流程
            log.info("3: 测试完整向量插入流程...");
            try {
                String testInsertResult = documentService.testVectorInsert();
                result.put("step3_vectorInsert", true);
                result.put("step3_message", testInsertResult);
                log.info("向量插入测试通过: {}", testInsertResult);
            } catch (Exception e) {
                result.put("step3_vectorInsert", false);
                result.put("step3_error", e.getMessage());
                result.put("step3_errorType", e.getClass().getName());
                log.error("向量插入测试失败: {}", e.getMessage(), e);
            }
            
            // 总结
            boolean allPassed = Boolean.TRUE.equals(result.get("step1_databaseConnection"))
                    && Boolean.TRUE.equals(result.get("step2_embeddingModel"))
                    && Boolean.TRUE.equals(result.get("step3_vectorInsert"));
            
            result.put("success", allPassed);
            result.put("message", allPassed ? "所有诊断测试通过" : "部分测试失败，请查看详细信息");
            
            log.info("=== 向量化诊断完成，结果: {} ===", allPassed ? "全部通过" : "存在失败");
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("向量化诊断过程发生异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getName());
            return Result.error("诊断失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试文档解析和向量化（完整流程测试）
     */
    @SaIgnore
    @PostMapping("/test-parse")
    public Result<Map<String, Object>> testDocumentParsing(@RequestParam("file") MultipartFile file) {
        try {
            // 简单的文件验证
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            log.info("=== 开始测试文档解析和向量化 ===");
            log.info("文件名: {}, 大小: {} bytes, 类型: {}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            // 调用文档服务进行解析测试（使用测试用户ID=999）
            Map<String, Object> result = documentService.uploadAndProcessDocument(file, 999L, 1, null);
            
            if ((Boolean) result.get("success")) {
                log.info("文档解析和向量化测试成功，处理了 {} 个片段", result.get("documentCount"));
                return Result.success(result);
            } else {
                log.error("文档解析测试失败: {}", result.get("message"));
                return Result.error(result.get("message").toString());
            }
            
        } catch (Exception e) {
            log.error("测试文档解析时发生异常: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getName());
            errorResult.put("stackTrace", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "无");
            return Result.error("测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证排序字段是否有效
     */
    private boolean isValidSortField(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return false;
        }

        // 定义允许的排序字段（数据库字段名）
        String[] validFields = {
                "id", "create_time", "update_time", "file_size",
                "original_filename", "file_name", "file_type", "mime_type", "file_hash"
        };

        for (String field : validFields) {
            if (field.equals(sortBy)) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * 重命名文件
     */
    @SaCheckLogin
    @PutMapping("/files/{fileId}/rename")
    public Result<FileUpInfo> renameFile(@PathVariable @NotNull Long fileId, 
                                         @RequestBody @Valid FileRenameDTO request) {
        try {
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            
            // 查询文件信息
            FileUpInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                return Result.error("文件不存在");
            }
            
            // 验证文件所有者
            if (!userId.equals(fileInfo.getUserId())) {
                return Result.error("无权限修改该文件");
            }
            
            // 更新文件名
            String newName = request.getNewName().trim();
            // 保留原扩展名
            String oldExtension = fileInfo.getFileExtension();
            if (oldExtension != null && !newName.endsWith("." + oldExtension)) {
                newName = newName + "." + oldExtension;
            }
            
            fileInfo.setOriginalFilename(newName);
            fileInfoService.updateById(fileInfo);
            
            log.info("用户 {} 重命名文件成功: {} -> {}", userId, fileInfo.getOriginalFilename(), newName);
            return Result.success(fileInfo);
            
        } catch (Exception e) {
            log.error("重命名文件失败: {}", e.getMessage(), e);
            return Result.error("重命名文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除文件
     */
    @SaCheckLogin
    @DeleteMapping("/files/batch-delete")
    public Result<Map<String, Object>> batchDeleteFiles(@RequestBody @Valid BatchFileOperationDTO request) {
        try {
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            List<Long> fileIds = request.getFileIds();
            
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long fileId : fileIds) {
                try {
                    FileUpInfo fileInfo = fileInfoService.getById(fileId);
                    if (fileInfo == null) {
                        failCount++;
                        errors.add("文件ID " + fileId + " 不存在");
                        continue;
                    }
                    
                    if (!userId.equals(fileInfo.getUserId())) {
                        failCount++;
                        errors.add("无权限删除文件ID " + fileId);
                        continue;
                    }
                    
                    // 删除向量数据
                    try {
                        documentService.deleteDocumentVectors(fileId, userId.toString());
                    } catch (Exception e) {
                        log.warn("删除文件 {} 的向量数据失败: {}", fileId, e.getMessage());
                    }
                    
                    // 删除文件记录
                    fileInfoService.removeById(fileId);
                    successCount++;
                    
                } catch (Exception e) {
                    failCount++;
                    errors.add("删除文件ID " + fileId + " 失败: " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);
            
            log.info("用户 {} 批量删除文件完成，成功: {}, 失败: {}", userId, successCount, failCount);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("批量删除文件失败: {}", e.getMessage(), e);
            return Result.error("批量删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 重建文档索引
     */
    @SaCheckLogin
    @PostMapping("/rebuild-index")
    public Result<Map<String, Object>> rebuildIndex(@RequestBody @Valid BatchFileOperationDTO request) {
        try {
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            List<Long> fileIds = request.getFileIds();
            
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Long fileId : fileIds) {
                try {
                    FileUpInfo fileInfo = fileInfoService.getById(fileId);
                    if (fileInfo == null) {
                        failCount++;
                        errors.add("文件ID " + fileId + " 不存在");
                        continue;
                    }
                    
                    if (!userId.equals(fileInfo.getUserId())) {
                        failCount++;
                        errors.add("无权限操作文件ID " + fileId);
                        continue;
                    }
                    
                    // 先删除旧的向量数据
                    try {
                        documentService.deleteDocumentVectors(fileId, userId.toString());
                    } catch (Exception e) {
                        log.warn("删除文件 {} 的旧向量数据失败: {}", fileId, e.getMessage());
                    }
                    
                    // 重新读取文件并处理
                    String filePath = fileInfo.getFileUrl();
                    try {
                        byte[] fileBytes = fileStorageService.download(filePath).bytes();
                        
                        Map<String, Object> processResult = documentService.uploadAndProcessDocumentFromBytes(
                                fileBytes,
                                fileInfo.getOriginalFilename(),
                                fileInfo.getMimeType(),
                                userId,
                                1, // 个人知识库
                                fileInfo.getId()
                        );
                        
                        if ((Boolean) processResult.get("success")) {
                            successCount++;
                        } else {
                            failCount++;
                            errors.add("文件ID " + fileId + " 索引重建失败: " + processResult.get("message"));
                        }
                    } catch (Exception e) {
                        failCount++;
                        errors.add("文件ID " + fileId + " 索引重建失败: " + e.getMessage());
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    errors.add("处理文件ID " + fileId + " 失败: " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("errors", errors);
            
            log.info("用户 {} 重建索引完成，成功: {}, 失败: {}", userId, successCount, failCount);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("重建索引失败: {}", e.getMessage(), e);
            return Result.error("重建索引失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     */
    @SaCheckLogin
    @GetMapping("/files/{fileId}/download")
    public void downloadFile(@PathVariable @NotNull Long fileId, HttpServletResponse response) {
        try {
            Long userId = Long.valueOf(StpUtil.getLoginId().toString());
            
            // 查询文件信息
            FileUpInfo fileInfo = fileInfoService.getById(fileId);
            if (fileInfo == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"code\":404,\"message\":\"文件不存在\"}");
                return;
            }
            
            // 验证文件所有者
            if (!userId.equals(fileInfo.getUserId())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"code\":403,\"message\":\"无权限下载该文件\"}");
                return;
            }
            
            // 获取文件URL并通过HTTP请求获取文件内容
            String fileUrl = fileInfo.getFileUrl();
            byte[] fileBytes;
            
            if (fileUrl != null && fileUrl.startsWith("http")) {
                // 文件存储在远程服务器（如阿里云OSS），通过HTTP请求获取
                java.net.URL url = new java.net.URL(fileUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                
                if (connection.getResponseCode() != 200) {
                    throw new IOException("从远程服务器获取文件失败, 状态码: " + connection.getResponseCode());
                }
                
                try (InputStream inputStream = connection.getInputStream();
                     java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    fileBytes = baos.toByteArray();
                }
            } else {
                // 本地文件或其他情况，尝试使用fileStorageService
                try {
                    fileBytes = fileStorageService.download(fileUrl).bytes();
                } catch (Exception e) {
                    log.error("使用fileStorageService下载失败，尝试直接读取: {}", e.getMessage());
                    // 如果有filePath字段，尝试从本地文件系统读取
                    String filePath = fileInfo.getFilePath();
                    if (filePath != null && !filePath.isEmpty()) {
                        java.nio.file.Path path = java.nio.file.Paths.get(filePath, fileInfo.getFileName());
                        fileBytes = java.nio.file.Files.readAllBytes(path);
                    } else {
                        throw new IOException("无法获取文件内容");
                    }
                }
            }
            
            // 设置响应头
            String encodedFilename = URLEncoder.encode(fileInfo.getOriginalFilename(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename*=UTF-8''" + encodedFilename);
            response.setContentLengthLong(fileBytes.length);
            
            // 写入响应
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(fileBytes);
                outputStream.flush();
            }
            
            log.info("用户 {} 下载文件成功: {}", userId, fileInfo.getOriginalFilename());
            
        } catch (Exception e) {
            log.error("下载文件失败: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":500,\"message\":\"下载文件失败\"}");
            } catch (IOException ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }
}
