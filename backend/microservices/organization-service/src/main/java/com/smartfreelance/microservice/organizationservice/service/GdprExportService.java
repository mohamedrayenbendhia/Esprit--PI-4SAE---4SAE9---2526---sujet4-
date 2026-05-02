package com.smartfreelance.microservice.organizationservice.service;

import java.util.Map;

public interface GdprExportService {
    Map<String, Object> exportUserData(String userId);
    void deleteUserData(String userId);
}
