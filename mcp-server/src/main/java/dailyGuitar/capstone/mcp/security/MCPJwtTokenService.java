package dailyGuitar.capstone.mcp.security;

import dailyGuitar.capstone.mcp.entity.User;
import dailyGuitar.capstone.mcp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * MCP 서버용 JWT 토큰 검증 서비스
 * 백엔드와 동일한 JWT 시크릿 키를 사용하여 토큰을 검증합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MCPJwtTokenService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    private final UserRepository userRepository;
    
    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public Long extractUserId(String token) {
        try {
            String username = extractUsername(token);
            log.debug("토큰에서 사용자명 추출: {}", username);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + username));
            log.debug("사용자 ID 추출 성공: {}", user.getId());
            return user.getId();
        } catch (Exception e) {
            log.error("사용자 ID 추출 실패", e);
            throw e;
        }
    }
    
    /**
     * 토큰 유효성 검증
     */
    public boolean isTokenValid(String token) {
        try {
            boolean valid = !isTokenExpired(token);
            if (!valid) {
                log.warn("토큰이 만료되었습니다");
            }
            return valid;
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 토큰 만료 여부 확인
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * 토큰 만료 시간 추출
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * 클레임 추출
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 모든 클레임 추출
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 서명 키 생성
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

