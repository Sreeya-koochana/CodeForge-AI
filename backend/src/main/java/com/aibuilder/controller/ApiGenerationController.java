package com.aibuilder.controller;

import com.aibuilder.dto.ApiDefinitionResponse;
import com.aibuilder.dto.GenerateApiRequest;
import com.aibuilder.service.ApiGenerationService;
import com.aibuilder.service.UserService;
import com.aibuilder.util.ZipBuilderUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generation")
public class ApiGenerationController {

    private final ApiGenerationService apiGenerationService;
    private final UserService userService;
    private final ZipBuilderUtil zipBuilderUtil;

    public ApiGenerationController(ApiGenerationService apiGenerationService, UserService userService, ZipBuilderUtil zipBuilderUtil) {
        this.apiGenerationService = apiGenerationService;
        this.userService = userService;
        this.zipBuilderUtil = zipBuilderUtil;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDefinitionResponse generateApi(@Valid @RequestBody GenerateApiRequest request) {
        return apiGenerationService.generate(request);
    }

    @GetMapping("/project/{projectId}")
    public List<ApiDefinitionResponse> getProjectApis(@PathVariable Long projectId, Authentication authentication) {
        return apiGenerationService.getProjectApis(projectId, userService.getAuthenticatedUser(authentication));
    }

    @GetMapping("/{apiId}")
    public ApiDefinitionResponse getApi(@PathVariable Long apiId, Authentication authentication) {
        return apiGenerationService.getApi(apiId, userService.getAuthenticatedUser(authentication));
    }

    @GetMapping(value = "/{apiId}/zip", produces = "application/zip")
    public ResponseEntity<byte[]> downloadZip(@PathVariable Long apiId, @RequestParam(defaultValue = "generated-api") String name, Authentication authentication) {
        ApiDefinitionResponse response = apiGenerationService.getApi(apiId, userService.getAuthenticatedUser(authentication));
        byte[] zip = zipBuilderUtil.buildZip(response.files());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".zip\"")
                .body(zip);
    }

    @DeleteMapping("/{apiId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApi(@PathVariable Long apiId, Authentication authentication) {
        apiGenerationService.deleteApi(apiId, userService.getAuthenticatedUser(authentication));
    }
}
