package com.taskcenter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class EnvCheckController {

    @Value("${spring.mail.username:NOT_FOUND}")
    private String mailUsername;

    @Value("${spring.mail.password:NOT_FOUND}")
    private String mailPassword;

    @Value("${spring.mail.host:NOT_FOUND}")
    private String mailHost;

    @GetMapping("/api/check-env")
    public String checkEnv() {
        return "Username: " + mailUsername + 
               "\nPassword length: " + (mailPassword.equals("NOT_FOUND") ? "0" : mailPassword.length()) +
               "\nHost: " + mailHost;
    }
}
