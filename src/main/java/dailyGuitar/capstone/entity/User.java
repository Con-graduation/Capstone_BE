package dailyGuitar.capstone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;
    
    // 사용자의 실명/표시 이름
    @NotBlank
    @Column(nullable = false)
    private String name;

    // 닉네임 (표시 이름), 고유
    @NotBlank
    @Column(unique = true, nullable = false, length = 50)
    private String nickname;

    @NotBlank
    @Column(nullable = false)
    private String password;
    
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$")
    @Column(unique = true, nullable = false)
    private String username;
    

    
    @Builder.Default
    private Boolean emailVerified = false;
    
    private LocalDateTime emailVerifiedAt;

    // 이메일 인증용 일회성 코드와 만료시각 (회원가입 시 사용)
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
    
    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Role {
        MEMBER, ADMIN
    }
}

