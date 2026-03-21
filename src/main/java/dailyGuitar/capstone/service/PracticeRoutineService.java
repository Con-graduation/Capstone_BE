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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PracticeRoutineService {
	private static final Logger log = LoggerFactory.getLogger(PracticeRoutineService.class);
	
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
		String normCt = contentType == null ? null : contentType.toLowerCase();
		if (normCt == null || !(normCt.equals("audio/wav") || normCt.equals("audio/wave") || normCt.equals("audio/x-wav"))) {
			throw new IllegalArgumentException("Only WAV files are allowed. Received: " + contentType);
		}
		
		// 파일 내용 확인 (첫 16바이트)
		try {
			byte[] firstBytes = new byte[16];
			int bytesRead = audioFile.getInputStream().read(firstBytes);
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < bytesRead; i++) {
				hex.append(String.format("%02X ", firstBytes[i]));
			}
			log.info("[complete] File first {} bytes: {}", bytesRead, hex.toString());
		} catch (IOException e) {
			log.warn("[complete] Failed to read file bytes: {}", e.getMessage());
		}
		
        // WAV 파일을 임시 디렉토리에 저장
        Path tempFile;
        try {
            tempFile = saveTemporaryFile(audioFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save temporary file", e);
        }

        try {
            // 분석 인자 구성
            java.util.List<String> args = new java.util.ArrayList<>();
            args.add("--mode");
            args.add(routine.getRoutineType().name().toLowerCase().contains("chord") ? "chord" : "chromatic");
            args.add("--audio");
            args.add(tempFile.toString());
            args.add("--bpm");
            args.add(String.valueOf(routine.getBpm()));
            args.add("--repeats");
            args.add(String.valueOf(routine.getRepeats()));
            if (routine.getRoutineType().name().toLowerCase().contains("chord")) {
                String chords = String.join(",", routine.getSequence());
                args.add("--chords");
                args.add(chords);
                args.add("--beats-per-chord");
                args.add("4");
            } else {
                // 크로매틱 기본 핑거 시퀀스 (필요 시 루틴에서 유도)
                args.add("--fingers");
                args.add(String.join(",", routine.getSequence()));
            }

            String analysisResult;
            try {
                analysisResult = audioAnalysisService.analyzeWithArgs(args);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to analyze audio", e);
            }

            // 경고/로그가 섞인 출력에서 JSON만 추출
            String jsonOnly = extractJsonPayload(analysisResult);
            // 분석 결과 파싱 및 PracticeSession 저장
            PracticeSession session = parseAnalysisResult(jsonOnly, userId, routineId);
            PracticeSession savedSession = practiceSessionRepository.save(session);
            practiceSessionRepository.flush(); // 즉시 DB에 반영하여 ID 할당 보장

            // 연습 횟수 증가 및 마지막 연습 시간 업데이트
            routine.setPracticeCount(routine.getPracticeCount() + 1);
            routine.setLastPracticedAt(Instant.now());
            practiceRoutineRepository.save(routine);

            // UserStatus 업데이트
            updateUserStatus(userId, routine, savedSession);

            // 보고서 생성 및 반환
            return generatePracticeReport(savedSession, routine);
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
     * 파이썬 표준출력에 경고/로그가 섞여 들어오는 경우 JSON 본문만 추출합니다.
     * 규칙: 가장 처음 나오는 '{'부터 마지막 '}'까지를 JSON으로 간주.
     */
    private String extractJsonPayload(String output) {
        if (output == null) return "";
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String json = output.substring(start, end + 1).trim();
            // 여러 JSON 라인이 있을 경우 마지막 줄을 선택하는 보정
            // (경고 후 한 줄 JSON 형태를 기본으로 가정)
            int newline = json.lastIndexOf('\n');
            if (newline > 0) {
                String maybeSingleLine = json.substring(newline + 1).trim();
                if (maybeSingleLine.startsWith("{") && maybeSingleLine.endsWith("}")) {
                    return maybeSingleLine;
                }
            }
            return json;
        }
        return output.trim();
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
					
					// 연습 시간 추가
					long newTotalSeconds = status.getTotalPracticeSeconds() + routine.getPracticeSecondsPerRun();
					status.setTotalPracticeSeconds(newTotalSeconds);
					
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
					status.setTotalPracticeSeconds((long) routine.getPracticeSecondsPerRun());
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
		report.setRhythmFeedback(generateRhythmFeedback(session.getRhythmAccuracy()));
		report.setRhythmWorstSection(getWorstSectionMessage(session.getWorstRhythmSection()));
		
		// 음정 정확도 정보
		report.setPitchAccuracy(session.getPitchAccuracy());
		report.setPitchFeedback(generatePitchFeedback(session.getPitchAccuracy()));
		report.setPitchWorstSection(getWorstSectionMessage(session.getWorstPitchSection()));
		
		// 이전 연습 기록 조회 (현재 세션 제외, 최대 5개)
		// session.getId()가 null일 수 있으므로 createdAt으로도 필터링
		Instant currentSessionCreatedAt = session.getCreatedAt();
		List<PracticeSession> previousSessions = practiceSessionRepository
				.findByUserIdAndRoutineIdOrderByCreatedAtDesc(session.getUserId(), session.getRoutineId())
				.stream()
				.filter(s -> {
					// 현재 세션 제외: ID가 있으면 ID로, 없으면 createdAt으로 비교
					if (session.getId() != null && s.getId() != null) {
						return !s.getId().equals(session.getId());
					} else {
						// ID가 없는 경우 createdAt으로 비교 (같은 시간이면 제외)
						return !s.getCreatedAt().equals(currentSessionCreatedAt);
					}
				})
				.limit(5) // 최대 5개
				.collect(Collectors.toList());
		
		// 박자 히스토리 생성
		List<PracticeReportResponseDto.PreviousPracticeDto> rhythmHistory = previousSessions.stream()
				.map(s -> {
					PracticeReportResponseDto.PreviousPracticeDto dto = new PracticeReportResponseDto.PreviousPracticeDto();
					dto.setAccuracy(s.getRhythmAccuracy());
					dto.setPracticedAt(s.getCreatedAt().toString());
					return dto;
				})
				.collect(Collectors.toList());
		report.setRhythmHistory(rhythmHistory.isEmpty() ? null : rhythmHistory);
		
		// 음정 히스토리 생성
		List<PracticeReportResponseDto.PreviousPracticeDto> pitchHistory = previousSessions.stream()
				.map(s -> {
					PracticeReportResponseDto.PreviousPracticeDto dto = new PracticeReportResponseDto.PreviousPracticeDto();
					dto.setAccuracy(s.getPitchAccuracy());
					dto.setPracticedAt(s.getCreatedAt().toString());
					return dto;
				})
				.collect(Collectors.toList());
		report.setPitchHistory(pitchHistory.isEmpty() ? null : pitchHistory);
		
		// 이전 연습과 비교 (직전 연습이 있는 경우)
		if (!previousSessions.isEmpty()) {
			PracticeSession lastSession = previousSessions.get(0);
			int rhythmDiff = session.getRhythmAccuracy() - lastSession.getRhythmAccuracy();
			int pitchDiff = session.getPitchAccuracy() - lastSession.getPitchAccuracy();
			
			if (rhythmDiff > 0) {
				report.setRhythmComparison(String.format("직전 연습보다 %d%% 향상", rhythmDiff));
			} else if (rhythmDiff < 0) {
				report.setRhythmComparison(String.format("직전 연습보다 %d%% 하락", Math.abs(rhythmDiff)));
			} else {
				report.setRhythmComparison("직전 연습과 동일한 수준");
			}
			
			if (pitchDiff > 0) {
				report.setPitchComparison(String.format("직전 연습보다 %d%% 향상", pitchDiff));
			} else if (pitchDiff < 0) {
				report.setPitchComparison(String.format("직전 연습보다 %d%% 하락", Math.abs(pitchDiff)));
			} else {
				report.setPitchComparison("직전 연습과 동일한 수준");
			}
		}
		
		// 종합 피드백 (이전 연습과의 비교를 고려)
		int overallAccuracy = (session.getRhythmAccuracy() + session.getPitchAccuracy()) / 2;
		report.setOverallFeedback(generateOverallFeedback(overallAccuracy, previousSessions));
		
		return report;
	}
	
	/**
	 * 리듬 정확도에 따른 피드백을 생성합니다.
	 */
	private String generateRhythmFeedback(int accuracy) {
		Random random = new Random();
		List<String> messages;
		
		if (accuracy >= 95) {
			messages = Arrays.asList(
				"완벽한 박자감이에요! 🎵",
				"리듬이 정말 정확해요!",
				"박자감이 탁월합니다!",
				"메트로놈처럼 정확한 박자예요!",
				"박자 실력이 프로 수준이에요!"
			);
		} else if (accuracy >= 90) {
			messages = Arrays.asList(
				"거의 완벽한 박자감이에요!",
				"리듬이 매우 안정적이에요!",
				"박자감이 훌륭합니다!",
				"리듬 실력이 뛰어나요!",
				"박자를 정확히 맞추고 있어요!"
			);
		} else if (accuracy >= 80) {
			messages = Arrays.asList(
				"박자감이 좋아요!",
				"리듬이 안정적이에요!",
				"박자를 잘 맞추고 있어요!",
				"리듬 실력이 꾸준히 향상되고 있어요!",
				"박자감이 개선되고 있어요!"
			);
		} else if (accuracy >= 70) {
			messages = Arrays.asList(
				"박자감이 괜찮아요!",
				"리듬 연습이 도움이 될 거예요!",
				"박자를 조금 더 정확히 맞추면 좋겠어요!",
				"리듬 실력이 향상되고 있어요!",
				"메트로놈과 함께 연습하면 더 좋을 거예요!"
			);
		} else if (accuracy >= 60) {
			messages = Arrays.asList(
				"박자 연습이 필요해요!",
				"리듬감을 키우기 위해 연습하세요!",
				"박자를 더 정확히 맞추면 좋겠어요!",
				"리듬 연습을 꾸준히 하면 나아질 거예요!",
				"메트로놈을 활용한 연습을 추천해요!"
			);
		} else if (accuracy >= 50) {
			messages = Arrays.asList(
				"박자 연습이 더 필요해요!",
				"리듬감을 기르기 위해 노력하세요!",
				"박자를 맞추는 연습을 해보세요!",
				"리듬 연습을 꾸준히 하면 좋아질 거예요!",
				"천천히 메트로놈에 맞춰 연습해보세요!"
			);
		} else {
			messages = Arrays.asList(
				"박자 연습이 많이 필요해요!",
				"리듬감을 키우기 위해 꾸준히 연습하세요!",
				"박자를 맞추는 기본 연습이 필요해요!",
				"리듬 연습을 차근차근 해보세요!",
				"메트로놈과 함께 천천히 연습하면 좋아질 거예요!"
			);
		}
		
		return messages.get(random.nextInt(messages.size()));
	}
	
	/**
	 * 음정 정확도에 따른 피드백을 생성합니다.
	 */
	private String generatePitchFeedback(int accuracy) {
		Random random = new Random();
		List<String> messages;
		
		if (accuracy >= 95) {
			messages = Arrays.asList(
				"완벽한 음정이에요! 🎶",
				"음정이 정말 정확해요!",
				"음정감이 탁월합니다!",
				"튜너처럼 정확한 음정이에요!",
				"음정 실력이 프로 수준이에요!"
			);
		} else if (accuracy >= 90) {
			messages = Arrays.asList(
				"거의 완벽한 음정이에요!",
				"음정이 매우 정확해요!",
				"음정감이 훌륭합니다!",
				"음정 실력이 뛰어나요!",
				"음정을 정확히 맞추고 있어요!"
			);
		} else if (accuracy >= 80) {
			messages = Arrays.asList(
				"음정이 좋아요!",
				"음정감이 안정적이에요!",
				"음정을 잘 맞추고 있어요!",
				"음정 실력이 꾸준히 향상되고 있어요!",
				"음정감이 개선되고 있어요!"
			);
		} else if (accuracy >= 70) {
			messages = Arrays.asList(
				"음정이 괜찮아요!",
				"음정 연습이 도움이 될 거예요!",
				"음정을 조금 더 정확히 맞추면 좋겠어요!",
				"음정 실력이 향상되고 있어요!",
				"튜너를 활용한 연습을 추천해요!"
			);
		} else if (accuracy >= 60) {
			messages = Arrays.asList(
				"음정 연습이 필요해요!",
				"음정감을 키우기 위해 연습하세요!",
				"음정을 더 정확히 맞추면 좋겠어요!",
				"음정 연습을 꾸준히 하면 나아질 거예요!",
				"튜너를 활용한 연습을 추천해요!"
			);
		} else if (accuracy >= 50) {
			messages = Arrays.asList(
				"음정 연습이 더 필요해요!",
				"음정감을 기르기 위해 노력하세요!",
				"음정을 맞추는 연습을 해보세요!",
				"음정 연습을 꾸준히 하면 좋아질 거예요!",
				"천천히 튜너에 맞춰 연습해보세요!"
			);
		} else {
			messages = Arrays.asList(
				"음정 연습이 많이 필요해요!",
				"음정감을 키우기 위해 꾸준히 연습하세요!",
				"음정을 맞추는 기본 연습이 필요해요!",
				"음정 연습을 차근차근 해보세요!",
				"튜너와 함께 천천히 연습하면 좋아질 거예요!"
			);
		}
		
		return messages.get(random.nextInt(messages.size()));
	}
	
	/**
	 * 종합 피드백을 생성합니다. (전체 정확도와 이전 연습 비교를 고려)
	 */
	private String generateOverallFeedback(int overallAccuracy, List<PracticeSession> previousSessions) {
		Random random = new Random();
		List<String> messages;
		
		// 이전 연습과의 비교
		boolean improved = false;
		boolean declined = false;
		if (!previousSessions.isEmpty()) {
			PracticeSession lastSession = previousSessions.get(0);
			int lastOverall = (lastSession.getRhythmAccuracy() + lastSession.getPitchAccuracy()) / 2;
			if (overallAccuracy > lastOverall + 5) {
				improved = true;
			} else if (overallAccuracy < lastOverall - 5) {
				declined = true;
			}
		}
		
		if (improved) {
			messages = Arrays.asList(
				"이전 연습보다 훨씬 좋아졌어요! 계속 이렇게 연습하세요! 🎉",
				"실력이 눈에 띄게 향상되었어요! 정말 대단해요!",
				"이전보다 많이 발전했어요! 꾸준히 연습하면 더 좋아질 거예요!",
				"연습 효과가 확실히 나타나고 있어요! 멋져요!",
				"실력 향상이 느껴져요! 계속 화이팅하세요!"
			);
		} else if (overallAccuracy >= 90) {
			messages = Arrays.asList(
				"완벽한 연습이었어요! 정말 훌륭합니다! 🎸",
				"실력이 정말 뛰어나요! 계속 이렇게 연습하세요!",
				"프로 수준의 연주예요! 대단합니다!",
				"정말 잘 연주하고 있어요! 멋져요!",
				"완벽에 가까운 연습이었어요! 계속 화이팅하세요!"
			);
		} else if (overallAccuracy >= 80) {
			messages = Arrays.asList(
				"좋은 연습이었어요! 계속 노력하세요!",
				"실력이 꾸준히 향상되고 있어요!",
				"잘 연주하고 있어요! 조금만 더 연습하면 완벽해질 거예요!",
				"안정적인 연주예요! 계속 이렇게 연습하세요!",
				"실력이 좋아지고 있어요! 화이팅!"
			);
		} else if (overallAccuracy >= 70) {
			messages = Arrays.asList(
				"괜찮은 연습이었어요! 조금만 더 노력하면 좋아질 거예요!",
				"실력이 향상되고 있어요! 계속 연습하세요!",
				"조금 더 연습하면 더 좋아질 거예요!",
				"꾸준히 연습하면 실력이 늘 거예요!",
				"좋은 방향으로 가고 있어요! 화이팅!"
			);
		} else if (overallAccuracy >= 60) {
			messages = Arrays.asList(
				"연습이 필요해요! 꾸준히 하면 좋아질 거예요!",
				"조금 더 노력하면 실력이 늘 거예요!",
				"기본기를 다지는 연습이 중요해요!",
				"천천히 차근차근 연습하면 좋아질 거예요!",
				"연습을 꾸준히 하면 실력이 향상될 거예요!"
			);
		} else if (overallAccuracy >= 50) {
			messages = Arrays.asList(
				"기본 연습이 더 필요해요! 꾸준히 하면 좋아질 거예요!",
				"차근차근 연습하면 실력이 늘 거예요!",
				"기본기를 탄탄히 다지는 게 중요해요!",
				"천천히 연습하면 좋아질 거예요!",
				"꾸준한 연습이 답이에요! 화이팅!"
			);
		} else {
			messages = Arrays.asList(
				"기본 연습을 꾸준히 하면 좋아질 거예요!",
				"차근차근 천천히 연습하세요!",
				"기본기를 다지는 게 중요해요!",
				"연습을 꾸준히 하면 실력이 늘 거예요!",
				"포기하지 말고 계속 연습하세요! 화이팅!"
			);
		}
		
		if (declined && overallAccuracy < 70) {
			messages = Arrays.asList(
				"이번엔 조금 아쉬웠지만, 다음엔 더 잘할 수 있어요!",
				"오늘은 컨디션이 안 좋았을 수도 있어요. 다음 연습에서 더 좋아질 거예요!",
				"가끔은 이런 날도 있어요. 꾸준히 연습하면 다시 좋아질 거예요!",
				"조금만 더 집중하면 좋아질 거예요! 다음 연습을 기대해요!",
				"오늘은 조금 아쉬웠지만, 다음엔 더 좋은 결과가 있을 거예요!"
			);
		}
		
		return messages.get(random.nextInt(messages.size()));
	}
	
	/**
	 * 섹션 정보를 메시지로 변환합니다.
	 */
	private String getWorstSectionMessage(PracticeSession.Section section) {
		Random random = new Random();
		List<String> messages;
		
		switch (section) {
			case EARLY:
				messages = Arrays.asList(
					"초반에서 연주가 가장 불안정했어요",
					"시작 부분의 안정성이 개선되면 좋겠어요",
					"초반 박자/음정을 더 정확히 맞추면 좋아질 거예요",
					"시작할 때 집중하면 더 좋은 결과가 있을 거예요",
					"초반 연습을 더 해보면 좋겠어요"
				);
				break;
			case MIDDLE:
				messages = Arrays.asList(
					"중반에서 연주가 가장 불안정했어요",
					"중간 부분의 안정성이 개선되면 좋겠어요",
					"중반 박자/음정을 더 정확히 맞추면 좋아질 거예요",
					"중간 부분에 집중하면 더 좋은 결과가 있을 거예요",
					"중반 연습을 더 해보면 좋겠어요"
				);
				break;
			case LATE:
				messages = Arrays.asList(
					"후반에서 연주가 가장 불안정했어요",
					"끝 부분의 안정성이 개선되면 좋겠어요",
					"후반 박자/음정을 더 정확히 맞추면 좋아질 거예요",
					"끝까지 집중하면 더 좋은 결과가 있을 거예요",
					"후반 연습을 더 해보면 좋겠어요"
				);
				break;
			default:
				messages = Arrays.asList(
					"전체적으로 안정적인 연주였어요",
					"모든 구간에서 균형 잡힌 연주예요",
					"전반적으로 안정적인 박자와 음정이에요",
					"구간별로 고른 실력을 보여주고 있어요",
					"전체적으로 일관된 연주예요"
				);
				break;
		}
		
		return messages.get(random.nextInt(messages.size()));
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
