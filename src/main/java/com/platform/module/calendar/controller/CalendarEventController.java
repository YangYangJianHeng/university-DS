package com.platform.module.calendar.controller;

import com.platform.common.result.Result;
import com.platform.common.util.UserContext;
import com.platform.module.calendar.entity.CalendarEvent;
import com.platform.module.calendar.service.CalendarEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "Calendar Management")
@RestController
@RequestMapping("/api/calendar")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    public CalendarEventController(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    @GetMapping("/events")
    @Operation(summary = "Get events by date range")
    public Result<List<CalendarEvent>> events(@RequestParam(required = false) String start,
                                              @RequestParam(required = false) String end) {
        Long userId = UserContext.getCurrentUserId();
        LocalDateTime startTime = start != null ? LocalDateTime.parse(start) : LocalDateTime.now().minusMonths(1);
        LocalDateTime endTime = end != null ? LocalDateTime.parse(end) : LocalDateTime.now().plusMonths(1);
        return Result.success(calendarEventService.getEventsByDateRange(userId, startTime, endTime));
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Detect time conflicts")
    public Result<List<Map<String, Object>>> conflicts(@RequestParam String start,
                                                       @RequestParam String end,
                                                       @RequestParam(required = false) Long excludeEventId) {
        Long userId = UserContext.getCurrentUserId();
        LocalDateTime startTime = LocalDateTime.parse(start);
        LocalDateTime endTime = LocalDateTime.parse(end);
        return Result.success(calendarEventService.detectConflicts(userId, startTime, endTime, excludeEventId));
    }

    @PostMapping("/check-conflict")
    @Operation(summary = "Check time conflict via POST")
    public Result<List<Map<String, Object>>> checkConflict(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getCurrentUserId();
        String start = (String) request.get("start");
        String end = (String) request.get("end");
        Long excludeEventId = request.get("excludeEventId") != null ?
                Long.valueOf(request.get("excludeEventId").toString()) : null;
        LocalDateTime startTime = start != null ? LocalDateTime.parse(start) : LocalDateTime.now();
        LocalDateTime endTime = end != null ? LocalDateTime.parse(end) : LocalDateTime.now().plusHours(1);
        return Result.success(calendarEventService.detectConflicts(userId, startTime, endTime, excludeEventId));
    }

    @GetMapping("/reminders")
    @Operation(summary = "Get events needing reminder")
    public Result<List<CalendarEvent>> reminders() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(calendarEventService.getReminders(userId));
    }

    @PostMapping("/event")
    @Operation(summary = "Add custom event")
    public Result<CalendarEvent> addEvent(@RequestBody CalendarEvent event) {
        Long userId = UserContext.getCurrentUserId();
        event.setUserId(userId);
        return Result.success(calendarEventService.addCustomEvent(event));
    }
}
