package tn.esprit.pi.nexlance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.pi.nexlance.dto.auth.ChangePasswordRequest;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.repositories.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setType(User.UserType.FREELANCE);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setSubscriptionType(User.SubscriptionType.FREE);
        user.setEmailVerified(true);
        user.setTwoFactorEnabled(false);
    }

    @Test
    void createUser_Success() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.createUser(user);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(user));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getUserById(userId));

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getUserByEmail("unknown@example.com"));
    }

    @Test
    void getAllUsers_ReturnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByStatusNot(User.UserStatus.DELETED, pageable)).thenReturn(page);

        Page<User> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getUsersByType_ReturnsFilteredResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByType(User.UserType.FREELANCE, pageable)).thenReturn(page);

        Page<User> result = userService.getUsersByType(User.UserType.FREELANCE, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateUser_Success() {
        User updateDetails = new User();
        updateDetails.setFirstName("Jane");
        updateDetails.setLastName("Smith");
        updateDetails.setPhoneNumber("123456789");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(userId, updateDetails);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateStatus_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateStatus(userId, User.UserStatus.SUSPENDED);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_SoftDelete() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deleteUser(userId);

        verify(userRepository).save(argThat(u -> u.getStatus() == User.UserStatus.DELETED));
    }

    @Test
    void verifyEmail_ActivatesUser() {
        user.setStatus(User.UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.verifyEmail(userId);

        assertNotNull(result);
        verify(userRepository).save(argThat(u ->
                u.getEmailVerified() && u.getStatus() == User.UserStatus.ACTIVE));
    }

    @Test
    void changeMyPassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHashedPassword");

        userService.changeMyPassword(userId, request);

        verify(userRepository).save(argThat(u -> u.getPassword().equals("newHashedPassword")));
    }

    @Test
    void changeMyPassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changeMyPassword(userId, request));

        assertEquals("Current password is incorrect", exception.getMessage());
    }

    @Test
    void changeMyPassword_MismatchConfirmation_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changeMyPassword(userId, request));

        assertEquals("New password and confirmation do not match", exception.getMessage());
    }

    @Test
    void updateLastLogin_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateLastLogin(userId);

        assertNotNull(result);
        verify(userRepository).save(argThat(u -> u.getLastLoginAt() != null));
    }
}
