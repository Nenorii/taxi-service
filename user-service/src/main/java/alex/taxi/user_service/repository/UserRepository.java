package alex.taxi.user_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import alex.taxi.user_service.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByIdAndUserType(Long id, String userType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userType = 'DRIVER' AND u.status = :status ORDER BY u.id ASC LIMIT 1")
    Optional<User> findFirstDriverByStatus(@Param("status") String status);

    @Query("SELECT u FROM User u WHERE u.userType = 'DRIVER' AND u.status = :status")
    List<User> findDriversByStatus(@Param("status") String status);
}
