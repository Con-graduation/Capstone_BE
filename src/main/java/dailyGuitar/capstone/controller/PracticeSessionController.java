package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.PracticeSessionCreateDto;
import dailyGuitar.capstone.dto.PracticeSessionResponseDto;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.service.PracticeSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Tag(name = "연습 세션", description = "연습 세션 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;

    @Operation(summary = "연습 세션 생성", description = "새로운 연습 세션을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<PracticeSessionResponseDto> createSession(
            @Valid @RequestBody PracticeSessionCreateDto createDto,
            @AuthenticationPrincipal User user) {
        PracticeSessionResponseDto session = practiceSessionService.createSession(createDto, user);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "연습 세션 목록 조회", description = "사용자의 연습 세션 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ResponseEntity<Page<PracticeSessionResponseDto>> getUserSessions(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) @Parameter(description = "페이징 정보") Pageable pageable) {
        Page<PracticeSessionResponseDto> sessions = practiceSessionService.getUserSessions(user, pageable);
        return ResponseEntity.ok(sessions);
    }

    @Operation(summary = "연습 세션 상세 조회", description = "특정 연습 세션의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/{sessionId}")
    public ResponseEntity<PracticeSessionResponseDto> getSession(
            @PathVariable @Parameter(description = "세션 ID") Long sessionId,
            @AuthenticationPrincipal User user) {
        PracticeSessionResponseDto session = practiceSessionService.getSessionById(sessionId, user);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "연습 세션 시작", description = "연습 세션을 시작합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 시작 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<PracticeSessionResponseDto> startSession(
            @PathVariable @Parameter(description = "세션 ID") Long sessionId,
            @AuthenticationPrincipal User user) {
        PracticeSessionResponseDto session = practiceSessionService.startSession(sessionId, user);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "연습 세션 완료", description = "연습 세션을 완료하고 결과를 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 완료 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<PracticeSessionResponseDto> completeSession(
            @PathVariable @Parameter(description = "세션 ID") Long sessionId,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @Parameter(description = "실제 BPM") Integer actualBpm,
            @RequestParam(required = false) @Parameter(description = "정확도 점수 (0-100)") Double accuracyScore,
            @RequestParam(required = false) @Parameter(description = "타이밍 점수 (0-100)") Double timingScore,
            @RequestParam(required = false) @Parameter(description = "음정 점수 (0-100)") Double pitchScore,
            @RequestParam(required = false) @Parameter(description = "종합 점수 (0-100)") Double overallScore,
            @RequestParam(required = false) @Parameter(description = "연습 시간(초)") Integer durationSeconds) {
        PracticeSessionResponseDto session = practiceSessionService.completeSession(
                sessionId, user, actualBpm, accuracyScore, timingScore, pitchScore, overallScore, durationSeconds);
        return ResponseEntity.ok(session);
    }

    @Operation(summary = "연습 세션 취소", description = "진행 중인 연습 세션을 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "세션 취소 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<PracticeSessionResponseDto> cancelSession(
            @PathVariable @Parameter(description = "세션 ID") Long sessionId,
            @AuthenticationPrincipal User user) {
        PracticeSessionResponseDto session = practiceSessionService.cancelSession(sessionId, user);
        return ResponseEntity.ok(session);
    }
}

