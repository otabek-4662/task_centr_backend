package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "name bo'sh bo'lishi mumkin emas")
    private String name;

    @NotBlank(message = "password bo'sh bo'lishi mumkin emas")
    @Size(min = 6, message = "password kamida 6 ta belgidan iborat bo'lishi kerak")
    private String password;
}
