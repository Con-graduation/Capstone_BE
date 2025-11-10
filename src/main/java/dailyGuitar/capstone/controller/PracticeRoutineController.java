package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.practice.PracticeRoutineCreateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineResponseDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineUpdateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeReportResponseDto;
import dailyGuitar.capstone.service.PracticeRoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/routines")
public class PracticeRoutineController {
	private final PracticeRoutineService practiceRoutineService;
    private static final Logger log = LoggerFactory.getLogger(PracticeRoutineController.class);

	public PracticeRoutineController(PracticeRoutineService practiceRoutineService) {
		this.practiceRoutineService = practiceRoutineService;
	}

	@Operation(summary = "연습 루틴 생성", description = "루틴 제목·유형·순서·반복수·BPM으로 새 루틴을 생성합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "생성 성공",
					content = @Content(schema = @Schema(implementation = PracticeRoutineResponseDto.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요"),
			@ApiResponse(responseCode = "400", description = "유효성 검사 실패")
	})
	@PostMapping
	public ResponseEntity<PracticeRoutineResponseDto> create(
			@Valid @RequestBody PracticeRoutineCreateRequestDto req) {
		PracticeRoutineResponseDto created = practiceRoutineService.create(req);
		return ResponseEntity.created(URI.create("/api/routines/" + created.getId())).body(created);
	}

	@Operation(summary = "연습 루틴 수정", description = "Path의 id에 해당하는 루틴의 제목/순서/반복수/BPM을 수정합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "수정 성공",
					content = @Content(schema = @Schema(implementation = PracticeRoutineResponseDto.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요"),
			@ApiResponse(responseCode = "404", description = "루틴 없음")
	})
	@PatchMapping("/{id}")
	public ResponseEntity<PracticeRoutineResponseDto> update(
			@PathVariable Long id,
			@Valid @RequestBody PracticeRoutineUpdateRequestDto req) {
		return ResponseEntity.ok(practiceRoutineService.update(id, req));
	}

	@Operation(summary = "연습 루틴 상세", description = "Path의 id에 해당하는 루틴 정보를 반환합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = PracticeRoutineResponseDto.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요"),
			@ApiResponse(responseCode = "404", description = "루틴 없음")
	})
	@GetMapping("/{id}")
	public ResponseEntity<PracticeRoutineResponseDto> get(@PathVariable Long id) {
		return ResponseEntity.ok(practiceRoutineService.getById(id));
	}

	@Operation(summary = "내 루틴 목록", description = "현재 사용자 소유의 모든 루틴을 반환합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = PracticeRoutineResponseDto.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@GetMapping
	public ResponseEntity<List<PracticeRoutineResponseDto>> listMine() {
		return ResponseEntity.ok(practiceRoutineService.listMine());
	}

	@Operation(summary = "연습 루틴 삭제", description = "Path의 id에 해당하는 루틴을 삭제합니다.")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "삭제 성공"),
			@ApiResponse(responseCode = "404", description = "루틴 없음"),
			@ApiResponse(responseCode = "401", description = "인증 필요")
	})
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		return practiceRoutineService.delete(id)
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}

	@Operation(summary = "연습 완료", description = "녹음된 WAV 파일과 루틴 정보를 받아 연습을 완료 처리합니다.")
	@ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PracticeReportResponseDto.class),
                            examples = @ExampleObject(value = "{\n  \"rhythmAccuracy\": 92,\n  \"rhythmFeedback\": \"안정적인 템포를 유지했어요. 후반에 약간 흔들림이 있었어요.\",\n  \"rhythmHistory\": [\n    { \"accuracy\": 85, \"practicedAt\": \"2025-10-20T21:03:00Z\" },\n    { \"accuracy\": 88, \"practicedAt\": \"2025-10-22T20:55:00Z\" }\n  ],\n  \"rhythmComparison\": \"직전 연습보다 5% 향상\",\n  \"rhythmWorstSection\": \"초반에서 연주가 가장 불안정했어요\",\n  \"rhythmBpmAdvice\": \"다음 연습은 BPM을 95로 낮춰보세요\",\n  \"pitchAccuracy\": 88,\n  \"pitchFeedback\": \"코드 전환 시 일부 음정이 낮게 들립니다. 손가락 각도를 조정해 보세요.\",\n  \"pitchHistory\": [\n    { \"accuracy\": 80, \"practicedAt\": \"2025-10-20T21:03:00Z\" },\n    { \"accuracy\": 85, \"practicedAt\": \"2025-10-22T20:55:00Z\" }\n  ],\n  \"pitchComparison\": \"직전 연습보다 3% 향상\",\n  \"pitchWorstSection\": \"중반의 코드 전환 구간에서 음정 편차가 컸어요\",\n  \"pitchDifficultyAdvice\": \"A–D 전환이 특히 어렵습니다. 왼손 포지션 연습을 추천해요.\",\n  \"overallFeedback\": \"리듬은 안정적이지만 일부 코드 전환에서 잡음이 발생했어요. 다음 연습에서 느린 템포로 전환 구간을 집중하세요.\"\n}"))),
			@ApiResponse(responseCode = "404", description = "루틴 없음"),
			@ApiResponse(responseCode = "401", description = "인증 필요"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청")
	})
	@PostMapping(value = "/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PracticeReportResponseDto> complete(
			@RequestParam @NotNull Long routineId,
			@RequestParam @NotNull MultipartFile audioFile) {
        if (audioFile == null) {
            log.info("[complete] routineId={}, audioFile=null", routineId);
        } else {
            log.info("[complete] routineId={}, audioFile name={}, size={}, contentType={}",
                    routineId,
                    audioFile.getOriginalFilename(),
                    audioFile.getSize(),
                    audioFile.getContentType());
        }
		PracticeReportResponseDto report = practiceRoutineService.complete(routineId, audioFile);
		return ResponseEntity.ok(report);
	}
}
