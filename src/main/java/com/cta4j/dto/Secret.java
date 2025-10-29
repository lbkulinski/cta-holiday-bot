package com.cta4j.dto;

public record Secret(
    TwitterSecret twitter
) {
    public record TwitterSecret(
        String clientId,
        String clientSecret,
        String accessToken,
        String refreshToken
    ) {}
}
