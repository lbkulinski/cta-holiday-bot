package app.cta4j.aws.client;

import app.cta4j.aws.dto.Secret;
import com.amazonaws.secretsmanager.caching.SecretCache;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@NullMarked
public final class SecretsClient {
    private final SecretCache secretCache;
    private final JsonMapper jsonMapper;

    private final String secretId;

    @Autowired
    public SecretsClient(
        SecretCache secretCache,
        JsonMapper jsonMapper,
        @Value("${app.aws.secrets-manager.secret-id}") String secretId
    ) {
        this.secretCache = secretCache;
        this.jsonMapper = jsonMapper;
        this.secretId = secretId;
    }

    public Secret getSecret() {
        String secretString = this.secretCache.getSecretString(this.secretId);

        if (secretString == null || secretString.isBlank()) {
            throw new IllegalStateException("Secret string is null or blank");
        }

        try {
            return this.jsonMapper.readValue(secretString, Secret.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize secret JSON string", e);
        }
    }
}
