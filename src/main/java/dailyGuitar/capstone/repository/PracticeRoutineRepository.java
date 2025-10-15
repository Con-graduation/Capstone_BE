package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.PracticeRoutine;
import dailyGuitar.capstone.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeRoutineRepository extends JpaRepository<PracticeRoutine, Long> {
    
    List<PracticeRoutine> findByUserAndIsActiveTrueOrderByUpdatedAtDesc(User user);
    
    Page<PracticeRoutine> findByIsPublicTrueAndIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    List<PracticeRoutine> findByUserAndRoutineTypeAndIsActiveTrue(User user, PracticeRoutine.RoutineType routineType);
    
    @Query("SELECT pr FROM PracticeRoutine pr WHERE pr.user = :user AND pr.isActive = true ORDER BY pr.createdAt DESC")
    List<PracticeRoutine> findActiveRoutinesByUser(@Param("user") User user);
}

