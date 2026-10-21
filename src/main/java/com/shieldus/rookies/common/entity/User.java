package com.shieldus.rookies.common.entity;

import com.shieldus.rookies.common.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    // 사이트 내에서 사용되는 코인 -> 상호 송금 가능
    @Column(name = "coin", nullable = false)
    private long coin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String username, String password, String nickname, String email, UserRole role, long coin) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.role = (role != null) ? role : UserRole.USER;
        this.coin = coin;
    }
}
