package com.taskcenter;

import com.taskcenter.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EmailTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testSendFakeInvitation() {
        System.out.println("TEST BO'YICHA EMAIL JO'NATISH BOSHLANDI...");
        emailService.sendInvitationEmail("otabeksotimov9@gmail.com", "Zo'r Loyiha", "Test Admin");
        System.out.println("TEST BO'YICHA EMAIL JO'NATISH YAKUNLANDI!");
    }
}
