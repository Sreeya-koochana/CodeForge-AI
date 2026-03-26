package com.aibuilder.controller;

import com.aibuilder.dto.ApiTestRequest;
import com.aibuilder.dto.ApiTestResponse;
import com.aibuilder.service.ApiTesterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tester")
public class ApiTesterController {

    private final ApiTesterService apiTesterService;

    public ApiTesterController(ApiTesterService apiTesterService) {
        this.apiTesterService = apiTesterService;
    }

    @PostMapping
    public ApiTestResponse send(@RequestBody ApiTestRequest request) {
        return apiTesterService.send(request);
    }
}
