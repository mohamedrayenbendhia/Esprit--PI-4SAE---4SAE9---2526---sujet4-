package tn.esprit.pi.nexlance.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nexlance.dto.UserListDTO;
import tn.esprit.pi.nexlance.dto.UserListResponse;
import tn.esprit.pi.nexlance.dto.UpdateStatusRequest;
import tn.esprit.pi.nexlance.entities.User;
import tn.esprit.pi.nexlance.services.UserManagementService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) User.UserType type,
            @RequestParam(required = false) User.UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<User> userPage = userManagementService.findUsers(search, type, status, pageable);
        
        List<UserListDTO> users = userPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        UserListResponse response = UserListResponse.builder()
                .users(users)
                .total(userPage.getTotalElements())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserListDTO> getUserById(@PathVariable UUID id) {
        User user = userManagementService.findById(id);
        return ResponseEntity.ok(convertToDTO(user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {
        
        userManagementService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        // Soft delete - mark as DELETED
        userManagementService.updateStatus(id, User.UserStatus.DELETED);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        Map<String, Long> stats = userManagementService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    private UserListDTO convertToDTO(User user) {
        return UserListDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .type(user.getType())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLoginAt())
                .build();
    }
}
