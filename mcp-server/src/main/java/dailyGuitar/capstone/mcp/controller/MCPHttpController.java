package dailyGuitar.capstone.mcp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dailyGuitar.capstone.mcp.server.MCPProtocolHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP HTTP API 컨트롤러
 * 프론트엔드에서 호출할 수 있는 REST API 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
@Slf4j
public class MCPHttpController {
    
    private final MCPProtocolHandler mcpProtocolHandler;
    private final ObjectMapper objectMapper;
    
    /**
     * 리소스 목록 조회
     */
    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> listResources(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            JsonNode request = createJsonRpcRequest("resources/list", Map.of(), generateId());
            JsonNode response = mcpProtocolHandler.handleRequest(request);
            return ResponseEntity.ok(objectMapper.convertValue(response.path("result"), Map.class));
        } catch (Exception e) {
            log.error("리소스 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 리소스 읽기
     */
    @GetMapping("/resources/{uri}")
    public ResponseEntity<Map<String, Object>> readResource(
            @PathVariable String uri,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            Map<String, Object> params = new HashMap<>();
            params.put("uri", uri);
            params.put("token", token);
            
            JsonNode request = createJsonRpcRequest("resources/read", params, generateId());
            JsonNode response = mcpProtocolHandler.handleRequest(request);
            
            if (response.has("error")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(objectMapper.convertValue(response.path("error"), Map.class));
            }
            
            return ResponseEntity.ok(objectMapper.convertValue(response.path("result"), Map.class));
        } catch (Exception e) {
            log.error("리소스 읽기 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 도구 목록 조회
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            JsonNode request = createJsonRpcRequest("tools/list", Map.of(), generateId());
            JsonNode response = mcpProtocolHandler.handleRequest(request);
            return ResponseEntity.ok(objectMapper.convertValue(response.path("result"), Map.class));
        } catch (Exception e) {
            log.error("도구 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 도구 호출 (구글 캘린더 이벤트 생성 등)
     */
    @PostMapping("/tools/{toolName}")
    public ResponseEntity<Map<String, Object>> callTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            
            // arguments에 token 추가
            Map<String, Object> params = new HashMap<>(arguments);
            params.put("token", token);
            
            Map<String, Object> toolParams = new HashMap<>();
            toolParams.put("name", toolName);
            toolParams.put("arguments", params);
            
            JsonNode request = createJsonRpcRequest("tools/call", toolParams, generateId());
            JsonNode response = mcpProtocolHandler.handleRequest(request);
            
            if (response.has("error")) {
                JsonNode error = response.path("error");
                int code = error.path("code").asInt();
                HttpStatus status = code == -32001 || code == -32003 
                    ? HttpStatus.UNAUTHORIZED 
                    : HttpStatus.BAD_REQUEST;
                return ResponseEntity.status(status)
                        .body(objectMapper.convertValue(error, Map.class));
            }
            
            return ResponseEntity.ok(objectMapper.convertValue(response.path("result"), Map.class));
        } catch (Exception e) {
            log.error("도구 호출 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Authorization 헤더에서 토큰 추출
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("Invalid Authorization header: {}", authHeader != null ? authHeader.substring(0, Math.min(20, authHeader.length())) : "null");
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String token = authHeader.substring(7);
        log.debug("토큰 추출 성공: {}", token.substring(0, Math.min(20, token.length())) + "...");
        return token;
    }
    
    /**
     * JSON-RPC 요청 생성
     */
    private JsonNode createJsonRpcRequest(String method, Object params, String id) {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", method);
        request.put("params", params);
        request.put("id", id);
        return objectMapper.valueToTree(request);
    }
    
    /**
     * 요청 ID 생성
     */
    private String generateId() {
        return String.valueOf(System.currentTimeMillis());
    }
}

