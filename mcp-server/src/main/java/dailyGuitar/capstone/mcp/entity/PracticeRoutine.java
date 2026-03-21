package dailyGuitar.capstone.mcp.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practice_routines")
public class PracticeRoutine {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(name = "routine_name", nullable = false, length = 100)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoutineType routineType;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "practice_routine_sequence", joinColumns = @JoinColumn(name = "routine_id"))
	@Column(name = "step", length = 10, nullable = false)
	private List<String> sequence = new ArrayList<>();

	@Column(nullable = false)
	private Integer repeats;

	@Column(nullable = false)
	private Integer bpm;

	@Column(nullable = false)
	private Long practiceCount = 0L;

	@Column(nullable = false)
	private Integer xpPerRun = 0;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Column
	private Instant lastPracticedAt;

	@PrePersist
	public void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
		recalculateXpPerRun();
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = Instant.now();
		recalculateXpPerRun();
	}

	private void recalculateXpPerRun() {
		if (repeats == null || bpm == null) { this.xpPerRun = 0; return; }
		double factor = 1.2 + Math.max(0, bpm - 20) * 0.01d;
		this.xpPerRun = (int)Math.round(repeats * factor);
	}

	public Long getId() { return id; }
	public Long getUserId() { return userId; }
	public String getTitle() { return title; }
	public RoutineType getRoutineType() { return routineType; }
	public List<String> getSequence() { return sequence; }
	public Integer getRepeats() { return repeats; }
	public Integer getBpm() { return bpm; }
	public Long getPracticeCount() { return practiceCount; }
	public Integer getXpPerRun() { return xpPerRun; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Instant getLastPracticedAt() { return lastPracticedAt; }

	public void setUserId(Long userId) { this.userId = userId; }
	public void setTitle(String title) { this.title = title; }
	public void setRoutineType(RoutineType routineType) { this.routineType = routineType; }
	public void setSequence(List<String> sequence) { this.sequence = sequence; }
	public void setRepeats(Integer repeats) { this.repeats = repeats; }
	public void setBpm(Integer bpm) { this.bpm = bpm; }
	public void setPracticeCount(Long practiceCount) { this.practiceCount = practiceCount; }
	public void setLastPracticedAt(Instant lastPracticedAt) { this.lastPracticedAt = lastPracticedAt; }
}

