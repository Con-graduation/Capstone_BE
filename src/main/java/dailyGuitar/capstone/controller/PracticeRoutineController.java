package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.practice.PracticeRoutineCreateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineResponseDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineUpdateRequestDto;
import dailyGuitar.capstone.service.PracticeRoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/routines")
public class PracticeRoutineController {
	private final PracticeRoutineService practiceRoutineService;

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
}
