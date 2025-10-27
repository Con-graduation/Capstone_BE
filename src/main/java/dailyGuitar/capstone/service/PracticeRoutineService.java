package dailyGuitar.capstone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dailyGuitar.capstone.dto.practice.PracticeRoutineCreateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineResponseDto;
import dailyGuitar.capstone.dto.practice.PracticeRoutineUpdateRequestDto;
import dailyGuitar.capstone.dto.practice.PracticeReportResponseDto;
import dailyGuitar.capstone.entity.PracticeRoutine;
import dailyGuitar.capstone.entity.PracticeSession;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.entity.UserStatus;
import dailyGuitar.capstone.repository.PracticeRoutineRepository;
import dailyGuitar.capstone.repository.PracticeSessionRepository;
import dailyGuitar.capstone.repository.UserRepository;
import dailyGuitar.capstone.repository.UserStatusRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PracticeRoutineService {
	private final PracticeRoutineRepository practiceRoutineRepository;
	private final UserRepository userRepository;
	private final AudioAnalysisService audioAnalysisService;
	private final PracticeSessionRepository practiceSessionRepository;
	private final UserStatusRepository userStatusRepository;
	private final ObjectMapper objectMapper;

	public PracticeRoutineService(
			PracticeRoutineRepository practiceRoutineRepository, 
			UserRepository userRepository,
			AudioAnalysisService audioAnalysisService,
			PracticeSessionRepository practiceSessionRepository,
			UserStatusRepository userStatusRepository) {
		this.practiceRoutineRepository = practiceRoutineRepository;
		this.userRepository = userRepository;
		this.audioAnalysisService = audioAnalysisService;
		this.practiceSessionRepository = practiceSessionRepository;
		this.userStatusRepository = userStatusRepository;
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
	public PracticeReportResponseDto complete(Long routineId, MultipartFile audioFile) {
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
			
			// UserStatus 업데이트
			updateUserStatus(userId, routine, session);
			
			// 보고서 생성 및 반환
			return generatePracticeReport(session, routine);
			
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
	
	/**
	 * UserStatus를 업데이트합니다.
	 */
	private void updateUserStatus(Long userId, PracticeRoutine routine, PracticeSession session) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NoSuchElementException("User not found"));
		
		userStatusRepository.findByUser(user).ifPresentOrElse(
				status -> {
					// 기존 UserStatus 업데이트
					status.setTotalPracticeCount(status.getTotalPracticeCount() + 1);
					
					// 전체 정확도 계산 (박자 + 음정 평균)
					int overallAccuracy = (session.getRhythmAccuracy() + session.getPitchAccuracy()) / 2;
					
					// 전체 평균 정확도 재계산
					List<PracticeSession> allSessions = practiceSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
					if (!allSessions.isEmpty()) {
						double avgAccuracy = allSessions.stream()
								.mapToInt(s -> (s.getRhythmAccuracy() + s.getPitchAccuracy()) / 2)
								.average()
								.orElse(0.0);
						status.setOverallAccuracy((int) Math.round(avgAccuracy));
					} else {
						status.setOverallAccuracy(overallAccuracy);
					}
					
					// 최고 정확도 업데이트
					if (overallAccuracy > status.getMaxAccuracy()) {
						status.setMaxAccuracy(overallAccuracy);
					}
					
					// 경험치 추가
					long newExperience = status.getTotalExperience() + routine.getXpPerRun();
					status.setTotalExperience(newExperience);
					
					// 레벨 계산 (경험치를 기반으로)
					int newLevel = calculateLevel(newExperience);
					status.setLevel(newLevel);
					
					// Streak 계산
					updateStreak(status, session);
					
					userStatusRepository.save(status);
				},
				() -> {
					// UserStatus가 없으면 생성
					UserStatus status = new UserStatus();
					status.setUser(user);
					status.setTotalPracticeCount(1L);
					int overallAccuracy = (session.getRhythmAccuracy() + session.getPitchAccuracy()) / 2;
					status.setOverallAccuracy(overallAccuracy);
					status.setMaxAccuracy(overallAccuracy);
					status.setTotalExperience((long) routine.getXpPerRun());
					userStatusRepository.save(status);
				}
		);
	}
	
	/**
	 * 연습 보고서를 생성합니다.
	 */
	private PracticeReportResponseDto generatePracticeReport(PracticeSession session, PracticeRoutine routine) {
		PracticeReportResponseDto report = new PracticeReportResponseDto();
		
		// 박자 정확도 정보
		report.setRhythmAccuracy(session.getRhythmAccuracy());
		report.setRhythmFeedback(generateAccuracyFeedback(session.getRhythmAccuracy()));
		report.setRhythmWorstSection(getWorstSectionMessage(session.getWorstRhythmSection()));
		
		// 음정 정확도 정보
		report.setPitchAccuracy(session.getPitchAccuracy());
		report.setPitchFeedback(generateAccuracyFeedback(session.getPitchAccuracy()));
		report.setPitchWorstSection(getWorstSectionMessage(session.getWorstPitchSection()));
		
		// TODO: 이전 연습과 비교, BPM 조언 등
		
		// 종합 피드백
		report.setOverallFeedback("이번 연습도 정말 잘했습니다!");
		
		return report;
	}
	
	/**
	 * 정확도에 따른 피드백을 생성합니다.
	 */
	private String generateAccuracyFeedback(int accuracy) {
		if (accuracy >= 90) {
			return "거의 완벽한 수준이에요!";
		} else if (accuracy >= 70) {
			return "잘 연주하고 있어요!";
		} else if (accuracy >= 50) {
			return "조금 더 연습하면 나아질 거예요!";
		} else {
			return "꾸준한 연습이 필요해요!";
		}
	}
	
	/**
	 * 섹션 정보를 메시지로 변환합니다.
	 */
	private String getWorstSectionMessage(PracticeSession.Section section) {
		switch (section) {
			case EARLY:
				return "초반에서 연주가 가장 불안정했어요";
			case MIDDLE:
				return "중반에서 연주가 가장 불안정했어요";
			case LATE:
				return "후반에서 연주가 가장 불안정했어요";
			default:
				return "전체적으로 안정적인 연주였어요";
		}
	}

	/**
 * 경험치를 기반으로 레벨을 계산합니다.
 */
private int calculateLevel(long experience) {
    return (int) Math.floor(Math.sqrt(experience / 50.0)) + 1;
}

/**
 * Streak을 업데이트합니다.
 */
private void updateStreak(UserStatus status, PracticeSession session) {
    String today = Instant.now().atZone(ZoneId.of("Asia/Seoul"))
            .format(DateTimeFormatter.ISO_LOCAL_DATE);
    
    Optional<PracticeSession> lastSession = practiceSessionRepository
            .findByUserIdOrderByCreatedAtDesc(status.getUser().getId())
            .stream()
            .skip(1)
            .findFirst();
    
    if (lastSession.isPresent()) {
        String lastDate = lastSession.get().getCreatedAt().atZone(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        if (!today.equals(lastDate)) {
            status.setStreakDays(status.getStreakDays() + 1);
        }
    } else {
        status.setStreakDays(1);
    }
}
}
