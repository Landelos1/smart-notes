package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.Template;
import com.ldd.initialization.domain.TemplateComment;
import com.ldd.initialization.domain.TemplateCommentLike;
import com.ldd.initialization.domain.TemplateFavorite;
import com.ldd.initialization.domain.TemplateReport;
import com.ldd.initialization.domain.TemplateTag;
import com.ldd.initialization.domain.NoteComment;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.dto.TemplateCommentCreateDTO;
import com.ldd.initialization.dto.TemplateCreateDTO;
import com.ldd.initialization.dto.TemplateReportDTO;
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
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.service.TemplateService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.TemplateCommentVO;
import com.ldd.initialization.vo.TemplateShareVO;
import com.ldd.initialization.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;
    private final TemplateTagMapper templateTagMapper;
    private final TemplateCommentMapper templateCommentMapper;
    private final TemplateFavoriteMapper templateFavoriteMapper;
    private final TemplateReportMapper templateReportMapper;
    private final TemplateCommentLikeMapper templateCommentLikeMapper;
    private final UserMapper userMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final NoteMapper noteMapper;
    private final NoteSquareMapper noteSquareMapper;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public PageResult<TemplateVO> searchTemplates(TemplateSearchDTO searchDTO) {
        // 构建查询条件
        LambdaQueryWrapper<Template> queryWrapper = new LambdaQueryWrapper<>();
        
        // 分类过滤（all表示全部）
        if (StringUtils.hasText(searchDTO.getCategory()) && !"all".equals(searchDTO.getCategory())) {
            queryWrapper.eq(Template::getCategory, searchDTO.getCategory());
        }
        
        // 关键词搜索
        if (StringUtils.hasText(searchDTO.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(Template::getTitle, searchDTO.getKeyword())
                .or()
                .like(Template::getDescription, searchDTO.getKeyword())
            );
        }
        
        // 按更新时间倒序
        queryWrapper.orderByDesc(Template::getUpdatedAt);
        
        // 分页查询
        Page<Template> page = new Page<>(searchDTO.getPage(), searchDTO.getPageSize());
        Page<Template> resultPage = templateMapper.selectPage(page, queryWrapper);
        
        // 转换为VO
        List<TemplateVO> templateVOs = convertToVOList(resultPage.getRecords());
        
        // 构建分页结果
        PageResult<TemplateVO> pageResult = new PageResult<>();
        pageResult.setData(templateVOs);
        pageResult.setTotal(resultPage.getTotal());
        return pageResult;
    }

    @Override
    public TemplateVO getTemplateById(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            return null;
        }
        
        // 增加浏览次数
        templateMapper.incrementViews(id);
        
        return convertToVO(template);
    }

    @Override
    public List<TemplateVO> getRelatedTemplates(Long templateId, Integer limit) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            return Collections.emptyList();
        }
        
        List<Template> relatedTemplates = templateMapper.selectRelatedTemplates(
            template.getCategory(), templateId, limit != null ? limit : 3
        );
        
        return convertToVOList(relatedTemplates);
    }

    @Override
    public List<TemplateCommentVO> getTemplateComments(Long templateId) {
        List<TemplateComment> comments = templateCommentMapper.selectByTemplateId(templateId);
        Template template = templateMapper.selectById(templateId);
        String templateTitle = template != null ? template.getTitle() : null;
        
        // 获取用户信息
        Set<Long> userIds = comments.stream()
            .map(TemplateComment::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }
        
        // 转换为VO
        List<TemplateCommentVO> commentVOs = new ArrayList<>();
        for (TemplateComment comment : comments) {
            TemplateCommentVO vo = new TemplateCommentVO();
            vo.setId(comment.getId());
            vo.setSourceType("TEMPLATE");
            vo.setTargetId(templateId);
            vo.setTargetTitle(templateTitle);
            vo.setContent(comment.getContent());
            vo.setRating(comment.getRating());
            vo.setCreatedAt(comment.getCreatedAt());
            vo.setLikes(comment.getLikes() != null ? comment.getLikes() : 0);
            
            // 设置作者信息
            if (comment.getUserId() != null && userMap.containsKey(comment.getUserId())) {
                User user = userMap.get(comment.getUserId());
                vo.setAuthor(user.getUsername());
                // 使用用户真实头像，如果没有则使用生成的头像
                if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                    vo.setAvatar(user.getAvatarUrl());
                } else {
                    vo.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getUsername());
                }
            } else {
                vo.setAuthor("匿名用户");
                vo.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Anonymous");
            }
            
            commentVOs.add(vo);
        }
        
        return commentVOs;
    }

    @Override
    public PageResult<TemplateCommentVO> listAdminComments(Integer current,
                                                           Integer size,
                                                           String sourceType,
                                                           Long targetId,
                                                           String keyword) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;
        String normalizedType = sourceType == null ? "ALL" : sourceType.toUpperCase();

        List<TemplateCommentVO> records = new ArrayList<>();
        long total = 0L;

        boolean queryTemplate = "ALL".equals(normalizedType) || "TEMPLATE".equals(normalizedType);
        boolean queryNote = "ALL".equals(normalizedType) || "NOTE".equals(normalizedType);

        if (queryTemplate) {
            List<TemplateCommentVO> templateRecords = templateCommentMapper.selectAdminComments(
                    targetId,
                    keyword
            );
            for (TemplateCommentVO vo : templateRecords) {
                vo.setSourceType("TEMPLATE");
                if (!StringUtils.hasText(vo.getTargetTitle()) && vo.getTargetId() != null) {
                    Template template = templateMapper.selectById(vo.getTargetId());
                    if (template != null) {
                        vo.setTargetTitle(template.getTitle());
                    }
                }
                records.add(vo);
            }
            total += templateCommentMapper.countAdminComments(targetId, keyword);
        }

        if (queryNote) {
            List<TemplateCommentVO> noteRecords = noteCommentMapper.selectAdminComments(
                    targetId,
                    keyword
            );
            for (TemplateCommentVO vo : noteRecords) {
                vo.setSourceType("NOTE");
                records.add(vo);
            }
            total += noteCommentMapper.countAdminComments(targetId, keyword);
        }

        records.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        int fromIndex = Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = Math.min(records.size(), fromIndex + pageSize);
        List<TemplateCommentVO> pageRecords = fromIndex >= records.size() ? Collections.emptyList() : records.subList(fromIndex, toIndex);

        PageResult<TemplateCommentVO> result = new PageResult<>();
        result.setData(pageRecords);
        result.setTotal(total);
        return result;
    }

    @Override
    public void useTemplate(Long templateId) {
        templateMapper.incrementDownloads(templateId);
    }

    @Override
    public void likeComment(Long commentId) {
        templateCommentMapper.incrementLikes(commentId);
    }

    /**
     * 批量转换Template为TemplateVO
     */
    private List<TemplateVO> convertToVOList(List<Template> templates) {
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 获取所有模板ID
        List<Long> templateIds = templates.stream()
            .map(Template::getId)
            .collect(Collectors.toList());
        
        // 批量获取标签
        List<TemplateTag> allTags = templateTagMapper.selectTagsByTemplateIds(templateIds);
        Map<Long, List<String>> tagMap = allTags.stream()
            .collect(Collectors.groupingBy(
                TemplateTag::getTemplateId,
                Collectors.mapping(TemplateTag::getTagName, Collectors.toList())
            ));
        
        // 获取作者信息
        Set<Long> authorIds = templates.stream()
            .map(Template::getAuthorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<Long, User> userMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(authorIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }
        
        // 转换
        List<TemplateVO> result = new ArrayList<>();
        for (Template template : templates) {
            TemplateVO vo = new TemplateVO();
            vo.setId(template.getId());
            vo.setTitle(template.getTitle());
            vo.setDescription(template.getDescription());
            vo.setCategory(template.getCategory());
            vo.setThumbnail(template.getThumbnail());
            vo.setContent(template.getContent());
            vo.setDownloads(template.getDownloads() != null ? template.getDownloads() : 0);
            vo.setRating(template.getRating());
            vo.setViews(template.getViews() != null ? template.getViews() : 0);
            vo.setAuthorId(template.getAuthorId());
            vo.setUpdatedAt(template.getUpdatedAt());
            vo.setTags(tagMap.getOrDefault(template.getId(), Collections.emptyList()));
            
            // 设置作者名称
            if (template.getAuthorId() != null && userMap.containsKey(template.getAuthorId())) {
                vo.setAuthor(userMap.get(template.getAuthorId()).getUsername());
            } else {
                vo.setAuthor("系统");
            }
            
            // 获取评论数量
            vo.setCommentCount(templateCommentMapper.countByTemplateId(template.getId()));
            
            result.add(vo);
        }
        
        return result;
    }

    /**
     * 转换单个Template为TemplateVO
     */
    private TemplateVO convertToVO(Template template) {
        return convertToVOList(Collections.singletonList(template)).get(0);
    }

    @Override
    @Transactional
    public TemplateVO createTemplate(TemplateCreateDTO createDTO, Long userId) {
        log.info("📝 创建模板, userId={}, title={}", userId, createDTO.getTitle());
        
        // 创建模板实体
        Template template = new Template();
        template.setTitle(createDTO.getTitle());
        template.setDescription(createDTO.getDescription());
        template.setCategory(createDTO.getCategory());
        template.setThumbnail(createDTO.getThumbnail());
        template.setContent(createDTO.getContent());
        template.setAuthorId(userId);
        template.setIsPublic(createDTO.getIsPublic() != null ? createDTO.getIsPublic() : true);
        template.setStatus(1); // 默认已发布
        template.setDownloads(0);
        template.setViews(0);
        template.setRating(BigDecimal.ZERO);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        
        templateMapper.insert(template);
        
        // 保存标签
        saveTemplateTags(template.getId(), createDTO.getTags());
        
        log.info("✅ 模板创建成功, templateId={}", template.getId());
        return convertToVO(template);
    }

    @Override
    @Transactional
    public TemplateVO updateTemplate(TemplateCreateDTO createDTO, Long userId) {
        log.info("✏️ 更新模板, userId={}, templateId={}", userId, createDTO.getId());
        
        // 检查模板是否存在
        Template template = templateMapper.selectById(createDTO.getId());
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        
        // 检查是否是作者或管理员
        if (!userId.equals(template.getAuthorId()) && !UserUtils.isAdmin()) {
            throw new RuntimeException("无权修改此模板");
        }
        
        // 更新模板信息
        template.setTitle(createDTO.getTitle());
        template.setDescription(createDTO.getDescription());
        template.setCategory(createDTO.getCategory());
        template.setThumbnail(createDTO.getThumbnail());
        template.setContent(createDTO.getContent());
        template.setIsPublic(createDTO.getIsPublic() != null ? createDTO.getIsPublic() : true);
        template.setUpdatedAt(LocalDateTime.now());
        
        templateMapper.updateById(template);
        
        // 更新标签（先删除旧的，再添加新的）
        templateTagMapper.delete(new LambdaQueryWrapper<TemplateTag>()
            .eq(TemplateTag::getTemplateId, template.getId()));
        saveTemplateTags(template.getId(), createDTO.getTags());
        
        log.info("✅ 模板更新成功, templateId={}", template.getId());
        return convertToVO(template);
    }

    @Override
    @Transactional
    public boolean deleteTemplate(Long templateId, Long userId) {
        log.info("🗑️ 删除模板, userId={}, templateId={}", userId, templateId);
        
        // 检查模板是否存在
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        
        // 检查是否是作者或管理员
        if (!userId.equals(template.getAuthorId()) && !UserUtils.isAdmin()) {
            throw new RuntimeException("无权删除此模板");
        }
        
        // 删除模板（级联删除标签和评论）
        templateMapper.deleteById(templateId);
        
        log.info("✅ 模板删除成功, templateId={}", templateId);
        return true;
    }

    @Override
    public List<TemplateVO> getMyTemplates(Long userId) {
        log.info("📚 获取用户模板列表, userId={}", userId);
        
        LambdaQueryWrapper<Template> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Template::getAuthorId, userId)
            .orderByDesc(Template::getUpdatedAt);
        
        List<Template> templates = templateMapper.selectList(queryWrapper);
        return convertToVOList(templates);
    }

    /**
     * 保存模板标签
     */
    private void saveTemplateTags(Long templateId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        
        for (String tagName : tags) {
            if (StringUtils.hasText(tagName)) {
                TemplateTag tag = new TemplateTag();
                tag.setTemplateId(templateId);
                tag.setTagName(tagName.trim());
                tag.setCreateTime(LocalDateTime.now());
                templateTagMapper.insert(tag);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAdminComment(Long commentId, String sourceType, Long userId) {
        if (!UserUtils.isAdmin()) {
            throw new RuntimeException("无权限删除评论");
        }
        String normalized = sourceType == null ? "" : sourceType.toUpperCase();
        if ("NOTE".equals(normalized)) {
            NoteComment comment = noteCommentMapper.selectById(commentId);
            if (comment == null) {
                throw new RuntimeException("评论不存在");
            }
            int affected = noteCommentMapper.deleteById(commentId);
            if (affected > 0) {
                noteSquareMapper.decrementCommentCount(comment.getNoteId());
            }
            return;
        }

        TemplateComment comment = templateCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        int affected = templateCommentMapper.deleteById(commentId);
        if (affected == 0) {
            throw new RuntimeException("删除失败");
        }
    }

    @Override
    public TemplateShareVO generateShareInfo(Long templateId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        TemplateShareVO vo = new TemplateShareVO();
        vo.setShareUrl(frontendUrl.replaceAll("/+$", "") + "/templates/" + templateId);
        vo.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + URLEncoder.encode(vo.getShareUrl(), StandardCharsets.UTF_8));
        vo.setShareTitle(template.getTitle());
        vo.setShareDescription(template.getDescription());
        return vo;
    }

    @Override
    @Transactional
    public void favoriteTemplate(Long templateId, Long userId) {
        // 检查是否已收藏
        TemplateFavorite existing = templateFavoriteMapper.selectByUserIdAndTemplateId(userId, templateId);
        if (existing != null) {
            throw new RuntimeException("已收藏该模板");
        }

        TemplateFavorite favorite = new TemplateFavorite();
        favorite.setUserId(userId);
        favorite.setTemplateId(templateId);
        favorite.setCreateTime(LocalDateTime.now());
        templateFavoriteMapper.insert(favorite);
    }

    @Override
    @Transactional
    public void unfavoriteTemplate(Long templateId, Long userId) {
        LambdaQueryWrapper<TemplateFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateFavorite::getUserId, userId)
               .eq(TemplateFavorite::getTemplateId, templateId);
        templateFavoriteMapper.delete(wrapper);
    }

    @Override
    public boolean getFavoriteStatus(Long templateId, Long userId) {
        TemplateFavorite favorite = templateFavoriteMapper.selectByUserIdAndTemplateId(userId, templateId);
        return favorite != null;
    }

    @Override
    public PageResult<TemplateVO> getFavoriteTemplates(Long userId, Integer page, Integer size) {
        // 查询用户收藏的模板ID列表
        LambdaQueryWrapper<TemplateFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateFavorite::getUserId, userId)
               .orderByDesc(TemplateFavorite::getCreateTime);

        Page<TemplateFavorite> favoritePage = new Page<>(page, size);
        Page<TemplateFavorite> resultPage = templateFavoriteMapper.selectPage(favoritePage, wrapper);

        if (resultPage.getRecords().isEmpty()) {
            PageResult<TemplateVO> emptyResult = new PageResult<>();
            emptyResult.setData(Collections.emptyList());
            emptyResult.setTotal(0L);
            return emptyResult;
        }

        // 获取模板ID列表
        List<Long> templateIds = resultPage.getRecords().stream()
            .map(TemplateFavorite::getTemplateId)
            .collect(Collectors.toList());

        // 查询模板详情
        List<Template> templates = templateMapper.selectBatchIds(templateIds);
        List<TemplateVO> templateVOs = convertToVOList(templates);

        PageResult<TemplateVO> pageResult = new PageResult<>();
        pageResult.setData(templateVOs);
        pageResult.setTotal(resultPage.getTotal());
        return pageResult;
    }

    @Override
    @Transactional
    public void reportTemplate(Long templateId, TemplateReportDTO reportDTO, Long userId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        TemplateReport report = new TemplateReport();
        report.setTemplateId(templateId);
        report.setReporterId(userId);
        report.setReason(reportDTO.getReason());
        report.setDescription(reportDTO.getDescription());
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());
        templateReportMapper.insert(report);
    }

    @Override
    @Transactional
    public TemplateCommentVO createComment(Long templateId, TemplateCommentCreateDTO commentDTO, Long userId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        // 插入评论
        TemplateComment comment = new TemplateComment();
        comment.setTemplateId(templateId);
        comment.setUserId(userId);
        comment.setContent(commentDTO.getContent());
        comment.setRating(commentDTO.getRating());
        comment.setLikes(0);
        comment.setCreatedAt(LocalDateTime.now());
        templateCommentMapper.insert(comment);

        // 重新计算模板平均评分
        updateTemplateRating(templateId);

        // 返回VO
        TemplateCommentVO vo = new TemplateCommentVO();
        vo.setId(comment.getId());
        vo.setContent(comment.getContent());
        vo.setRating(comment.getRating());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setLikes(0);
        vo.setSourceType("TEMPLATE");
        vo.setTargetId(templateId);
        vo.setTargetTitle(template.getTitle());

        User user = userMapper.selectById(userId);
        if (user != null) {
            vo.setAuthor(user.getUsername());
            // 使用用户真实头像，如果没有则使用生成的头像
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                vo.setAvatar(user.getAvatarUrl());
            } else {
                vo.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getUsername());
            }
        } else {
            vo.setAuthor("匿名用户");
            vo.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=Anonymous");
        }

        return vo;
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        LambdaQueryWrapper<TemplateCommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TemplateCommentLike::getCommentId, commentId)
               .eq(TemplateCommentLike::getUserId, userId);

        TemplateCommentLike like = templateCommentLikeMapper.selectOne(wrapper);
        if (like != null) {
            templateCommentLikeMapper.deleteById(like.getId());
            // 减少评论点赞数
            templateCommentMapper.decrementLikes(commentId);
        }
    }

    /**
     * 重新计算模板平均评分
     */
    private void updateTemplateRating(Long templateId) {
        List<TemplateComment> comments = templateCommentMapper.selectByTemplateId(templateId);
        if (comments.isEmpty()) {
            return;
        }

        double avgRating = comments.stream()
            .mapToInt(TemplateComment::getRating)
            .average()
            .orElse(0.0);

        Template template = new Template();
        template.setId(templateId);
        template.setRating(BigDecimal.valueOf(avgRating).setScale(2, java.math.RoundingMode.HALF_UP));
        templateMapper.updateById(template);
    }
}
