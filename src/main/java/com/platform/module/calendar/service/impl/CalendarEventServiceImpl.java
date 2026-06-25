package com.platform.module.calendar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.platform.module.calendar.entity.CalendarEvent;
import com.platform.module.calendar.mapper.CalendarEventMapper;
import com.platform.module.calendar.service.CalendarEventService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarEventServiceImpl extends ServiceImpl<CalendarEventMapper, CalendarEvent> implements CalendarEventService {

    @Override
    public List<CalendarEvent> getEventsByDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return list(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getUserId, userId)
                .ge(CalendarEvent::getStartTime, start)
                .le(CalendarEvent::getEndTime, end)
                .orderByAsc(CalendarEvent::getStartTime));
    }

    @Override
    public List<Map<String, Object>> detectConflicts(Long userId, LocalDateTime start, LocalDateTime end, Long excludeEventId) {
        LambdaQueryWrapper<CalendarEvent> wrapper = new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getUserId, userId)
                .lt(CalendarEvent::getStartTime, end)
                .gt(CalendarEvent::getEndTime, start);
        if (excludeEventId != null) {
            wrapper.ne(CalendarEvent::getId, excludeEventId);
        }
        List<CalendarEvent> conflicting = list(wrapper);

        return conflicting.stream().map(e -> {
            Map<String, Object> conflict = new HashMap<>();
            conflict.put("id", e.getId());
            conflict.put("title", e.getTitle());
            conflict.put("startTime", e.getStartTime());
            conflict.put("endTime", e.getEndTime());
            conflict.put("type", e.getType());
            return conflict;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CalendarEvent> getReminders(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return list(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getUserId, userId)
                .eq(CalendarEvent::getIsReminded, 0)
                .le(CalendarEvent::getRemindTime, now)
                .orderByAsc(CalendarEvent::getStartTime));
    }

    @Override
    public CalendarEvent addCustomEvent(CalendarEvent event) {
        event.setIsReminded(0);
        save(event);
        return event;
    }
}
