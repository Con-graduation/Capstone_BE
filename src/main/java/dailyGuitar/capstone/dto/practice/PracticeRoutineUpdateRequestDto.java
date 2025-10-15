package dailyGuitar.capstone.dto.practice;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class PracticeRoutineUpdateRequestDto {
	@NotNull
	private Long id;

	@Size(min = 1, max = 100)
	private String title;

	@Size(min = 4, max = 4)
	private List<@Size(max = 4) String> sequence;

	private Integer repeats;
	private Integer bpm;

	public Long getId() { return id; }
	public String getTitle() { return title; }
	public List<String> getSequence() { return sequence; }
	public Integer getRepeats() { return repeats; }
	public Integer getBpm() { return bpm; }

	public void setId(Long id) { this.id = id; }
	public void setTitle(String title) { this.title = title; }
	public void setSequence(List<String> sequence) { this.sequence = sequence; }
	public void setRepeats(Integer repeats) { this.repeats = repeats; }
	public void setBpm(Integer bpm) { this.bpm = bpm; }
}
