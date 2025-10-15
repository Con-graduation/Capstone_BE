package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.UserRegistrationDto;
import dailyGuitar.capstone.dto.UserResponseDto;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.exception.UserAlreadyExistsException;
import dailyGuitar.capstone.exception.UserNotFoundException;
import dailyGuitar.capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        // 이메일로 임시 사용자 찾기
        User existingUser = userRepository.findByEmail(registrationDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 인증이 필요합니다."));
        
        // 임시 사용자인지 확인 (TEMP로 시작하는 username)
        if (!existingUser.getUsername().startsWith("TEMP_")) {
            throw new UserAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        // 사용자명 중복 확인
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("이미 사용 중인 사용자명입니다.");
        }

        // 닉네임 중복 확인 (User 도메인에서 관리)
        if (userRepository.existsByNickname(registrationDto.getNickname())) {
            throw new UserAlreadyExistsException("이미 사용 중인 닉네임입니다.");
        }

        // 임시 사용자를 실제 사용자로 업데이트
        existingUser.setName(registrationDto.getName());
        existingUser.setUsername(registrationDto.getUsername());
        existingUser.setNickname(registrationDto.getNickname());
        existingUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        existingUser.setEmailVerified(true);
        existingUser.setEmailVerifiedAt(LocalDateTime.now());
        existingUser.setEmailVerificationCode(null);
        existingUser.setEmailVerificationExpiresAt(null);

        User savedUser = userRepository.save(existingUser);

        return UserResponseDto.from(savedUser);
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(User::getEmailVerified)
                .orElse(false);
    }

    public void verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + email));

        if (user.getEmailVerificationCode() == null || user.getEmailVerificationExpiresAt() == null) {
            throw new IllegalStateException("인증 코드가 없습니다. 다시 요청해주세요.");
        }

        if (LocalDateTime.now().isAfter(user.getEmailVerificationExpiresAt())) {
            throw new IllegalStateException("인증 코드가 만료되었습니다.");
        }

        if (!user.getEmailVerificationCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void issueAndSendEmailVerificationCode(String email) {
        // 기존 사용자 확인 및 삭제
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getUsername().startsWith("TEMP_")) {
                // 임시 사용자 삭제 (flush로 즉시 반영)
                userRepository.delete(user);
                userRepository.flush(); // 즉시 DB에 반영
            } else {
                // 실제 가입된 사용자
                throw new IllegalArgumentException("이미 가입된 이메일입니다.");
            }
        }

        // 임시 인증 코드 저장을 위한 임시 사용자 생성 (가입 전 단계)
        String verificationCode = String.format("%06d", new Random().nextInt(1_000_000));
        
        // 임시 사용자 생성 (emailVerified는 false, 나머지는 기본값)
        User tempUser = User.builder()
                .email(email)
                .name("TEMP") // 임시값
                .nickname("TEMP") // 임시값
                .username("TEMP_" + System.currentTimeMillis()) // 임시값
                .password("TEMP") // 임시값
                .emailVerified(false)
                .emailVerificationCode(verificationCode)
                .emailVerificationExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        
        userRepository.save(tempUser);

        String subject = "[DailyGuitar] 이메일 인증 코드";
        String body = "<p>회원가입을 위한 이메일 인증 코드를 발송합니다.</p>" +
                "<p>아래 인증코드를 10분 내에 입력해 주세요:</p>" +
                "<h2>" + verificationCode + "</h2>";
        emailService.send(email, subject, body);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));
        return UserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return UserResponseDto.from(user);
    }

    public void updateLastLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + id));
    }

    @Transactional(readOnly = true)
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
}

