package tn.esprit.pi.nexlance.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.pi.nexlance.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);
    
    Page<User> findByType(User.UserType type, Pageable pageable);

    List<User> findAllByType(User.UserType type);
    
    Page<User> findByStatusNot(User.UserStatus status, Pageable pageable);
    
    // Admin Management Methods
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findBySearch(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.type = :type AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findBySearchAndType(@Param("search") String search, @Param("type") User.UserType type, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findBySearchAndStatus(@Param("search") String search, @Param("status") User.UserStatus status, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.type = :type AND u.status = :status AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findBySearchAndTypeAndStatus(@Param("search") String search, @Param("type") User.UserType type, @Param("status") User.UserStatus status, Pageable pageable);
    
    Page<User> findByTypeAndStatus(User.UserType type, User.UserStatus status, Pageable pageable);
    
    Long countByStatus(User.UserStatus status);
    
    Long countByType(User.UserType type);
}

