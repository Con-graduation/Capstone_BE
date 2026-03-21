package dailyGuitar.capstone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@Tag(name = "도구", description = "메트로놈, 튜너 등 연습 도구 API (비회원 접근 가능)")
public class ToolsController {

    @Operation(summary = "메트로놈 프리셋 조회", description = "사용 가능한 메트로놈 프리셋을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/metronome/presets")
    public ResponseEntity<Map<String, Object>> getMetronomePresets() {
        // 기본 메트로놈 프리셋 반환
        Map<String, Object> presets = new HashMap<>();
        presets.put("default", Map.of(
                "bpm", 120,
                "timeSignature", "4/4",
                "soundType", "click"
        ));
        return ResponseEntity.ok(presets);
    }

    @Operation(summary = "메트로놈 시작", description = "메트로놈을 시작합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메트로놈 시작 성공")
    })
    @PostMapping("/metronome/start")
    public ResponseEntity<Map<String, String>> startMetronome(@RequestBody @Parameter(description = "메트로놈 설정") Map<String, Object> settings) {
        // 메트로놈 시작 로직 (프론트엔드에서 Web Audio API로 처리)
        Map<String, String> response = new HashMap<>();
        response.put("status", "started");
        response.put("message", "메트로놈이 시작되었습니다.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "메트로놈 정지", description = "메트로놈을 정지합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "메트로놈 정지 성공")
    })
    @PostMapping("/metronome/stop")
    public ResponseEntity<Map<String, String>> stopMetronome() {
        // 메트로놈 정지 로직
        Map<String, String> response = new HashMap<>();
        response.put("status", "stopped");
        response.put("message", "메트로놈이 정지되었습니다.");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "튜너 프리셋 조회", description = "사용 가능한 튜너 프리셋을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/tuner/presets")
    public ResponseEntity<Map<String, Object>> getTunerPresets() {
        // 기본 튜너 프리셋 반환
        Map<String, Object> presets = new HashMap<>();
        presets.put("standard", Map.of(
                "tuning", "EADGBE",
                "referenceFrequency", 440.0,
                "strings", new String[]{"E", "A", "D", "G", "B", "E"}
        ));
        return ResponseEntity.ok(presets);
    }

    @Operation(summary = "음정 분석", description = "오디오 데이터를 분석하여 음정을 감지합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 성공")
    })
    @PostMapping("/tuner/analyze")
    public ResponseEntity<Map<String, Object>> analyzePitch(@RequestBody @Parameter(description = "오디오 데이터") Map<String, Object> audioData) {
        // 음정 분석 로직 (실제로는 Python 스크립트와 연동)
        Map<String, Object> response = new HashMap<>();
        response.put("detectedNote", "A");
        response.put("frequency", 440.0);
        response.put("accuracy", 100);
        response.put("message", "음정 분석이 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
}

