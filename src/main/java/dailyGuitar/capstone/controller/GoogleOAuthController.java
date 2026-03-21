package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.repository.UserRepository;
import dailyGuitar.capstone.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 구글 OAuth 인증 컨트롤러
 * 사용자가 구글 캘린더 연동을 위해 구글 로그인을 수행하고, 토큰을 DB에 저장합니다.
 */
@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@Tag(name = "구글 OAuth", description = "구글 캘린더 연동을 위한 OAuth 인증 API")
@Slf4j
public class GoogleOAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Operation(
        summary = "구글 캘린더 연동 상태 확인",
        description = """
            현재 사용자의 구글 캘린더 연동 상태를 확인합니다.
            
            **사용 시나리오:**
            - 사용자가 구글 캘린더 연동 여부를 확인하고 싶을 때
            - 연동 버튼 표시 여부를 결정할 때
            
            **응답 설명:**
            - `connected`: 구글 캘린더 연동 여부 (true/false)
            - `googleId`: 구글 계정 ID (연동된 경우에만 값이 있음)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "연동 상태 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "연동된 경우",
                        value = """
                            {
                              "connected": true,
                              "googleId": "12345678901234567890"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "연동되지 않은 경우",
                        value = """
                            {
                              "connected": false,
                              "googleId": ""
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패 (JWT 토큰이 없거나 유효하지 않음)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "인증이 필요합니다."
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConnectionStatus(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        String username = jwtTokenProvider.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        return ResponseEntity.ok(Map.of(
            "connected", user.getGoogleCalendarConnected() != null && user.getGoogleCalendarConnected(),
            "googleId", user.getGoogleId() != null ? user.getGoogleId() : ""
        ));
    }

    @Operation(
        summary = "구글 액세스 토큰 조회",
        description = """
            프론트엔드에서 구글 캘린더 API 호출에 사용할 액세스 토큰을 조회합니다.
            
            **자동 토큰 갱신:**
            - 토큰이 만료되었거나 5분 이내 만료 예정인 경우 자동으로 refresh_token을 사용하여 새 토큰을 발급합니다.
            - 새로 발급된 토큰은 DB에 저장됩니다.
            
            **사용 시나리오:**
            1. 프론트엔드에서 구글 캘린더 API를 호출하기 전에 이 엔드포인트를 호출
            2. 받은 accessToken을 Authorization 헤더에 포함하여 구글 캘린더 API 호출
            3. 토큰이 만료되면 자동으로 갱신되므로 항상 유효한 토큰을 받을 수 있음
            
            **주의사항:**
            - 구글 캘린더가 연동되어 있어야 합니다.
            - refresh_token이 없으면 토큰 갱신이 불가능하므로 다시 연동해야 합니다.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "토큰 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "accessToken": "ya29.a0AfH6SMC...",
                          "expiresAt": "2025-11-14T16:00:00"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "구글 캘린더가 연동되지 않았거나 refresh_token이 없음",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "연동되지 않은 경우",
                        value = """
                            {
                              "error": "구글 캘린더가 연동되지 않았습니다."
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "refresh_token이 없는 경우",
                        value = """
                            {
                              "error": "리프레시 토큰이 없습니다. 구글 계정을 다시 연동해주세요."
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "인증이 필요합니다."
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "토큰 갱신 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "토큰 갱신에 실패했습니다."
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/token")
    @Transactional
    public ResponseEntity<Map<String, Object>> getAccessToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            String username = jwtTokenProvider.extractUsername(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            if (!Boolean.TRUE.equals(user.getGoogleCalendarConnected())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "구글 캘린더가 연동되지 않았습니다."));
            }
            
            // 토큰이 만료되었거나 곧 만료될 경우 갱신
            String accessToken = user.getGoogleAccessToken();
            if (user.getGoogleTokenExpiresAt() == null || 
                java.time.LocalDateTime.now().isAfter(user.getGoogleTokenExpiresAt().minusMinutes(5))) {
                
                // Refresh Token으로 새 Access Token 발급
                if (user.getGoogleRefreshToken() == null || user.getGoogleRefreshToken().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "리프레시 토큰이 없습니다. 구글 계정을 다시 연동해주세요."));
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
                
                HttpEntity<String> refreshRequest = new HttpEntity<>(body, headers);
                @SuppressWarnings("unchecked")
                ResponseEntity<Map<String, Object>> refreshResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(tokenUrl, refreshRequest, Map.class);
                
                Map<String, Object> tokenResponse = refreshResponse.getBody();
                if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                    accessToken = (String) tokenResponse.get("access_token");
                    int expiresIn = (Integer) tokenResponse.getOrDefault("expires_in", 3600);
                    
                    user.setGoogleAccessToken(accessToken);
                    user.setGoogleTokenExpiresAt(java.time.LocalDateTime.now().plusSeconds(expiresIn - 60));
                    userRepository.save(user);
                    
                    log.info("구글 액세스 토큰 갱신 완료");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "토큰 갱신에 실패했습니다."));
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "expiresAt", user.getGoogleTokenExpiresAt() != null 
                    ? user.getGoogleTokenExpiresAt().toString() 
                    : ""
            ));
            
        } catch (Exception e) {
            log.error("구글 액세스 토큰 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "토큰 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "구글 계정 연동",
        description = """
            프론트엔드에서 구글 로그인 후 받은 토큰을 전달하여 구글 캘린더를 연동합니다.
            
            **프론트엔드에서 구글 로그인 방법:**
            ```javascript
            const tokenClient = google.accounts.oauth2.initTokenClient({
              client_id: 'YOUR_CLIENT_ID',
              scope: 'openid profile email https://www.googleapis.com/auth/calendar',
              callback: (response) => {
                // response.access_token, response.refresh_token을 백엔드로 전달
              }
            });
            tokenClient.requestAccessToken({ prompt: 'consent' }); // refresh_token 받기 위해 필요
            ```
            
            **요청 파라미터:**
            - `accessToken` (필수): 구글에서 받은 액세스 토큰
            - `refreshToken` (선택): 구글에서 받은 리프레시 토큰 (prompt: 'consent' 사용 시 받을 수 있음)
            - `expiresIn` (선택): 토큰 만료 시간(초), 기본값: 3600초
            
            **동작 과정:**
            1. 구글 액세스 토큰으로 사용자 정보 조회
            2. 구글 사용자 ID 추출
            3. DB에 토큰 및 연동 정보 저장
            4. 연동 완료 응답 반환
            
            **주의사항:**
            - refresh_token을 받으려면 프론트엔드에서 `prompt: 'consent'`를 사용해야 합니다.
            - refresh_token이 없으면 토큰 만료 후 갱신이 불가능합니다.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "구글 캘린더 연동 성공",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "refresh_token이 있는 경우",
                        value = """
                            {
                              "success": true,
                              "message": "구글 캘린더 연동이 완료되었습니다.",
                              "googleId": "12345678901234567890",
                              "hasRefreshToken": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "refresh_token이 없는 경우",
                        value = """
                            {
                              "success": true,
                              "message": "구글 캘린더 연동이 완료되었습니다.",
                              "googleId": "12345678901234567890",
                              "hasRefreshToken": false
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (토큰 없음, 사용자 정보 조회 실패 등)",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "액세스 토큰이 없는 경우",
                        value = """
                            {
                              "error": "구글 액세스 토큰이 필요합니다."
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "사용자 정보 조회 실패",
                        value = """
                            {
                              "error": "구글 사용자 정보를 가져오는데 실패했습니다."
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "구글 ID 추출 실패",
                        value = """
                            {
                              "error": "구글 사용자 ID를 가져올 수 없습니다."
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "인증이 필요합니다."
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "구글 연동 처리 중 오류가 발생했습니다: [오류 메시지]"
                        }
                        """
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "구글 로그인 후 받은 토큰 정보",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = """
                    {
                      "accessToken": "ya29.a0AfH6SMC...",
                      "refreshToken": "1//0g...",
                      "expiresIn": "3600"
                    }
                    """
            ),
            examples = {
                @ExampleObject(
                    name = "refresh_token 포함",
                    value = """
                        {
                          "accessToken": "ya29.a0AfH6SMC...",
                          "refreshToken": "1//0g...",
                          "expiresIn": "3600"
                        }
                        """
                ),
                @ExampleObject(
                    name = "refresh_token 없음 (권장하지 않음)",
                    value = """
                        {
                          "accessToken": "ya29.a0AfH6SMC...",
                          "expiresIn": "3600"
                        }
                        """
                )
            }
        )
    )
    @PostMapping("/connect")
    @Transactional
    public ResponseEntity<Map<String, Object>> connectGoogleAccount(
            HttpServletRequest request,
            @RequestBody Map<String, String> googleTokenRequest) {
        
        String token = extractTokenFromRequest(request);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        String googleAccessToken = googleTokenRequest.get("accessToken");
        if (googleAccessToken == null || googleAccessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "구글 액세스 토큰이 필요합니다."));
        }

        try {
            String username = jwtTokenProvider.extractUsername(token);
            
            // 구글 사용자 정보 가져오기
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(googleAccessToken);
            HttpEntity<Void> userInfoRequest = new HttpEntity<>(userInfoHeaders);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> userInfoResponse = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, userInfoRequest, Map.class
            );
            
            if (!userInfoResponse.getStatusCode().is2xxSuccessful() || userInfoResponse.getBody() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "구글 사용자 정보를 가져오는데 실패했습니다."));
            }
            
            Map<String, Object> userInfo = userInfoResponse.getBody();
            if (userInfo == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "구글 사용자 정보가 비어있습니다."));
            }
            String googleId = (String) userInfo.get("id");
            if (googleId == null || googleId.isEmpty()) {
                log.error("구글 사용자 ID를 가져올 수 없습니다. userInfo: {}", userInfo);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "구글 사용자 ID를 가져올 수 없습니다."));
            }
            
            log.info("구글 사용자 정보 조회 성공: googleId={}, email={}", googleId, userInfo.get("email"));
            
            // 사용자 정보 업데이트
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
            
            user.setGoogleId(googleId);
            user.setGoogleAccessToken(googleAccessToken);
            
            // 프론트엔드에서 refresh token도 받을 수 있으면 저장
            String refreshToken = googleTokenRequest.get("refreshToken");
            if (refreshToken != null && !refreshToken.isEmpty()) {
                user.setGoogleRefreshToken(refreshToken);
                log.info("리프레시 토큰 저장 완료");
            } else {
                log.warn("리프레시 토큰이 없습니다. 프론트엔드에서 prompt: 'consent'를 사용했는지 확인하세요.");
            }
            
            // 토큰 만료 시간 설정 (프론트엔드에서 받은 값 또는 기본 1시간)
            Integer expiresIn = null;
            try {
                String expiresInStr = googleTokenRequest.get("expiresIn");
                if (expiresInStr != null && !expiresInStr.isEmpty()) {
                    expiresIn = Integer.parseInt(expiresInStr);
                }
            } catch (NumberFormatException e) {
                log.warn("토큰 만료 시간 파싱 실패, 기본값 사용");
            }
            
            if (expiresIn != null && expiresIn > 0) {
                user.setGoogleTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn - 60)); // 1분 여유
            } else {
                user.setGoogleTokenExpiresAt(LocalDateTime.now().plusHours(1));
            }
            user.setGoogleCalendarConnected(true);
            
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "구글 캘린더 연동이 완료되었습니다.",
                "googleId", googleId,
                "hasRefreshToken", refreshToken != null && !refreshToken.isEmpty()
            ));
            
        } catch (Exception e) {
            log.error("구글 계정 연동 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "구글 연동 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "구글 캘린더 연동 해제",
        description = """
            구글 캘린더 연동을 해제합니다.
            
            **동작:**
            - DB에 저장된 모든 구글 관련 정보를 삭제합니다.
            - googleId, accessToken, refreshToken, tokenExpiresAt을 null로 설정
            - googleCalendarConnected를 false로 설정
            
            **사용 시나리오:**
            - 사용자가 구글 캘린더 연동을 해제하고 싶을 때
            - 다른 구글 계정으로 다시 연동하기 전에 기존 연동 해제
            
            **주의사항:**
            - 연동 해제 후에는 구글 캘린더 기능을 사용할 수 없습니다.
            - 다시 사용하려면 `/connect` 엔드포인트를 통해 재연동해야 합니다.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "연동 해제 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "message": "구글 캘린더 연동이 해제되었습니다."
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "인증이 필요합니다."
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/disconnect")
    @Transactional
    public ResponseEntity<Map<String, String>> disconnect(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || !isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "인증이 필요합니다."));
        }

        String username = jwtTokenProvider.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        user.setGoogleId(null);
        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleTokenExpiresAt(null);
        user.setGoogleCalendarConnected(false);
        
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("message", "구글 캘린더 연동이 해제되었습니다."));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isTokenValid(String token) {
        try {
            jwtTokenProvider.extractUsername(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

