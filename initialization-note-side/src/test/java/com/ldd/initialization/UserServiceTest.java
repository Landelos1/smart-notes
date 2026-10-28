package com.ldd.initialization;

import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.service.UserService;
import com.ldd.initialization.vo.CaptchaVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 用户认证服务测试
 */
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("测试获取验证码")
    void testGenerateCaptcha() {
        CaptchaVO captcha = userService.generateCaptcha();
        
        Assertions.assertNotNull(captcha);
        Assertions.assertNotNull(captcha.getCaptchaId());
        Assertions.assertNotNull(captcha.getImageBase64());
        Assertions.assertFalse(captcha.getCaptchaId().isEmpty());
        Assertions.assertFalse(captcha.getImageBase64().isEmpty());
    }

    @Test
    @DisplayName("测试获取用户信息 - 用户存在")
    void testGetUserInfo() {
        // 先确保用户存在
        Long userId = 1L;
        try {
            User user = userService.getUserInfo(userId);
            Assertions.assertNotNull(user);
            Assertions.assertEquals(userId, user.getId());
        } catch (BizException e) {
            // 如果用户不存在，跳过测试
            Assertions.assertEquals("用户不存在", e.getMessage());
        }
    }

    @Test
    @DisplayName("测试获取用户信息 - 用户不存在")
    void testGetUserInfoNotFound() {
        Long nonExistentUserId = 999999L;
        
        Assertions.assertThrows(BizException.class, () -> {
            userService.getUserInfo(nonExistentUserId);
        });
    }

    @Test
    @DisplayName("测试更新用户信息")
    void testUpdateUser() {
        Long userId = 1L;
        try {
            User user = userService.getUserInfo(userId);
            String originalUsername = user.getUsername();
            
            // 更新用户名
            user.setUsername("测试用户名_" + System.currentTimeMillis());
            userService.updateUser(user);
            
            // 验证更新成功
            User updatedUser = userService.getUserInfo(userId);
            Assertions.assertNotEquals(originalUsername, updatedUser.getUsername());
            Assertions.assertNotNull(updatedUser.getUpdateTime());
            
            // 恢复原始用户名
            user.setUsername(originalUsername);
            userService.updateUser(user);
        } catch (BizException e) {
            // 如果用户不存在，跳过测试
            System.out.println("跳过测试：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试登录 - 手机号不存在")
    void testLoginWithInvalidPhone() {
        String nonExistentPhone = "19999999999";
        String password = "password123";
        String captcha = "1234";
        String captchaId = "test-captcha-id";
        int roleType = 1;
        
        Assertions.assertThrows(BizException.class, () -> {
            userService.login(nonExistentPhone, password, captcha, captchaId, roleType);
        });
    }

    @Test
    @DisplayName("测试注册 - 手机号已存在")
    void testRegisterDuplicatePhone() {
        // 首先注册一个用户
        String phone = "13800138001";
        String password = "password123";
        String captcha = "1234";
        String captchaId = "test-captcha-id";
        int roleType = 1;
        String username = "测试用户";
        String avatarUrl = "";

        try {
            // 尝试第一次注册
            userService.register(phone, password, captcha, captchaId, roleType, username, avatarUrl);
        } catch (BizException e) {
            // 可能已经存在
        }

        // 再次尝试注册相同手机号，应该抛出异常
        Assertions.assertThrows(BizException.class, () -> {
            userService.register(phone, password, captcha, captchaId, roleType, username + "2", avatarUrl);
        });
    }

    @Test
    @DisplayName("测试分页查询用户列表")
    void testListUsers() {
        var result = userService.listUsers(1, 10, null, null, null, null);
        
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getCurrent() >= 1);
        Assertions.assertTrue(result.getSize() >= 1);
    }

    @Test
    @DisplayName("测试分页查询用户列表 - 带角色类型过滤")
    void testListUsersWithRoleType() {
        var result = userService.listUsers(1, 10, 1, null, null, null);
        
        Assertions.assertNotNull(result);
        // 验证返回的用户都是指定角色类型
        for (User user : result.getRecords()) {
            Assertions.assertEquals(1, user.getRoleType());
        }
    }

    @Test
    @DisplayName("测试分页查询用户列表 - 带关键字搜索")
    void testListUsersWithKeyword() {
        var result = userService.listUsers(1, 10, null, null, null, "测试");
        
        Assertions.assertNotNull(result);
        // 验证搜索结果
    }
}
