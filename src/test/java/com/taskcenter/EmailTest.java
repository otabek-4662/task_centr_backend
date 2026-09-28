package com.taskcenter;

import com.taskcenter.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class EmailTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testSendFakeInvitation() {
        System.out.println("TEST BO'YICHA EMAIL JO'NATISH BOSHLANDI...");
        emailService.sendInvitationEmail("yusupovayubxon2010@gmail.com", "Zo'r Loyiha", "Test Admin", "MEMBER", "test-token-123");
        System.out.println("TEST BO'YICHA EMAIL JO'NATISH YAKUNLANDI!");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
