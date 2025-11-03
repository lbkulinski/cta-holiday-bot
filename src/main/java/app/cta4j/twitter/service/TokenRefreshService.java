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
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.net.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
public final class TokenRefreshService {
    private static final String SCHEME = "https";
    private static final String HOST_NAME = "api.x.com";
    private static final String OAUTH_TOKEN_ENDPOINT = "/2/oauth2/token";

    private final SecretService secretService;

    @Autowired
    public TokenRefreshService(SecretService secretService) {
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
            String message = "Failed to build URI for token refresh endpoint";

            throw new TwitterException(message, e);
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

    public void refreshAccessToken() {
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
            String message = "Failed to send token refresh request";

            throw new TwitterException(message, e);
        }

        TokenResponse tokenResponse;

        try {
            tokenResponse = TokenResponse.parse(response);
        } catch (ParseException e) {
            String message = "Failed to parse token refresh response";

            throw new TwitterException(message, e);
        }

        if (!tokenResponse.indicatesSuccess()) {
            String message = "Token refresh request was not successful";

            throw new TwitterException(message);
        }

        AccessTokenResponse successResponse = tokenResponse.toSuccessResponse();

        var tokens = successResponse.getTokens();

        String newAccessToken = tokens.getAccessToken()
                                      .getValue();

        String newRefreshToken = tokens.getRefreshToken()
                                       .getValue();

        this.secretService.setTwitterTokens(newAccessToken, newRefreshToken);
    }
}
