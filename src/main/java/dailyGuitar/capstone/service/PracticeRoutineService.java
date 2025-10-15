package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.practice.PracticeRoutineCreateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineResponseDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineUpdateRequestDto;
import dailyGuitar.capstone.entity.PracticeRoutine;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.repository.PracticeRoutineRepository;
import dailyGuitar.capstone.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class PracticeRoutineService {
	private final PracticeRoutineRepository practiceRoutineRepository;
	private final UserRepository userRepository;

	public PracticeRoutineService(PracticeRoutineRepository practiceRoutineRepository, UserRepository userRepository) {
		this.practiceRoutineRepository = practiceRoutineRepository;
		this.userRepository = userRepository;
	}

	private Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			throw new IllegalStateException("Unauthenticated");
		}
		User user = userRepository.findByUsername(auth.getName())
				.orElseThrow(() -> new NoSuchElementException("User not found: " + auth.getName()));
		return user.getId();
	}

	@Transactional
	public PracticeRoutineResponseDto create(PracticeRoutineCreateRequestDto req) {
		Long userId = getCurrentUserId();
		PracticeRoutine routine = new PracticeRoutine();
		routine.setUserId(userId);
		routine.setTitle(req.getTitle());
		routine.setRoutineType(req.getRoutineType());
		routine.setSequence(req.getSequence());
		routine.setRepeats(req.getRepeats());
		routine.setBpm(req.getBpm());
		PracticeRoutine saved = practiceRoutineRepository.save(routine);
		return toResponse(saved);
	}

	@Transactional
	public PracticeRoutineResponseDto update(Long id, PracticeRoutineUpdateRequestDto req) {
		Long userId = getCurrentUserId();
		PracticeRoutine routine = practiceRoutineRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Routine not found: " + id));
		if (!routine.getUserId().equals(userId)) {
			throw new SecurityException("Not owner of routine");
		}
		if (req.getTitle() != null) routine.setTitle(req.getTitle());
		if (req.getSequence() != null) routine.setSequence(req.getSequence());
		if (req.getRepeats() != null) routine.setRepeats(req.getRepeats());
		if (req.getBpm() != null) routine.setBpm(req.getBpm());
		PracticeRoutine saved = practiceRoutineRepository.save(routine);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public PracticeRoutineResponseDto getById(Long id) {
		Long userId = getCurrentUserId();
		PracticeRoutine routine = practiceRoutineRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Routine not found: " + id));
		if (!routine.getUserId().equals(userId)) {
			throw new SecurityException("Not owner of routine");
		}
		return toResponse(routine);
	}

	@Transactional(readOnly = true)
	public List<PracticeRoutineResponseDto> listMine() {
		Long userId = getCurrentUserId();
		return practiceRoutineRepository.findByUserId(userId).stream()
				.map(this::toResponse)
				.collect(Collectors.toList());
	}

	private PracticeRoutineResponseDto toResponse(PracticeRoutine r) {
		PracticeRoutineResponseDto dto = new PracticeRoutineResponseDto();
		dto.setId(r.getId());
		dto.setUserId(r.getUserId());
		dto.setTitle(r.getTitle());
		dto.setRoutineType(r.getRoutineType());
		dto.setSequence(r.getSequence());
		dto.setRepeats(r.getRepeats());
		dto.setBpm(r.getBpm());
		dto.setPracticeCount(r.getPracticeCount());
		dto.setCreatedAt(r.getCreatedAt());
		dto.setUpdatedAt(r.getUpdatedAt());
		dto.setLastPracticedAt(r.getLastPracticedAt());
		return dto;
	}
}
