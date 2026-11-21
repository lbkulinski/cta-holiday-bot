package app.cta4j.bluesky.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@NullMarked
public record Session(
    String did,
    String accessJwt
) {
    public Session {
        Objects.requireNonNull(did);
        Objects.requireNonNull(accessJwt);
    }
}
