package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    Optional<UserProfile> findByUser(User user);
    
    Optional<UserProfile> findByUserId(Long userId);

    boolean existsByDisplayName(String displayName);
}

