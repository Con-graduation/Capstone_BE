package dailyGuitar.capstone.dto.practice;

import dailyGuitar.capstone.entity.RoutineType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(name = "PracticeRoutineCreateRequest", description = "연습 루틴 생성 요청 DTO")
public class PracticeRoutineCreateRequestDto {
	@Schema(description = "루틴 제목", example = "코드 전환 루틴 A", maxLength = 100)
	@NotBlank
	@Size(min = 1, max = 100)
	private String title;

	@Schema(description = "연습 유형", example = "CHORD_CHANGE, CHROMATIC")
	@NotNull
	private RoutineType routineType;

	@Schema(description = "순서 (코드 또는 손가락)", example = "[\"C\",\"E\",\"D\",\"F\"]")
	@NotNull
	@Size(min = 4, max = 4)
	private List<@NotBlank @Size(max = 4) String> sequence;

	@Schema(description = "반복 횟수 (프론트에서 제약)", example = "10")
	@NotNull
	@Min(1)
	private Integer repeats;

	@Schema(description = "BPM (프론트에서 제약)", example = "60")
	@NotNull
	private Integer bpm;

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
