package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.service.PresignedUrlService;
import dailyGuitar.capstone.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile-image")
public class ProfileImageController {
	private final PresignedUrlService presignedUrlService;
	private final UserService userService;

	public ProfileImageController(PresignedUrlService presignedUrlService, UserService userService) {
		this.presignedUrlService = presignedUrlService;
		this.userService = userService;
	}

	@Operation(summary = "프로필 이미지 업로드 URL 발급", description = "contentType과 filename을 받아 S3 PUT presigned URL을 반환합니다.")
	@PostMapping("/upload-url")
	public ResponseEntity<Map<String, String>> createUploadUrl(
			@RequestParam @NotBlank @Schema(example = "image/png") String contentType,
			@RequestParam @NotBlank @Schema(example = "avatar.png") String filename
	) {
		PresignedUrlService.Result result = presignedUrlService.createUploadUrl(contentType, filename);
		return ResponseEntity.ok(Map.of(
				"uploadUrl", result.uploadUrl(),
				"objectKey", result.objectKey()
		));
	}

	@Operation(summary = "업로드 완료 확인", description = "프론트가 업로드 완료 후 objectKey를 보내면 사용자 프로필에 저장합니다.")
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirmUpload(@RequestParam @NotBlank String objectKey) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userService.updateProfileImageObjectKey(auth.getName(), objectKey);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "프로필 이미지 다운로드 URL 발급", description = "현재 로그인한 사용자의 프로필 이미지 다운로드용 presigned URL을 반환합니다.")
	@GetMapping("/download-url")
	public ResponseEntity<Map<String, String>> getDownloadUrl() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String username = auth.getName();
		
		String objectKey = userService.getProfileImageObjectKey(username);
		if (objectKey == null || objectKey.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		
		String downloadUrl = presignedUrlService.createDownloadUrl(objectKey);
		return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
	}
}
