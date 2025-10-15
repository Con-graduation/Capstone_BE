package dailyGuitar.capstone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "practice_routines")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeRoutine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String routineName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RoutineType routineType = RoutineType.CUSTOM;
    
    @Column(columnDefinition = "TEXT")
    private String exerciseItems; // JSON string
    
    private Integer targetDurationMinutes;
    
    private Integer targetBpm;
    
    @Min(1)
    @Max(5)
    @Builder.Default
    private Integer difficultyLevel = 1;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Boolean isPublic = false;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum RoutineType {
        WARMUP, TECHNIQUE, CHORD, SCALE, CUSTOM
    }
}

