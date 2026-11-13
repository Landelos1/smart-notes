package com.ldd.initialization.service;

import com.ldd.initialization.vo.LearningPlanVO;

public interface LearningPlanService {
    LearningPlanVO generateForNote(Long noteId, Long userId, boolean replaceExisting);

    default LearningPlanVO generateForNote(Long noteId, Long userId) {
        return generateForNote(noteId, userId, false);
    }
}
