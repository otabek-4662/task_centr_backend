package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.CalendarLinkDto;
import com.taskcenter.model.User;
import com.taskcenter.service.CalendarAndReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@Tag(name = "Calendar", description = "iCal (.ics) kalendar sinxronizatsiyasi")
public class CalendarController {

    private final CalendarAndReportService calendarAndReportService;

    public CalendarController(CalendarAndReportService calendarAndReportService) {
        this.calendarAndReportService = calendarAndReportService;
    }

    @GetMapping("/api/users/me/calendar-link")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Foydalanuvchining shaxsiy iCal va Google Calendar havolasini olish")
    public ResponseEntity<ApiResponse<CalendarLinkDto>> getCalendarLink(
            @RequestParam(defaultValue = "false") boolean regenerate,
            @AuthenticationPrincipal User currentUser) {
        CalendarLinkDto dto = calendarAndReportService.getOrCreateCalendarLink(currentUser, regenerate);
        return ResponseEntity.ok(ApiResponse.success("Kalendar havolasi muvaffaqiyatli olindi", dto));
    }

    @GetMapping(value = {"/api/calendar/feed/{token}", "/api/calendar/feed/{token}.ics"}, produces = "text/calendar; charset=UTF-8")
    @Operation(summary = "Ommaviy iCal (.ics) kalendar oqimi (Google Calendar / Apple Calendar ulanishi)")
    public ResponseEntity<byte[]> getCalendarFeed(@PathVariable String token) {
        String icsContent = calendarAndReportService.generateIcalFeed(token);
        byte[] bytes = icsContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tasks.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(bytes);
    }
}
