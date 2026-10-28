package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.Trash;
import com.ldd.initialization.dto.TrashListDTO;
import com.ldd.initialization.mapper.TrashMapper;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.service.FileInfoService;
import com.ldd.initialization.service.NoteService;
import com.ldd.initialization.service.TrashService;
import com.ldd.initialization.vo.TrashItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 回收站Service实现类
 */
@Service
@Slf4j
public class TrashServiceImpl extends ServiceImpl<TrashMapper, Trash> implements TrashService {

    @Autowired
    @Lazy
    private NoteService noteService;

    @Autowired
    private FileInfoService fileInfoService;

    private static final int TRASH_RETENTION_DAYS = 30;

    @Override
    public IPage<TrashItemVO> getTrashList(TrashListDTO listDTO, Long userId) {
        Page<Trash> page = new Page<>(listDTO.getPage(), listDTO.getSize());
        IPage<Trash> trashPage = baseMapper.selectTrashListByPage(page, userId, listDTO.getTrashType(), listDTO.getKeyword());
        
        return trashPage.convert(this::convertToVO);
    }

    @Override
    @Transactional
    public void moveNoteToTrash(Note note, Long userId) {
        Trash trash = new Trash();
        trash.setTrashType(Trash.TYPE_NOTE);
        trash.setSourceId(note.getId());
        trash.setUserId(userId);
        trash.setDeleteTime(LocalDateTime.now());
        trash.setRestoreDeadline(LocalDateTime.now().plusDays(TRASH_RETENTION_DAYS));
        
        // 复制笔记字段
        trash.setNoteTitle(note.getTitle());
        trash.setNoteContentMd(note.getContentMd());
        trash.setNotebookId(note.getNotebookId());
        trash.setNoteIsPinned(note.getIsPinned());
        trash.setNoteStatus(note.getStatus());
        trash.setNoteViewCount(note.getViewCount());
        trash.setNoteWordCount(note.getWordCount());
        trash.setNoteCreateTime(note.getCreateTime());
        trash.setNoteUpdateTime(note.getUpdateTime());
        
        this.save(trash);
        log.info("笔记 {} 已移入回收站，回收站ID: {}", note.getId(), trash.getId());
    }

    @Override
    @Transactional
    public void moveFileToTrash(FileUpInfo fileInfo, Long userId) {
        Trash trash = new Trash();
        trash.setTrashType(Trash.TYPE_FILE);
        trash.setSourceId(fileInfo.getId());
        trash.setUserId(userId);
        trash.setDeleteTime(LocalDateTime.now());
        trash.setRestoreDeadline(LocalDateTime.now().plusDays(TRASH_RETENTION_DAYS));
        
        // 复制文件字段
        trash.setFileOriginalFilename(fileInfo.getOriginalFilename());
        trash.setFileName(fileInfo.getFileName());
        trash.setFileExtension(fileInfo.getFileExtension());
        trash.setFileSize(fileInfo.getFileSize());
        trash.setFilePath(fileInfo.getFilePath());
        trash.setFileUrl(fileInfo.getFileUrl());
        trash.setFileMd5(fileInfo.getFileMd5());
        trash.setFileType(fileInfo.getFileType());
        trash.setMimeType(fileInfo.getMimeType());
        trash.setFileHash(fileInfo.getFileHash());
        trash.setSourceType(fileInfo.getSourceType());
        trash.setSourceNoteId(fileInfo.getSourceNoteId());
        trash.setSourceNoteTitle(fileInfo.getSourceNoteTitle());
        trash.setFileCreateTime(fileInfo.getCreateTime());
        trash.setFileUpdateTime(fileInfo.getUpdateTime());
        
        this.save(trash);
        log.info("文件 {} 已移入回收站，回收站ID: {}", fileInfo.getId(), trash.getId());
    }

    @Override
    @Transactional
    public Long restoreNote(Long trashId, Long userId) {
        Trash trash = getTrashRecord(trashId, userId);
        
        if (trash.getTrashType() != Trash.TYPE_NOTE) {
            throw new BizException(BizResponseCode.ERROR_1, "该记录不是笔记类型");
        }
        
        // 恢复笔记
        Note note = new Note();
        note.setTitle(trash.getNoteTitle());
        note.setContentMd(trash.getNoteContentMd());
        note.setNotebookId(trash.getNotebookId());
        note.setUserId(userId);
        note.setIsPinned(trash.getNoteIsPinned());
        note.setStatus(trash.getNoteStatus());
        note.setViewCount(trash.getNoteViewCount());
        note.setWordCount(trash.getNoteWordCount());
        note.setCreateTime(trash.getNoteCreateTime());
        note.setUpdateTime(LocalDateTime.now());
        
        noteService.save(note);
        
        // 删除回收站记录
        this.removeById(trashId);
        
        log.info("笔记已从回收站恢复，新笔记ID: {}", note.getId());
        return note.getId();
    }

    @Override
    @Transactional
    public Long restoreFile(Long trashId, Long userId) {
        Trash trash = getTrashRecord(trashId, userId);
        
        if (trash.getTrashType() != Trash.TYPE_FILE) {
            throw new BizException(BizResponseCode.ERROR_1, "该记录不是文件类型");
        }
        
        // 恢复文件
        FileUpInfo fileInfo = new FileUpInfo();
        fileInfo.setOriginalFilename(trash.getFileOriginalFilename());
        fileInfo.setFileName(trash.getFileName());
        fileInfo.setFileExtension(trash.getFileExtension());
        fileInfo.setFileSize(trash.getFileSize());
        fileInfo.setFilePath(trash.getFilePath());
        fileInfo.setFileUrl(trash.getFileUrl());
        fileInfo.setFileMd5(trash.getFileMd5());
        fileInfo.setFileType(trash.getFileType());
        fileInfo.setMimeType(trash.getMimeType());
        fileInfo.setFileHash(trash.getFileHash());
        fileInfo.setSourceType(trash.getSourceType());
        fileInfo.setSourceNoteId(trash.getSourceNoteId());
        fileInfo.setSourceNoteTitle(trash.getSourceNoteTitle());
        fileInfo.setUserId(userId);
        fileInfo.setCreateTime(trash.getFileCreateTime());
        fileInfo.setUpdateTime(LocalDateTime.now());
        
        fileInfoService.save(fileInfo);
        
        // 删除回收站记录
        this.removeById(trashId);
        
        log.info("文件已从回收站恢复，新文件ID: {}", fileInfo.getId());
        return fileInfo.getId();
    }

    @Override
    @Transactional
    public Long restoreItem(Long trashId, Long userId) {
        Trash trash = getTrashRecord(trashId, userId);
        
        if (trash.getTrashType() == Trash.TYPE_NOTE) {
            return restoreNote(trashId, userId);
        } else if (trash.getTrashType() == Trash.TYPE_FILE) {
            return restoreFile(trashId, userId);
        } else {
            throw new BizException(BizResponseCode.ERROR_1, "未知的回收站类型");
        }
    }

    @Override
    @Transactional
    public void permanentDelete(Long trashId, Long userId) {
        Trash trash = getTrashRecord(trashId, userId);
        
        // 如果是文件类型，可能需要删除实际文件（这里仅删除数据库记录）
        // 实际文件删除可根据业务需求实现
        
        this.removeById(trashId);
        log.info("回收站记录 {} 已彻底删除", trashId);
    }

    @Override
    @Transactional
    public void batchPermanentDelete(Long[] trashIds, Long userId) {
        for (Long trashId : trashIds) {
            permanentDelete(trashId, userId);
        }
    }

    @Override
    @Transactional
    public void emptyTrash(Long userId) {
        int deleted = baseMapper.deleteAllByUserId(userId);
        log.info("用户 {} 的回收站已清空，删除 {} 条记录", userId, deleted);
    }

    @Override
    @Transactional
    public int cleanExpiredTrash() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = baseMapper.deleteExpiredTrash(now);
        if (deleted > 0) {
            log.info("自动清理了 {} 条过期的回收站记录", deleted);
        }
        return deleted;
    }

    @Override
    public Long countTrash(Long userId, Integer trashType) {
        return baseMapper.countByUserId(userId, trashType);
    }

    /**
     * 获取并验证回收站记录
     */
    private Trash getTrashRecord(Long trashId, Long userId) {
        Trash trash = baseMapper.selectByIdAndUserId(trashId, userId);
        if (trash == null) {
            throw new BizException(BizResponseCode.ERROR_1, "回收站记录不存在或无权限");
        }
        return trash;
    }

    /**
     * 将Trash实体转换为TrashItemVO
     */
    private TrashItemVO convertToVO(Trash trash) {
        TrashItemVO vo = new TrashItemVO();
        vo.setId(trash.getId());
        vo.setTrashType(trash.getTrashType());
        vo.setTypeName(trash.getTrashType() == Trash.TYPE_NOTE ? "笔记" : "文件");
        vo.setSourceId(trash.getSourceId());
        vo.setDeleteTime(trash.getDeleteTime());
        vo.setRestoreDeadline(trash.getRestoreDeadline());
        vo.setUserId(trash.getUserId());
        
        // 计算剩余天数
        if (trash.getRestoreDeadline() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), trash.getRestoreDeadline());
            vo.setDaysLeft((int) Math.max(0, daysLeft));
        } else {
            vo.setDaysLeft(0);
        }
        
        if (trash.getTrashType() == Trash.TYPE_NOTE) {
            // 笔记类型
            vo.setTitle(trash.getNoteTitle());
            vo.setNotebookId(trash.getNotebookId());
            vo.setOriginalCreateTime(trash.getNoteCreateTime());
            // 生成摘要
            if (StringUtils.hasText(trash.getNoteContentMd())) {
                String content = trash.getNoteContentMd();
                vo.setSummary(content.length() > 100 ? content.substring(0, 100) + "..." : content);
            }
        } else {
            // 文件类型
            vo.setTitle(trash.getFileOriginalFilename());
            vo.setFileExtension(trash.getFileExtension());
            vo.setFileSize(trash.getFileSize());
            vo.setFileSizeFormatted(formatFileSize(trash.getFileSize()));
            vo.setFileType(trash.getFileType());
            vo.setOriginalCreateTime(trash.getFileCreateTime());
        }
        
        return vo;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "0 B";
        }
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        
        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
