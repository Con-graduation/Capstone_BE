package dailyGuitar.capstone.dto.practice;

import jakarta.validation.constraints.Size;
import java.util.List;

public class PracticeRoutineUpdateRequestDto {
	@Size(min = 1, max = 100)
	private String title;

	@Size(min = 4, max = 4)
	private List<@Size(max = 4) String> sequence;

	private Integer repeats;
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
