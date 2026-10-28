package com.ldd.initialization.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class TokenTrendItem implements Serializable {
    private String period;
    private Long totalTokens;
}
