package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.PracticeSession;
import dailyGuitar.capstone.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {
    
    Page<PracticeSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<PracticeSession> findByUserAndStatusOrderByCreatedAtDesc(User user, PracticeSession.SessionStatus status);
    
    @Query("SELECT ps FROM PracticeSession ps WHERE ps.user = :user AND ps.createdAt >= :startDate AND ps.createdAt < :endDate")
    List<PracticeSession> findByUserAndDateRange(@Param("user") User user, 
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(ps) FROM PracticeSession ps WHERE ps.user = :user AND ps.status = 'COMPLETED' AND ps.createdAt >= :startDate")
    Long countCompletedSessionsByUserAndDate(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT AVG(ps.overallScore) FROM PracticeSession ps WHERE ps.user = :user AND ps.status = 'COMPLETED' AND ps.createdAt >= :startDate")
    Double getAverageScoreByUserAndDate(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}

