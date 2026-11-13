package com.ldd.initialization.vo;

import lombok.Data;

import java.util.List;

/** AI 根据笔记生成并保存的学习安排。 */
@Data
public class LearningPlanVO {
    private Long noteId;
    private String noteTitle;
    private List<TaskVO> tasks;
    private List<ReviewPlanVO> reviewPlans;
}
