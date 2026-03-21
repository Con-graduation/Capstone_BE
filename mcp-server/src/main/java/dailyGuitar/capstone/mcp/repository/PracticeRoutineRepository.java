package dailyGuitar.capstone.mcp.repository;

import dailyGuitar.capstone.mcp.entity.PracticeRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeRoutineRepository extends JpaRepository<PracticeRoutine, Long> {
	List<PracticeRoutine> findByUserId(Long userId);
}

