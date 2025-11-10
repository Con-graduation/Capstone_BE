package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.ProfileStatsResponseDto;
import dailyGuitar.capstone.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "프로필 통계 조회", description = "현재 로그인 사용자의 프로필 통계를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProfileStatsResponseDto.class),
                            examples = @ExampleObject(value = "{\n  \"requiredExpForLevel\": 500,\n  \"expToNextLevel\": 120,\n  \"maxAccuracy\": 92,\n  \"totalPracticeCount\": 57,\n  \"totalPracticeMinutes\": 840,\n  \"streakDays\": 30,\n  \"averageAccuracy\": 78\n}")))
    })
    @GetMapping("/stats")
    public ResponseEntity<ProfileStatsResponseDto> getStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ProfileStatsResponseDto dto = userService.getProfileStats(auth.getName());
        return ResponseEntity.ok(dto);
    }
}


