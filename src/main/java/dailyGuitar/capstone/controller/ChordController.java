package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.ChordResponseDto;
import dailyGuitar.capstone.entity.Chord;
import dailyGuitar.capstone.service.ChordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chords")
@RequiredArgsConstructor
@Tag(name = "코드 도감", description = "기타 코드 정보 조회 API (비회원 접근 가능)")
public class ChordController {

    private final ChordService chordService;

    @Operation(summary = "코드 목록 조회 (페이징)", description = "모든 활성 코드를 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<Page<ChordResponseDto>> getAllChords(
            @PageableDefault(size = 20) @Parameter(description = "페이징 정보") Pageable pageable) {
        Page<ChordResponseDto> chords = chordService.getAllChords(pageable);
        return ResponseEntity.ok(chords);
    }

    @Operation(summary = "모든 코드 조회", description = "모든 활성 코드를 목록으로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/all")
    public ResponseEntity<List<ChordResponseDto>> getAllActiveChords() {
        List<ChordResponseDto> chords = chordService.getAllActiveChords();
        return ResponseEntity.ok(chords);
    }

    @Operation(summary = "코드 상세 조회 (ID)", description = "ID로 특정 코드의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "코드를 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ChordResponseDto> getChordById(@PathVariable @Parameter(description = "코드 ID") Long id) {
        ChordResponseDto chord = chordService.getChordById(id);
        return ResponseEntity.ok(chord);
    }

    @Operation(summary = "코드 상세 조회 (이름)", description = "코드 이름으로 특정 코드의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "코드를 찾을 수 없음")
    })
    @GetMapping("/name/{chordName}")
    public ResponseEntity<ChordResponseDto> getChordByName(@PathVariable @Parameter(description = "코드 이름") String chordName) {
        ChordResponseDto chord = chordService.getChordByName(chordName);
        return ResponseEntity.ok(chord);
    }

    @Operation(summary = "코드 검색", description = "코드 이름 또는 심볼로 코드를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공")
    })
    @GetMapping("/search")
    public ResponseEntity<List<ChordResponseDto>> searchChords(@RequestParam @Parameter(description = "검색어") String q) {
        List<ChordResponseDto> chords = chordService.searchChords(q);
        return ResponseEntity.ok(chords);
    }

    @Operation(summary = "근음별 코드 조회", description = "특정 근음의 모든 코드를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/root/{rootNote}")
    public ResponseEntity<List<ChordResponseDto>> getChordsByRootNote(@PathVariable @Parameter(description = "근음 (C, D, E, F, G, A, B)") String rootNote) {
        List<ChordResponseDto> chords = chordService.getChordsByRootNote(rootNote);
        return ResponseEntity.ok(chords);
    }

    @Operation(summary = "타입별 코드 조회", description = "특정 타입의 모든 코드를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/type/{chordType}")
    public ResponseEntity<List<ChordResponseDto>> getChordsByType(@PathVariable @Parameter(description = "코드 타입") Chord.ChordType chordType) {
        List<ChordResponseDto> chords = chordService.getChordsByType(chordType);
        return ResponseEntity.ok(chords);
    }

    @Operation(summary = "난이도별 코드 조회", description = "특정 난이도의 모든 코드를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/difficulty/{difficultyLevel}")
    public ResponseEntity<List<ChordResponseDto>> getChordsByDifficulty(@PathVariable @Parameter(description = "난이도 (BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)") String difficultyLevel) {
        List<ChordResponseDto> chords = chordService.getChordsByDifficulty(difficultyLevel);
        return ResponseEntity.ok(chords);
    }
}

