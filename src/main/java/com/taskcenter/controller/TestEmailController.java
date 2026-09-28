package com.taskcenter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import java.io.PrintWriter;
import java.io.StringWriter;

@RestController
public class TestEmailController {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:not_set}")
    private String mailUsername;

    public TestEmailController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @GetMapping("/api/test-email-direct")
    public String testEmail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername.equals("not_set") ? "test@gmail.com" : mailUsername);
            helper.setTo("yusupovayubxon2010@gmail.com");
            helper.setSubject("Test Email from Render");
            helper.setText("This is a direct test to see if SMTP is blocked on Render.");
            
            mailSender.send(message);
            return "SUCCESS! Sent from: " + mailUsername;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "FAILED! Username loaded: " + mailUsername + "\n\nError:\n" + sw.toString();
        }
    }
}
