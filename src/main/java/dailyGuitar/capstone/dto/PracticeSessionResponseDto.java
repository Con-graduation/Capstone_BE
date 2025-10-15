package dailyGuitar.capstone.dto;

import dailyGuitar.capstone.entity.PracticeRoutine;
import dailyGuitar.capstone.entity.PracticeSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSessionResponseDto {

    private Long id;
    private String sessionName;
    private PracticeSession.SessionType sessionType;
    private PracticeSession.SessionStatus status;
    private Integer targetBpm;
    private Integer actualBpm;
    private BigDecimal accuracyScore;
    private BigDecimal timingScore;
    private BigDecimal pitchScore;
    private BigDecimal overallScore;
    private Integer durationSeconds;
    private String audioFileUrl;
    private String feedbackSummary;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long routineId;
    private String routineName;

    public static PracticeSessionResponseDto from(PracticeSession session) {
        return PracticeSessionResponseDto.builder()
                .id(session.getId())
                .sessionName(session.getSessionName())
                .sessionType(session.getSessionType())
                .status(session.getStatus())
                .targetBpm(session.getTargetBpm())
                .actualBpm(session.getActualBpm())
                .accuracyScore(session.getAccuracyScore())
                .timingScore(session.getTimingScore())
                .pitchScore(session.getPitchScore())
                .overallScore(session.getOverallScore())
                .durationSeconds(session.getDurationSeconds())
                .audioFileUrl(session.getAudioFileUrl())
                .feedbackSummary(session.getFeedbackSummary())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .routineId(session.getRoutine() != null ? session.getRoutine().getId() : null)
                .routineName(session.getRoutine() != null ? session.getRoutine().getRoutineName() : null)
                .build();
    }
}

