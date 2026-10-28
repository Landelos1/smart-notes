package com.ldd.initialization.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class NoteDailyCountVO implements Serializable {
    private String day;
    private Long count;
}
