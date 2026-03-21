package dailyGuitar.capstone.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dailyGuitar.capstone.mcp.entity.PracticeRoutine;
import dailyGuitar.capstone.mcp.entity.PracticeSession;
import dailyGuitar.capstone.mcp.security.MCPJwtTokenService;
import dailyGuitar.capstone.mcp.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP 프로토콜 핸들러
 * stdio를 통해 JSON-RPC 메시지를 주고받습니다.
 */
@Component
@Slf4j
public class MCPProtocolHandler {
    
    private final MCPService mcpService;
    private final GoogleCalendarService googleCalendarService;
    private final MCPJwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final PrintWriter writer = new PrintWriter(System.out, true);
    
    // 생성자에서 ObjectMapper 주입 (Spring 빈 사용)
    public MCPProtocolHandler(MCPService mcpService, 
                              GoogleCalendarService googleCalendarService,
                              MCPJwtTokenService jwtTokenService,
                              ObjectMapper objectMapper) {
        this.mcpService = mcpService;
        this.googleCalendarService = googleCalendarService;
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * MCP 서버 실행
     */
    public void run() {
        log.info("MCP 서버 시작 - stdio 모드");
        
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                try {
                    JsonNode request = objectMapper.readTree(line);
                    JsonNode response = handleRequest(request);
                    
                    if (response != null) {
                        writer.println(objectMapper.writeValueAsString(response));
                    }
                } catch (Exception e) {
                    log.error("요청 처리 중 오류 발생", e);
                    sendError(null, -32603, "Internal error", e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("입력 읽기 중 오류 발생", e);
        }
    }
    
    /**
     * 요청 처리 (HTTP 컨트롤러에서도 사용 가능하도록 public으로 변경)
     */
    public JsonNode handleRequest(JsonNode request) {
        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        String id = request.path("id").asText();
        
        log.debug("MCP 요청 수신: method={}, id={}", method, id);
        
        return switch (method) {
            case "initialize" -> handleInitialize(id, params);
            case "resources/list" -> handleResourcesList(id);
            case "resources/read" -> handleResourceRead(id, params);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolCall(id, params);
            default -> sendError(id, -32601, "Method not found", "Unknown method: " + method);
        };
    }
    
    /**
     * 서버 초기화
     */
    private JsonNode handleInitialize(String id, JsonNode params) {
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
            "resources", Map.of(),
            "tools", Map.of()
        ));
        result.put("serverInfo", Map.of(
            "name", "daily-guitar-mcp-server",
            "version", "1.0.0"
        ));
        
        return createResponse(id, result);
    }
    
    /**
     * 리소스 목록 조회
     */
    private JsonNode handleResourcesList(String id) {
        List<Map<String, Object>> resources = List.of(
            Map.of(
                "uri", "practice-routines",
                "name", "Practice Routines",
                "description", "사용자의 연습 루틴 목록",
                "mimeType", "application/json"
            ),
            Map.of(
                "uri", "practice-sessions",
                "name", "Practice Sessions",
                "description", "사용자의 연습 세션 기록",
                "mimeType", "application/json"
            )
        );
        
        return createResponse(id, Map.of("resources", resources));
    }
    
    /**
     * 리소스 읽기
     */
    private JsonNode handleResourceRead(String id, JsonNode params) {
        String uri = params.path("uri").asText();
        String token = params.path("token").asText(); // JWT 토큰
        
        if (token == null || token.isEmpty()) {
            return sendError(id, -32602, "Invalid params", "token is required");
        }
        
        try {
            // JWT 토큰 검증 및 userId 추출
            if (!jwtTokenService.isTokenValid(token)) {
                return sendError(id, -32001, "Unauthorized", "Invalid or expired token");
            }
            
            Long userId = jwtTokenService.extractUserId(token);
            
            String content = switch (uri) {
                case "practice-routines" -> readPracticeRoutines(userId);
                case "practice-sessions" -> readPracticeSessions(userId);
                default -> null;
            };
            
            if (content == null) {
                return sendError(id, -32602, "Invalid params", "Unknown resource: " + uri);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", content
            )));
            
            return createResponse(id, result);
        } catch (IllegalStateException e) {
            return sendError(id, -32001, "Unauthorized", e.getMessage());
        } catch (Exception e) {
            log.error("리소스 읽기 중 오류 발생", e);
            return sendError(id, -32603, "Internal error", e.getMessage());
        }
    }
    
    /**
     * 도구 목록 조회
     */
    private JsonNode handleToolsList(String id) {
        List<Map<String, Object>> tools = List.of(
            Map.of(
                "name", "create-calendar-event",
                "description", "구글 캘린더에 연습 일정을 추가합니다",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "token", Map.of("type", "string", "description", "JWT 인증 토큰"),
                        "routineId", Map.of("type", "number", "description", "루틴 ID"),
                        "startTime", Map.of("type", "string", "description", "시작 시간 (ISO 8601)"),
                        "endTime", Map.of("type", "string", "description", "종료 시간 (ISO 8601)")
                    ),
                    "required", List.of("token", "routineId")
                )
            ),
            Map.of(
                "name", "recommend-song",
                "description", "사용자의 루틴 실력을 기반으로 곡을 추천합니다",
                "inputSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "token", Map.of("type", "string", "description", "JWT 인증 토큰"),
                        "genre", Map.of("type", "string", "description", "장르 (예: rock, pop, jazz, blues 등)"),
                        "bpm", Map.of("type", "number", "description", "원하는 BPM (선택사항)")
                    ),
                    "required", List.of("token", "genre")
                )
            )
        );
        
        return createResponse(id, Map.of("tools", tools));
    }
    
    /**
     * 도구 호출
     */
    private JsonNode handleToolCall(String id, JsonNode params) {
        String name = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        
        log.debug("도구 호출: name={}, arguments={}", name, arguments);
        
        return switch (name) {
            case "create-calendar-event" -> {
                log.debug("create-calendar-event 도구 호출");
                yield handleCreateCalendarEvent(id, arguments);
            }
            case "recommend-song" -> {
                log.debug("recommend-song 도구 호출");
                yield handleRecommendSong(id, arguments);
            }
            default -> {
                log.error("알 수 없는 도구: {}", name);
                yield sendError(id, -32601, "Method not found", "Unknown tool: " + name);
            }
        };
    }
    
    /**
     * 캘린더 이벤트 생성 처리
     */
    private JsonNode handleCreateCalendarEvent(String id, JsonNode arguments) {
        try {
            log.debug("캘린더 이벤트 생성 시작");
            String token = arguments.path("token").asText(); // JWT 토큰
            
            if (token == null || token.isEmpty()) {
                log.error("토큰이 없습니다");
                return sendError(id, -32602, "Invalid params", "token is required");
            }
            
            // JWT 토큰 검증 및 userId 추출
            if (!jwtTokenService.isTokenValid(token)) {
                log.error("토큰 검증 실패");
                return sendError(id, -32001, "Unauthorized", "Invalid or expired token");
            }
            
            Long userId = jwtTokenService.extractUserId(token);
            log.debug("사용자 ID: {}", userId);
            
            Long routineId = arguments.path("routineId").asLong();
            log.debug("루틴 ID: {}", routineId);
            
            String startTimeStr = arguments.path("startTime").asText();
            String endTimeStr = arguments.path("endTime").asText();
            log.debug("시작 시간: {}, 종료 시간: {}", startTimeStr, endTimeStr);
            
            // 루틴 조회 및 권한 확인 (본인의 루틴인지)
            log.debug("루틴 조회 시작: routineId={}", routineId);
            PracticeRoutine routine = mcpService.getRoutineById(routineId)
                    .orElseThrow(() -> {
                        log.error("루틴을 찾을 수 없습니다: {}", routineId);
                        return new IllegalArgumentException("루틴을 찾을 수 없습니다: " + routineId);
                    });
            
            log.debug("루틴 조회 성공: userId={}, routine.userId={}", userId, routine.getUserId());
            
            if (!routine.getUserId().equals(userId)) {
                log.error("권한 없음: 사용자 {}가 루틴 {}에 접근 시도", userId, routineId);
                return sendError(id, -32003, "Forbidden", "해당 루틴에 대한 권한이 없습니다");
            }
            
            // 시간 파싱
            log.debug("시간 파싱 시작");
            LocalDateTime startTime = startTimeStr != null && !startTimeStr.isEmpty()
                ? LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now();
            LocalDateTime endTime = endTimeStr != null && !endTimeStr.isEmpty()
                ? LocalDateTime.parse(endTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : startTime.plusMinutes(30);
            log.debug("파싱된 시간: startTime={}, endTime={}", startTime, endTime);
            
            // 이벤트 제목 및 설명 생성
            String title = "기타 연습: " + routine.getTitle();
            String description = String.format(
                "루틴 타입: %s\nBPM: %d\n반복 횟수: %d\n연습 횟수: %d회",
                routine.getRoutineType(),
                routine.getBpm(),
                routine.getRepeats(),
                routine.getPracticeCount()
            );
            log.debug("이벤트 정보: title={}, description={}", title, description);
            
            // ISO 8601 형식으로 시간 변환 (프론트엔드에서 사용할 형식)
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime startZoned = startTime.atZone(zoneId);
            ZonedDateTime endZoned = endTime.atZone(zoneId);
            
            // 구글 캘린더 API 형식으로 이벤트 데이터 준비 (프론트엔드에서 사용)
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("summary", title);
            eventData.put("description", description);
            
            Map<String, String> start = new HashMap<>();
            start.put("dateTime", startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            eventData.put("start", start);
            
            Map<String, String> end = new HashMap<>();
            end.put("dateTime", endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            eventData.put("end", end);
            
            log.debug("이벤트 데이터 준비 완료: {}", eventData);
            
            // 이벤트 데이터만 반환 (프론트엔드에서 구글 캘린더 API 호출)
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "캘린더 이벤트 데이터가 준비되었습니다");
            result.put("routineId", routineId);
            result.put("eventData", eventData); // 프론트엔드에서 사용할 이벤트 데이터
            
            log.debug("캘린더 이벤트 데이터 준비 성공");
            return createResponse(id, result);
        } catch (IllegalStateException e) {
            log.error("IllegalStateException 발생: {}", e.getMessage(), e);
            return sendError(id, -32001, "Unauthorized", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException 발생: {}", e.getMessage(), e);
            return sendError(id, -32602, "Invalid params", e.getMessage());
        } catch (Exception e) {
            log.error("캘린더 이벤트 생성 중 오류 발생", e);
            return sendError(id, -32603, "Internal error", e.getMessage());
        }
    }
    
    /**
     * 곡 추천 처리
     */
    private JsonNode handleRecommendSong(String id, JsonNode arguments) {
        try {
            log.debug("곡 추천 시작");
            String token = arguments.path("token").asText();
            
            if (token == null || token.isEmpty()) {
                log.error("토큰이 없습니다");
                return sendError(id, -32602, "Invalid params", "token is required");
            }
            
            // JWT 토큰 검증 및 userId 추출
            if (!jwtTokenService.isTokenValid(token)) {
                log.error("토큰 검증 실패");
                return sendError(id, -32001, "Unauthorized", "Invalid or expired token");
            }
            
            Long userId = jwtTokenService.extractUserId(token);
            log.debug("사용자 ID: {}", userId);
            
            String genre = arguments.path("genre").asText();
            if (genre == null || genre.isEmpty()) {
                return sendError(id, -32602, "Invalid params", "genre is required");
            }
            
            Integer bpm = arguments.path("bpm").isNull() ? null : arguments.path("bpm").asInt();
            log.debug("장르: {}, BPM: {}", genre, bpm);
            
            // 사용자의 루틴 및 세션 데이터 조회
            List<PracticeRoutine> routines = mcpService.getRoutinesByUserId(userId);
            List<PracticeSession> sessions = mcpService.getSessionsByUserId(userId);
            
            // 잘 하는 루틴 파악 (평균 정확도가 높은 루틴)
            Map<Long, Double> routineAvgAccuracy = new HashMap<>();
            for (PracticeRoutine routine : routines) {
                List<PracticeSession> routineSessions = sessions.stream()
                        .filter(s -> s.getRoutineId().equals(routine.getId()))
                        .collect(Collectors.toList());
                
                if (!routineSessions.isEmpty()) {
                    double avgAccuracy = routineSessions.stream()
                            .mapToDouble(s -> (s.getRhythmAccuracy() + s.getPitchAccuracy()) / 2.0)
                            .average()
                            .orElse(0.0);
                    routineAvgAccuracy.put(routine.getId(), avgAccuracy);
                }
            }
            
            // 가장 잘 하는 루틴 찾기
            Optional<Map.Entry<Long, Double>> bestRoutine = routineAvgAccuracy.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            
            // 사용자 실력 정보 수집
            Map<String, Object> userSkillInfo = new HashMap<>();
            if (bestRoutine.isPresent()) {
                Long bestRoutineId = bestRoutine.get().getKey();
                Double bestAccuracy = bestRoutine.get().getValue();
                PracticeRoutine bestRoutineEntity = routines.stream()
                        .filter(r -> r.getId().equals(bestRoutineId))
                        .findFirst()
                        .orElse(null);
                
                if (bestRoutineEntity != null) {
                    userSkillInfo.put("bestRoutineBpm", bestRoutineEntity.getBpm());
                    userSkillInfo.put("bestRoutineType", bestRoutineEntity.getRoutineType().toString());
                    userSkillInfo.put("bestAccuracy", bestAccuracy);
                    userSkillInfo.put("practiceCount", bestRoutineEntity.getPracticeCount());
                }
            }
            
            // 전체 평균 정확도 계산
            if (!sessions.isEmpty()) {
                double overallAvgAccuracy = sessions.stream()
                        .mapToDouble(s -> (s.getRhythmAccuracy() + s.getPitchAccuracy()) / 2.0)
                        .average()
                        .orElse(0.0);
                userSkillInfo.put("overallAvgAccuracy", overallAvgAccuracy);
            }
            
            // 사용자 실력 정보 반환 (백엔드에서 OpenAI API 호출에 사용)
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("genre", genre);
            result.put("requestedBpm", bpm);
            result.put("userSkillInfo", userSkillInfo);
            result.put("routines", routines.stream()
                    .map(r -> {
                        Map<String, Object> rMap = new HashMap<>();
                        rMap.put("id", r.getId());
                        rMap.put("title", r.getTitle());
                        rMap.put("bpm", r.getBpm());
                        rMap.put("routineType", r.getRoutineType().toString());
                        rMap.put("practiceCount", r.getPracticeCount());
                        rMap.put("avgAccuracy", routineAvgAccuracy.getOrDefault(r.getId(), 0.0));
                        return rMap;
                    })
                    .collect(Collectors.toList()));
            
            log.debug("사용자 실력 정보 수집 완료: {}", result);
            return createResponse(id, result);
        } catch (Exception e) {
            log.error("곡 추천 중 오류 발생", e);
            return sendError(id, -32603, "Internal error", e.getMessage());
        }
    }
    
    /**
     * 루틴 목록 읽기
     */
    private String readPracticeRoutines(Long userId) throws Exception {
        List<PracticeRoutine> routines = mcpService.getRoutinesByUserId(userId);
        return objectMapper.writeValueAsString(routines);
    }
    
    /**
     * 세션 목록 읽기
     */
    private String readPracticeSessions(Long userId) throws Exception {
        List<PracticeSession> sessions = mcpService.getSessionsByUserId(userId);
        return objectMapper.writeValueAsString(sessions);
    }
    
    /**
     * 성공 응답 생성
     */
    private JsonNode createResponse(String id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return objectMapper.valueToTree(response);
    }
    
    /**
     * 에러 응답 생성
     */
    private JsonNode sendError(String id, int code, String message, String data) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", data);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", error);
        
        return objectMapper.valueToTree(response);
    }
}

