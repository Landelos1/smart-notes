package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.NoteComment;
import com.ldd.initialization.domain.NoteFavorite;
import com.ldd.initialization.domain.NoteLike;
import com.ldd.initialization.domain.NoteSquare;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.dto.NoteCommentCreateDTO;
import com.ldd.initialization.dto.NoteSquarePublishDTO;
import com.ldd.initialization.dto.NoteSquareSearchDTO;
import com.ldd.initialization.mapper.NoteCommentMapper;
import com.ldd.initialization.mapper.NoteFavoriteMapper;
import com.ldd.initialization.mapper.NoteLikeMapper;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.NoteSquareMapper;
import com.ldd.initialization.mapper.NoteTagMapper;
import com.ldd.initialization.mapper.UserMapper;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.service.NoteSquareService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.NoteCommentVO;
import com.ldd.initialization.vo.NoteSquareDetailVO;
import com.ldd.initialization.vo.NoteSquareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Note square service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteSquareServiceImpl implements NoteSquareService {

    private static final int SUMMARY_LIMIT = 200;
    private static final Pattern HTML_IMAGE_PATTERN = Pattern.compile("<img[^>]+src=['\"]([^'\"]+)['\"]|<img[^>]+src=([^\\s>]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*\\]\\(([^)]+?)(?:\\s+['\"][^'\"]*['\"])?\\)");

    private final NoteSquareMapper noteSquareMapper;
    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public NoteSquareDetailVO publishNote(NoteSquarePublishDTO publishDTO, Long userId) {
        Note note = noteMapper.selectById(publishDTO.getNoteId());
        if (note == null) {
            throw new BizException(BizResponseCode.ERROR_1, "Note not found");
        }
        if (!UserUtils.isAdmin() && !note.getUserId().equals(userId)) {
            throw new BizException(BizResponseCode.ERR_11004, "No permission to publish this note");
        }

        LocalDateTime now = LocalDateTime.now();
        NoteSquare existing = noteSquareMapper.selectByNoteId(note.getId());

        String summary = StringUtils.hasText(publishDTO.getSummary())
                ? publishDTO.getSummary()
                : (existing != null ? existing.getSummary() : null);
        if (!StringUtils.hasText(summary)) {
            summary = buildSummary(note.getContentMd());
        }

        String coverUrl = StringUtils.hasText(publishDTO.getCoverUrl())
                ? publishDTO.getCoverUrl()
                : (existing != null ? existing.getCoverUrl() : null);
        if (!StringUtils.hasText(coverUrl)) {
            coverUrl = extractFirstImageUrl(note.getContentMd());
        }

        if (existing == null) {
            NoteSquare noteSquare = new NoteSquare();
            noteSquare.setNoteId(note.getId());
            noteSquare.setUserId(note.getUserId());
            noteSquare.setStatus(1);
            noteSquare.setRecommendScore(0);
            noteSquare.setIsPinned(false);
            noteSquare.setSummary(summary);
            noteSquare.setCoverUrl(coverUrl);
            noteSquare.setViewCount(0);
            noteSquare.setLikeCount(0);
            noteSquare.setFavoriteCount(0);
            noteSquare.setCommentCount(0);
            noteSquare.setPublishTime(now);
            noteSquare.setUpdateTime(now);
            noteSquareMapper.insert(noteSquare);
        } else {
            existing.setStatus(1);
            existing.setSummary(summary);
            existing.setCoverUrl(coverUrl);
            existing.setPublishTime(now);
            existing.setUpdateTime(now);
            noteSquareMapper.updateById(existing);
        }

        return getNoteDetail(note.getId(), userId);
    }

    @Override
    @Transactional
    public void unpublishNote(Long noteId, Long userId) {
        NoteSquare noteSquare = noteSquareMapper.selectByNoteId(noteId);
        if (noteSquare == null) {
            throw new BizException(BizResponseCode.ERROR_1, "Note is not published");
        }
        if (!UserUtils.isAdmin() && !noteSquare.getUserId().equals(userId)) {
            throw new BizException(BizResponseCode.ERR_11004, "No permission to unpublish this note");
        }
        noteSquare.setStatus(0);
        noteSquare.setUpdateTime(LocalDateTime.now());
        noteSquareMapper.updateById(noteSquare);
    }

    @Override
    public PageResult<NoteSquareVO> getSquareNotes(NoteSquareSearchDTO searchDTO, Long userId) {
        Page<NoteSquareVO> page = new Page<>(searchDTO.getPage(), searchDTO.getSize());
        List<String> keywords = splitKeywords(searchDTO.getKeyword());
        IPage<NoteSquareVO> result = noteSquareMapper.selectSquareList(
                page,
                searchDTO.getKeyword(),
                keywords,
                normalizeSortBy(searchDTO.getSortBy()),
                normalizeSortOrder(searchDTO.getSortOrder()),
                userId
        );

        normalizePreviewList(result.getRecords());
        attachTags(result.getRecords());
        return PageResult.convert(result);
    }

    @Override
    public PageResult<NoteSquareVO> getFavoriteNotes(NoteSquareSearchDTO searchDTO, Long userId) {
        Page<NoteSquareVO> page = new Page<>(searchDTO.getPage(), searchDTO.getSize());
        List<String> keywords = splitKeywords(searchDTO.getKeyword());
        IPage<NoteSquareVO> result = noteSquareMapper.selectFavoriteList(page, searchDTO.getKeyword(), keywords, userId);
        normalizePreviewList(result.getRecords());
        attachTags(result.getRecords());
        return PageResult.convert(result);
    }

    @Override
    public PageResult<NoteSquareVO> getLikedNotes(NoteSquareSearchDTO searchDTO, Long userId) {
        Page<NoteSquareVO> page = new Page<>(searchDTO.getPage(), searchDTO.getSize());
        List<String> keywords = splitKeywords(searchDTO.getKeyword());
        IPage<NoteSquareVO> result = noteSquareMapper.selectLikeList(page, searchDTO.getKeyword(), keywords, userId);
        normalizePreviewList(result.getRecords());
        attachTags(result.getRecords());
        return PageResult.convert(result);
    }

    @Override
    public PageResult<NoteSquareVO> getMyNotes(NoteSquareSearchDTO searchDTO, Long userId) {
        Page<NoteSquareVO> page = new Page<>(searchDTO.getPage(), searchDTO.getSize());
        List<String> keywords = splitKeywords(searchDTO.getKeyword());
        IPage<NoteSquareVO> result = noteSquareMapper.selectMyNotes(page, searchDTO.getKeyword(), keywords, userId);
        normalizePreviewList(result.getRecords());
        attachTags(result.getRecords());
        return PageResult.convert(result);
    }

    @Override
    public NoteSquareDetailVO getNoteDetail(Long noteId, Long userId) {
        NoteSquareDetailVO detail = noteSquareMapper.selectDetail(noteId, userId);
        if (detail == null) {
            throw new BizException(BizResponseCode.ERROR_1, "Note not found");
        }

        boolean isOwner = detail.getAuthorId() != null && detail.getAuthorId().equals(userId);
        boolean isAdmin = UserUtils.isAdmin();

        Integer squareStatus = detail.getSquareStatus();
        if (squareStatus == null) {
            squareStatus = 0;
            detail.setSquareStatus(0);
        }

        if (squareStatus != 1 && !isOwner && !isAdmin) {
            throw new BizException(BizResponseCode.ERR_11004, "Note is not published");
        }

        if (squareStatus == 1) {
            noteSquareMapper.incrementViewCount(noteId);
            noteMapper.incrementViewCount(noteId);
        }

        if (detail.getIsLiked() == null) {
            detail.setIsLiked(false);
        }
        if (detail.getIsFavorited() == null) {
            detail.setIsFavorited(false);
        }
        if (!StringUtils.hasText(detail.getSummary())) {
            detail.setSummary(buildSummary(detail.getContent()));
        }
        detail.setTags(noteTagMapper.selectTagsByNoteId(noteId));
        return detail;
    }

    @Override
    @Transactional
    public void likeNote(Long noteId, Long userId) {
        ensurePublished(noteId);
        NoteLike existing = noteLikeMapper.selectByUserIdAndNoteId(userId, noteId);
        if (existing != null) {
            return;
        }
        NoteLike like = new NoteLike();
        like.setNoteId(noteId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        noteLikeMapper.insert(like);
        noteSquareMapper.incrementLikeCount(noteId);
    }

    @Override
    @Transactional
    public void unlikeNote(Long noteId, Long userId) {
        NoteLike existing = noteLikeMapper.selectByUserIdAndNoteId(userId, noteId);
        if (existing == null) {
            return;
        }
        noteLikeMapper.deleteById(existing.getId());
        noteSquareMapper.decrementLikeCount(noteId);
    }

    @Override
    @Transactional
    public void favoriteNote(Long noteId, Long userId) {
        ensurePublished(noteId);
        NoteFavorite existing = noteFavoriteMapper.selectByUserIdAndNoteId(userId, noteId);
        if (existing != null) {
            return;
        }
        NoteFavorite favorite = new NoteFavorite();
        favorite.setNoteId(noteId);
        favorite.setUserId(userId);
        favorite.setCreateTime(LocalDateTime.now());
        noteFavoriteMapper.insert(favorite);
        noteSquareMapper.incrementFavoriteCount(noteId);
    }

    @Override
    @Transactional
    public void unfavoriteNote(Long noteId, Long userId) {
        NoteFavorite existing = noteFavoriteMapper.selectByUserIdAndNoteId(userId, noteId);
        if (existing == null) {
            return;
        }
        noteFavoriteMapper.deleteById(existing.getId());
        noteSquareMapper.decrementFavoriteCount(noteId);
    }

    @Override
    public List<NoteCommentVO> getNoteComments(Long noteId, Long userId) {
        ensurePublished(noteId);
        List<NoteCommentVO> comments = noteCommentMapper.selectByNoteId(noteId);
        return buildCommentTree(comments, userId);
    }

    @Override
    @Transactional
    public NoteCommentVO createNoteComment(Long noteId, NoteCommentCreateDTO commentDTO, Long userId) {
        ensurePublished(noteId);

        NoteComment parent = null;
        if (commentDTO.getParentId() != null) {
            parent = noteCommentMapper.selectById(commentDTO.getParentId());
            if (parent == null) {
                throw new BizException(BizResponseCode.ERROR_1, "Parent comment not found");
            }
            if (!noteId.equals(parent.getNoteId())) {
                throw new BizException(BizResponseCode.ERROR_1, "Invalid parent comment");
            }
        }

        Long replyToUserId = commentDTO.getReplyToUserId();
        if (replyToUserId == null && parent != null) {
            replyToUserId = parent.getUserId();
        }

        NoteComment comment = new NoteComment();
        comment.setNoteId(noteId);
        comment.setUserId(userId);
        comment.setParentId(commentDTO.getParentId());
        comment.setReplyToUserId(replyToUserId);
        comment.setContent(commentDTO.getContent().trim());
        comment.setLikes(0);
        noteCommentMapper.insert(comment);
        noteSquareMapper.incrementCommentCount(noteId);

        return buildCommentVO(comment, userId);
    }

    @Override
    @Transactional
    public void deleteNoteComment(Long commentId, Long userId) {
        NoteComment comment = noteCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(BizResponseCode.ERROR_1, "Comment not found");
        }
        if (!UserUtils.isAdmin() && !comment.getUserId().equals(userId)) {
            throw new BizException(BizResponseCode.ERR_11004, "No permission to delete this comment");
        }
        int affected = noteCommentMapper.deleteById(commentId);
        if (affected > 0) {
            noteSquareMapper.decrementCommentCount(comment.getNoteId());
        }
    }

    private void ensurePublished(Long noteId) {
        NoteSquare noteSquare = noteSquareMapper.selectByNoteId(noteId);
        if (noteSquare == null || noteSquare.getStatus() == null || noteSquare.getStatus() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "Note is not published");
        }
    }

    private void normalizePreviewList(List<NoteSquareVO> notes) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        for (NoteSquareVO note : notes) {
            if (note.getIsLiked() == null) {
                note.setIsLiked(false);
            }
            if (note.getIsFavorited() == null) {
                note.setIsFavorited(false);
            }
            if (note.getSquareStatus() == null) {
                note.setSquareStatus(0);
            }
            if (!StringUtils.hasText(note.getCoverUrl())) {
                note.setCoverUrl(extractFirstImageUrl(note.getContent()));
            }
            String summary = note.getSummary();
            if (!StringUtils.hasText(summary)) {
                summary = buildSummary(note.getContent());
            }
            note.setSummary(summary);
            note.setContent(summary);
        }
    }

    private void attachTags(List<NoteSquareVO> notes) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        for (NoteSquareVO note : notes) {
            note.setTags(noteTagMapper.selectTagsByNoteId(note.getId()));
        }
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() > SUMMARY_LIMIT) {
            return normalized.substring(0, SUMMARY_LIMIT) + "...";
        }
        return normalized;
    }

    private String extractFirstImageUrl(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Matcher htmlMatch = HTML_IMAGE_PATTERN.matcher(content);
        if (htmlMatch.find()) {
            String url = htmlMatch.group(1);
            if (url == null) {
                url = htmlMatch.group(2);
            }
            if (StringUtils.hasText(url)) {
                return url.trim();
            }
        }
        Matcher mdMatch = MARKDOWN_IMAGE_PATTERN.matcher(content);
        if (mdMatch.find()) {
            String url = mdMatch.group(1);
            if (StringUtils.hasText(url)) {
                return url.trim();
            }
        }
        return null;
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "recommend";
        }
        return sortBy;
    }

    private String normalizeSortOrder(String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return "desc";
        }
        return sortOrder;
    }

    private List<String> splitKeywords(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String normalized = keyword.trim();
        String[] parts = normalized.split("[\\s,，]+");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                keywords.add(part.trim());
            }
        }
        return keywords;
    }

    private List<NoteCommentVO> buildCommentTree(List<NoteCommentVO> comments, Long userId) {
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, NoteCommentVO> commentMap = new LinkedHashMap<>();
        for (NoteCommentVO comment : comments) {
            if (comment.getLikes() == null) {
                comment.setLikes(0);
            }
            comment.setIsOwner(comment.getUserId() != null && comment.getUserId().equals(userId));
            normalizeAvatar(comment);
            commentMap.put(comment.getId(), comment);
        }

        List<NoteCommentVO> roots = new ArrayList<>();
        for (NoteCommentVO comment : commentMap.values()) {
            Long parentId = comment.getParentId();
            if (parentId != null && commentMap.containsKey(parentId)) {
                NoteCommentVO parent = commentMap.get(parentId);
                if (parent.getReplies() == null) {
                    parent.setReplies(new ArrayList<>());
                }
                parent.getReplies().add(comment);
            } else {
                roots.add(comment);
            }
        }
        return roots;
    }

    private NoteCommentVO buildCommentVO(NoteComment comment, Long userId) {
        NoteCommentVO vo = new NoteCommentVO();
        vo.setId(comment.getId());
        vo.setNoteId(comment.getNoteId());
        vo.setUserId(comment.getUserId());
        vo.setParentId(comment.getParentId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setContent(comment.getContent());
        vo.setLikes(comment.getLikes());
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        vo.setIsOwner(comment.getUserId() != null && comment.getUserId().equals(userId));

        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setAuthorName(user.getUsername());
            vo.setAuthorAvatar(resolveAvatar(user.getAvatarUrl(), user.getUsername()));
        } else {
            vo.setAuthorName("匿名用户");
            vo.setAuthorAvatar(resolveAvatar(null, "Anonymous"));
        }

        if (comment.getReplyToUserId() != null) {
            User replyUser = userMapper.selectById(comment.getReplyToUserId());
            if (replyUser != null) {
                vo.setReplyToUserName(replyUser.getUsername());
                vo.setReplyToUserAvatar(resolveAvatar(replyUser.getAvatarUrl(), replyUser.getUsername()));
            }
        }
        return vo;
    }

    private void normalizeAvatar(NoteCommentVO comment) {
        comment.setAuthorAvatar(resolveAvatar(comment.getAuthorAvatar(), comment.getAuthorName()));
        if (comment.getReplyToUserId() != null) {
            comment.setReplyToUserAvatar(resolveAvatar(comment.getReplyToUserAvatar(), comment.getReplyToUserName()));
        }
    }

    private String resolveAvatar(String avatarUrl, String username) {
        if (StringUtils.hasText(avatarUrl)) {
            return avatarUrl;
        }
        if (!StringUtils.hasText(username)) {
            return "https://api.dicebear.com/7.x/avataaars/svg?seed=Anonymous";
        }
        return "https://api.dicebear.com/7.x/avataaars/svg?seed=" + username;
    }
}
