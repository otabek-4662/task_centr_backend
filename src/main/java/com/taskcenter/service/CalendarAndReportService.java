package com.taskcenter.service;

import com.taskcenter.dto.CalendarLinkDto;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class CalendarAndReportService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final String miniappBaseUrl;

    public CalendarAndReportService(
            UserRepository userRepository,
            TaskRepository taskRepository,
            WorkspaceRepository workspaceRepository,
            ColumnRepository columnRepository,
            WorkspaceAuthorizationService authorizationService,
            @Value("${telegram.miniapp.base-url:https://task-centr-backend.onrender.com}") String miniappBaseUrl) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.workspaceRepository = workspaceRepository;
        this.columnRepository = columnRepository;
        this.authorizationService = authorizationService;
        this.miniappBaseUrl = miniappBaseUrl;
    }

    @Transactional
    public CalendarLinkDto getOrCreateCalendarLink(User user, boolean regenerate) {
        if (user == null) {
            throw new ResourceNotFoundException("Foydalanuvchi topilmadi");
        }

        if (user.getCalendarToken() == null || user.getCalendarToken().isBlank() || regenerate) {
            String token = UUID.randomUUID().toString().replace("-", "");
            user.setCalendarToken(token);
            userRepository.save(user);
        }

        String base = miniappBaseUrl.replaceAll("/+$", "");
        String icsUrl = base + "/api/calendar/feed/" + user.getCalendarToken() + ".ics";
        String webcalUrl = icsUrl.replaceFirst("^https?://", "webcal://");
        String googleCalendarUrl = "https://calendar.google.com/calendar/r?cid=" + URLEncoder.encode(icsUrl, StandardCharsets.UTF_8);

        return CalendarLinkDto.builder()
                .icsUrl(icsUrl)
                .webcalUrl(webcalUrl)
                .googleCalendarUrl(googleCalendarUrl)
                .calendarToken(user.getCalendarToken())
                .build();
    }

    @Transactional(readOnly = true)
    public String generateIcalFeed(String token) {
        String cleanToken = token != null ? token.replace(".ics", "").trim() : "";
        User user = userRepository.findByCalendarToken(cleanToken)
                .orElseThrow(() -> new ResourceNotFoundException("Kalendar topilmadi: " + token));

        List<Task> activeTasks = taskRepository.findActiveTasksByUserId(user.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Task Center//Task Manager//UZ\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:Task Center - ").append(escapeIcal(user.getName())).append("\r\n");
        sb.append("X-WR-TIMEZONE:Asia/Tashkent\r\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String todayStr = LocalDate.now().format(dtf);

        for (Task t : activeTasks) {
            if (t.getDeletedAt() != null || Boolean.TRUE.equals(t.getIsArchived())) {
                continue;
            }
            LocalDate due = t.getDueDate() != null ? t.getDueDate() : t.getNextOccurrenceDate();
            if (due == null) {
                continue;
            }
            String dtStr = due.format(dtf);
            String code = t.getPublicId() != null ? "[" + t.getPublicId() + "] " : "";

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:task-").append(t.getId()).append("@taskcenter.com\r\n");
            sb.append("DTSTAMP:").append(todayStr).append("T000000Z\r\n");
            sb.append("DTSTART;VALUE=DATE:").append(dtStr).append("\r\n");
            sb.append("DTEND;VALUE=DATE:").append(dtStr).append("\r\n");
            sb.append("SUMMARY:").append(escapeIcal(code + t.getTitle())).append("\r\n");
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                sb.append("DESCRIPTION:").append(escapeIcal(t.getDescription())).append("\r\n");
            }
            sb.append("STATUS:CONFIRMED\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public byte[] generateDailyWorkspacePdfReport(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace topilmadi: " + workspaceId));

        List<Task> tasks = taskRepository.findByWorkspaceId(workspaceId);
        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);

        long totalTasks = tasks.stream().filter(t -> t.getDeletedAt() == null).count();
        long completedTasks = tasks.stream().filter(t -> {
            if (t.getDeletedAt() != null) return false;
            return columns.stream().anyMatch(c -> c.getId().equals(t.getColumnId()) && Boolean.TRUE.equals(c.getIsDone()));
        }).count();

        return buildSimplePdfReport(ws, totalTasks, completedTasks, tasks);
    }

    private byte[] buildSimplePdfReport(Workspace ws, long total, long completed, List<Task> tasks) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StringBuilder textContent = new StringBuilder();

            textContent.append("BT\n");
            textContent.append("/F1 18 Tf\n");
            textContent.append("50 740 Td\n");
            textContent.append("(").append(sanitizePdfText("Task Center - Kundalik Loyiha Hisoboti")).append(") Tj\n");

            textContent.append("/F1 12 Tf\n");
            textContent.append("0 -25 Td\n");
            textContent.append("(").append(sanitizePdfText("Loyiha: " + ws.getTitle() + " [" + ws.getKeyPrefix() + "]")).append(") Tj\n");

            textContent.append("0 -18 Td\n");
            textContent.append("(").append(sanitizePdfText("Sana: " + LocalDate.now() + " | Jami vazifalar: " + total + " ta | Bajarildi: " + completed + " ta")).append(") Tj\n");

            textContent.append("0 -25 Td\n");
            textContent.append("(").append(sanitizePdfText("--- Faol Vazifalar Ro'yxati ---")).append(") Tj\n");

            int yOffset = 0;
            int count = 0;
            for (Task t : tasks) {
                if (t.getDeletedAt() != null || count >= 25) continue;
                String code = t.getPublicId() != null ? "[" + t.getPublicId() + "] " : "";
                String due = t.getDueDate() != null ? " (Muddat: " + t.getDueDate() + ")" : "";
                String line = "- " + code + t.getTitle() + due;

                textContent.append("0 -16 Td\n");
                textContent.append("(").append(sanitizePdfText(line)).append(") Tj\n");
                count++;
            }

            textContent.append("ET\n");

            byte[] streamBytes = textContent.toString().getBytes(StandardCharsets.ISO_8859_1);

            List<Integer> offsets = new ArrayList<>();
            writePdfString(out, "%PDF-1.4\n");

            // 1 0 obj: Catalog
            offsets.add(out.size());
            writePdfString(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // 2 0 obj: Pages
            offsets.add(out.size());
            writePdfString(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            // 3 0 obj: Page
            offsets.add(out.size());
            writePdfString(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");

            // 4 0 obj: Contents stream
            offsets.add(out.size());
            writePdfString(out, "4 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            out.write(streamBytes);
            writePdfString(out, "\nendstream\nendobj\n");

            // 5 0 obj: Font
            offsets.add(out.size());
            writePdfString(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            // xref
            int startXref = out.size();
            writePdfString(out, "xref\n0 6\n0000000000 65535 f \n");
            for (int off : offsets) {
                writePdfString(out, String.format(Locale.US, "%010d 00000 n \n", off));
            }
            writePdfString(out, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + startXref + "\n%%EOF\n");

            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF report", e);
            return new byte[0];
        }
    }

    private void writePdfString(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    private String sanitizePdfText(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", " ");
    }

    private String escapeIcal(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
