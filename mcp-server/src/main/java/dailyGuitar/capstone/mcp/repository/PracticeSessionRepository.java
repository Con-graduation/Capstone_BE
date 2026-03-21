package dailyGuitar.capstone.mcp.repository;

import dailyGuitar.capstone.mcp.entity.PracticeSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {
	List<PracticeSession> findByUserIdOrderByCreatedAtDesc(Long userId);
	List<PracticeSession> findByUserIdAndRoutineIdOrderByCreatedAtDesc(Long userId, Long routineId);
	Page<PracticeSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}

