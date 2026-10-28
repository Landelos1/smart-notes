package com.ldd.initialization.service;

import com.ldd.initialization.dto.TemplateCommentCreateDTO;
import com.ldd.initialization.dto.TemplateCreateDTO;
import com.ldd.initialization.dto.TemplateReportDTO;
import com.ldd.initialization.dto.TemplateSearchDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.vo.TemplateCommentVO;
import com.ldd.initialization.vo.TemplateShareVO;
import com.ldd.initialization.vo.TemplateVO;

import java.util.List;

/**
 * 模板服务接口
 */
public interface TemplateService {

    /**
     * 搜索模板列表
     * @param searchDTO 搜索条件
     * @return 分页结果
     */
    PageResult<TemplateVO> searchTemplates(TemplateSearchDTO searchDTO);

    /**
     * 获取模板详情
     * @param id 模板ID
     * @return 模板详情
     */
    TemplateVO getTemplateById(Long id);

    /**
     * 获取相关模板
     * @param templateId 当前模板ID
     * @param limit 限制数量
     * @return 相关模板列表
     */
    List<TemplateVO> getRelatedTemplates(Long templateId, Integer limit);

    /**
     * 获取模板评论列表
     * @param templateId 模板ID
     * @return 评论列表
     */
    List<TemplateCommentVO> getTemplateComments(Long templateId);

    /**
     * 管理员获取评论列表（模板/笔记广场）
     *
     * @param current 当前页
     * @param size 每页数量
     * @param sourceType 评论来源类型（TEMPLATE/NOTE/ALL）
     * @param targetId 目标ID（模板ID/笔记ID）
     * @param keyword 关键词
     * @return 评论分页列表
     */
    PageResult<TemplateCommentVO> listAdminComments(Integer current,
                                                    Integer size,
                                                    String sourceType,
                                                    Long targetId,
                                                    String keyword);

    /**
     * 使用模板（增加下载次数）
     * @param templateId 模板ID
     */
    void useTemplate(Long templateId);

    /**
     * 点赞评论
     * @param commentId 评论ID
     */
    void likeComment(Long commentId);

    /**
     * 创建模板
     * @param createDTO 创建模板请求
     * @param userId 用户ID
     * @return 创建后的模板
     */
    TemplateVO createTemplate(TemplateCreateDTO createDTO, Long userId);

    /**
     * 更新模板
     * @param createDTO 更新模板请求
     * @param userId 用户ID
     * @return 更新后的模板
     */
    TemplateVO updateTemplate(TemplateCreateDTO createDTO, Long userId);

    /**
     * 删除模板
     * @param templateId 模板ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteTemplate(Long templateId, Long userId);

    /**
     * 获取用户创建的模板列表
     * @param userId 用户ID
     * @return 模板列表
     */
    List<TemplateVO> getMyTemplates(Long userId);

    /**
     * 生成模板分享信息
     * @param templateId 模板ID
     * @return 分享信息
     */
    TemplateShareVO generateShareInfo(Long templateId);

    /**
     * 收藏模板
     * @param templateId 模板ID
     * @param userId 用户ID
     */
    void favoriteTemplate(Long templateId, Long userId);

    /**
     * 取消收藏模板
     * @param templateId 模板ID
     * @param userId 用户ID
     */
    void unfavoriteTemplate(Long templateId, Long userId);

    /**
     * 查询收藏状态
     * @param templateId 模板ID
     * @param userId 用户ID
     * @return 是否已收藏
     */
    boolean getFavoriteStatus(Long templateId, Long userId);

    /**
     * 获取用户收藏的模板列表
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页数量
     * @return 收藏的模板列表
     */
    PageResult<TemplateVO> getFavoriteTemplates(Long userId, Integer page, Integer size);

    /**
     * 举报模板
     * @param templateId 模板ID
     * @param reportDTO 举报信息
     * @param userId 用户ID
     */
    void reportTemplate(Long templateId, TemplateReportDTO reportDTO, Long userId);

    /**
     * 发表评论
     * @param templateId 模板ID
     * @param commentDTO 评论信息
     * @param userId 用户ID
     * @return 评论详情
     */
    TemplateCommentVO createComment(Long templateId, TemplateCommentCreateDTO commentDTO, Long userId);

    /**
     * 取消点赞评论
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    void unlikeComment(Long commentId, Long userId);

    /**
     * 管理员删除评论
     *
     * @param commentId 评论ID
     * @param sourceType 评论来源类型（TEMPLATE/NOTE）
     * @param userId 操作人
     */
    void deleteAdminComment(Long commentId, String sourceType, Long userId);
}
