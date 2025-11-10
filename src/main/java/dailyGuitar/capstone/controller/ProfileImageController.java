package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.ConfirmUploadRequestDto;
import dailyGuitar.capstone.dto.UploadUrlRequestDto;
import dailyGuitar.capstone.service.PresignedUrlService;
import dailyGuitar.capstone.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\n  \"uploadUrl\": \"https://s3.amazonaws.com/bucket/profiles/uuid?X-Amz-Algorithm=...\",\n  \"objectKey\": \"profiles/550e8400-e29b-41d4-a716-446655440000.jpg\"\n}"))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"Bad Request\",\n  \"message\": \"허용되지 않은 파일 확장자입니다. 허용: jpg, jpeg, png, webp\"\n}"))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
	@PostMapping("/upload-url")
	public ResponseEntity<Map<String, String>> createUploadUrl(@Valid @RequestBody UploadUrlRequestDto request) {
		PresignedUrlService.Result result = presignedUrlService.createUploadUrl(request.getContentType(), request.getFilename());
		return ResponseEntity.ok(Map.of(
				"uploadUrl", result.uploadUrl(),
				"objectKey", result.objectKey()
		));
	}

	@Operation(summary = "업로드 완료 확인", description = "프론트가 업로드 완료 후 objectKey를 보내면 사용자 프로필에 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공 - 내용 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\n  \"error\": \"Bad Request\",\n  \"message\": \"objectKey는 필수입니다\"\n}"))),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirmUpload(@Valid @RequestBody ConfirmUploadRequestDto request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userService.updateProfileImageObjectKey(auth.getName(), request.getObjectKey());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "프로필 이미지 다운로드 URL 발급", description = "현재 로그인한 사용자의 프로필 이미지 다운로드용 presigned URL을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\n  \"downloadUrl\": \"https://s3.amazonaws.com/bucket/profiles/550e8400-e29b-41d4-a716-446655440000.jpg?X-Amz-Algorithm=...\"\n}"))),
            @ApiResponse(responseCode = "404", description = "프로필 이미지 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
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
