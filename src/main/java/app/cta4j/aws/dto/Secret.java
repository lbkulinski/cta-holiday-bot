package app.cta4j.aws.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.time.Instant;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Secret(
    TwitterSecret twitter,
    BlueskySecret bluesky,
    MastodonSecret mastodon,
    MapboxSecret mapbox,
    CtaSecret cta,
    RollbarSecret rollbar
) {
    public Secret {
        Objects.requireNonNull(twitter);
        Objects.requireNonNull(bluesky);
        Objects.requireNonNull(mastodon);
        Objects.requireNonNull(mapbox);
        Objects.requireNonNull(cta);
        Objects.requireNonNull(rollbar);
    }

    public record TwitterSecret(
        String clientId,
        String clientSecret,
        String accessToken,
        String refreshToken,
        Instant expirationTime
    ) {
        public TwitterSecret {
            Objects.requireNonNull(clientId);
            Objects.requireNonNull(clientSecret);
            Objects.requireNonNull(accessToken);
            Objects.requireNonNull(refreshToken);
            Objects.requireNonNull(expirationTime);
        }

        @Override
        public String toString() {
            return """
                TwitterSecret{clientId=%s, \
                clientSecret=***, \
                accessToken=***, \
                refreshToken=***, \
                expirationTime=%s}""".formatted(this.clientId, this.expirationTime);
        }
    }

    public record BlueskySecret(String identifier, String appPassword) {
        public BlueskySecret {
            Objects.requireNonNull(identifier);
            Objects.requireNonNull(appPassword);
        }

        @Override
        public String toString() {
            return "BlueskySecret{identifier=%s, appPassword=***}".formatted(this.identifier);
        }
    }

    public record MastodonSecret(String accessToken) {
        public MastodonSecret {
            Objects.requireNonNull(accessToken);
        }

        @Override
        public String toString() {
            return "MastodonSecret{accessToken=***}";
        }
    }

    public record MapboxSecret(String accessToken) {
        public MapboxSecret {
            Objects.requireNonNull(accessToken);
        }

        @Override
        public String toString() {
            return "MapboxSecret{accessToken=***}";
        }
    }

    public record CtaSecret(String trainApiKey) {
        public CtaSecret {
            Objects.requireNonNull(trainApiKey);
        }

        @Override
        public String toString() {
            return "CtaSecret{trainApiKey=***}";
        }
    }

    public record RollbarSecret(String accessToken) {
        public RollbarSecret {
            Objects.requireNonNull(accessToken);
        }

        @Override
        public String toString() {
            return "RollbarSecret{accessToken=***}";
        }
    }
}
