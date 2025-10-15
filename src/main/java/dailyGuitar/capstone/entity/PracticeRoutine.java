package dailyGuitar.capstone.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

	@Column(nullable = false, length = 100)
	private String title; // 루틴 이름

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoutineType routineType; // CHORD_CHANGE, CHROMATIC

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "practice_routine_sequence", joinColumns = @JoinColumn(name = "routine_id"))
	@Column(name = "step", length = 10, nullable = false)
	private List<String> sequence = new ArrayList<>(); // 코드/손가락 순서

	@Column(nullable = false)
	private Integer repeats; // 5,10,20

	@Column(nullable = false)
	private Integer bpm; // 타입별 제약

	@Column(nullable = false)
	private Long practiceCount = 0L; // 총 연습 횟수

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
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = Instant.now();
	}

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

	public void setUserId(Long userId) { this.userId = userId; }
	public void setTitle(String title) { this.title = title; }
	public void setRoutineType(RoutineType routineType) { this.routineType = routineType; }
	public void setSequence(List<String> sequence) { this.sequence = sequence; }
	public void setRepeats(Integer repeats) { this.repeats = repeats; }
	public void setBpm(Integer bpm) { this.bpm = bpm; }
	public void setPracticeCount(Long practiceCount) { this.practiceCount = practiceCount; }
	public void setLastPracticedAt(Instant lastPracticedAt) { this.lastPracticedAt = lastPracticedAt; }
}

