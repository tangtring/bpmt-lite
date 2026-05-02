package com.riversoft.api.modules.database_operations;

import com.riversoft.api.http.ApiRequest;

import java.util.Map;

public class DatabaseOperationController {

    private final DatabaseOperationService service;

    public DatabaseOperationController() {
        this(new DatabaseOperationService());
    }

    public DatabaseOperationController(DatabaseOperationService service) {
        this.service = service;
    }

    public Map<String, Object> query(ApiRequest request) {
        return service.query(request.readJson(DatabaseOperationRequest.class));
    }

    public Map<String, Object> find(ApiRequest request) {
        return service.find(request.readJson(DatabaseOperationRequest.class));
    }

    public Map<String, Object> save(ApiRequest request) {
        return service.save(request.readJson(DatabaseOperationRequest.class));
    }

    public Map<String, Object> exec(ApiRequest request) {
        return service.exec(request.readJson(DatabaseOperationRequest.class));
    }
}
