package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.PracticeRoutine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeRoutineRepository extends JpaRepository<PracticeRoutine, Long> {
	List<PracticeRoutine> findByUserId(Long userId);
}

