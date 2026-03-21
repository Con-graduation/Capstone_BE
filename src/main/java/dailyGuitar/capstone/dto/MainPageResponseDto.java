package dailyGuitar.capstone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메인 페이지 정보 응답 DTO")
public class MainPageResponseDto {

    @Schema(description = "유저의 연속 스트릭 일수", example = "7")
    private Integer streakDays;

    @Schema(description = "최근 일주일간 루틴 완료 횟수 (날짜별)", 
            example = "{\"2025-11-08\": 2, \"2025-11-09\": 1, \"2025-11-10\": 3, \"2025-11-11\": 0, \"2025-11-12\": 1, \"2025-11-13\": 2, \"2025-11-14\": 1}")
    private Map<String, Integer> weeklyPracticeCount;
}

