package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.Task;
import com.ldd.initialization.dto.TaskCreateDTO;
import com.ldd.initialization.dto.TaskUpdateDTO;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.TaskMapper;
import com.ldd.initialization.service.TaskService;
import com.ldd.initialization.vo.TaskVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {
    private final NoteMapper noteMapper;

    public TaskServiceImpl(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    @Override
    public List<TaskVO> list(Long userId) {
        return baseMapper.selectByUserId(userId).stream().map(this::toVO).toList();
    }

    @Override
    public TaskVO create(TaskCreateDTO dto, Long userId) {
        validateTitle(dto.getTitle());
        validateNote(dto.getNoteId(), userId);
        Task task = new Task();
        BeanUtils.copyProperties(dto, task);
        task.setUserId(userId);
        task.setStatus(0);
        task.setPriority(dto.getPriority() == null ? 2 : Math.max(1, Math.min(3, dto.getPriority())));
        task.setSourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : "manual");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        save(task);
        return toVO(task);
    }

    @Override
    public TaskVO update(Long id, TaskUpdateDTO dto, Long userId) {
        validateTitle(dto.getTitle());
        Task task = owned(id, userId);
        task.setTitle(dto.getTitle().trim());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        task.setDueTime(dto.getDueTime());
        if (dto.getPriority() != null) task.setPriority(Math.max(1, Math.min(3, dto.getPriority())));
        if (dto.getStatus() != null) {
            task.setStatus(Math.max(0, Math.min(2, dto.getStatus())));
            task.setCompletedTime(task.getStatus() == 2 ? LocalDateTime.now() : null);
        }
        task.setUpdateTime(LocalDateTime.now());
        updateById(task);
        return toVO(task);
    }

    @Override
    public void delete(Long id, Long userId) {
        removeById(owned(id, userId).getId());
    }

    @Override
    public TaskVO toVO(Task task) {
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    @Override
    public TaskVO saveGenerated(Task task) {
        save(task);
        return toVO(task);
    }

    @Override
    public List<TaskVO> listGeneratedByNote(Long noteId, Long userId) {
        return baseMapper.selectGeneratedByNote(noteId, userId).stream().map(this::toVO).toList();
    }

    @Override
    public void deleteGeneratedByNote(Long noteId, Long userId) {
        baseMapper.deleteGeneratedByNote(noteId, userId);
    }

    private void validateTitle(String title) {
        if (!StringUtils.hasText(title)) throw new BizException("任务标题不能为空");
        if (title.length() > 255) throw new BizException("任务标题不能超过255个字符");
    }

    private void validateNote(Long noteId, Long userId) {
        if (noteId == null) return;
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) throw new BizException("关联笔记不存在或无权限");
    }

    private Task owned(Long id, Long userId) {
        Task task = baseMapper.selectByIdAndUserId(id, userId);
        if (task == null) throw new BizException("任务不存在或无权限");
        return task;
    }
}
