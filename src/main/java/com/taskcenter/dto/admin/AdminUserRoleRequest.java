package com.taskcenter.dto.admin;

import com.taskcenter.model.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRoleRequest {
    @NotNull(message = "Rol ko'rsatilishi shart (USER yoki ADMIN)")
    private User.Role role;
}
