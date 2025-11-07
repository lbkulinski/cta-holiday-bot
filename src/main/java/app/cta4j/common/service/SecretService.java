package app.cta4j.common.service;

import app.cta4j.common.dto.Secret;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Synchronized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

import java.time.Instant;
import java.util.Objects;

@Service
public final class SecretService {
    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper;

    private final String secretId;

    @Getter(onMethod_ = @Synchronized)
    private volatile Secret secret;

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

    private static Secret loadSecret(
        SecretsManagerClient secretsManagerClient,
        ObjectMapper objectMapper,
        String secretId
    ) {
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

    public synchronized void setTwitterTokens(String accessToken, String refreshToken, Instant expirationTime) {
        Objects.requireNonNull(accessToken);
        Objects.requireNonNull(refreshToken);
        Objects.requireNonNull(expirationTime);

        Secret newSecret = this.secret.withTwitterTokens(accessToken, refreshToken, expirationTime);

        String secretString;

        try {
            secretString = this.objectMapper.writeValueAsString(newSecret);
        } catch (JsonProcessingException e) {
            String message = "Failed to serialize secret to JSON string";

            throw new IllegalStateException(message, e);
        }

        PutSecretValueRequest request = PutSecretValueRequest.builder()
                                                             .secretId(this.secretId)
                                                             .secretString(secretString)
                                                             .build();

        this.secretsManagerClient.putSecretValue(request);

        this.secret = newSecret;
    }
}
