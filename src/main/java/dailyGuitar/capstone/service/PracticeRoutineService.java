package dailyGuitar.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dailyGuitar.capstone.dto.practice.PracticeRoutineCreateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineResponseDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineUpdateRequestDto;
import dailyGuitar.capstone.entity.PracticeRoutine;
import dailyGuitar.capstone.entity.PracticeSession;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.repository.PracticeRoutineRepository;
import dailyGuitar.capstone.repository.PracticeSessionRepository;
import dailyGuitar.capstone.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PracticeRoutineService {
	private final PracticeRoutineRepository practiceRoutineRepository;
	private final UserRepository userRepository;
	private final AudioAnalysisService audioAnalysisService;
	private final PracticeSessionRepository practiceSessionRepository;
	private final ObjectMapper objectMapper;

	public PracticeRoutineService(
			PracticeRoutineRepository practiceRoutineRepository, 
			UserRepository userRepository,
			AudioAnalysisService audioAnalysisService,
			PracticeSessionRepository practiceSessionRepository) {
		this.practiceRoutineRepository = practiceRoutineRepository;
		this.userRepository = userRepository;
		this.audioAnalysisService = audioAnalysisService;
		this.practiceSessionRepository = practiceSessionRepository;
		this.objectMapper = new ObjectMapper();
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
		
		// WAV 파일을 임시 디렉토리에 저장
		Path tempFile;
		try {
			tempFile = saveTemporaryFile(audioFile);
		} catch (IOException e) {
			throw new RuntimeException("Failed to save temporary file", e);
		}
		
		try {
			// AI 분석 서비스로 파일 경로 전달
			String analysisResult;
			try {
				analysisResult = audioAnalysisService.analyzeAudio(tempFile.toString());
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException("Failed to analyze audio", e);
			}
			
			// 분석 결과 파싱 및 PracticeSession 저장
			PracticeSession session = parseAnalysisResult(analysisResult, userId, routineId);
			practiceSessionRepository.save(session);
			
			// 연습 횟수 증가 및 마지막 연습 시간 업데이트
			routine.setPracticeCount(routine.getPracticeCount() + 1);
			routine.setLastPracticedAt(Instant.now());
			practiceRoutineRepository.save(routine);
			
		} finally {
			// 임시 파일 삭제
			deleteTemporaryFile(tempFile);
		}
	}
	
	/**
	 * AI 분석 결과를 파싱하여 PracticeSession 엔티티를 생성합니다.
	 */
	private PracticeSession parseAnalysisResult(String analysisResult, Long userId, Long routineId) {
		try {
			JsonNode jsonNode = objectMapper.readTree(analysisResult);
			
			// 필수 필드 추출
			int rhythmAccuracy = jsonNode.get("rhythm_accuracy").asInt();
			int pitchAccuracy = jsonNode.get("pitch_accuracy").asInt();
			
			// 섹션별 점수 추출
			String rhythmSectionScores = objectMapper.writeValueAsString(jsonNode.get("rhythm_sections"));
			String pitchSectionScores = objectMapper.writeValueAsString(jsonNode.get("pitch_sections"));
			
			// 가장 점수가 낮은 섹션 추출
			PracticeSession.Section worstRhythmSection = extractWorstSection(jsonNode.get("rhythm_sections"));
			PracticeSession.Section worstPitchSection = extractWorstSection(jsonNode.get("pitch_sections"));
			
			// PracticeSession 생성
			PracticeSession session = new PracticeSession();
			session.setUserId(userId);
			session.setRoutineId(routineId);
			session.setRhythmAccuracy(rhythmAccuracy);
			session.setPitchAccuracy(pitchAccuracy);
			session.setRhythmSectionScores(rhythmSectionScores);
			session.setPitchSectionScores(pitchSectionScores);
			session.setWorstRhythmSection(worstRhythmSection);
			session.setWorstPitchSection(worstPitchSection);
			
			return session;
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse analysis result: " + analysisResult, e);
		}
	}
	
	/**
	 * 섹션별 점수에서 가장 낮은 점수의 섹션을 찾습니다.
	 */
	private PracticeSession.Section extractWorstSection(JsonNode sections) {
		int minScore = Integer.MAX_VALUE;
		String worstSection = "EARLY"; // 기본값
		
		for (String section : new String[]{"early", "middle", "late"}) {
			if (sections.has(section)) {
				int score = sections.get(section).asInt();
				if (score < minScore) {
					minScore = score;
					worstSection = section.toUpperCase();
				}
			}
		}
		
		return PracticeSession.Section.valueOf(worstSection);
	}
	
	/**
	 * MultipartFile을 임시 디렉토리에 저장합니다.
	 */
	private Path saveTemporaryFile(MultipartFile file) throws IOException {
		Path tempDir = Files.createTempDirectory("guitar-practice-");
		Path tempFile = tempDir.resolve(UUID.randomUUID().toString() + ".wav");
		Files.copy(file.getInputStream(), tempFile);
		return tempFile;
	}
	
	/**
	 * 임시 파일을 삭제합니다.
	 */
	private void deleteTemporaryFile(Path tempFile) {
		try {
			Files.deleteIfExists(tempFile);
			// 임시 디렉토리도 삭제 시도
			Files.deleteIfExists(tempFile.getParent());
		} catch (IOException e) {
			System.err.println("Failed to delete temporary file: " + tempFile);
		}
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
