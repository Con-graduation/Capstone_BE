package dailyGuitar.capstone.dto.practice;

import dailyGuitar.capstone.entity.RoutineType;
import java.time.Instant;
import java.util.List;

public class PracticeRoutineResponseDto {
	private Long id;
	private Long userId;
	private String title;
	private RoutineType routineType;
	private List<String> sequence;
	private Integer repeats;
	private Integer bpm;
	private Long practiceCount;
	private Instant createdAt;
	private Instant updatedAt;
	private Instant lastPracticedAt;

	public Long getId() { return id; }
	public Long getUserId() { return userId; }
	public String getTitle() { return title; }
	public RoutineType getRoutineType() { return routineType; }
	public List<String> getSequence() { return sequence; }
	public Integer getRepeats() { return repeats; }
	public Integer getBpm() { return bpm; }
	public Long getPracticeCount() { return practiceCount; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Instant getLastPracticedAt() { return lastPracticedAt; }

	public void setId(Long id) { this.id = id; }
	public void setUserId(Long userId) { this.userId = userId; }
	public void setTitle(String title) { this.title = title; }
	public void setRoutineType(RoutineType routineType) { this.routineType = routineType; }
	public void setSequence(List<String> sequence) { this.sequence = sequence; }
	public void setRepeats(Integer repeats) { this.repeats = repeats; }
	public void setBpm(Integer bpm) { this.bpm = bpm; }
	public void setPracticeCount(Long practiceCount) { this.practiceCount = practiceCount; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
	public void setLastPracticedAt(Instant lastPracticedAt) { this.lastPracticedAt = lastPracticedAt; }
}
