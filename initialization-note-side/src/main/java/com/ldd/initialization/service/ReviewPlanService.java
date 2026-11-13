package com.ldd.initialization.service;

import com.ldd.initialization.domain.ReviewPlan;
import com.ldd.initialization.dto.ReviewPlanCreateDTO;
import com.ldd.initialization.dto.ReviewPlanUpdateDTO;
import com.ldd.initialization.vo.ReviewPlanVO;

import java.util.List;

public interface ReviewPlanService {
    List<ReviewPlanVO> list(Long userId);
    ReviewPlanVO create(ReviewPlanCreateDTO dto, Long userId);
    ReviewPlanVO update(Long id, ReviewPlanUpdateDTO dto, Long userId);
    void delete(Long id, Long userId);
    ReviewPlanVO toVO(ReviewPlan plan);
    ReviewPlanVO saveGenerated(ReviewPlan plan);
    List<ReviewPlanVO> listGeneratedByNote(Long noteId, Long userId);
    void deleteGeneratedByNote(Long noteId, Long userId);
}
