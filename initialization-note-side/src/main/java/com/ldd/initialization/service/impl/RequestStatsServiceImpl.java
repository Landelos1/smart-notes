package com.ldd.initialization.service.impl;

import com.ldd.initialization.enums.ChatRoleEnum;
import com.ldd.initialization.mapper.ChatMessageMapper;
import com.ldd.initialization.service.RequestStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestStatsServiceImpl implements RequestStatsService {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Long getTodayRequestCount() {
        Long count = chatMessageMapper.countTodayRequests(ChatRoleEnum.USER.getValue());
        return count != null ? count : 0L;
    }

    @Override
    public Long getLastWeekRequestCount() {
        Long count = chatMessageMapper.countLastWeekRequests(ChatRoleEnum.USER.getValue());
        return count != null ? count : 0L;
    }
}
