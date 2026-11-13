package com.ldd.initialization.service;

import com.ldd.initialization.domain.Note;
import com.ldd.initialization.dto.AgentToolRequestDTO;
import com.ldd.initialization.dto.ReviewPlanCreateDTO;
import com.ldd.initialization.dto.TaskCreateDTO;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.service.ai.UserConsultantService;
import com.ldd.initialization.vo.ReviewPlanVO;
import com.ldd.initialization.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 的业务工具层。所有查询和写入都从当前会话传入 userId，模型只能决定 action 和业务参数。
 */
@Service
public class AgentToolsService {
    private final NoteMapper noteMapper;
    private final TaskService taskService;
    private final ReviewPlanService reviewPlanService;
    private final UserConsultantService userConsultantService;

    public AgentToolsService(NoteMapper noteMapper, TaskService taskService, ReviewPlanService reviewPlanService,
                             UserConsultantService userConsultantService) {
        this.noteMapper = noteMapper;
        this.taskService = taskService;
        this.reviewPlanService = reviewPlanService;
        this.userConsultantService = userConsultantService;
    }

    public Object execute(AgentToolRequestDTO request, Long userId) {
        String action = request.getAction() == null ? "" : request.getAction().trim().toLowerCase();
        return switch (action) {
            case "list_notes" -> listNotes(userId, request.getQuery());
            case "search_knowledge" -> searchKnowledge(userId, request.getQuery());
            case "list_tasks" -> taskService.list(userId);
            case "list_review_plans" -> reviewPlanService.list(userId);
            case "create_task" -> createTask(request, userId);
            case "create_review_plan" -> createReviewPlan(request, userId);
            default -> throw new IllegalArgumentException("不支持的 Agent 工具: " + action);
        };
    }

    public String buildContext(Long userId, String message) {
        return buildContext(userId, message, true);
    }

    /** 为业务型 Agent 注入当前用户数据。知识库已经由聊天请求完成 RAG 时不再重复检索。 */
    public String buildContext(Long userId, String message, boolean includeKnowledge) {
        if (!StringUtils.hasText(message)) return "";
        String lower = message.toLowerCase();
        StringBuilder context = new StringBuilder();
        if (lower.contains("任务") || lower.contains("待办") || lower.contains("todo")) {
            context.append("\n【当前用户任务】\n");
            taskService.list(userId).stream().limit(10).forEach(t -> context.append("- ")
                    .append(t.getTitle()).append("（状态").append(t.getStatus()).append("，截止")
                    .append(t.getDueTime() == null ? "未设置" : t.getDueTime()).append("）\n"));
        }
        if (lower.contains("复习") || lower.contains("计划")) {
            context.append("\n【当前用户复习计划】\n");
            reviewPlanService.list(userId).stream().limit(10).forEach(p -> context.append("- ")
                    .append(p.getTitle()).append("（").append(p.getStartDate()).append(" 至 ").append(p.getEndDate()).append("）\n"));
        }
        if (lower.contains("笔记") || lower.contains("知识库") || lower.contains("资料")) {
            context.append("\n【当前用户笔记】\n");
            // 上下文问题通常是自然语言完整句子，直接作为标题关键词会导致“我的笔记有哪些”检索为空；
            // 这里读取当前用户最近笔记，精确检索仍由 list_notes 工具或 search_knowledge 负责。
            listNotes(userId, null).stream().limit(10).forEach(n -> context.append("- ")
                    .append(n.get("title")).append("：").append(n.get("content")).append("\n"));
        }
        if (includeKnowledge && (lower.contains("知识库") || lower.contains("资料") || lower.contains("检索"))) {
            String knowledgeContext = userConsultantService.buildKnowledgeContextOnly(
                    String.valueOf(userId), message, 5);
            if (StringUtils.hasText(knowledgeContext) && !knowledgeContext.equals(message)) {
                context.append("\n【当前用户知识库检索结果】\n").append(knowledgeContext).append("\n");
            }
        }
        if (context.length() == 0) return "";
        return context + "\n请把这些内容作为当前用户的真实业务数据参考；不要编造不存在的任务或笔记。\n";
    }

    private Map<String, Object> searchKnowledge(Long userId, String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("知识库检索内容不能为空");
        }
        return Map.of("query", query,
                "context", userConsultantService.buildKnowledgeContextOnly(String.valueOf(userId), query, 5));
    }

    private List<Map<String, Object>> listNotes(Long userId, String query) {
        List<Note> notes = noteMapper.selectByUserIdForAgent(userId, query);
        return notes.stream().map(n -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", n.getId()); item.put("title", n.getTitle());
            String content = n.getContentMd() == null ? "" : n.getContentMd().replaceAll("\\s+", " ");
            item.put("content", content.length() > 240 ? content.substring(0, 240) + "…" : content);
            item.put("updatedAt", n.getUpdateTime()); return item;
        }).toList();
    }

    private TaskVO createTask(AgentToolRequestDTO request, Long userId) {
        TaskCreateDTO dto = new TaskCreateDTO();
        dto.setTitle(request.getTitle()); dto.setDescription(request.getDescription()); dto.setNoteId(request.getNoteId());
        dto.setPriority(request.getPriority()); dto.setDueTime(request.getDueTime()); dto.setSourceType("agent");
        return taskService.create(dto, userId);
    }

    private ReviewPlanVO createReviewPlan(AgentToolRequestDTO request, Long userId) {
        ReviewPlanCreateDTO dto = new ReviewPlanCreateDTO();
        dto.setTitle(request.getTitle()); dto.setGoal(request.getGoal()); dto.setNoteId(request.getNoteId());
        dto.setFrequency(request.getFrequency()); dto.setStartDate(request.getStartDate() == null ? LocalDate.now() : request.getStartDate());
        dto.setEndDate(request.getEndDate()); dto.setSourceType("agent");
        return reviewPlanService.create(dto, userId);
    }
}
