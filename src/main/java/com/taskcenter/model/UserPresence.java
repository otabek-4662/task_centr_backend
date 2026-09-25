package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresence {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean online = false;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}
