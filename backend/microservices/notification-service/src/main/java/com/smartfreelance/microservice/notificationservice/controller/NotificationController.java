package com.smartfreelance.microservice.notificationservice.controller;

import com.smartfreelance.microservice.notificationservice.dto.NotificationRequestDTO;
import com.smartfreelance.microservice.notificationservice.dto.NotificationResponseDTO;
import com.smartfreelance.microservice.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> create(@Valid @RequestBody NotificationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponseDTO>> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.getByUser(userId, page, size));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponseDTO>> getUnread(@PathVariable String userId) {
        return ResponseEntity.ok(service.getUnreadByUser(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("count", service.countUnread(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable String id) {
        return ResponseEntity.ok(service.markAsRead(id));
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@PathVariable String userId) {
        int count = service.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("marked", count));
    }
}
