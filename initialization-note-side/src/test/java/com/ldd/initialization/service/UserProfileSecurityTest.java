package com.ldd.initialization.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.dto.PasswordChangeDTO;
import com.ldd.initialization.dto.UserProfileUpdateDTO;
import com.ldd.initialization.mapper.UserMapper;
import com.ldd.initialization.service.impl.UserServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 独立测试，不连接数据库或 Redis。 */
class UserProfileSecurityTest {
    private UserServiceImpl service;
    private UserMapper mapper;
    private User user;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @BeforeEach
    void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), User.class);
        mapper = mock(UserMapper.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        user = new User();
        user.setId(42L);
        user.setRoleType(1);
        user.setPhone("19912345678");
        user.setPassword(DigestUtil.sha256Hex("original-password"));
        when(mapper.selectById(42L)).thenReturn(user);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
    }

    @Test
    void injectedPrivilegesAndForeignIdNeverBecomeUpdateColumns() throws Exception {
        var request = json.readValue("""
                {"id":7,"roleType":2,"status":1,"password":"attacker",
                 "idCard":"private","createTime":"2000-01-01","isMember":true,"username":"新昵称"}
                """, UserProfileUpdateDTO.class);
        service.updateUserProfile(42L, request);
        var update = capturedUpdate();
        assertFalse(update.getSqlSet().contains("role_type"));
        assertFalse(update.getSqlSet().contains("status"));
        assertFalse(update.getSqlSet().contains("password"));
        assertFalse(update.getSqlSet().contains("id_card"));
        assertFalse(update.getSqlSet().contains("create_time"));
        assertTrue(update.getSqlSet().contains("username"));
        update.getSqlSegment();
        assertTrue(update.getParamNameValuePairs().containsValue(42L));
        assertFalse(update.getParamNameValuePairs().containsValue(7L));
        assertFalse(update.getParamNameValuePairs().containsValue("attacker"));
    }

    @Test
    void partialAvatarUpdatePreservesBirthdayButExplicitNullClearsIt() throws Exception {
        service.updateUserProfile(42L, json.readValue("{\"avatarUrl\":\"https://example.org/a.png\"}", UserProfileUpdateDTO.class));
        assertFalse(capturedUpdate().getSqlSet().contains("birth_date"));
        clearInvocations(mapper);
        service.updateUserProfile(42L, json.readValue("{\"birthDate\":null}", UserProfileUpdateDTO.class));
        assertTrue(capturedUpdate().getSqlSet().contains("birth_date"));
    }

    @Test
    void phoneCannotBeChangedToAnExistingAccount() {
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        var request = new UserProfileUpdateDTO();
        request.setPhone("19987654321");
        assertThrows(BizException.class, () -> service.updateUserProfile(42L, request));
        verify(mapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void incorrectCurrentPasswordCannotChangePassword() {
        var request = new PasswordChangeDTO();
        request.setCurrentPassword("wrong");
        request.setNewPassword("new-password");
        assertThrows(BizException.class, () -> service.changePassword(42L, request));
        verify(mapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void successfulPasswordChangeStoresHashAndInvalidatesSessions() {
        var request = new PasswordChangeDTO();
        request.setCurrentPassword("original-password");
        request.setNewPassword("new-password");
        try (var auth = mockStatic(StpUtil.class)) {
            service.changePassword(42L, request);
            auth.verify(() -> StpUtil.logout(42L));
        }
        var update = capturedUpdate();
        update.getSqlSegment();
        assertTrue(update.getParamNameValuePairs().containsValue(DigestUtil.sha256Hex("new-password")));
        assertFalse(update.getParamNameValuePairs().containsValue("new-password"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private LambdaUpdateWrapper<User> capturedUpdate() {
        ArgumentCaptor<Wrapper> capture = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).update(isNull(), capture.capture());
        return (LambdaUpdateWrapper<User>) capture.getValue();
    }
}
