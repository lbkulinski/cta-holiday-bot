package com.cta4j.service;

import com.cta4j.dto.Secret;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

import java.util.Objects;

@Service
public final class SecretService {
    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper;

    private final String secretId;

    @Getter
    private final Secret secret;

    @Autowired
    public SecretService(
        SecretsManagerClient secretsManagerClient,
        ObjectMapper objectMapper,
        @Value("${app.aws.secrets-manager.secret-id}") String secretId
    ) {
        this.secretsManagerClient = secretsManagerClient;
        this.objectMapper = objectMapper;
        this.secretId = secretId;
        this.secret = loadSecret(secretsManagerClient, objectMapper, secretId);
    }

    private static Secret loadSecret(SecretsManagerClient secretsManagerClient, ObjectMapper objectMapper,
        String secretId) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                                                             .secretId(secretId)
                                                             .build();

        GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);

        String secretString = response.secretString();

        Secret secret;

        try {
            secret = objectMapper.readValue(secretString, Secret.class);
        } catch (JsonProcessingException e) {
            String message = "Failed to parse secret JSON string from AWS Secrets Manager";

            throw new IllegalStateException(message, e);
        }

        return secret;
    }

    public void setTwitterTokens(String accessToken, String refreshToken) {
        Objects.requireNonNull(accessToken);

        Objects.requireNonNull(refreshToken);

        Secret.TwitterSecret twitterSecret = this.secret.twitter();

        twitterSecret.setAccessToken(accessToken);

        twitterSecret.setRefreshToken(refreshToken);

        String secretString;

        try {
            secretString = this.objectMapper.writeValueAsString(this.secret);
        } catch (JsonProcessingException e) {
            String message = "Failed to serialize secret to JSON string";

            throw new IllegalStateException(message, e);
        }

        PutSecretValueRequest request = PutSecretValueRequest.builder()
                                                             .secretId(this.secretId)
                                                             .secretString(secretString)
                                                             .build();

        this.secretsManagerClient.putSecretValue(request);
    }
}
