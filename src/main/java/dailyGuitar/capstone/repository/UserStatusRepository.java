package dailyGuitar.capstone.repository;

import dailyGuitar.capstone.entity.User;
import dailyGuitar.capstone.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
	Optional<UserStatus> findByUser(User user);
}

