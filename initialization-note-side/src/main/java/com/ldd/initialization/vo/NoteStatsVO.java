package com.ldd.initialization.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class NoteStatsVO implements Serializable {
    private Long total;
    private String mostProductiveDate;
    private Long mostProductiveCount;
}
