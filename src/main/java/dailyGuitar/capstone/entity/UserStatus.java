package dailyGuitar.capstone.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_status")
public class UserStatus {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(nullable = false)
	private Integer level = 1; // 사용자 레벨

	@Column(nullable = false)
	private Long totalPracticeCount = 0L; // 총 연습 횟수

	@Column(nullable = false)
	private Long totalPracticeSeconds = 0L; // 총 연습 시간(초)

	@Column(nullable = false)
	private Integer streakDays = 0; // 연속 streak(일)

	@Column(nullable = false)
	private Integer overallAccuracy = 0; // 전체 정확도(0~100)

	@Column(nullable = false)
	private Integer maxAccuracy = 0; // 최고 정확도(0~100)

	@Column(nullable = false)
	private Long totalExperience = 0L; // 누적 경험치

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@PreUpdate
	public void onUpdate() { this.updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public User getUser() { return user; }
	public Integer getLevel() { return level; }
	public Long getTotalPracticeCount() { return totalPracticeCount; }
	public Long getTotalPracticeSeconds() { return totalPracticeSeconds; }
	public Integer getStreakDays() { return streakDays; }
	public Integer getOverallAccuracy() { return overallAccuracy; }
	public Integer getMaxAccuracy() { return maxAccuracy; }
	public Long getTotalExperience() { return totalExperience; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public void setUser(User user) { this.user = user; }
	public void setLevel(Integer level) { this.level = level; }
	public void setTotalPracticeCount(Long totalPracticeCount) { this.totalPracticeCount = totalPracticeCount; }
	public void setTotalPracticeSeconds(Long totalPracticeSeconds) { this.totalPracticeSeconds = totalPracticeSeconds; }
	public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
	public void setOverallAccuracy(Integer overallAccuracy) { this.overallAccuracy = overallAccuracy; }
	public void setMaxAccuracy(Integer maxAccuracy) { this.maxAccuracy = maxAccuracy; }
	public void setTotalExperience(Long totalExperience) { this.totalExperience = totalExperience; }
}
