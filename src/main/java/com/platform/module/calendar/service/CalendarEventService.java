package com.platform.module.calendar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.module.calendar.entity.CalendarEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CalendarEventService extends IService<CalendarEvent> {

    List<CalendarEvent> getEventsByDateRange(Long userId, LocalDateTime start, LocalDateTime end);

    List<Map<String, Object>> detectConflicts(Long userId, LocalDateTime start, LocalDateTime end, Long excludeEventId);

    List<CalendarEvent> getReminders(Long userId);

    CalendarEvent addCustomEvent(CalendarEvent event);
}
