package dailyGuitar.capstone.mcp.entity;

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
	
	@Column(nullable = false)
	private Integer rhythmAccuracy;
	
	@Column(nullable = false)
	private Integer pitchAccuracy;
	
	@Column(columnDefinition = "TEXT")
	private String rhythmSectionScores;
	
	@Column(columnDefinition = "TEXT")
	private String pitchSectionScores;
	
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Section worstRhythmSection;
	
	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Section worstPitchSection;

	@Column(name = "session_name", nullable = true, length = 100)
	private String sessionName;
	
	@Column(nullable = false, updatable = false)
	private Instant createdAt;
	
	@PrePersist
	public void onCreate() {
		this.createdAt = Instant.now();
	}
	
	public enum Section {
		EARLY, MIDDLE, LATE
	}
	
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
	public String getSessionName() { return sessionName; }
	
	public void setUserId(Long userId) { this.userId = userId; }
	public void setRoutineId(Long routineId) { this.routineId = routineId; }
	public void setRhythmAccuracy(Integer rhythmAccuracy) { this.rhythmAccuracy = rhythmAccuracy; }
	public void setPitchAccuracy(Integer pitchAccuracy) { this.pitchAccuracy = pitchAccuracy; }
	public void setRhythmSectionScores(String rhythmSectionScores) { this.rhythmSectionScores = rhythmSectionScores; }
	public void setPitchSectionScores(String pitchSectionScores) { this.pitchSectionScores = pitchSectionScores; }
	public void setWorstRhythmSection(Section worstRhythmSection) { this.worstRhythmSection = worstRhythmSection; }
	public void setWorstPitchSection(Section worstPitchSection) { this.worstPitchSection = worstPitchSection; }
	public void setSessionName(String sessionName) { this.sessionName = sessionName; }
}

