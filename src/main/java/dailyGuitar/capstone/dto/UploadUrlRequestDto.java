package dailyGuitar.capstone.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadUrlRequestDto {

    @NotBlank(message = "contentType은 필수입니다.")
    @Schema(example = "image/png", description = "파일의 MIME 타입")
    private String contentType;

    @NotBlank(message = "filename은 필수입니다.")
    @Schema(example = "avatar.png", description = "파일명")
    private String filename;
}

