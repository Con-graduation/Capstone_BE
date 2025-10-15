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
public class AvailabilityResponseDto {

    @Schema(description = "사용 가능 여부", example = "true")
    private boolean available;

    @Schema(description = "상세 메시지", example = "사용 가능한 사용자명입니다.")
    private String message;
}


