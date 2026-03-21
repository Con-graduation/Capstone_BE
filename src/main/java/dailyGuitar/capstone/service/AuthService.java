package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.LoginRequestDto;
import dailyGuitar.capstone.dto.LoginResponseDto;
import dailyGuitar.capstone.dto.UserRegistrationDto;
import dailyGuitar.capstone.dto.UserResponseDto;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.entity.UserStatus;
import dailyGuitar.capstone.exception.UserNotFoundException;
import dailyGuitar.capstone.repository.UserRepository;
import dailyGuitar.capstone.repository.UserStatusRepository;
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
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;

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

        // 사용자 정보 조회
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + loginRequest.getUsername()));
        Integer level = userStatusRepository.findByUser(user)
                .map(UserStatus::getLevel)
                .orElse(1);

        return LoginResponseDto.builder()
                .token(token)
                .name(user.getName())
                .nickname(user.getNickname())
                .level(level)
                .build();
    }

    @Transactional
    public void requestEmailCode(String email) {
        userService.issueAndSendEmailVerificationCode(email);
    }
}

