package app.cta4j.common.dto;

public record Response<T>(int statusCode, T data) {
}
