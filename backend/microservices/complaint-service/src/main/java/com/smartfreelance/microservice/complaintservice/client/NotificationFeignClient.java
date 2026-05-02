package com.smartfreelance.microservice.complaintservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour le notification-service.
 *
 * Utilise la découverte Eureka (nom du service = "notification-service").
 * L'URL de fallback est définie dans application.yaml via la propriété
 * notification.feign.url (vide par défaut = Eureka actif).
 */
@FeignClient(
        name = "notification-service",
        url  = "${notification.feign.url:}",
        path = "/api/notifications"
)
public interface NotificationFeignClient {

    /**
     * Crée une notification in-app dans le notification-service.
     * POST /api/notifications
     */
    @PostMapping
    ResponseEntity<Void> send(@RequestBody NotificationRequest request);
}
