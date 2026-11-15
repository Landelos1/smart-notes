package com.ldd.initialization.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.ReviewPlan;
import com.ldd.initialization.domain.Task;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.service.LearningPlanService;
import com.ldd.initialization.service.ReviewPlanService;
import com.ldd.initialization.service.TaskService;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import com.ldd.initialization.vo.LearningPlanVO;
import com.ldd.initialization.vo.ReviewPlanVO;
import com.ldd.initialization.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 将笔记内容交给模型生成结构化学习安排，并经过服务端校验后落库。 */
@Service
public class LearningPlanServiceImpl implements LearningPlanService {
    private final NoteMapper noteMapper;
    private final TaskService taskService;
    private final ReviewPlanService reviewPlanService;
    private final ConsultantService consultantService;
    private final ObjectMapper objectMapper;

    public LearningPlanServiceImpl(NoteMapper noteMapper, TaskService taskService, ReviewPlanService reviewPlanService,
                                   ConsultantService consultantService, ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.taskService = taskService;
        this.reviewPlanService = reviewPlanService;
        this.consultantService = consultantService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public LearningPlanVO generateForNote(Long noteId, Long userId, boolean replaceExisting) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) throw new BizException("笔记不存在或无权限");
        if (replaceExisting) {
            taskService.deleteGeneratedByNote(noteId, userId);
            reviewPlanService.deleteGeneratedByNote(noteId, userId);
        }
        String content = StringUtils.hasText(note.getContentMd()) ? note.getContentMd() : note.getTitle();
        String prompt = "你是学习规划助手。请根据笔记生成适合个人执行的任务与复习计划。只返回严格 JSON，不要 Markdown 代码块。格式："
                + "{\"tasks\":[{\"title\":\"\",\"description\":\"\",\"priority\":1,\"daysFromNow\":1}],"
                + "\"reviewPlans\":[{\"title\":\"\",\"goal\":\"\",\"frequency\":\"daily|weekly|custom\",\"intervalDays\":7,\"durationDays\":14}]}"
                + "。任务最多 5 个，复习计划最多 2 个，字段必须简洁。笔记标题：" + note.getTitle() + "\n笔记内容：" + content;
        String raw = consultantService.simpleChat("learning-plan-" + userId + "-" + noteId, prompt);
        JsonNode root = parseJson(raw);
        LearningPlanVO result = new LearningPlanVO();
        result.setNoteId(noteId);
        result.setNoteTitle(note.getTitle());
        List<TaskVO> tasks = new ArrayList<>();
        JsonNode taskNodes = root.path("tasks");
        if (taskNodes.isArray()) {
            for (JsonNode n : taskNodes) {
                if (!StringUtils.hasText(n.path("title").asText())) continue;
                Task task = new Task();
                task.setUserId(userId); task.setNoteId(noteId); task.setTitle(n.path("title").asText());
                task.setDescription(n.path("description").asText("")); task.setStatus(0);
                task.setPriority(Math.max(1, Math.min(3, n.path("priority").asInt(2))));
                task.setDueTime(LocalDateTime.now().plusDays(Math.max(1, n.path("daysFromNow").asInt(1))));
                task.setSourceType("note-ai"); task.setCreateTime(LocalDateTime.now()); task.setUpdateTime(LocalDateTime.now());
                tasks.add(taskService.saveGenerated(task));
                if (tasks.size() >= 5) break;
            }
        }
        List<ReviewPlanVO> plans = new ArrayList<>();
        JsonNode planNodes = root.path("reviewPlans");
        if (planNodes.isArray()) {
            for (JsonNode n : planNodes) {
                if (!StringUtils.hasText(n.path("title").asText())) continue;
                ReviewPlan plan = new ReviewPlan(); plan.setUserId(userId); plan.setNoteId(noteId);
                plan.setTitle(n.path("title").asText()); plan.setGoal(n.path("goal").asText("巩固笔记核心知识"));
                String frequency = n.path("frequency").asText("weekly").toLowerCase();
                if (!List.of("daily", "weekly", "custom").contains(frequency)) frequency = "weekly";
                plan.setFrequency(frequency); plan.setStartDate(LocalDate.now());
                plan.setRepeatDay("weekly".equals(frequency) ? plan.getStartDate().getDayOfWeek().getValue() : null);
                plan.setIntervalDays("daily".equals(frequency) ? 1 : "custom".equals(frequency)
                        ? Math.max(1, Math.min(365, n.path("intervalDays").asInt(7))) : 7);
                plan.setEndDate(LocalDate.now().plusDays(Math.max(7, n.path("durationDays").asInt(14)))); plan.setStatus(0);
                plan.setSourceType("note-ai"); plan.setCreateTime(LocalDateTime.now()); plan.setUpdateTime(LocalDateTime.now());
                plans.add(reviewPlanService.saveGenerated(plan));
                if (plans.size() >= 2) break;
            }
        }
        if (tasks.isEmpty() && plans.isEmpty()) {
            throw new BizException("AI 未生成有效的学习安排，请检查模型配置后重试");
        }
        result.setTasks(tasks); result.setReviewPlans(plans); return result;
    }

    private JsonNode parseJson(String raw) {
        try {
            String json = raw == null ? "{}" : raw.trim();
            int start = json.indexOf('{'), end = json.lastIndexOf('}');
            if (start >= 0 && end > start) json = json.substring(start, end + 1);
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new BizException("AI 返回的学习计划格式无法解析，请稍后重试");
        }
    }
}
