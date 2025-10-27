package dailyGuitar.capstone.dto.practice;

import lombok.Data;

import java.util.List;

@Data
public class PracticeReportResponseDto {
	// 박자 정확도 정보
	private Integer rhythmAccuracy;
	private String rhythmFeedback;
	private List<PreviousPracticeDto> rhythmHistory;
	private String rhythmComparison; // "직전 연습보다 5% 향상"
	private String rhythmWorstSection; // "초반에서 연주가 가장 불안정했어요"
	private String rhythmBpmAdvice; // BPM 조정 조언
	
	// 음정 정확도 정보
	private Integer pitchAccuracy;
	private String pitchFeedback;
	private List<PreviousPracticeDto> pitchHistory;
	private String pitchComparison; // "직전 연습보다 3% 향상"
	private String pitchWorstSection; // "초반에서 연주가 가장 불都정했어요"
	private String pitchDifficultyAdvice; // 어려운 조합 조언
	
	// 종합 피드백
	private String overallFeedback;
	
	@Data
	public static class PreviousPracticeDto {
		private Integer accuracy;
		private String practicedAt;
	}
}

