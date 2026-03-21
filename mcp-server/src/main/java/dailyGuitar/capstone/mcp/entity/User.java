package dailyGuitar.capstone.mcp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP 서버용 User 엔티티
 * 백엔드와 동일한 테이블을 사용하지만, MCP 서버에서는 UserDetails 구현 불필요
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @Column(length = 255)
    private String profileImageObjectKey;

    @Column(nullable = false)
    private String password;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Builder.Default
    private Boolean emailVerified = false;
    
    private LocalDateTime emailVerifiedAt;
    
    @Column(length = 20)
    private String emailVerificationCode;

    private LocalDateTime emailVerificationExpiresAt;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.MEMBER;
    
    // 구글 OAuth 연동 관련 필드
    @Column(length = 255)
    private String googleId; // 구글 사용자 ID
    
    @Column(length = 2000)
    private String googleAccessToken; // 구글 액세스 토큰
    
    @Column(length = 2000)
    private String googleRefreshToken; // 구글 리프레시 토큰
    
    private LocalDateTime googleTokenExpiresAt; // 토큰 만료 시간
    
    @Builder.Default
    private Boolean googleCalendarConnected = false; // 구글 캘린더 연동 여부
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Role {
        MEMBER, ADMIN
    }
}

