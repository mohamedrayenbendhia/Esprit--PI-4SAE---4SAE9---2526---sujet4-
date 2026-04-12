package tn.esprit.pi.nexlance.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserManagementService {

    private final UserRepository userRepository;

    public Page<User> findUsers(String search, User.UserType type, User.UserStatus status, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            if (type != null && status != null) {
                return userRepository.findBySearchAndTypeAndStatus(search, type, status, pageable);
            } else if (type != null) {
                return userRepository.findBySearchAndType(search, type, pageable);
            } else if (status != null) {
                return userRepository.findBySearchAndStatus(search, status, pageable);
            } else {
                return userRepository.findBySearch(search, pageable);
            }
        } else {
            if (type != null && status != null) {
                return userRepository.findByTypeAndStatus(type, status, pageable);
            } else if (type != null) {
                return userRepository.findByType(type, pageable);
            } else if (status != null) {
                return userRepository.findByStatus(status, pageable);
            } else {
                return userRepository.findAll(pageable);
            }
        }
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public void updateStatus(UUID id, User.UserStatus status) {
        User user = findById(id);
        
        // Validate admin cannot be deleted
        if (user.getType() == User.UserType.ADMIN && status == User.UserStatus.DELETED) {
            throw new RuntimeException("Cannot delete an admin user");
        }
        
        user.setStatus(status);
        userRepository.save(user);
        
        log.info("User {} status updated to {}", id, status);
    }

    public Map<String, Long> getUserStatistics() {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("total", userRepository.count());
        stats.put("active", userRepository.countByStatus(User.UserStatus.ACTIVE));
        stats.put("suspended", userRepository.countByStatus(User.UserStatus.SUSPENDED));
        stats.put("deleted", userRepository.countByStatus(User.UserStatus.DELETED));
        stats.put("clients", userRepository.countByType(User.UserType.CLIENT));
        stats.put("freelancers", userRepository.countByType(User.UserType.FREELANCE));
        stats.put("admins", userRepository.countByType(User.UserType.ADMIN));
        
        return stats;
    }
}
