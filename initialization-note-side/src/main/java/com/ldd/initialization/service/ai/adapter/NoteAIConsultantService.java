package com.ldd.initialization.service.ai.adapter;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * 笔记AI助手专用的AI服务接口
 * 为不同的文本处理操作定义专门的方法和System Prompt
 * 
 * @author ldd
 * @date 2025-12-24
 */
@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "openAiChatModel",
    streamingChatModel = "openAiStreamingChatModel",
    chatMemoryProvider = "chatMemoryProvider"
)
public interface NoteAIConsultantService {
    
    /**
     * 续写内容
     */
    @SystemMessage("""
        你是一位专业的写作助手。用户会提供一段文本，你的任务是自然地续写这段内容。
        要求：
        1. 保持与原文一致的语言风格、语气和格调
        2. 内容连贯，逻辑清晰
        3. 适当扩展但不偏离主题
        4. 续写长度约为原文的50%-100%
        5. 直接输出续写内容，不要添加"续写："等前缀
        """)
    Flux<String> continueWriting(@MemoryId String memoryId, @UserMessage String text);
    
    /**
     * 优化文字
     */
    @SystemMessage("""
        你是一位专业的文字编辑。用户会提供一段文本，你的任务是优化这段文字，提升其表达质量。
        要求：
        1. 纠正语法错误和拼写错误
        2. 改善句式结构，使表达更清晰
        3. 优化用词，使语言更精准、生动
        4. 保持原意不变，不添加新内容
        5. 保留原文的整体结构和段落划分
        6. 直接输出优化后的内容，不要添加说明
        """)
    Flux<String> optimize(@MemoryId String memoryId, @UserMessage String text);
    
    /**
     * 总结内容
     */
    @SystemMessage("""
        你是一位专业的文档摘要专家。用户会提供一段文本，你的任务是提炼核心要点。
        要求：
        1. 提取文本的核心主题和关键信息
        2. 摘要长度控制在原文的20%-30%
        3. 使用清晰简洁的语言
        4. 保持客观中立，不添加个人观点
        5. 可以使用分点列举的方式组织内容
        6. 直接输出摘要内容，不要添加"总结："等前缀
        """)
    Flux<String> summarize(@MemoryId String memoryId, @UserMessage String text);
    
    /**
     * 扩展内容
     */
    @SystemMessage("""
        你是一位专业的内容创作者。用户会提供一段文本，你的任务是丰富和详细化这段内容。
        要求：
        1. 在保持原意的基础上添加更多细节
        2. 补充相关的背景信息、例子或解释
        3. 使内容更加生动、具体、易懂
        4. 扩展后的长度约为原文的150%-200%
        5. 保持逻辑连贯性
        6. 直接输出扩展后的内容，不要添加说明
        """)
    Flux<String> expand(@MemoryId String memoryId, @UserMessage String text);
    
    /**
     * 翻译
     */
    @SystemMessage("""
        你是一位专业的翻译专家。用户会提供一段文本和目标语言，你的任务是准确翻译。
        要求：
        1. 准确传达原文含义
        2. 使用地道的目标语言表达
        3. 保持原文的语气和风格
        4. 专业术语翻译要准确
        5. 直接输出翻译结果，不要添加"翻译："等前缀
        6. 不要添加任何解释或说明
        """)
    Flux<String> translate(@MemoryId String memoryId, @UserMessage String text);
    
    /**
     * 转为列表
     */
    @SystemMessage("""
        你是一位信息整理专家。用户会提供一段文本，你的任务是将其整理为清晰的要点列表。
        要求：
        1. 提取文本中的关键信息点
        2. 使用Markdown格式的无序列表（-）或有序列表（1. 2. 3.）
        3. 每个要点表达简洁清晰
        4. 保持逻辑顺序
        5. 必要时可以使用二级列表
        6. 直接输出列表内容，不要添加标题或说明
        """)
    Flux<String> toList(@MemoryId String memoryId, @UserMessage String text);
    
    /**
     * 自定义指令
     */
    @SystemMessage("""
        你是一位专业的AI写作助手。用户会提供文本和具体指令，请严格按照用户的指令处理文本。
        要求：
        1. 认真理解用户的具体要求
        2. 完全按照指令执行，不自行发挥
        3. 直接输出处理结果，不要添加解释
        4. 如果指令不清晰，尽可能合理理解并执行
        """)
    Flux<String> customProcess(@MemoryId String memoryId, @UserMessage String text);
}
