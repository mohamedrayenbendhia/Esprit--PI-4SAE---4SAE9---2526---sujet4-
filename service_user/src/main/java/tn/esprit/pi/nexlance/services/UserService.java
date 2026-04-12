package tn.esprit.pi.nexlance.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nexlance.dto.auth.ChangePasswordRequest;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setEmailVerified(false);
        user.setStatus(User.UserStatus.PENDING_VERIFICATION);
        user.setSubscriptionType(User.SubscriptionType.FREE);
        return userRepository.save(user);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findByStatusNot(User.UserStatus.DELETED, pageable);
    }

    public Page<User> getUsersByType(User.UserType type, Pageable pageable) {
        return userRepository.findByType(type, pageable);
    }

    public Page<User> getUsersByStatus(User.UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    @Transactional
    public User updateUser(UUID id, User userDetails) {
        User user = getUserById(id);
        
        if (userDetails.getFirstName() != null) {
            user.setFirstName(userDetails.getFirstName());
        }
        if (userDetails.getLastName() != null) {
            user.setLastName(userDetails.getLastName());
        }
        if (userDetails.getPhoneNumber() != null) {
            user.setPhoneNumber(userDetails.getPhoneNumber());
        }
        if (userDetails.getAvatar() != null) {
            user.setAvatar(userDetails.getAvatar());
        }
        if (userDetails.getSubscriptionType() != null) {
            user.setSubscriptionType(userDetails.getSubscriptionType());
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(UUID id, String newPassword) {
        User user = getUserById(id);
        user.setPassword(newPassword); // Should be hashed before calling this method
        return userRepository.save(user);
    }

    @Transactional
    public void changeMyPassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Verify new password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public User updateStatus(UUID id, User.UserStatus status) {
        User user = getUserById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
    }

    @Transactional
    public User verifyEmail(UUID id) {
        User user = getUserById(id);
        user.setEmailVerified(true);
        if (user.getStatus() == User.UserStatus.PENDING_VERIFICATION) {
            user.setStatus(User.UserStatus.ACTIVE);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateLastLogin(UUID id) {
        User user = getUserById(id);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
