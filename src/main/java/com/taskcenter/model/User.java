package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_name", columnList = "name")
})
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_link_token")
    private String telegramLinkToken;

    @Column(name = "telegram_link_token_expires_at")
    private LocalDateTime telegramLinkTokenExpiresAt;

    @Column(name = "telegram_notify_assigned", nullable = false)
    @Builder.Default
    private boolean telegramNotifyAssigned = true;

    @Column(name = "telegram_notify_comments", nullable = false)
    @Builder.Default
    private boolean telegramNotifyComments = true;

    @Column(name = "telegram_notify_deadlines", nullable = false)
    @Builder.Default
    private boolean telegramNotifyDeadlines = true;

    @Column(name = "telegram_daily_digest", nullable = false)
    @Builder.Default
    private boolean telegramDailyDigest = true;

    @Column(name = "telegram_quiet_start")
    private java.time.LocalTime telegramQuietStart;

    @Column(name = "telegram_quiet_end")
    private java.time.LocalTime telegramQuietEnd;

    @Column(name = "telegram_notify_moves", nullable = false)
    @Builder.Default
    private boolean telegramNotifyMoves = true;

    @Column(name = "telegram_notify_invites", nullable = false)
    @Builder.Default
    private boolean telegramNotifyInvites = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.fullName == null) {
            this.fullName = this.name;
        }
        if (this.email == null) {
            this.email = "user_" + this.id + "@taskcenter.local";
        }
    }

    public enum Role {
        USER, ADMIN
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
