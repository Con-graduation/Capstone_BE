package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.PracticeSessionCreateDto;
import dailyGuitar.capstone.dto.PracticeSessionResponseDto;
import dailyGuitar.capstone.entity.PracticeRoutine;
import dailyGuitar.capstone.entity.PracticeSession;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.exception.PracticeRoutineNotFoundException;
import dailyGuitar.capstone.exception.PracticeSessionNotFoundException;
import dailyGuitar.capstone.repository.PracticeRoutineRepository;
import dailyGuitar.capstone.repository.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PracticeSessionService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeRoutineRepository practiceRoutineRepository;

    public PracticeSessionResponseDto createSession(PracticeSessionCreateDto createDto, User user) {
        PracticeRoutine routine = null;
        if (createDto.getRoutineId() != null) {
            routine = practiceRoutineRepository.findById(createDto.getRoutineId())
                    .orElseThrow(() -> new PracticeRoutineNotFoundException("루틴을 찾을 수 없습니다: " + createDto.getRoutineId()));
        }

        PracticeSession session = PracticeSession.builder()
                .user(user)
                .routine(routine)
                .sessionName(createDto.getSessionName())
                .sessionType(createDto.getSessionType())
                .targetBpm(createDto.getTargetBpm())
                .status(PracticeSession.SessionStatus.PLANNED)
                .build();

        PracticeSession savedSession = practiceSessionRepository.save(session);
        return PracticeSessionResponseDto.from(savedSession);
    }

    @Transactional(readOnly = true)
    public Page<PracticeSessionResponseDto> getUserSessions(User user, Pageable pageable) {
        Page<PracticeSession> sessions = practiceSessionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return sessions.map(PracticeSessionResponseDto::from);
    }

    @Transactional(readOnly = true)
    public PracticeSessionResponseDto getSessionById(Long sessionId, User user) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId));

        // 세션 소유자 확인
        if (!session.getUser().getId().equals(user.getId())) {
            throw new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId);
        }

        return PracticeSessionResponseDto.from(session);
    }

    public PracticeSessionResponseDto startSession(Long sessionId, User user) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId);
        }

        session.setStatus(PracticeSession.SessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());

        PracticeSession savedSession = practiceSessionRepository.save(session);
        return PracticeSessionResponseDto.from(savedSession);
    }

    public PracticeSessionResponseDto completeSession(Long sessionId, User user, 
                                                     Integer actualBpm, 
                                                     Double accuracyScore,
                                                     Double timingScore,
                                                     Double pitchScore,
                                                     Double overallScore,
                                                     Integer durationSeconds) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId);
        }

        session.setStatus(PracticeSession.SessionStatus.COMPLETED);
        session.setActualBpm(actualBpm);
        session.setAccuracyScore(accuracyScore != null ? BigDecimal.valueOf(accuracyScore) : null);
        session.setTimingScore(timingScore != null ? BigDecimal.valueOf(timingScore) : null);
        session.setPitchScore(pitchScore != null ? BigDecimal.valueOf(pitchScore) : null);
        session.setOverallScore(overallScore != null ? BigDecimal.valueOf(overallScore) : null);
        session.setDurationSeconds(durationSeconds != null ? durationSeconds : 0);
        session.setEndedAt(LocalDateTime.now());

        PracticeSession savedSession = practiceSessionRepository.save(session);
        return PracticeSessionResponseDto.from(savedSession);
    }

    public PracticeSessionResponseDto cancelSession(Long sessionId, User user) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new PracticeSessionNotFoundException("연습 세션을 찾을 수 없습니다: " + sessionId);
        }

        session.setStatus(PracticeSession.SessionStatus.CANCELLED);
        session.setEndedAt(LocalDateTime.now());

        PracticeSession savedSession = practiceSessionRepository.save(session);
        return PracticeSessionResponseDto.from(savedSession);
    }
}

