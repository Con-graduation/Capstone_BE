package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.LoginRequestDto;
import dailyGuitar.capstone.dto.LoginResponseDto;
import dailyGuitar.capstone.dto.UserRegistrationDto;
import dailyGuitar.capstone.dto.UserResponseDto;
import dailyGuitar.capstone.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserResponseDto register(UserRegistrationDto registrationDto) {
        return userService.registerUser(registrationDto);
    }

    public boolean checkUsername(String username) {
        return userService.isUsernameAvailable(username);
    }

    public boolean checkEmail(String email) {
        return userService.isEmailAvailable(email);
    }

    public boolean checkNickname(String nickname) {
        return userService.isNicknameAvailable(nickname);
    }

    public void verifyEmail(String email, String code) {
        userService.verifyEmail(email, code);
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto loginRequest) {
        // 인증 수행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken((UserDetails) authentication.getPrincipal());

        // 마지막 로그인 시간 업데이트
        userService.updateLastLogin(loginRequest.getUsername());

        return LoginResponseDto.builder()
                .token(token)
                .build();
    }

    @Transactional
    public void requestEmailCode(String email) {
        userService.issueAndSendEmailVerificationCode(email);
    }
}

