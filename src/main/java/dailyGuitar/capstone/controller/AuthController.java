package dailyGuitar.capstone.controller;

import dailyGuitar.capstone.dto.LoginRequestDto;
import dailyGuitar.capstone.dto.LoginResponseDto;
import dailyGuitar.capstone.dto.UserRegistrationDto;
import dailyGuitar.capstone.dto.UserResponseDto;
import dailyGuitar.capstone.dto.AvailabilityResponseDto;
import dailyGuitar.capstone.dto.EmailCodeRequestDto;
import dailyGuitar.capstone.dto.VerifyEmailRequestDto;
import dailyGuitar.capstone.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "사용자 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일 인증 완료 후 새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "이메일 인증이 필요하거나 잘못된 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자명 또는 닉네임")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        UserResponseDto user = authService.register(registrationDto);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "닉네임 사용 가능 여부 확인", description = "닉네임 중복 여부를 확인합니다.")
    @GetMapping("/check/nickname")
    public ResponseEntity<AvailabilityResponseDto> checkNickname(@RequestParam String nickname) {
        boolean available = authService.checkNickname(nickname);
        String message = available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다.";
        return ResponseEntity.ok(AvailabilityResponseDto.builder().available(available).message(message).build());
    }

    @GetMapping("/check/username")
    public ResponseEntity<AvailabilityResponseDto> checkUsername(@RequestParam String username) {
        boolean available = authService.checkUsername(username);
        String message = available ? "사용 가능한 사용자명입니다." : "이미 사용 중인 사용자명입니다.";
        return ResponseEntity.ok(AvailabilityResponseDto.builder().available(available).message(message).build());
    }

    @GetMapping("/check/email")
    public ResponseEntity<AvailabilityResponseDto> checkEmail(@RequestParam String email) {
        boolean available = authService.checkEmail(email);
        String message = available ? "사용 가능한 이메일입니다." : "이미 가입된 이메일입니다.";
        return ResponseEntity.ok(AvailabilityResponseDto.builder().available(available).message(message).build());
    }

    @Operation(summary = "이메일 인증", description = "발송된 6자리 인증코드로 이메일을 인증합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증코드이거나 만료된 코드")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<AvailabilityResponseDto> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto request) {
        authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(AvailabilityResponseDto.builder()
                .available(true)
                .message("이메일 인증이 완료되었습니다. 이제 회원가입을 진행할 수 있습니다.")
                .build());
    }

    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        LoginResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 인증코드 발송", description = "회원가입을 위한 이메일 인증코드를 발송합니다. 가입되지 않은 이메일만 요청 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증코드 발송 성공"),
            @ApiResponse(responseCode = "400", description = "이미 가입된 이메일이거나 잘못된 요청 데이터")
    })
    @PostMapping("/request-email-code")
    public ResponseEntity<AvailabilityResponseDto> requestEmailCode(@Valid @RequestBody EmailCodeRequestDto request) {
        authService.requestEmailCode(request.getEmail());
        return ResponseEntity.ok(AvailabilityResponseDto.builder()
                .available(true)
                .message("인증 코드가 이메일로 발송되었습니다. 10분 내에 인증해 주세요.")
                .build());
    }
}

