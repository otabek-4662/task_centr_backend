package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskcenter.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    @Schema(description = "Foydalanuvchi UUID si", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Foydalanuvchi login nomi", example = "xusanboy")
    private String name;

    @Schema(description = "To'liq ismi", example = "Xusanboy Developer")
    private String fullName;

    @Schema(description = "Elektron pochta manzili", example = "xusanboy@example.com")
    private String email;

    @Schema(description = "Foydalanuvchi roli (USER, ADMIN)", example = "USER")
    private String role;

    @Schema(description = "Workspacega allaqachon a'zo bo'lganmi yoki yo'q", example = "true")
    private Boolean isAdded;

    @Schema(description = "Vazifalar soni", example = "15")
    private Long taskCount;

    @Schema(description = "Foydalanuvchining o'zbekona maqomi / unvoni", example = "O'zimizdan")
    private String statusNickname;

    @Schema(description = "Telegram bot bilan ulanganmi (faqat joriy foydalanuvchi uchun qaytadi)", example = "true")
    private Boolean telegramLinked;

    public static String calculateStatusNickname(long count) {
        if (count <= 0) return "Begona bola";
        if (count <= 10) return "Do'konga chopuvchi";
        if (count <= 25) return "O'zimizdan";
        if (count <= 50) return "Ishonganimiz";
        if (count <= 100) return "Ko'cha ko'rgan";
        if (count <= 200) return "Katta uka";
        return "Katta aka";
    }

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .taskCount(0L)
                .statusNickname("Begona bola")
                .build();
    }

    public static UserDto fromEntity(User user, long taskCount) {
        UserDto dto = fromEntity(user);
        dto.setTaskCount(taskCount);
        dto.setStatusNickname(calculateStatusNickname(taskCount));
        return dto;
    }

    public static UserDto fromEntity(User user, boolean isAdded) {
        UserDto dto = fromEntity(user);
        dto.setIsAdded(isAdded);
        return dto;
    }
}
