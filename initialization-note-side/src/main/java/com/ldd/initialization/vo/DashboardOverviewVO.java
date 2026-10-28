package com.ldd.initialization.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class DashboardOverviewVO implements Serializable {
    private Long activeUsers;
    private Long newNotes;
    private Long paidOrders;
    private Long requestCount;

    private String activeUsersChange;
    private String newNotesChange;
    private String paidOrdersChange;
    private String requestCountChange;
}
