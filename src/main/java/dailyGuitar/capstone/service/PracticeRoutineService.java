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
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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

	@Transactional
	public boolean delete(Long id) {
		Long userId = getCurrentUserId();
		return practiceRoutineRepository.findById(id)
				.filter(r -> r.getUserId().equals(userId))
				.map(r -> { practiceRoutineRepository.delete(r); return true; })
				.orElse(false);
	}

	@Transactional
	public void complete(Long routineId, MultipartFile audioFile) {
		Long userId = getCurrentUserId();
		
		// 루틴 조회 및 권한 확인
		PracticeRoutine routine = practiceRoutineRepository.findById(routineId)
				.orElseThrow(() -> new NoSuchElementException("Routine not found: " + routineId));
		
		if (!routine.getUserId().equals(userId)) {
			throw new SecurityException("Not owner of routine");
		}
		
		// 파일 검증
		if (audioFile.isEmpty()) {
			throw new IllegalArgumentException("Audio file is empty");
		}
		
		String contentType = audioFile.getContentType();
		if (contentType == null || !contentType.equals("audio/wav")) {
			throw new IllegalArgumentException("Only WAV files are allowed. Received: " + contentType);
		}
		
		// 여기서 나중에 AI 분석 로직 추가 예정
		// 현재는 파일을 받아서 저장/처리하는 기본 구조만 구현
		
		// TODO: AI 분석 서비스로 파일 전달
		// aiAnalysisService.analyze(audioFile);
		
		// 연습 횟수 증가 및 마지막 연습 시간 업데이트
		routine.setPracticeCount(routine.getPracticeCount() + 1);
		routine.setLastPracticedAt(Instant.now());
		practiceRoutineRepository.save(routine);
		
		// TODO: S3에 파일 저장하거나 AI 분석 결과 처리
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
