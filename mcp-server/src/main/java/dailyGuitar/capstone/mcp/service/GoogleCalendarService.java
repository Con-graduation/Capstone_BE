package dailyGuitar.capstone.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dailyGuitar.capstone.mcp.entity.User;
import dailyGuitar.capstone.mcp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 구글 캘린더 연동 서비스
 * MCP 서버에서 구글 캘린더 API를 호출합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {
    
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${google.client.id}")
    private String clientId;
    
    @Value("${google.client.secret}")
    private String clientSecret;
    
    /**
     * 구글 액세스 토큰 갱신
     */
    @Transactional
    public String refreshAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null) {
            throw new IllegalStateException("구글 리프레시 토큰이 없습니다.");
        }

        String tokenUrl = "https://oauth2.googleapis.com/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        String body = String.format(
            "client_id=%s&client_secret=%s&refresh_token=%s&grant_type=refresh_token",
            clientId,
            clientSecret,
            user.getGoogleRefreshToken()
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(tokenUrl, request, Map.class);
            Map<String, Object> tokenResponse = response.getBody();
            
            if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                String newAccessToken = (String) tokenResponse.get("access_token");
                int expiresIn = (Integer) tokenResponse.getOrDefault("expires_in", 3600);
                
                user.setGoogleAccessToken(newAccessToken);
                user.setGoogleTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60));
                userRepository.save(user);
                
                return newAccessToken;
            } else {
                throw new IllegalStateException("토큰 갱신에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("구글 토큰 갱신 실패", e);
            throw new IllegalStateException("구글 토큰 갱신에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 유효한 구글 액세스 토큰 가져오기 (만료 시 자동 갱신)
     */
    @Transactional(readOnly = true)
    public String getValidAccessToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + userId));
        
        if (!Boolean.TRUE.equals(user.getGoogleCalendarConnected())) {
            throw new IllegalStateException("구글 캘린더가 연동되지 않았습니다.");
        }
        
        // 토큰이 만료되었거나 곧 만료될 경우 갱신
        if (user.getGoogleTokenExpiresAt() == null || 
            LocalDateTime.now().isAfter(user.getGoogleTokenExpiresAt().minusMinutes(5))) {
            return refreshAccessToken(user);
        }
        
        return user.getGoogleAccessToken();
    }

    /**
     * 구글 캘린더에 이벤트 추가
     */
    @Transactional
    public void addEventToCalendar(Long userId, String title, String description, 
                                   LocalDateTime startTime, LocalDateTime endTime) {
        String accessToken = getValidAccessToken(userId);
        
        String calendarUrl = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // ISO 8601 형식으로 변환
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime startZoned = startTime.atZone(zoneId);
        ZonedDateTime endZoned = endTime.atZone(zoneId);
        
        Map<String, Object> event = new HashMap<>();
        event.put("summary", title);
        event.put("description", description);
        
        Map<String, String> start = new HashMap<>();
        start.put("dateTime", startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        start.put("timeZone", zoneId.getId());
        
        Map<String, String> end = new HashMap<>();
        end.put("dateTime", endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        end.put("timeZone", zoneId.getId());
        
        event.put("start", start);
        event.put("end", end);
        
        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(event, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(calendarUrl, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("구글 캘린더에 이벤트가 추가되었습니다: {}", title);
            } else {
                log.error("구글 캘린더 이벤트 추가 실패: {}", response.getStatusCode());
                throw new IllegalStateException("구글 캘린더에 이벤트를 추가하는데 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("구글 캘린더 이벤트 추가 중 오류 발생", e);
            throw new IllegalStateException("구글 캘린더 이벤트 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

