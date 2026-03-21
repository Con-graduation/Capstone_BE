package dailyGuitar.capstone.service;

import dailyGuitar.capstone.dto.UserRegistrationDto;
import dailyGuitar.capstone.dto.UserResponseDto;
import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.entity.UserStatus;
import dailyGuitar.capstone.dto.ProfileStatsResponseDto;
import dailyGuitar.capstone.dto.MainPageResponseDto;
import dailyGuitar.capstone.entity.PracticeSession;
import dailyGuitar.capstone.exception.UserAlreadyExistsException;
import dailyGuitar.capstone.exception.UserNotFoundException;
import dailyGuitar.capstone.repository.UserRepository;
import dailyGuitar.capstone.repository.UserStatusRepository;
import dailyGuitar.capstone.repository.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserStatusRepository userStatusRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        // 이메일로 임시 사용자 찾기
        User existingUser = userRepository.findByEmail(registrationDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 인증이 필요합니다."));
        
        // 임시 사용자인지 확인 (TEMP로 시작하는 username)
        if (!existingUser.getUsername().startsWith("TEMP_")) {
            throw new UserAlreadyExistsException("이미 가입된 이메일입니다.");
        }

        // 사용자명 중복 확인 (임시 사용자 자신 제외)
        Optional<User> userWithSameUsername = userRepository.findByUsername(registrationDto.getUsername());
        if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(existingUser.getId())) {
            throw new UserAlreadyExistsException("이미 사용 중인 사용자명입니다.");
        }

        // 닉네임 중복 확인 (임시 사용자 자신 제외)
        Optional<User> userWithSameNickname = userRepository.findByNickname(registrationDto.getNickname());
        if (userWithSameNickname.isPresent() && !userWithSameNickname.get().getId().equals(existingUser.getId())) {
            throw new UserAlreadyExistsException("이미 사용 중인 닉네임입니다.");
        }

        // 임시 사용자를 실제 사용자로 업데이트
        LocalDateTime now = LocalDateTime.now();
        
        // 모든 필드를 명시적으로 업데이트
        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        existingUser.setName(registrationDto.getName());
        existingUser.setUsername(registrationDto.getUsername());
        existingUser.setNickname(registrationDto.getNickname());
        existingUser.setPassword(encodedPassword);
        existingUser.setEmailVerified(true);
        existingUser.setEmailVerifiedAt(now);
        existingUser.setEmailVerificationCode(null);
        existingUser.setEmailVerificationExpiresAt(null);
        existingUser.setUpdatedAt(now);

        // 저장 및 즉시 DB에 반영
        User savedUser = userRepository.save(existingUser);
        userRepository.flush(); // 즉시 DB에 반영하여 변경사항 확실히 저장

        // UserStatus 초기화 (회원가입 시 생성)
        UserStatus userStatus = new UserStatus();
        userStatus.setUser(savedUser);
        userStatus.setLevel(1);
        userStatus.setTotalPracticeCount(0L);
        userStatus.setTotalPracticeSeconds(0L);
        userStatus.setStreakDays(0);
        userStatus.setOverallAccuracy(0);
        userStatus.setMaxAccuracy(0);
        userStatus.setTotalExperience(0L);
        userStatusRepository.save(userStatus);

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
        long timestamp = System.currentTimeMillis();
        
        // 임시 사용자 생성 (emailVerified는 false, 나머지는 기본값)
        // nickname도 고유하게 생성하여 동시 요청 시 중복 방지
        User tempUser = User.builder()
                .email(email)
                .name("TEMP") // 임시값
                .nickname("TEMP_" + timestamp) // 임시값 (고유하게 생성)
                .username("TEMP_" + timestamp) // 임시값
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

    public void updateProfileImageObjectKey(String username, String objectKey) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
        user.setProfileImageObjectKey(objectKey);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String getProfileImageObjectKey(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return user.getProfileImageObjectKey();
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

    @Transactional(readOnly = true)
    public ProfileStatsResponseDto getProfileStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
        UserStatus status = userStatusRepository.findByUser(user)
                .orElseGet(() -> {
                    UserStatus s = new UserStatus();
                    s.setUser(user);
                    s.setLevel(1);
                    return s;
                });

        int level = status.getLevel() == null ? 1 : status.getLevel();
        long totalSeconds = status.getTotalPracticeSeconds() == null ? 0L : status.getTotalPracticeSeconds();

        int required = requiredExpForLevel(level);
        int toNext = expToNextLevel(level, status.getTotalExperience() == null ? 0L : status.getTotalExperience());

        return ProfileStatsResponseDto.builder()
                .requiredExpForLevel(required)
                .expToNextLevel(toNext)
                .maxAccuracy(nullSafe(status.getMaxAccuracy()))
                .totalPracticeCount(nullSafe(status.getTotalPracticeCount()))
                .totalPracticeMinutes(totalSeconds / 60)
                .streakDays(nullSafe(status.getStreakDays()))
                .averageAccuracy(nullSafe(status.getOverallAccuracy()))
                .build();
    }

    private int requiredExpForLevel(int level) {
        if (level >= 50) return 0; // 만렙
        if (level <= 10) return 200;
        if (level <= 30) return 500;
        if (level <= 40) return 1000;
        return 2000; // 41~49
    }

    private int expToNextLevel(int level, long totalExp) {
        if (level >= 50) return 0;
        // 누적 경험치에서 현재 레벨에 필요한 경험치만큼 남은 양을 단순 계산
        int req = requiredExpForLevel(level);
        long mod = totalExp % req; // 현재 레벨 진행도
        return (int) (req - mod);
    }

    @Transactional(readOnly = true)
    public MainPageResponseDto getMainPageInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        // 1. 스트릭 정보 가져오기
        UserStatus status = userStatusRepository.findByUser(user)
                .orElseGet(() -> {
                    UserStatus s = new UserStatus();
                    s.setUser(user);
                    s.setLevel(1);
                    s.setStreakDays(0);
                    return s;
                });
        Integer streakDays = nullSafe(status.getStreakDays());
        
        // 2. 최근 일주일간 루틴 완료 횟수 (날짜별)
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minusSeconds(7 * 24 * 60 * 60); // 7일 전
        
        List<PracticeSession> sessions = practiceSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(session -> session.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        
        // 날짜별로 집계 (한국 시간대 기준)
        Map<String, Integer> weeklyPracticeCount = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        
        // 최근 7일간의 날짜 초기화 (0으로 시작)
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now(koreaZone).minusDays(i);
            weeklyPracticeCount.put(date.format(dateFormatter), 0);
        }
        
        // 세션을 날짜별로 집계
        for (PracticeSession session : sessions) {
            LocalDate sessionDate = session.getCreatedAt()
                    .atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(koreaZone)
                    .toLocalDate();
            String dateKey = sessionDate.format(dateFormatter);
            weeklyPracticeCount.put(dateKey, weeklyPracticeCount.getOrDefault(dateKey, 0) + 1);
        }
        
        return MainPageResponseDto.builder()
                .streakDays(streakDays)
                .weeklyPracticeCount(weeklyPracticeCount)
                .build();
    }

    private int nullSafe(Integer v) { return v == null ? 0 : v; }
    private long nullSafe(Long v) { return v == null ? 0L : v; }
}

