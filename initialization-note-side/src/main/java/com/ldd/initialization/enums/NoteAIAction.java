package com.ldd.initialization.enums;

import lombok.Getter;

/**
 * 笔记AI助手操作类型枚举
 * 
 * @author ldd
 * @date 2025-12-24
 */
@Getter
public enum NoteAIAction {
    
    CONTINUE("continue", "续写内容"),
    OPTIMIZE("optimize", "优化文字"),
    SUMMARIZE("summarize", "总结内容"),
    EXPAND("expand", "扩展内容"),
    TRANSLATE("translate", "翻译"),
    LIST("list", "转为列表"),
    CUSTOM("custom", "自定义指令");
    
    private final String code;
    private final String description;
    
    NoteAIAction(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取枚举
     * 
     * @param code 操作代码
     * @return 对应的枚举，如果不存在返回null
     */
    public static NoteAIAction fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        for (NoteAIAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
    
    /**
     * 验证操作代码是否有效
     * 
     * @param code 操作代码
     * @return 是否有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
