package com.aibuilder.service;

import com.aibuilder.dto.ApiTestRequest;
import com.aibuilder.dto.ApiTestResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiTesterService {

    private final RestTemplate restTemplate = new RestTemplate();

    public ApiTestResponse send(ApiTestRequest request) {
        if (request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("Enter a full URL for the API tester.");
        }
        if (!request.url().startsWith("http://") && !request.url().startsWith("https://")) {
            throw new IllegalArgumentException("API tester URL must start with http:// or https://");
        }

        HttpHeaders headers = new HttpHeaders();
        if (request.headers() != null) {
            request.headers().forEach(headers::set);
        }

        HttpEntity<String> entity = new HttpEntity<>(request.body(), headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    request.url(),
                    HttpMethod.valueOf(request.method().toUpperCase()),
                    entity,
                    String.class
            );
        } catch (HttpStatusCodeException exception) {
            return new ApiTestResponse(
                    exception.getStatusCode().value(),
                    flattenHeaders(exception.getResponseHeaders()),
                    exception.getResponseBodyAsString()
            );
        }

        Map<String, String> responseHeaders = flattenHeaders(response.getHeaders());

        return new ApiTestResponse(
                response.getStatusCode().value(),
                responseHeaders,
                response.getBody() == null ? "" : response.getBody()
        );
    }

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> responseHeaders = new LinkedHashMap<>();
        if (headers == null) {
            return responseHeaders;
        }
        headers.forEach((key, values) -> responseHeaders.put(key, String.join(", ", values)));
        return responseHeaders;
    }
}
