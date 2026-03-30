package com.aibuilder.service;

import com.aibuilder.config.AiGenerationProperties;
import com.aibuilder.ai.GeminiClient;
import com.aibuilder.dto.ApiDefinitionResponse;
import com.aibuilder.dto.GenerateApiRequest;
import com.aibuilder.dto.GeneratedFileResponse;
import com.aibuilder.entity.ApiDefinition;
import com.aibuilder.entity.GeneratedFile;
import com.aibuilder.entity.Project;
import com.aibuilder.entity.User;
import com.aibuilder.repository.ApiDefinitionRepository;
import com.aibuilder.repository.GeneratedFileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApiGenerationService {

    private final ProjectService projectService;
    private final PromptBuilderService promptBuilderService;
    private final GeminiClient geminiClient;
    private final LocalTemplateGenerationService localTemplateGenerationService;
    private final AiGenerationProperties aiGenerationProperties;
    private final CodeProcessorService codeProcessorService;
    private final ApiDefinitionRepository apiDefinitionRepository;
    private final GeneratedFileRepository generatedFileRepository;

    public ApiGenerationService(
            ProjectService projectService,
            PromptBuilderService promptBuilderService,
            GeminiClient geminiClient,
            LocalTemplateGenerationService localTemplateGenerationService,
            AiGenerationProperties aiGenerationProperties,
            CodeProcessorService codeProcessorService,
            ApiDefinitionRepository apiDefinitionRepository,
            GeneratedFileRepository generatedFileRepository
    ) {
        this.projectService = projectService;
        this.promptBuilderService = promptBuilderService;
        this.geminiClient = geminiClient;
        this.localTemplateGenerationService = localTemplateGenerationService;
        this.aiGenerationProperties = aiGenerationProperties;
        this.codeProcessorService = codeProcessorService;
        this.apiDefinitionRepository = apiDefinitionRepository;
        this.generatedFileRepository = generatedFileRepository;
    }

    public ApiDefinitionResponse generate(GenerateApiRequest request) {
        Project project = projectService.getRequiredProject(request.projectId());
        String prompt = promptBuilderService.buildPrompt(request);
        String generatedCode = "gemini".equalsIgnoreCase(aiGenerationProperties.provider())
                ? geminiClient.generateCode(prompt)
                : localTemplateGenerationService.generate(request);

        ApiDefinition definition = apiDefinitionRepository.save(ApiDefinition.builder()
                .project(project)
                .prompt(prompt)
                .generatedCode(generatedCode)
                .build());

        List<GeneratedFile> files = codeProcessorService.extractFiles(generatedCode, request.entityName()).stream()
                .map(file -> GeneratedFile.builder()
                        .apiDefinition(definition)
                        .fileName(file.fileName())
                        .fileContent(file.content())
                        .build())
                .toList();

        List<GeneratedFile> savedFiles = generatedFileRepository.saveAll(files);
        return mapDefinition(definition, savedFiles);
    }

    public List<ApiDefinitionResponse> getProjectApis(Long projectId) {
        return apiDefinitionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(definition -> mapDefinition(
                        definition,
                        generatedFileRepository.findByApiDefinitionIdOrderByFileNameAsc(definition.getId())
                ))
                .toList();
    }

    public List<ApiDefinitionResponse> getProjectApis(Long projectId, User user) {
        projectService.getOwnedProject(projectId, user);
        return getProjectApis(projectId);
    }

    public ApiDefinitionResponse getApi(Long apiId) {
        ApiDefinition definition = apiDefinitionRepository.findById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("API definition not found: " + apiId));

        return mapDefinition(
                definition,
                generatedFileRepository.findByApiDefinitionIdOrderByFileNameAsc(apiId)
        );
    }

    public ApiDefinitionResponse getApi(Long apiId, User user) {
        ApiDefinition definition = getOwnedApi(apiId, user);
        return mapDefinition(
                definition,
                generatedFileRepository.findByApiDefinitionIdOrderByFileNameAsc(apiId)
        );
    }

    public void deleteApi(Long apiId, User user) {
        ApiDefinition definition = getOwnedApi(apiId, user);
        apiDefinitionRepository.delete(definition);
    }

    public long countApis() {
        return apiDefinitionRepository.count();
    }

    public long countFiles() {
        return generatedFileRepository.count();
    }

    public long countApisForUser(User user) {
        return apiDefinitionRepository.countByProjectUserId(user.getId());
    }

    public long countFilesForUser(User user) {
        return generatedFileRepository.countByApiDefinitionProjectUserId(user.getId());
    }

    private ApiDefinition getOwnedApi(Long apiId, User user) {
        ApiDefinition definition = apiDefinitionRepository.findById(apiId)
                .orElseThrow(() -> new IllegalArgumentException("API definition not found: " + apiId));
        if (!definition.getProject().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You do not have access to this generated API.");
        }
        return definition;
    }

    private ApiDefinitionResponse mapDefinition(ApiDefinition definition, List<GeneratedFile> files) {
        return new ApiDefinitionResponse(
                definition.getId(),
                definition.getProject().getId(),
                definition.getPrompt(),
                definition.getGeneratedCode(),
                definition.getCreatedAt(),
                files.stream()
                        .map(file -> new GeneratedFileResponse(file.getId(), file.getFileName(), file.getFileContent()))
                        .toList()
        );
    }
}
