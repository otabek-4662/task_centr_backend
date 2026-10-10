package com.taskcenter.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EmailTemplateRenderTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    @DisplayName("invitation.html: inviterName va workspaceRole null bo'lganda ham xatosiz render bo'ladi va 'a''zosi' matni chiqadi")
    void renderInvitationTemplate_withNullOptionals_containsAzo() {
        Context context = new Context();
        context.setVariable("workspaceTitle", "Backend Jamoasi");
        context.setVariable("invitationLink", "https://task-center.com/invite/tok123");
        context.setVariable("recipientUsername", "jasur");
        // inviterName va workspaceRole qasddan berilmaydi (fallback tekshirish uchun)

        String html = assertDoesNotThrow(() -> templateEngine.process("email/invitation", context));

        assertNotNull(html);
        assertTrue(html.contains("Backend Jamoasi"));
        assertTrue(html.contains("https://task-center.com/invite/tok123"));
        assertTrue(html.contains("jasur"));
        // SpEL fallback tekshiruvi: "Jamoa a'zosi" va "Rol: A'zo"
        assertTrue(html.contains("Jamoa a&#39;zosi") || html.contains("Jamoa a'zosi"), "Matnda 'Jamoa a'zosi' bo'lishi kerak");
        assertTrue(html.contains("Rol: A&#39;zo") || html.contains("Rol: A'zo"), "Matnda 'Rol: A'zo' bo'lishi kerak");
    }

    @Test
    @DisplayName("invitation.html: barcha maydonlar to'liq berilganda EMAIL taklifi to'g'ri render bo'ladi")
    void renderInvitationTemplate_withAllVariables_success() {
        Context context = new Context();
        context.setVariable("workspaceTitle", "Mobile Team");
        context.setVariable("invitationLink", "https://task-center.com/invite/xyz789");
        context.setVariable("recipientUsername", "nodir");
        context.setVariable("inviterName", "Otabek");
        context.setVariable("workspaceRole", "ADMIN");

        String html = assertDoesNotThrow(() -> templateEngine.process("email/invitation", context));

        assertNotNull(html);
        assertTrue(html.contains("Mobile Team"));
        assertTrue(html.contains("Otabek"));
        assertTrue(html.contains("Rol: ADMIN"));
        assertTrue(html.contains("G&#39;alvaga qo&#39;shilish") || html.contains("G'alvaga qo'shilish"));
    }
}
