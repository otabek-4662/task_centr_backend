package com.taskcenter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarLinkDto {
    private String icsUrl;
    private String webcalUrl;
    private String googleCalendarUrl;
    private String calendarToken;
}
