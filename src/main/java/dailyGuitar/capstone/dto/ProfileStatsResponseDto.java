package dailyGuitar.capstone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatsResponseDto {

    @Schema(description = "현재 레벨의 요구 경험치 (만렙은 0)")
    private Integer requiredExpForLevel;

    @Schema(description = "레벨업까지 남은 경험치 (만렙은 0)")
    private Integer expToNextLevel;

    @Schema(description = "최고 정확도(%)")
    private Integer maxAccuracy;

    @Schema(description = "총 연습 횟수")
    private Long totalPracticeCount;

    @Schema(description = "총 연습 시간(분)")
    private Long totalPracticeMinutes;

    @Schema(description = "연속 스트릭(일)")
    private Integer streakDays;

    @Schema(description = "평균 정확도(%)")
    private Integer averageAccuracy;
}


