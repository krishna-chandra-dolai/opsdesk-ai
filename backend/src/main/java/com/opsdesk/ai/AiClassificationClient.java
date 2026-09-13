package com.opsdesk.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class AiClassificationClient {
    private static final Logger log = LoggerFactory.getLogger(AiClassificationClient.class);

    private final RestClient restClient;

    public AiClassificationClient(RestClient.Builder builder,
                                  @Value("${opsdesk.ai.url}") String serviceUrl,
                                  @Value("${opsdesk.ai.timeout-milliseconds}") int timeoutMilliseconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMilliseconds);
        requestFactory.setReadTimeout(timeoutMilliseconds);
        this.restClient = builder.baseUrl(serviceUrl).requestFactory(requestFactory).build();
    }

    public Optional<AiPrediction> classify(String title, String description) {
        try {
            PredictionResponse response = restClient.post()
                    .uri("/predict")
                    .body(new PredictionRequest(title, description))
                    .retrieve()
                    .body(PredictionResponse.class);
            return response == null ? Optional.empty()
                    : Optional.of(new AiPrediction(response.category(), response.confidence()));
        } catch (RestClientException exception) {
            log.warn("AI classification unavailable; incident creation will continue");
            return Optional.empty();
        }
    }

    private record PredictionRequest(String title, String description) {
    }

    private record PredictionResponse(com.opsdesk.incident.Category category, double confidence) {
    }
}
