package com.ldd.initialization.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.domain.UserMembership;
import com.ldd.initialization.dto.AdminUserCreateDTO;
import com.ldd.initialization.dto.AdminUserUpdateDTO;
import com.ldd.initialization.enums.RoleType;
import com.ldd.initialization.enums.StatusEnum;
import com.ldd.initialization.mapper.SharedKnowledgeBaseMapper;
import com.ldd.initialization.mapper.UserMembershipMapper;
import com.ldd.initialization.mapper.UserMapper;
import com.ldd.initialization.service.MembershipQuotaService;
import com.ldd.initialization.service.UserService;
import com.ldd.initialization.vo.CaptchaVO;
import com.ldd.initialization.vo.MembershipQuotaVO;
import com.ldd.initialization.vo.UserInfoVO;
import io.github.yindz.random.RandomSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-02-21 13:43:03
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Autowired
    private StringRedisTemplate redisTemplate; // 用于存储验证码

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private MembershipQuotaService membershipQuotaService;

    @Autowired
    private SharedKnowledgeBaseMapper sharedKnowledgeBaseMapper;

    private static final String CAPTCHA_PREFIX = "captcha:";

    @Override
    public void register(String phone, String password, String captcha, String captchaId, int roleType, String username,
            String avatarUrl) {
        log.info("[用户注册] 开始注册, 手机号: {}, 角色类型: {}", phone, roleType);

        // 校验验证码
        // todo
        String storedCaptcha = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaId);
        // if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
        // throw new BizException("验证码错误或已过期");
        // }
        // if (roleType == RoleType.ADMIN.getValue()) {
        // throw new BizException("管理员权限不允许注册");
        // }

        // 检查手机号是否重复：手机号必须全局唯一，不能按角色分组
        // （曾经按 phone + role_type 判重，导致同一手机号可再注册另一角色，登录时按手机号
        //   查询会命中 2 条，MyBatis selectOne 直接抛 TooManyResultsException）
        if (count(new QueryWrapper<User>().eq("phone", phone)) > 0) {
            log.warn("[用户注册] 手机号已被注册: {}", phone);
            throw new BizException("手机号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setPassword(DigestUtil.sha256Hex(password)); // SHA256 加密
        user.setRoleType(roleType);
        // roleType=1 普通用户：启用；roleType=2 管理用户：用于后台权限控制
        user.setStatus(roleType == 2 ? StatusEnum.PENDING.getCode() : StatusEnum.ENABLED.getCode());
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        String name = StrUtil.isNotBlank(username) ? username : RandomSource.personInfoSource().randomChineseName();
        user.setUsername(name);
        user.setRealName(name);
        if (StrUtil.isNotBlank(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
        } else {
            user.setAvatarUrl(null);
        }
        // 保存用户
        save(user);
        log.info("[用户注册] 注册成功, 用户ID: {}, 手机号: {}", user.getId(), phone);

        // 删除验证码
        redisTemplate.delete(CAPTCHA_PREFIX + captchaId);

    }

    @Override
    public String login(String phone, String password, String captcha, String captchaId, int roleType) {
        log.info("[用户登录] 开始登录, 手机号: {}, 角色类型: {}", phone, roleType);

        // 校验验证码
        String storedCaptcha = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaId);
        if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
            throw new BizException("验证码错误或已过期");
        }

        // 查询用户：用 list 而不是 getOne。
        // 手机上若存在历史重复数据，getOne 会抛 TooManyResultsException（接口直接 500），
        // 这里按请求角色精确挑出目标账号，既兼容脏数据又能给出准确的错误提示。
        List<User> candidates = list(new QueryWrapper<User>().eq("phone", phone));
        if (candidates.isEmpty()) {
            log.warn("[用户登录] 用户不存在, 手机号: {}", phone);
            throw new BizException("手机号或密码错误");
        }
        User user = null;
        if (candidates.size() > 1) {
            log.warn("[用户登录] 手机号命中 {} 条记录，按角色 roleType={} 精确匹配: {}", candidates.size(), roleType, phone);
        }
        for (User candidate : candidates) {
            if (Objects.equals(candidate.getRoleType(), roleType)) {
                user = candidate;
                break;
            }
        }
        if (user == null) {
            log.warn("[用户登录] 角色不匹配, 手机号: {}, 请求角色: {}", phone, roleType);
            throw new BizException("账号权限不足");
        }

        // 校验密码（使用相同的 SHA256 加密方式）
        String encryptedPassword = DigestUtil.sha256Hex(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            log.warn("[用户登录] 密码错误, 用户ID: {}", user.getId());
            throw new BizException("手机号或密码错误");
        }

        // 校验角色权限（避免因为 role_type 条件查询导致误报“手机号或密码错误”）
        if (!Objects.equals(user.getRoleType(), roleType)) {
            log.warn("[用户登录] 角色不匹配, 用户ID: {}, 用户角色: {}, 请求角色: {}", user.getId(), user.getRoleType(), roleType);
            throw new BizException("账号权限不足");
        }

        // 检查账号状态
        if (Objects.equals(user.getStatus(), StatusEnum.DISABLED.getCode())) {
            log.warn("[用户登录] 账号已被禁用, 用户ID: {}", user.getId());
            throw new BizException("账号已被禁用");
        }

        String roleTypeStr = RoleType.getDescByValue(user.getRoleType());
        if (StrUtil.isBlank(roleTypeStr)) {
            log.warn("[用户登录] 未识别的角色类型: {}", user.getRoleType());
            roleTypeStr = "USER";
        }

        // 执行登录
        StpUtil.login(user.getId());
        // 保存用户权限
        StpUtil.getSession().set("role", roleTypeStr);

        // 设置角色
        StpUtil.getSession().set("user_info", user);
        // 更新登录信息
        user.setLastLoginTime(new Date());
        updateById(user);

        User userObj = (User) StpUtil.getSession().get("user_info");
        log.info("[用户登录] 登录成功, 用户ID: {}, 用户名: {}", user.getId(), userObj.getUsername());
        // 删除验证码
        redisTemplate.delete(CAPTCHA_PREFIX + captchaId);
        // 返回 Token
        return StpUtil.getTokenValue();
    }

    @Override
    public User getUserInfo(Long userId) {
        log.debug("[用户信息] 获取用户信息, 用户ID: {}", userId);
        User user = getById(userId);
        if (user == null) {
            log.warn("[用户信息] 用户不存在, 用户ID: {}", userId);
            throw new BizException("用户不存在");
        }

        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserMembership> mw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            mw.eq(UserMembership::getUserId, userId)
                    .orderByDesc(UserMembership::getExpireTime)
                    .last("limit 1");
            UserMembership m = userMembershipMapper.selectOne(mw);
            if (m != null) {
                user.setMemberExpireTime(m.getExpireTime());
                user.setMemberLevelName("专业会员");
                boolean active = m.getExpireTime() != null && m.getExpireTime().after(new Date());
                user.setIsMember(active);
            } else {
                user.setIsMember(false);
            }
        } catch (Exception ignored) {
            user.setIsMember(false);
        }
        return user;
    }

    @Override
    public void updateUser(User user) {
        log.info("[用户信息] 更新用户信息, 用户ID: {}", user.getId());
        user.setUpdateTime(new Date());
        updateById(user);
        log.info("[用户信息] 用户信息更新成功, 用户ID: {}", user.getId());
    }

    @Override
    public Page<User> listUsers(int page, int size, Integer roleType, Integer status, Integer gender, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (roleType != null) {
            wrapper.eq("role_type", roleType);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (gender != null) {
            wrapper.eq("gender", gender);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(q -> q.like("username", keyword)
                    .or().like("phone", keyword));
        }
        page(userPage, wrapper);
        List<User> records = userPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(User::getId).filter(Objects::nonNull).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                List<UserMembership> memberships = userMembershipMapper
                        .selectList(new QueryWrapper<UserMembership>().in("user_id", userIds));
                Map<Long, UserMembership> membershipMap = new HashMap<>();
                for (UserMembership membership : memberships) {
                    if (membership == null || membership.getUserId() == null) {
                        continue;
                    }
                    UserMembership existing = membershipMap.get(membership.getUserId());
                    if (existing == null) {
                        membershipMap.put(membership.getUserId(), membership);
                        continue;
                    }
                    Date existingExpire = existing.getExpireTime();
                    Date currentExpire = membership.getExpireTime();
                    if (existingExpire == null) {
                        if (currentExpire != null) {
                            membershipMap.put(membership.getUserId(), membership);
                        }
                    } else if (currentExpire != null && currentExpire.after(existingExpire)) {
                        membershipMap.put(membership.getUserId(), membership);
                    }
                }
                Date now = new Date();
                for (User user : records) {
                    UserMembership membership = membershipMap.get(user.getId());
                    if (membership != null) {
                        user.setMemberExpireTime(membership.getExpireTime());
                        user.setMemberLevelName("专业会员");
                        Boolean active = membership.getIsActive();
                        if (active == null) {
                            active = membership.getExpireTime() != null && membership.getExpireTime().after(now);
                        }
                        user.setIsMember(active);
                    } else {
                        user.setIsMember(false);
                    }
                }
            }
        }
        return userPage;
    }

    @Override
    public void batchDisableUsers(List<Long> userIds) {
        update(null, new LambdaUpdateWrapper<User>()
                .in(User::getId, userIds)
                .set(User::getStatus, StatusEnum.DISABLED.getCode())
                .set(User::getUpdateTime, new Date()));
    }

    @Override
    public User createUserByAdmin(AdminUserCreateDTO request) {
        if (request == null) {
            throw new BizException("请求参数不能为空");
        }

        // 检查手机号是否重复（不区分角色）
        if (count(new QueryWrapper<User>().eq("phone", request.getPhone())) > 0) {
            throw new BizException("手机号已被注册");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(DigestUtil.sha256Hex(request.getPassword()));
        user.setRoleType(request.getRoleType());
        user.setUsername(StrUtil.isNotBlank(request.getUsername()) ? request.getUsername() : request.getPhone());
        user.setRealName(request.getRealName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());

        Integer status = request.getStatus();
        user.setStatus(status != null ? status : StatusEnum.ENABLED.getCode());

        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        save(user);
        return user;
    }

    @Override
    public void updateUserByAdmin(Long userId, AdminUserUpdateDTO request) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        if (request == null) {
            throw new BizException("请求参数不能为空");
        }

        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (StrUtil.isNotBlank(request.getUsername())) {
            user.setUsername(request.getUsername());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getRoleType() != null) {
            user.setRoleType(request.getRoleType());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (StrUtil.isNotBlank(request.getPassword())) {
            user.setPassword(DigestUtil.sha256Hex(request.getPassword()));
        }

        applyMembershipUpdate(userId, request);

        user.setUpdateTime(new Date());
        updateById(user);
    }

    private void applyMembershipUpdate(Long userId, AdminUserUpdateDTO request) {
        if (request.getMemberActive() == null && request.getMemberExpireTime() == null) {
            return;
        }

        LambdaQueryWrapper<UserMembership> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMembership::getUserId, userId)
                .orderByDesc(UserMembership::getExpireTime)
                .last("limit 1");
        UserMembership membership = userMembershipMapper.selectOne(wrapper);
        Date now = new Date();
        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setCreateTime(now);
        }

        if (request.getMemberExpireTime() != null) {
            membership.setExpireTime(request.getMemberExpireTime());
        }
        if (request.getMemberActive() != null) {
            membership.setIsActive(request.getMemberActive());
        } else if (request.getMemberExpireTime() != null) {
            membership.setIsActive(request.getMemberExpireTime().after(now));
        }

        membership.setUpdateTime(now);

        if (membership.getId() == null) {
            userMembershipMapper.insert(membership);
        } else {
            userMembershipMapper.updateById(membership);
        }
    }

    @Override
    public void disableUserByAdmin(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }

        boolean updated = update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, StatusEnum.DISABLED.getCode())
                .set(User::getUpdateTime, new Date()));

        if (!updated) {
            throw new BizException("用户不存在或更新失败");
        }
    }

    @Override
    public void hardDeleteUserByAdmin(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }

        boolean removed = removeById(userId);
        if (!removed) {
            throw new BizException("用户不存在或删除失败");
        }
    }

    @Override
    public void enableUserByAdmin(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }

        boolean updated = update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, StatusEnum.ENABLED.getCode())
                .set(User::getUpdateTime, new Date()));

        if (!updated) {
            throw new BizException("用户不存在或更新失败");
        }
    }

    @Override
    public CaptchaVO generateCaptcha() {
        // 使用 Hutool 生成图形验证码（宽120，高40，4位字符）
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode(); // 获取验证码文本
        String imageBase64 = lineCaptcha.getImageBase64(); // 获取Base64编码图片

        // 生成唯一ID
        String captchaId = IdUtil.simpleUUID();

        // 存储验证码到 Redis，5分钟有效期
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + captchaId, code, 5, TimeUnit.MINUTES);
        log.debug("[验证码] 生成验证码, captchaId: {}", captchaId);

        // 返回验证码响应
        CaptchaVO response = new CaptchaVO();
        response.setCaptchaId(captchaId);
        response.setImageBase64(imageBase64);
        return response;
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        User user = baseMapper.selectById(userId);
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    @Override
    public void batchEnableUsers(List<Long> userIds) {
        update(null, new LambdaUpdateWrapper<User>()
                .in(User::getId, userIds)
                .set(User::getStatus, StatusEnum.ENABLED.getCode()) // status=1 表示启用
                .set(User::getUpdateTime, new Date()));
    }

    @Override
    public MembershipQuotaVO getUserMembershipQuota(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }

        // 获取会员配额信息
        UserMembership membership = membershipQuotaService.getMembershipQuota(userId);
        if (membership == null) {
            throw new BizException("用户会员信息不存在");
        }

        // 构建配额VO对象
        MembershipQuotaVO quotaVO = new MembershipQuotaVO();

        // AI调用配额信息
        quotaVO.setAiCallQuota(membership.getAiCallQuota());
        quotaVO.setAiCallUsed(membership.getAiCallUsed());
        int aiRemaining = membership.getAiCallQuota() - membership.getAiCallUsed();
        quotaVO.setAiCallRemaining(Math.max(0, aiRemaining));

        // 知识库配额信息
        quotaVO.setKbQuota(membership.getKbQuota());
        int kbUsed = (int) Math.min(Integer.MAX_VALUE, sharedKnowledgeBaseMapper.countCreatedByUserId(userId));
        quotaVO.setKbUsed(kbUsed);

        // 判断是否无限制知识库（付费会员kb_quota=-1表示无限）
        boolean isUnlimited = membership.getKbQuota() == -1;
        quotaVO.setIsUnlimitedKb(isUnlimited);

        if (isUnlimited) {
            quotaVO.setKbRemaining(-1); // -1表示无限制
        } else {
            int kbRemaining = membership.getKbQuota() - kbUsed;
            quotaVO.setKbRemaining(Math.max(0, kbRemaining));
        }

        // 计算下次重置时间（下月1号0点）
        Calendar nextReset = Calendar.getInstance();
        nextReset.add(Calendar.MONTH, 1);
        nextReset.set(Calendar.DAY_OF_MONTH, 1);
        nextReset.set(Calendar.HOUR_OF_DAY, 0);
        nextReset.set(Calendar.MINUTE, 0);
        nextReset.set(Calendar.SECOND, 0);
        nextReset.set(Calendar.MILLISECOND, 0);

        // 格式化为字符串
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        quotaVO.setNextResetTime(sdf.format(nextReset.getTime()));

        log.info("[配额信息] 获取用户配额信息成功, 用户ID: {}, AI剩余: {}/{}, 知识库: {}/{}",
                userId, quotaVO.getAiCallRemaining(), quotaVO.getAiCallQuota(),
                quotaVO.getKbUsed(), isUnlimited ? "无限" : quotaVO.getKbQuota());

        return quotaVO;
    }

}
