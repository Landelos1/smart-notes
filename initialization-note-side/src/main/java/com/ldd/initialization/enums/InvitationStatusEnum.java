package com.ldd.initialization.enums;

import lombok.Getter;

/**
 * 知识库邀请状态枚举
 */
@Getter
public enum InvitationStatusEnum {
    
    PENDING(0, "待处理"),
    ACCEPTED(1, "已接受"),
    REJECTED(2, "已拒绝"),
    EXPIRED(3, "已过期");

    private final int value;
    private final String desc;

    InvitationStatusEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据 value 获取对应的枚举
     */
    public static InvitationStatusEnum getByValue(int value) {
        for (InvitationStatusEnum status : InvitationStatusEnum.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取对应的 desc
     */
    public static String getDescByValue(int value) {
        InvitationStatusEnum status = getByValue(value);
        return status != null ? status.getDesc() : null;
    }
}
