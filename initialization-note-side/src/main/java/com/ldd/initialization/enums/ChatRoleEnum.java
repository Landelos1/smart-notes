package com.ldd.initialization.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@RequiredArgsConstructor
public enum ChatRoleEnum {
    USER(1, "user"),
    ASSISTANT(2, "assistant");

    private final int value;
    private final String key;

    public static ChatRoleEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ChatRoleEnum role : values()) {
            if (role.value == value) {
                return role;
            }
        }
        return null;
    }

    public static ChatRoleEnum fromKey(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        for (ChatRoleEnum role : values()) {
            if (role.key.equals(key)) {
                return role;
            }
        }
        return null;
    }

    public static String keyOf(Integer value) {
        ChatRoleEnum role = fromValue(value);
        return role != null ? role.key : null;
    }
}
