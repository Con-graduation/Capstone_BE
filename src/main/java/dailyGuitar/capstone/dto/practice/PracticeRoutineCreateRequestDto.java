package dailyGuitar.capstone.dto.practice;

import dailyGuitar.capstone.entity.RoutineType;
import jakarta.validation.constraints.*;
import java.util.List;

public class PracticeRoutineCreateRequestDto {
	@NotBlank
	@Size(min = 1, max = 100)
	private String title;

	@NotNull
	private RoutineType routineType;

	@NotNull
	@Size(min = 4, max = 4)
	private List<@NotBlank @Size(max = 4) String> sequence;

	@NotNull
	@Min(1)
	private Integer repeats; // 서버에서 허용값 검증

	@NotNull
	private Integer bpm; // 서버에서 타입별 규칙 검증

	public String getTitle() { return title; }
	public RoutineType getRoutineType() { return routineType; }
	public List<String> getSequence() { return sequence; }
	public Integer getRepeats() { return repeats; }
	public Integer getBpm() { return bpm; }

	public void setTitle(String title) { this.title = title; }
	public void setRoutineType(RoutineType routineType) { this.routineType = routineType; }
	public void setSequence(List<String> sequence) { this.sequence = sequence; }
	public void setRepeats(Integer repeats) { this.repeats = repeats; }
	public void setBpm(Integer bpm) { this.bpm = bpm; }
}
