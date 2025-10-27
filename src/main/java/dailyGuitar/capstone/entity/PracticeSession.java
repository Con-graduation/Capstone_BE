package dailyGuitar.capstone.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "practice_sessions")
public class PracticeSession {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private Long userId;
	
	@Column(nullable = false)
	private Long routineId;
	
	// 박자 정확도 (0~100)
	@Column(nullable = false)
	private Integer rhythmAccuracy;
	
	// 음정 정확도 (0~100)
	@Column(nullable = false)
	private Integer pitchAccuracy;
	
	// 섹션별 박자 점수 (JSON 형식으로 저장)
	@Column(columnDefinition = "TEXT")
	private String rhythmSectionScores; // 예: {"early": 85, "middle": 92, "late": 88}
	
	// 섹션별 음정 점수 (JSON 형식으로 저장)
	@Column(columnDefinition = "TEXT")
	private String pitchSectionScores; // 예: {"early": 88, "middle": 82, "late": 90}
	
	// 박자에서 가장 점수가 낮았던 섹션 (EARLY, MIDDLE, LATE)
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Section worstRhythmSection;
	
	// 음정에서 가장 점수가 낮았던 섹션
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Section worstPitchSection;
	
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	
	@PrePersist
	public void onCreate() {
		this.createdAt = Instant.now();
	}
	
	public enum Section {
		EARLY, MIDDLE, LATE
	}
	
	// Getters
	public Long getId() { return id; }
	public Long getUserId() { return userId; }
	public Long getRoutineId() { return routineId; }
	public Integer getRhythmAccuracy() { return rhythmAccuracy; }
	public Integer getPitchAccuracy() { return pitchAccuracy; }
	public String getRhythmSectionScores() { return rhythmSectionScores; }
	public String getPitchSectionScores() { return pitchSectionScores; }
	public Section getWorstRhythmSection() { return worstRhythmSection; }
	public Section getWorstPitchSection() { return worstPitchSection; }
	public Instant getCreatedAt() { return createdAt; }
	
	// Setters
	public void setUserId(Long userId) { this.userId = userId; }
	public void setRoutineId(Long routineId) { this.routineId = routineId; }
	public void setRhythmAccuracy(Integer rhythmAccuracy) { this.rhythmAccuracy = rhythmAccuracy; }
	public void setPitchAccuracy(Integer pitchAccuracy) { this.pitchAccuracy = pitchAccuracy; }
	public void setRhythmSectionScores(String rhythmSectionScores) { this.rhythmSectionScores = rhythmSectionScores; }
	public void setPitchSectionScores(String pitchSectionScores) { this.pitchSectionScores = pitchSectionScores; }
	public void setWorstRhythmSection(Section worstRhythmSection) { this.worstRhythmSection = worstRhythmSection; }
	public void setWorstPitchSection(Section worstPitchSection) { this.worstPitchSection = worstPitchSection; }
}
