package com.ldd.initialization.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.dto.AdminUserCreateDTO;
import com.ldd.initialization.dto.AdminUserUpdateDTO;
import com.ldd.initialization.vo.CaptchaVO;
import com.ldd.initialization.vo.MembershipQuotaVO;
import com.ldd.initialization.vo.UserInfoVO;

import java.util.List;

/**
 * @author Administrator
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2025-02-21 13:43:03
 */
public interface UserService extends IService<User> {

        /**
         * 用户注册
         * 
         * @param phone     手机号
         * @param password  密码
         * @param captcha   验证码
         * @param captchaId 验证码ID
         * @param roleType  角色类型
         */
        void register(String phone, String password, String captcha, String captchaId, int roleType, String username,
                        String avatarUrl);

        /**
         * 用户登录
         * 
         * @param phone     手机号
         * @param password  密码
         * @param captcha   验证码
         * @param captchaId 验证码ID
         * @return Token
         */
        String login(String phone, String password, String captcha, String captchaId, int roleType);

        User getUserInfo(Long userId);

        void updateUser(User user);

        Page<User> listUsers(int page, int size, Integer roleType, Integer status, Integer gender, String keyword);

        void batchDisableUsers(List<Long> userIds);

        User createUserByAdmin(AdminUserCreateDTO request);

        void updateUserByAdmin(Long userId, AdminUserUpdateDTO request);

        void disableUserByAdmin(Long userId);

        void enableUserByAdmin(Long userId);

        void hardDeleteUserByAdmin(Long userId);

        CaptchaVO generateCaptcha();

        UserInfoVO getUserInfoById(Long userId);

        void batchEnableUsers(List<Long> userIds);

        /**
         * 获取用户会员配额信息
         * 
         * @param userId 用户ID
         * @return 配额信息
         */
        MembershipQuotaVO getUserMembershipQuota(Long userId);
}
