package com.ldd.initialization.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Template;
import com.ldd.initialization.dto.TemplateSearchDTO;
import com.ldd.initialization.mapper.NoteCommentMapper;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.NoteSquareMapper;
import com.ldd.initialization.mapper.TemplateCommentLikeMapper;
import com.ldd.initialization.mapper.TemplateCommentMapper;
import com.ldd.initialization.mapper.TemplateFavoriteMapper;
import com.ldd.initialization.mapper.TemplateMapper;
import com.ldd.initialization.mapper.TemplateReportMapper;
import com.ldd.initialization.mapper.TemplateTagMapper;
import com.ldd.initialization.mapper.UserMapper;
import com.ldd.initialization.service.impl.TemplateServiceImpl;
import com.ldd.initialization.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateAccessAuthorizationTest {

    private TemplateMapper templateMapper;
    private TemplateCommentMapper templateCommentMapper;
    private TemplateTagMapper templateTagMapper;
    private UserMapper userMapper;
    private TemplateServiceImpl service;
    private Template template;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Template.class);
        templateMapper = mock(TemplateMapper.class);
        templateCommentMapper = mock(TemplateCommentMapper.class);
        templateTagMapper = mock(TemplateTagMapper.class);
        userMapper = mock(UserMapper.class);
        service = new TemplateServiceImpl(
                templateMapper,
                templateTagMapper,
                templateCommentMapper,
                mock(TemplateFavoriteMapper.class),
                mock(TemplateReportMapper.class),
                mock(TemplateCommentLikeMapper.class),
                userMapper,
                mock(NoteCommentMapper.class),
                mock(NoteMapper.class),
                mock(NoteSquareMapper.class));

        template = new Template();
        template.setId(33L);
        template.setTitle("Private template");
        template.setAuthorId(10L);
        template.setIsPublic(false);
        template.setStatus(1);
        when(templateMapper.selectById(33L)).thenReturn(template);
        when(templateTagMapper.selectTagsByTemplateIds(anyList())).thenReturn(Collections.emptyList());
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(Collections.emptyList());
        when(templateCommentMapper.countByTemplateId(33L)).thenReturn(0);
    }

    @Test
    void anotherUserCannotReadPrivateTemplateOrItsComments() {
        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::isAdmin).thenReturn(false);
            userUtils.when(UserUtils::getCurrentUserId).thenReturn(20L);

            assertThrows(BizException.class, () -> service.getTemplateById(33L));
            assertThrows(BizException.class, () -> service.getTemplateComments(33L));
            verify(templateMapper, never()).incrementViews(33L);
            verify(templateCommentMapper, never()).selectByTemplateId(33L);
        }
    }

    @Test
    void authorAndAdministratorCanReadPrivateTemplate() {
        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::isAdmin).thenReturn(false);
            userUtils.when(UserUtils::getCurrentUserId).thenReturn(10L);
            assertEquals("Private template", service.getTemplateById(33L).getTitle());
        }

        template.setStatus(0);
        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::isAdmin).thenReturn(true);
            assertEquals("Private template", service.getTemplateById(33L).getTitle());
        }
    }

    @Test
    void onlyPublishedPublicTemplatesArePubliclyReadable() {
        template.setIsPublic(true);
        template.setStatus(0);
        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::isAdmin).thenReturn(false);
            userUtils.when(UserUtils::getCurrentUserId).thenReturn(20L);
            assertThrows(BizException.class, () -> service.getTemplateById(33L));
        }

        template.setStatus(1);
        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::isAdmin).thenReturn(false);
            assertEquals("Private template", service.getTemplateById(33L).getTitle());
        }
    }

    @Test
    void ordinaryUserSearchIsLimitedToPublicPublishedAndOwnTemplates() {
        when(templateMapper.selectPage(any(Page.class), any(Wrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        TemplateSearchDTO search = new TemplateSearchDTO();
        search.setCategory("all");

        try (MockedStatic<UserUtils> userUtils = mockStatic(UserUtils.class)) {
            userUtils.when(UserUtils::isAdmin).thenReturn(false);
            userUtils.when(UserUtils::getCurrentUserId).thenReturn(20L);
            service.searchTemplates(search);
        }

        ArgumentCaptor<Wrapper<Template>> wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(templateMapper).selectPage(any(Page.class), wrapper.capture());
        String conditions = wrapper.getValue().getSqlSegment();
        assertTrue(conditions.contains("is_public"));
        assertTrue(conditions.contains("status"));
        assertTrue(conditions.contains("author_id"));
    }
}
