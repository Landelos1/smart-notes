package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.ReviewPlan;
import com.ldd.initialization.dto.ReviewPlanCreateDTO;
import com.ldd.initialization.dto.ReviewPlanUpdateDTO;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.ReviewPlanMapper;
import com.ldd.initialization.service.ReviewPlanService;
import com.ldd.initialization.vo.ReviewPlanVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewPlanServiceImpl extends ServiceImpl<ReviewPlanMapper, ReviewPlan> implements ReviewPlanService {
    private final NoteMapper noteMapper;

    public ReviewPlanServiceImpl(NoteMapper noteMapper) {
        this.noteMapper = noteMapper;
    }

    @Override
    public List<ReviewPlanVO> list(Long userId) {
        return baseMapper.selectByUserId(userId).stream().map(this::toVO).toList();
    }

    @Override
    public ReviewPlanVO create(ReviewPlanCreateDTO dto, Long userId) {
        validateNote(dto.getNoteId(), userId);
        ReviewPlan plan = new ReviewPlan();
        BeanUtils.copyProperties(dto, plan);
        plan.setUserId(userId);
        plan.setFrequency(StringUtils.hasText(dto.getFrequency()) ? dto.getFrequency() : "weekly");
        plan.setStartDate(dto.getStartDate() == null ? LocalDate.now() : dto.getStartDate());
        plan.setEndDate(dto.getEndDate() == null ? plan.getStartDate().plusDays(28) : dto.getEndDate());
        if (plan.getEndDate().isBefore(plan.getStartDate())) throw new BizException("结束日期不能早于开始日期");
        plan.setStatus(0);
        plan.setSourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : "manual");
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        save(plan);
        return toVO(plan);
    }

    @Override
    public ReviewPlanVO update(Long id, ReviewPlanUpdateDTO dto, Long userId) {
        ReviewPlan plan = owned(id, userId);
        BeanUtils.copyProperties(dto, plan);
        if (plan.getStartDate() != null && plan.getEndDate() != null && plan.getEndDate().isBefore(plan.getStartDate())) {
            throw new BizException("结束日期不能早于开始日期");
        }
        if (dto.getStatus() != null) plan.setStatus(Math.max(0, Math.min(2, dto.getStatus())));
        plan.setUpdateTime(LocalDateTime.now());
        updateById(plan);
        return toVO(plan);
    }

    @Override
    public void delete(Long id, Long userId) {
        removeById(owned(id, userId).getId());
    }

    @Override
    public ReviewPlanVO toVO(ReviewPlan plan) {
        ReviewPlanVO vo = new ReviewPlanVO();
        BeanUtils.copyProperties(plan, vo);
        return vo;
    }

    @Override
    public ReviewPlanVO saveGenerated(ReviewPlan plan) {
        save(plan);
        return toVO(plan);
    }

    @Override
    public List<ReviewPlanVO> listGeneratedByNote(Long noteId, Long userId) {
        return baseMapper.selectGeneratedByNote(noteId, userId).stream().map(this::toVO).toList();
    }

    @Override
    public void deleteGeneratedByNote(Long noteId, Long userId) {
        baseMapper.deleteGeneratedByNote(noteId, userId);
    }

    private void validateNote(Long noteId, Long userId) {
        if (noteId == null) return;
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) throw new BizException("关联笔记不存在或无权限");
    }

    private ReviewPlan owned(Long id, Long userId) {
        ReviewPlan plan = baseMapper.selectByIdAndUserId(id, userId);
        if (plan == null) throw new BizException("复习计划不存在或无权限");
        return plan;
    }
}
