package dailyGuitar.capstone.mcp.server;

import dailyGuitar.capstone.mcp.repository.PracticeRoutineRepository;
import dailyGuitar.capstone.mcp.repository.UserRepository;
import dailyGuitar.capstone.mcp.entity.PracticeRoutine;
import dailyGuitar.capstone.mcp.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MCP 서버의 리소스 및 도구를 제공하는 서비스
 * DB에 직접 접근하여 데이터를 조회/수정합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MCPService {
    
    private final PracticeRoutineRepository practiceRoutineRepository;
    private final UserRepository userRepository;
    
    /**
     * 사용자의 루틴 목록을 시간순으로 조회 (MCP 리소스)
     */
    public List<PracticeRoutine> getRoutinesByUserId(Long userId) {
        log.info("MCP: 사용자 {}의 루틴 조회", userId);
        
        List<PracticeRoutine> routines = practiceRoutineRepository.findByUserId(userId);
        
        // 시간순 정렬 (최근 것이 먼저)
        return routines.stream()
                .sorted((r1, r2) -> {
                    Instant time1 = r1.getLastPracticedAt() != null ? r1.getLastPracticedAt() : r1.getCreatedAt();
                    Instant time2 = r2.getLastPracticedAt() != null ? r2.getLastPracticedAt() : r2.getCreatedAt();
                    if (time1 == null && time2 == null) return 0;
                    if (time1 == null) return 1;
                    if (time2 == null) return -1;
                    return time2.compareTo(time1);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자 ID로 사용자 조회
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    /**
     * 사용자명으로 사용자 조회
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 루틴 ID로 루틴 조회
     */
    public Optional<PracticeRoutine> getRoutineById(Long routineId) {
        return practiceRoutineRepository.findById(routineId);
    }
}

