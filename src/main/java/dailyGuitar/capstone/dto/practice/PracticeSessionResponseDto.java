package dailyGuitar.capstone.dto.practice;

import dailyGuitar.capstone.entity.PracticeSession;
import lombok.Data;

import java.time.Instant;

@Data
public class PracticeSessionResponseDto {
	private Long id;
	private Long userId;
	private Long routineId;
	private Integer rhythmAccuracy;
	private Integer pitchAccuracy;
	private String rhythmSectionScores;
	private String pitchSectionScores;
	private PracticeSession.Section worstRhythmSection;
	private PracticeSession.Section worstPitchSection;
	private Instant createdAt;
	
	public static PracticeSessionResponseDto from(PracticeSession session) {
		PracticeSessionResponseDto dto = new PracticeSessionResponseDto();
		dto.setId(session.getId());
		dto.setUserId(session.getUserId());
		dto.setRoutineId(session.getRoutineId());
		dto.setRhythmAccuracy(session.getRhythmAccuracy());
		dto.setPitchAccuracy(session.getPitchAccuracy());
		dto.setRhythmSectionScores(session.getRhythmSectionScores());
		dto.setPitchSectionScores(session.getPitchSectionScores());
		dto.setWorstRhythmSection(session.getWorstRhythmSection());
		dto.setWorstPitchSection(session.getWorstPitchSection());
		dto.setCreatedAt(session.getCreatedAt());
		return dto;
	}
}
