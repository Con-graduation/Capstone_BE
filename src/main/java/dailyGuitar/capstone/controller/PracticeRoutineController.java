package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.common.IdRequest;
import dailyGuitar.capstone.dto.practice.PracticeRoutineCreateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineResponseDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineUpdateRequestDto;
import dailyGuitar.capstone.service.PracticeRoutineService;
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

	@PostMapping
	public ResponseEntity<PracticeRoutineResponseDto> create(@Valid @RequestBody PracticeRoutineCreateRequestDto req) {
		PracticeRoutineResponseDto created = practiceRoutineService.create(req);
		return ResponseEntity.created(URI.create("/api/routines/" + created.getId())).body(created);
	}

	@PostMapping("/update")
	public ResponseEntity<PracticeRoutineResponseDto> update(@Valid @RequestBody PracticeRoutineUpdateRequestDto req) {
		return ResponseEntity.ok(practiceRoutineService.update(req.getId(), req));
	}

	@PostMapping("/detail")
	public ResponseEntity<PracticeRoutineResponseDto> get(@Valid @RequestBody IdRequest req) {
		return ResponseEntity.ok(practiceRoutineService.getById(req.getId()));
	}

	@PostMapping("/list")
	public ResponseEntity<List<PracticeRoutineResponseDto>> listMine() {
		return ResponseEntity.ok(practiceRoutineService.listMine());
	}

	@PostMapping("/delete")
	public ResponseEntity<Void> delete(@Valid @RequestBody IdRequest req) {
		return practiceRoutineService.delete(req.getId())
				? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}
}
