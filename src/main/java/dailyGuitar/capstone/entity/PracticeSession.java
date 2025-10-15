package dailyGuitar.capstone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private PracticeRoutine routine;
    
    @Column(nullable = false)
    private String sessionName;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionType sessionType = SessionType.FREESTYLE;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SessionStatus status = SessionStatus.PLANNED;
    
    private Integer targetBpm;
    
    private Integer actualBpm;
    
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal accuracyScore;
    
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal timingScore;
    
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal pitchScore;
    
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal overallScore;
    
    @Min(0)
    @Builder.Default
    private Integer durationSeconds = 0;
    
    private String audioFileUrl;
    
    @Column(columnDefinition = "TEXT")
    private String analysisData; // JSON string
    
    @Column(columnDefinition = "TEXT")
    private String feedbackSummary;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime endedAt;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum SessionType {
        ROUTINE_BASED, FREESTYLE, SONG_PRACTICE
    }
    
    public enum SessionStatus {
        PLANNED, IN_PROGRESS, COMPLETED, CANCELLED, FAILED
    }
}

