package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.config.ai.PgVectorConnectionPool;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.dto.AdminUserCreateDTO;
import com.ldd.initialization.dto.AdminUserUpdateDTO;
import com.ldd.initialization.dto.LoginDTO;
import com.ldd.initialization.dto.RegisterDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.FileInfoService;
import com.ldd.initialization.service.UserService;

import com.ldd.initialization.vo.CaptchaVO;
import com.ldd.initialization.vo.MembershipQuotaVO;
import com.ldd.initialization.vo.UserInfoVO;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用接口
 *
 * @author lxt
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private PgVectorConnectionPool connectionPool;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @SaIgnore
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO request) {
        userService.register(
                request.getPhone(),
                request.getPassword(),
                request.getCaptcha(),
                request.getCaptchaId(),
                request.getRoleType(),
                request.getUsername(),
                request.getAvatarUrl());
        return Result.success(null);
    }

    /**
     * 获取图形验证码
     *
     * @return 验证码图片和ID
     */
    @SaIgnore
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captcha = userService.generateCaptcha();
        return Result.success(captcha);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return Token
     */
    @SaIgnore
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO request) {
        String token = userService.login(request.getPhone(), request.getPassword(), request.getCaptcha(),
                request.getCaptchaId(), request.getRoleType());
        return Result.success(token);
    }

    /**
     * 退出登录
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success(null);
    }

    /**
     * 获取当前用户信息
     */
    @SaCheckLogin
    @GetMapping("/user/info")
    public Result<User> getUserInfo() {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }

    @SaCheckLogin
    @GetMapping("/users/{userId}/quota")
    public Result<MembershipQuotaVO> getUserMembershipQuota(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(userId)) {
            return Result.error("无权限访问");
        }

        MembershipQuotaVO quota = userService.getUserMembershipQuota(userId);
        return Result.success(quota);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info/{userId}")
    public Result<UserInfoVO> getUserInfoById(@PathVariable Long userId) {
        UserInfoVO user = userService.getUserInfoById(userId);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/update")
    public Result<Void> updateUser(@RequestBody User user) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        user.setId(userId);
        userService.updateUser(user);
        return Result.success(null);
    }

    /**
     * 文件上传（特殊情况使用MultipartFile）
     */
    @SaCheckLogin
    @PostMapping("/file/upload")
    public Result<FileUpInfo> uploadFile(@RequestParam("file") MultipartFile file) {

        Long userId = StpUtil.getLoginIdAsLong();
        FileUpInfo fileInfo = fileInfoService.uploadFile(file, userId);
        return Result.success(fileInfo);
    }

    /**
     * 分页查询用户列表
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) String keyword) {
        Page<User> userPage = userService.listUsers(page, size, roleType, status, gender, keyword);
        return Result.success(PageResult.convert(userPage));
    }

    /**
     * 管理员创建用户
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/users")
    public Result<User> createUserByAdmin(@Valid @RequestBody AdminUserCreateDTO request) {
        User user = userService.createUserByAdmin(request);
        return Result.success(user);
    }

    /**
     * 管理员更新指定用户
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/users/{userId}")
    public Result<Void> updateUserByAdmin(@PathVariable Long userId, @Valid @RequestBody AdminUserUpdateDTO request) {
        userService.updateUserByAdmin(userId, request);
        return Result.success(null);
    }

    /**
     * 管理员禁用(软删除)用户
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/users/{userId}")
    public Result<Void> disableUserByAdmin(@PathVariable Long userId) {
        userService.disableUserByAdmin(userId);
        return Result.success(null);
    }

    /**
     * 管理员启用用户
     */
    @SaCheckRole("ADMIN")
    @PostMapping("/users/{userId}/enable")
    public Result<Void> enableUserByAdmin(@PathVariable Long userId) {
        userService.enableUserByAdmin(userId);
        return Result.success(null);
    }

    @SaCheckRole("ADMIN")
    @PostMapping("/users/{userId}/hard-delete")
    public Result<Void> hardDeleteUserByAdmin(@PathVariable Long userId) {
        userService.hardDeleteUserByAdmin(userId);
        return Result.success(null);
    }

    /**
     * 测试PgVector连接池状态
     * 
     * @return 连接池状态信息
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/test/connection-pool")
    public Result<Map<String, Object>> testConnectionPool() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取连接池状态
            String poolStatus = connectionPool.getPoolStatus();
            result.put("poolStatus", poolStatus);

            // 测试连接是否正常
            boolean connectionTest = connectionPool.testConnection();
            result.put("connectionTest", connectionTest);

            result.put("success", true);
            result.put("message", "连接池状态检查完成");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "连接池状态检查失败");
        }

        return Result.success(result);
    }

    /**
     * 诊断向量数据库状态（检查pgvector扩展和documents表）
     * 
     * @param userId 可选的用户ID，用于检查特定用户的向量数据
     * @return 诊断结果
     */
    @SaIgnore
    @GetMapping("/diagnose/vector-db")
    public Result<Map<String, Object>> diagnoseVectorDatabase(@RequestParam(required = false) Long userId) {
        Map<String, Object> result = new HashMap<>();

        try (var connection = connectionPool.getConnection()) {
            // 1. 检查 pgvector 扩展是否已安装
            String checkExtensionSql = "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'vector') as extension_exists";
            boolean extensionExists = false;
            try (var stmt = connection.prepareStatement(checkExtensionSql)) {
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        extensionExists = rs.getBoolean("extension_exists");
                    }
                }
            }
            result.put("pgvector_extension_installed", extensionExists);

            if (!extensionExists) {
                result.put("success", false);
                result.put("error", "pgvector扩展未安装");
                result.put("solution", "请执行SQL: CREATE EXTENSION IF NOT EXISTS vector;");
                return Result.success(result);
            }

            // 2. 检查 documents 表是否存在
            String checkTableSql = "SELECT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'documents') as table_exists";
            boolean tableExists = false;
            try (var stmt = connection.prepareStatement(checkTableSql)) {
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        tableExists = rs.getBoolean("table_exists");
                    }
                }
            }
            result.put("documents_table_exists", tableExists);

            if (!tableExists) {
                result.put("success", false);
                result.put("error", "documents表不存在");
                result.put("solution", "请运行 sql/complete_database_init.sql 初始化数据库");
                return Result.success(result);
            }

            // 3. 检查 documents 表的总记录数
            String countAllSql = "SELECT COUNT(*) as total FROM documents";
            int totalCount = 0;
            try (var stmt = connection.prepareStatement(countAllSql)) {
                try (var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        totalCount = rs.getInt("total");
                    }
                }
            }
            result.put("total_documents", totalCount);

            // 4. 如果提供了用户ID，检查该用户的个人知识库数据
            if (userId != null) {
                String countUserSql = "SELECT COUNT(*) as user_count FROM documents WHERE knowledge_base_id = ? AND knowledge_base_type = 1";
                int userCount = 0;
                try (var stmt = connection.prepareStatement(countUserSql)) {
                    stmt.setLong(1, userId);
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            userCount = rs.getInt("user_count");
                        }
                    }
                }
                result.put("user_" + userId + "_documents", userCount);

                // 检查用户的文件ID列表
                String fileIdsSql = "SELECT DISTINCT file_id FROM documents WHERE knowledge_base_id = ? AND knowledge_base_type = 1 AND file_id IS NOT NULL";
                java.util.List<Long> fileIds = new java.util.ArrayList<>();
                try (var stmt = connection.prepareStatement(fileIdsSql)) {
                    stmt.setLong(1, userId);
                    try (var rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            fileIds.add(rs.getLong("file_id"));
                        }
                    }
                }
                result.put("user_" + userId + "_file_ids", fileIds);
            }

            // 5. 检查按知识库类型分组的统计
            String countByTypeSql = "SELECT knowledge_base_type, COUNT(*) as count FROM documents GROUP BY knowledge_base_type";
            Map<String, Integer> typeStats = new HashMap<>();
            try (var stmt = connection.prepareStatement(countByTypeSql)) {
                try (var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int type = rs.getInt("knowledge_base_type");
                        int count = rs.getInt("count");
                        String typeName = type == 1 ? "个人知识库" : (type == 2 ? "共享知识库" : "未知类型");
                        typeStats.put(typeName, count);
                    }
                }
            }
            result.put("documents_by_type", typeStats);

            result.put("success", true);
            result.put("message", "向量数据库诊断完成");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "向量数据库诊断失败");
            e.printStackTrace();
        }

        return Result.success(result);
    }

}
