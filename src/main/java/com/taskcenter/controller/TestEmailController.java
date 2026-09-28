package com.taskcenter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import java.io.PrintWriter;
import java.io.StringWriter;
import com.taskcenter.dto.ApiResponse;

@RestController
public class TestEmailController {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:not_set}")
    private String mailUsername;

    @Value("${spring.mail.password:not_set}")
    private String mailPassword;

    public TestEmailController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @GetMapping("/api/test-email-direct")
    public ApiResponse<String> testEmail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String sender = mailUsername.equals("not_set") ? "test@gmail.com" : mailUsername;
            helper.setFrom(sender);
            helper.setTo("yusupovayubxon2010@gmail.com");
            helper.setSubject("Test Email from Render");
            helper.setText("This is a direct test. Pass length: " + mailPassword.length());
            
            mailSender.send(message);
            return ApiResponse.success("ok", "SUCCESS! Sent from: " + sender);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return ApiResponse.success("ok", "FAILED! Error: " + sw.toString());
        }
    }
}
