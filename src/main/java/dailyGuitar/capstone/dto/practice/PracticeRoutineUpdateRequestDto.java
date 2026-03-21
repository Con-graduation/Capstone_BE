package dailyGuitar.capstone.dto.practice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(name = "PracticeRoutineUpdateRequest", description = "연습 루틴 수정 요청 DTO")
public class PracticeRoutineUpdateRequestDto {
	@Schema(description = "루틴 제목", example = "코드 전환 루틴 A(수정)", maxLength = 100)
	@Size(min = 1, max = 100)
	private String title;

	@Schema(description = "순서 (코드 또는 손가락)", example = "[\"C\",\"E\",\"D\",\"F\"]")
	@Size(min = 4, max = 4)
	private List<@Size(max = 4) String> sequence;

	@Schema(description = "반복 횟수 (프론트 제약)", example = "20")
	private Integer repeats;

	@Schema(description = "BPM (프론트 제약)", example = "80")
	private Integer bpm;

	public String getTitle() { return title; }
	public List<String> getSequence() { return sequence; }
	public Integer getRepeats() { return repeats; }
	public Integer getBpm() { return bpm; }

	public void setTitle(String title) { this.title = title; }
	public void setSequence(List<String> sequence) { this.sequence = sequence; }
	public void setRepeats(Integer repeats) { this.repeats = repeats; }
	public void setBpm(Integer bpm) { this.bpm = bpm; }
}
