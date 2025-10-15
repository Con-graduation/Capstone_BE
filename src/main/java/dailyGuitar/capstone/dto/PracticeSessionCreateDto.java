package dailyGuitar.capstone.dto;

import dailyGuitar.capstone.entity.PracticeSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSessionCreateDto {

    @NotBlank(message = "세션명은 필수입니다.")
    private String sessionName;

    @NotNull(message = "세션 타입은 필수입니다.")
    private PracticeSession.SessionType sessionType;

    private Long routineId;

    private Integer targetBpm;
}

