package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.PracticeSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {
	
	// 사용자의 모든 연습 기록 조회 (최신순)
	List<PracticeSession> findByUserIdOrderByCreatedAtDesc(Long userId);
	
	// 특정 루틴의 연습 기록 조회
	List<PracticeSession> findByUserIdAndRoutineIdOrderByCreatedAtDesc(Long userId, Long routineId);
	
	// 페이징 지원
	Page<PracticeSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

