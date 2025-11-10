package app.cta4j.twitter.service;

import app.cta4j.twitter.exception.TwitterException;
import app.cta4j.common.service.SecretService;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

@Service
public final class TwitterTokenRefreshService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String OAUTH_TOKEN_ENDPOINT = "/2/oauth2/token";

    private final SecretService secretService;

    @Autowired
    public TwitterTokenRefreshService(SecretService secretService) {
        this.secretService = secretService;
    }

    private URI getUri() {
        URI uri;

        try {
            uri = new URIBuilder()
                .setScheme(SCHEME)
                .setHost(HOST_NAME)
                .setPath(OAUTH_TOKEN_ENDPOINT)
                .build();
        } catch (URISyntaxException e) {
            throw new TwitterException("Failed to build URI for token refresh endpoint", e);
        }

        return uri;
    }

    private ClientAuthentication getAuthentication() {
        var twitterSecret = this.secretService.getSecret()
                                              .twitter();

        ClientID clientId = new ClientID(twitterSecret.clientId());

        Secret clientSecret = new Secret(twitterSecret.clientSecret());

        return new ClientSecretBasic(clientId, clientSecret);
    }

    private RefreshTokenGrant getGrant() {
        String refreshToken = this.secretService.getSecret()
                                                .twitter()
                                                .refreshToken();

        RefreshToken token = new RefreshToken(refreshToken);

        return new RefreshTokenGrant(token);
    }

    private Scope getScope() {
        return new Scope("tweet.read", "tweet.write", "users.read", "offline.access");
    }

    public String refreshAccessToken() {
        URI uri = this.getUri();

        ClientAuthentication authentication = this.getAuthentication();

        RefreshTokenGrant grant = this.getGrant();

        Scope scope = this.getScope();

        TokenRequest request = new TokenRequest(uri, authentication, grant, scope);

        HTTPResponse response;

        try {
            response = request.toHTTPRequest()
                              .send();
        } catch (IOException e) {
            throw new TwitterException("Failed to send token refresh request", e);
        }

        TokenResponse tokenResponse;

        try {
            tokenResponse = TokenResponse.parse(response);
        } catch (ParseException e) {
            throw new TwitterException("Failed to parse token refresh response", e);
        }

        if (!tokenResponse.indicatesSuccess()) {
            throw new TwitterException("Token refresh request was not successful");
        }

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();

        Tokens tokens = successResponse.getTokens();

        String newAccessToken = tokens.getAccessToken()
                                      .getValue();

        RefreshToken refreshToken = tokens.getRefreshToken();

        String newRefreshToken;

        if (refreshToken == null) {
            newRefreshToken = this.secretService.getSecret()
                                                .twitter()
                                                .refreshToken();
        } else {
            newRefreshToken = tokens.getRefreshToken()
                                    .getValue();

        }

        long lifetime = tokens.getAccessToken()
                              .getLifetime();

        Instant expirationTime = Instant.now()
                                        .plusSeconds(lifetime);

        this.secretService.setTwitterTokens(newAccessToken, newRefreshToken, expirationTime);

        return newAccessToken;
    }
}
