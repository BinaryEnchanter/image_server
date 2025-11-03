package image.server.image_server.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import image.server.image_server.model.User;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.uuid = :uuid")
    Optional<User> findByUuidForUpdate(UUID uuid);
    
}
