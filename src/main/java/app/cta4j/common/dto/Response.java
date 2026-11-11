package app.cta4j.common.dto;

import org.apache.hc.core5.http.HttpStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record Response<T>(
    int statusCode,
    @Nullable T data
) {
    public Response {
        if (statusCode < HttpStatus.SC_CONTINUE || statusCode > HttpStatus.SC_NETWORK_AUTHENTICATION_REQUIRED) {
            throw new IllegalArgumentException("Invalid HTTP status code: %d".formatted(statusCode));
        }
    }
}
