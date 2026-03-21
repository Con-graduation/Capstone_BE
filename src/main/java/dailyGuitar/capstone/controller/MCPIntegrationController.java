package dailyGuitar.capstone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 서버와 AI를 통합하는 컨트롤러
 * 프론트엔드에서 간단하게 AI 기능을 사용할 수 있도록 합니다.
 */
@RestController
@RequestMapping("/api/mcp-ai")
@RequiredArgsConstructor
@Tag(name = "MCP AI 통합", description = "MCP 서버와 AI를 활용한 자연어 기반 기능 제공 API")
@Slf4j
public class MCPIntegrationController {
    
    private final RestTemplate restTemplate;
    
    @Value("${mcp.server.url:http://localhost:8081}")
    private String mcpServerUrl;
    
    @Value("${ai.openai.api.key:}")
    private String openAiApiKey;
    
    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;
    
    @Operation(
        summary = "AI 대화 (루틴/세션 조회 및 분석)",
        description = """
            AI에게 프롬프트를 보내고 MCP 리소스(루틴 또는 연습 세션)를 활용한 응답을 받습니다.
            
            **사용 시나리오:**
            - 루틴 조회 및 정리: "내 루틴을 조회해서 현재 시각 기준으로 가까운 순으로 루틴을 나열해줘"
            - 루틴 분석: "내 루틴 중 가장 많이 연습한 루틴은 뭐야?"
            - 연습 세션 통계: "내 연습 세션 통계를 요약해줘"
            - 루틴 추천: "BPM이 80인 루틴을 추천해줘"
            
            **주의사항:**
            - 구글 캘린더에 이벤트를 추가하려면 `/calendar-prompt` 엔드포인트를 사용하세요.
            - 이 엔드포인트는 데이터 조회 및 분석만 수행합니다.
            
            **resourceType 파라미터:**
            - `practice-routines`: 루틴 데이터 조회 (기본값)
            - `practice-sessions`: 연습 세션 데이터 조회
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "AI 응답 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "resourceData": {
                            "contents": [
                              {
                                "mimeType": "application/json",
                                "text": "[{\"id\":1,\"title\":\"CEAmG5\",...}]",
                                "uri": "practice-routines"
                              }
                            ]
                          },
                          "aiResponse": {
                            "id": "chatcmpl-...",
                            "object": "chat.completion",
                            "created": 1763099557,
                            "model": "gpt-4o-mini-2024-07-18",
                            "choices": [
                              {
                                "index": 0,
                                "message": {
                                  "role": "assistant",
                                  "content": "사용자의 루틴을 시간순으로 정리했습니다..."
                                }
                              }
                            ],
                            "usage": {
                              "prompt_tokens": 541,
                              "completion_tokens": 472,
                              "total_tokens": 1013
                            }
                          }
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
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류 (MCP 서버 연결 실패, AI API 오류 등)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "[오류 메시지]"
                        }
                        """
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "AI에게 보낼 프롬프트 및 리소스 타입",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = """
                    {
                      "prompt": "내 루틴을 조회해서 현재 시각 기준으로 가까운 순으로 루틴을 나열해줘",
                      "resourceType": "practice-routines"
                    }
                    """
            ),
            examples = {
                @ExampleObject(
                    name = "루틴 조회 및 정리",
                    value = """
                        {
                          "prompt": "내 루틴을 조회해서 현재 시각 기준으로 가까운 순으로 루틴을 나열해줘",
                          "resourceType": "practice-routines"
                        }
                        """
                ),
                @ExampleObject(
                    name = "연습 세션 통계",
                    value = """
                        {
                          "prompt": "내 연습 세션 통계를 요약해줘",
                          "resourceType": "practice-sessions"
                        }
                        """
                ),
                @ExampleObject(
                    name = "루틴 분석",
                    value = """
                        {
                          "prompt": "내 루틴 중 가장 많이 연습한 루틴은 뭐야?",
                          "resourceType": "practice-routines"
                        }
                        """
                )
            }
        )
    )
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chatWithAI(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        try {
            String userPrompt = request.get("prompt");
            String resourceType = request.getOrDefault("resourceType", "practice-routines"); // practice-routines 또는 practice-sessions
            
            // 1. MCP 서버에서 리소스 가져오기
            String resourceUrl = mcpServerUrl + "/api/mcp/resources/" + resourceType;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> resourceResponse = restTemplate.exchange(
                resourceUrl, HttpMethod.GET, entity, 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> resourceData = resourceResponse.getBody();
            
            // 2. AI API 호출
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("model", openAiModel);
            aiRequest.put("messages", List.of(
                Map.of("role", "system", 
                    "content", "당신은 기타 연습을 도와주는 AI 어시스턴트입니다. 사용자의 루틴과 연습 데이터를 분석하고 도움을 제공합니다."),
                Map.of("role", "user", 
                    "content", String.format("사용자 요청: %s\n\n데이터: %s", userPrompt, resourceData))
            ));
            
            HttpHeaders aiHeaders = new HttpHeaders();
            aiHeaders.set("Authorization", "Bearer " + openAiApiKey);
            aiHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> aiEntity = new HttpEntity<>(aiRequest, aiHeaders);
            
            ResponseEntity<Map<String, Object>> aiResponse = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                aiEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("aiResponse", aiResponse.getBody());
            result.put("resourceData", resourceData);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("AI 통합 요청 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
        summary = "프롬프트 기반 구글 캘린더 이벤트 생성",
        description = """
            자연어 프롬프트를 통해 구글 캘린더에 루틴 연습 일정을 추가합니다.
            
            **동작 과정:**
            1. 사용자의 자연어 프롬프트를 AI가 분석하여 루틴 ID/제목과 시간을 추출
            2. MCP 서버에서 해당 루틴 정보 조회 및 이벤트 데이터 준비
            3. 프론트엔드에서 사용할 이벤트 데이터 반환
            4. 프론트엔드에서 구글 캘린더 API를 직접 호출하여 이벤트 추가
            
            **지원하는 프롬프트 형식:**
            - 루틴 제목 + 시간: "CEAmG5 루틴을 내일 오후 3시에 구글 캘린더에 추가해줘"
            - 루틴 ID + 시간: "루틴 ID 1을 2025-11-15 15:00에 캘린더에 저장해줘"
            - 상대적 시간: "ㅂㅇㅂ 루틴을 다음주 월요일 오전 10시에 연습 일정으로 추가해줘"
            - 시간 생략: "CEAmG5 루틴을 캘린더에 추가해줘" (현재 시간 기준으로 추가)
            
            **AI 분석 능력:**
            - 루틴 제목을 루틴 ID로 변환
            - 자연어 시간 표현을 ISO 8601 형식으로 변환 ("내일 오후 3시" → "2025-11-15T15:00:00")
            - 시간이 없으면 현재 시간 기준으로 적절한 시간 추론
            
            **응답 구조:**
            - `eventData`: 프론트엔드에서 구글 캘린더 API 호출에 사용할 이벤트 데이터
            - `aiAnalysis`: AI가 분석한 루틴 ID, 제목, 시간 정보
            
            **프론트엔드 처리:**
            - 받은 `eventData`를 구글 캘린더 API에 전달
            - `/api/auth/google/token` 엔드포인트에서 액세스 토큰 조회
            - 구글 캘린더 API: `POST https://www.googleapis.com/calendar/v3/calendars/primary/events`
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "이벤트 데이터 준비 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "success": true,
                          "message": "캘린더 이벤트 데이터가 준비되었습니다. 프론트엔드에서 구글 캘린더 API를 호출하세요.",
                          "aiAnalysis": {
                            "routineId": 1,
                            "routineTitle": "CEAmG5",
                            "startTime": "2025-11-15T15:00:00",
                            "endTime": "2025-11-15T15:30:00"
                          },
                          "eventData": {
                            "summary": "기타 연습: CEAmG5",
                            "description": "루틴 타입: CHORD_CHANGE\\nBPM: 80\\n반복 횟수: 5\\n연습 횟수: 20회",
                            "start": {
                              "dateTime": "2025-11-15T15:00:00+09:00"
                            },
                            "end": {
                              "dateTime": "2025-11-15T15:30:00+09:00"
                            }
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (프롬프트 없음, 루틴을 찾을 수 없음 등)",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "프롬프트 없음",
                        value = """
                            {
                              "error": "prompt is required"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "루틴을 찾을 수 없음",
                        value = """
                            {
                              "error": "루틴을 찾을 수 없습니다. 루틴 ID나 제목을 확인해주세요."
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
            description = "서버 오류 (MCP 서버 연결 실패, AI API 오류 등)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "[오류 메시지]"
                        }
                        """
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "구글 캘린더에 추가할 루틴과 시간을 포함한 자연어 프롬프트",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = """
                    {
                      "prompt": "CEAmG5 루틴을 내일 오후 3시에 구글 캘린더에 추가해줘"
                    }
                    """
            ),
            examples = {
                @ExampleObject(
                    name = "루틴 제목 + 상대적 시간",
                    value = """
                        {
                          "prompt": "CEAmG5 루틴을 내일 오후 3시에 구글 캘린더에 추가해줘"
                        }
                        """
                ),
                @ExampleObject(
                    name = "루틴 ID + 절대 시간",
                    value = """
                        {
                          "prompt": "루틴 ID 1을 2025-11-15 15:00에 캘린더에 저장해줘"
                        }
                        """
                ),
                @ExampleObject(
                    name = "다음주 요일 지정",
                    value = """
                        {
                          "prompt": "ㅂㅇㅂ 루틴을 다음주 월요일 오전 10시에 연습 일정으로 추가해줘"
                        }
                        """
                ),
                @ExampleObject(
                    name = "시간 생략 (현재 시간 기준)",
                    value = """
                        {
                          "prompt": "CEAmG5 루틴을 캘린더에 추가해줘"
                        }
                        """
                )
            }
        )
    )
    @PostMapping("/calendar-prompt")
    public ResponseEntity<Map<String, Object>> addToCalendarByPrompt(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        try {
            String userPrompt = request.get("prompt");
            if (userPrompt == null || userPrompt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "prompt is required"));
            }
            
            // 1. MCP 서버에서 루틴 목록 가져오기
            String resourceUrl = mcpServerUrl + "/api/mcp/resources/practice-routines";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> resourceResponse = restTemplate.exchange(
                resourceUrl, HttpMethod.GET, entity, 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> resourceData = resourceResponse.getBody();
            if (resourceData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "루틴 데이터를 가져올 수 없습니다"));
            }
            
            // 루틴이 없는지 확인
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contents = (List<Map<String, Object>>) resourceData.get("contents");
            boolean hasRoutines = false;
            if (contents != null && !contents.isEmpty()) {
                Map<String, Object> firstContent = contents.get(0);
                String text = (String) firstContent.get("text");
                // 빈 배열이거나 null이 아닌지 확인
                if (text != null && !text.trim().equals("[]") && !text.trim().isEmpty()) {
                    hasRoutines = true;
                }
            }
            
            if (!hasRoutines) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "등록된 루틴이 없습니다.",
                            "message", "먼저 루틴을 생성한 후 캘린더에 추가할 수 있습니다.",
                            "suggestion", "루틴 생성 페이지에서 연습 루틴을 만들어주세요."
                        ));
            }
            
            // 2. AI에게 프롬프트 분석 요청 (루틴 ID와 시간 추출)
            // 현재 시간을 한국 시간대로 가져오기
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            String currentDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String currentYear = String.valueOf(now.getYear());
            
            String systemPrompt = String.format("""
                당신은 사용자의 프롬프트를 분석하여 다음 정보를 JSON 형식으로 추출해야 합니다:
                1. routineId: 루틴 ID (숫자) 또는 루틴 제목 (문자열)
                2. startTime: 시작 시간 (ISO 8601 형식: YYYY-MM-DDTHH:mm:ss, 예: %sT15:00:00)
                3. endTime: 종료 시간 (ISO 8601 형식, 없으면 startTime + 30분)
                
                **중요: 현재 날짜와 시간 정보**
                - 현재 날짜: %s
                - 현재 시간: %s
                - 현재 연도: %s
                
                **시간 추론 규칙:**
                - "내일", "tomorrow" → 현재 날짜 + 1일
                - "다음주", "next week" → 현재 날짜 + 7일
                - "오늘", "today" → 현재 날짜
                - 시간이 명시되지 않으면 오늘 또는 내일의 적절한 시간(예: 오후 3시)을 추론하세요.
                - 연도는 반드시 %s년을 사용하세요. 절대 2023년이나 다른 과거 연도를 사용하지 마세요.
                
                사용 가능한 루틴 데이터를 참고하여 루틴 제목이 주어지면 해당 루틴의 ID를 찾아주세요.
                
                응답은 반드시 다음 JSON 형식으로만 제공하세요 (다른 설명 없이):
                {
                    "routineId": 숫자 또는 null,
                    "routineTitle": 문자열 또는 null,
                    "startTime": "YYYY-MM-DDTHH:mm:ss" 형식 또는 null,
                    "endTime": "YYYY-MM-DDTHH:mm:ss" 형식 또는 null
                }
                """, currentDate, currentDate, currentDateTime, currentYear, currentYear);
            
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("model", openAiModel);
            aiRequest.put("response_format", Map.of("type", "json_object"));
            aiRequest.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", 
                    "content", String.format("사용자 요청: %s\n\n사용 가능한 루틴 데이터: %s", userPrompt, resourceData))
            ));
            
            HttpHeaders aiHeaders = new HttpHeaders();
            aiHeaders.set("Authorization", "Bearer " + openAiApiKey);
            aiHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> aiEntity = new HttpEntity<>(aiRequest, aiHeaders);
            
            ResponseEntity<Map<String, Object>> aiResponse = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                aiEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // 3. AI 응답에서 JSON 추출
            Map<String, Object> aiResponseBody = aiResponse.getBody();
            if (aiResponseBody == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI 응답이 없습니다"));
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) aiResponseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI 응답 형식이 올바르지 않습니다"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            
            // JSON 파싱 (간단한 파싱, 실제로는 Jackson ObjectMapper 사용 권장)
            Long routineId = null;
            String routineTitle = null;
            String startTime = null;
            String endTime = null;
            
            try {
                // content에서 JSON 추출 (간단한 파싱)
                if (content.contains("\"routineId\"")) {
                    String routineIdStr = content.substring(
                        content.indexOf("\"routineId\":") + 12,
                        content.indexOf(",", content.indexOf("\"routineId\":"))
                    ).trim().replaceAll("\"", "");
                    if (!routineIdStr.equals("null")) {
                        routineId = Long.parseLong(routineIdStr);
                    }
                }
                if (content.contains("\"routineTitle\"")) {
                    int startIdx = content.indexOf("\"routineTitle\":") + 15;
                    int endIdx = content.indexOf(",", startIdx);
                    if (endIdx == -1) endIdx = content.indexOf("}", startIdx);
                    routineTitle = content.substring(startIdx, endIdx).trim().replaceAll("\"", "");
                    if (routineTitle.equals("null")) routineTitle = null;
                }
                if (content.contains("\"startTime\"")) {
                    int startIdx = content.indexOf("\"startTime\":") + 12;
                    int endIdx = content.indexOf(",", startIdx);
                    if (endIdx == -1) endIdx = content.indexOf("}", startIdx);
                    startTime = content.substring(startIdx, endIdx).trim().replaceAll("\"", "");
                    if (startTime.equals("null")) startTime = null;
                }
                if (content.contains("\"endTime\"")) {
                    int startIdx = content.indexOf("\"endTime\":") + 10;
                    int endIdx = content.indexOf(",", startIdx);
                    if (endIdx == -1) endIdx = content.indexOf("}", startIdx);
                    endTime = content.substring(startIdx, endIdx).trim().replaceAll("\"", "");
                    if (endTime.equals("null")) endTime = null;
                }
            } catch (Exception e) {
                log.warn("JSON 파싱 실패, content: {}", content);
            }
            
            // 루틴 제목으로 루틴 ID 찾기
            if (routineId == null && routineTitle != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentsList = (List<Map<String, Object>>) resourceData.get("contents");
                if (contentsList != null && !contentsList.isEmpty()) {
                    Map<String, Object> firstContent = contentsList.get(0);
                    String text = (String) firstContent.get("text");
                    // JSON 배열에서 루틴 찾기
                    if (text != null && text.contains(routineTitle)) {
                        // 간단한 파싱 (실제로는 Jackson 사용 권장)
                        int titleIdx = text.indexOf("\"title\":\"" + routineTitle + "\"");
                        if (titleIdx > 0) {
                            int idIdx = text.lastIndexOf("\"id\":", titleIdx);
                            if (idIdx > 0) {
                                int idEndIdx = text.indexOf(",", idIdx);
                                if (idEndIdx == -1) idEndIdx = text.indexOf("}", idIdx);
                                if (idEndIdx > idIdx) {
                                    String idStr = text.substring(idIdx + 5, idEndIdx).trim();
                                    try {
                                        routineId = Long.parseLong(idStr);
                                    } catch (NumberFormatException e) {
                                        log.warn("루틴 ID 파싱 실패: {}", idStr);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (routineId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "루틴을 찾을 수 없습니다. 루틴 ID나 제목을 확인해주세요."));
            }
            
            // 4. MCP 서버의 create-calendar-event 도구 호출
            String toolUrl = mcpServerUrl + "/api/mcp/tools/create-calendar-event";
            HttpHeaders toolHeaders = new HttpHeaders();
            toolHeaders.set("Authorization", authHeader);
            toolHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> toolRequest = new HashMap<>();
            toolRequest.put("routineId", routineId);
            toolRequest.put("startTime", startTime != null ? startTime : "");
            toolRequest.put("endTime", endTime != null ? endTime : "");
            
            HttpEntity<Map<String, Object>> toolEntity = new HttpEntity<>(toolRequest, toolHeaders);
            ResponseEntity<Map<String, Object>> toolResponse = restTemplate.exchange(
                toolUrl, HttpMethod.POST, toolEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            
            Map<String, Object> toolResponseBody = toolResponse.getBody();
            if (toolResponseBody == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "MCP 서버 응답이 비어있습니다."));
            }
            
            // MCP 서버에서 받은 이벤트 데이터 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) toolResponseBody.get("eventData");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "캘린더 이벤트 데이터가 준비되었습니다. 프론트엔드에서 구글 캘린더 API를 호출하세요.");
            result.put("aiAnalysis", Map.of(
                "routineId", routineId,
                "routineTitle", routineTitle != null ? routineTitle : "N/A",
                "startTime", startTime != null ? startTime : "N/A",
                "endTime", endTime != null ? endTime : "N/A"
            ));
            result.put("eventData", eventData); // 프론트엔드에서 구글 캘린더 API 호출에 사용
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("프롬프트 기반 캘린더 추가 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
        summary = "통합 프롬프트 처리 (AI 자동 라우팅)",
        description = """
            사용자의 자연어 프롬프트를 받아 AI가 의도를 분석하고 적절한 액션을 수행합니다.
            
            **자동 라우팅:**
            - AI가 프롬프트를 분석하여 "calendar" 또는 "chat" 액션을 결정
            - "calendar": 구글 캘린더 이벤트 생성 관련 → `/calendar-prompt` 로직 실행
            - "chat": 루틴/세션 조회 및 분석 → `/chat` 로직 실행
            
            **지원하는 프롬프트 예시:**
            
            **캘린더 관련 (자동으로 calendar-prompt 실행):**
            - "CEAmG5 루틴을 내일 오후 3시에 구글 캘린더에 추가해줘"
            - "루틴 ID 1을 캘린더에 저장해줘"
            - "ㅂㅇㅂ 루틴을 다음주 월요일 오전 10시에 연습 일정으로 추가해줘"
            
            **일반 조회/분석 (자동으로 chat 실행):**
            - "내 루틴을 조회해서 현재 시각 기준으로 가까운 순으로 루틴을 나열해줘"
            - "내 루틴 중 가장 많이 연습한 루틴은 뭐야?"
            - "내 연습 세션 통계를 요약해줘"
            - "BPM이 80인 루틴을 추천해줘"
            
            **장점:**
            - 프론트엔드에서 하나의 엔드포인트만 호출하면 됨
            - 사용자가 어떤 프롬프트를 입력하든 자동으로 적절한 처리
            - AI가 프롬프트 의도를 정확히 파악하여 라우팅
            
            **응답 구조:**
            - `action`: 수행된 액션 타입 ("calendar" 또는 "chat")
            - `data`: 액션에 따른 응답 데이터
              - calendar: `eventData` 포함
              - chat: `aiResponse`, `resourceData` 포함
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "프롬프트 처리 성공",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "캘린더 액션",
                        value = """
                            {
                              "action": "calendar",
                              "data": {
                                "success": true,
                                "message": "캘린더 이벤트 데이터가 준비되었습니다...",
                                "aiAnalysis": {
                                  "routineId": 1,
                                  "routineTitle": "CEAmG5",
                                  "startTime": "2025-11-15T15:00:00",
                                  "endTime": "2025-11-15T15:30:00"
                                },
                                "eventData": {
                                  "summary": "기타 연습: CEAmG5",
                                  "description": "...",
                                  "start": { "dateTime": "2025-11-15T15:00:00+09:00" },
                                  "end": { "dateTime": "2025-11-15T15:30:00+09:00" }
                                }
                              }
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "채팅 액션",
                        value = """
                            {
                              "action": "chat",
                              "data": {
                                "resourceData": { ... },
                                "aiResponse": {
                                  "choices": [
                                    {
                                      "message": {
                                        "content": "사용자의 루틴을 시간순으로 정리했습니다..."
                                      }
                                    }
                                  ]
                                }
                              }
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "prompt is required"
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
        ),
        @ApiResponse(
            responseCode = "500",
            description = "서버 오류",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "error": "[오류 메시지]"
                        }
                        """
                )
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "사용자의 자연어 프롬프트 (AI가 자동으로 의도 분석 및 라우팅)",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                type = "object",
                example = """
                    {
                      "prompt": "CEAmG5 루틴을 내일 오후 3시에 구글 캘린더에 추가해줘"
                    }
                    """
            ),
            examples = {
                @ExampleObject(
                    name = "캘린더 추가",
                    value = """
                        {
                          "prompt": "CEAmG5 루틴을 내일 오후 3시에 구글 캘린더에 추가해줘"
                        }
                        """
                ),
                @ExampleObject(
                    name = "루틴 조회",
                    value = """
                        {
                          "prompt": "내 루틴을 조회해서 현재 시각 기준으로 가까운 순으로 루틴을 나열해줘"
                        }
                        """
                ),
                @ExampleObject(
                    name = "루틴 분석",
                    value = """
                        {
                          "prompt": "내 루틴 중 가장 많이 연습한 루틴은 뭐야?"
                        }
                        """
                )
            }
        )
    )
    @PostMapping("/prompt")
    public ResponseEntity<Map<String, Object>> handlePrompt(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        
        try {
            String userPrompt = request.get("prompt");
            if (userPrompt == null || userPrompt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "prompt is required"));
            }
            
            // 1. AI에게 프롬프트 의도 분석 요청
            String intentAnalysisPrompt = """
                사용자의 프롬프트를 분석하여 다음 중 하나의 액션을 결정하세요:
                - "calendar": 구글 캘린더에 이벤트를 추가하는 요청 (예: "캘린더에 추가", "일정 저장", "캘린더에 저장" 등)
                - "chat": 루틴이나 연습 세션을 조회하거나 분석하는 요청 (예: "루틴 조회", "통계", "분석", "추천" 등)
                
                응답은 반드시 다음 JSON 형식으로만 제공하세요:
                {
                    "action": "calendar" 또는 "chat",
                    "reason": "액션을 선택한 이유 (한 문장)"
                }
                """;
            
            Map<String, Object> intentRequest = new HashMap<>();
            intentRequest.put("model", openAiModel);
            intentRequest.put("response_format", Map.of("type", "json_object"));
            intentRequest.put("messages", List.of(
                Map.of("role", "system", "content", intentAnalysisPrompt),
                Map.of("role", "user", "content", "사용자 프롬프트: " + userPrompt)
            ));
            
            HttpHeaders aiHeaders = new HttpHeaders();
            aiHeaders.set("Authorization", "Bearer " + openAiApiKey);
            aiHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> intentEntity = new HttpEntity<>(intentRequest, aiHeaders);
            
            ResponseEntity<Map<String, Object>> intentResponse = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                intentEntity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            // 2. AI 응답에서 액션 추출
            Map<String, Object> intentResponseBody = intentResponse.getBody();
            if (intentResponseBody == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI 의도 분석 응답이 없습니다"));
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) intentResponseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "AI 응답 형식이 올바르지 않습니다"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String intentContent = (String) message.get("content");
            
            // JSON 파싱 (간단한 파싱)
            String action = "chat"; // 기본값
            try {
                if (intentContent.contains("\"action\"")) {
                    int actionStartIdx = intentContent.indexOf("\"action\":") + 9;
                    int actionEndIdx = intentContent.indexOf(",", actionStartIdx);
                    if (actionEndIdx == -1) actionEndIdx = intentContent.indexOf("}", actionStartIdx);
                    String actionStr = intentContent.substring(actionStartIdx, actionEndIdx).trim().replaceAll("\"", "");
                    if (actionStr.equals("calendar") || actionStr.equals("chat")) {
                        action = actionStr;
                    }
                }
            } catch (Exception e) {
                log.warn("의도 분석 JSON 파싱 실패, 기본값(chat) 사용: {}", intentContent);
            }
            
            log.info("프롬프트 의도 분석 결과: action={}, prompt={}", action, userPrompt);
            
            // 3. 액션에 따라 적절한 로직 실행
            Map<String, Object> result = new HashMap<>();
            result.put("action", action);
            
            if ("calendar".equals(action)) {
                // calendar-prompt 로직 실행
                ResponseEntity<Map<String, Object>> calendarResponse = addToCalendarByPrompt(authHeader, request);
                result.put("data", calendarResponse.getBody());
                return ResponseEntity.status(calendarResponse.getStatusCode())
                        .body(result);
            } else {
                // chat 로직 실행
                String resourceType = request.getOrDefault("resourceType", "practice-routines");
                Map<String, String> chatRequest = new HashMap<>();
                chatRequest.put("prompt", userPrompt);
                chatRequest.put("resourceType", resourceType);
                
                ResponseEntity<Map<String, Object>> chatResponse = chatWithAI(authHeader, chatRequest);
                result.put("data", chatResponse.getBody());
                return ResponseEntity.status(chatResponse.getStatusCode())
                        .body(result);
            }
            
        } catch (Exception e) {
            log.error("통합 프롬프트 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

