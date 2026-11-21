package app.cta4j.mastodon.client;

import app.cta4j.aws.client.SecretsClient;
import app.cta4j.mastodon.dto.CreateStatusRequest;
import app.cta4j.mastodon.dto.Media;
import app.cta4j.mastodon.dto.Status;
import app.cta4j.mastodon.exception.MastodonException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Component
@NullMarked
public final class MastodonClient {
    private static final String SCHEME = "https";
    private static final String HOST = "mastodon.social";

    private static final String MEDIA_ENDPOINT = "/api/v2/media";
    private static final String STATUSES_ENDPOINT = "/api/v1/statuses";

    private static final URI MEDIA_URI = UriComponentsBuilder.newInstance()
                                                             .scheme(SCHEME)
                                                             .host(HOST)
                                                             .path(MEDIA_ENDPOINT)
                                                             .build()
                                                             .toUri();

    private static final URI STATUSES_URI = UriComponentsBuilder.newInstance()
                                                                .scheme(SCHEME)
                                                                .host(HOST)
                                                                .path(STATUSES_ENDPOINT)
                                                                .build()
                                                                .toUri();

    private static final String AUTH_HEADER_TEMPLATE = "Bearer %s";

    private static final String FILE_PART_NAME = "file";
    private static final String DESCRIPTION_PART_NAME = "description";

    private final SecretsClient secretsClient;
    private final RestClient restClient;

    @Autowired
    public MastodonClient(
        SecretsClient secretsClient,
        RestClient.Builder builder
    ) {
        this.secretsClient = secretsClient;
        this.restClient = builder.build();
    }

    public Media uploadMedia(File file, String description) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(description);

        String accessToken = this.secretsClient.getSecret()
                                               .mastodon()
                                               .accessToken();

        var authHeader = AUTH_HEADER_TEMPLATE.formatted(accessToken);

        var body = new LinkedMultiValueMap<String, Object>();

        body.add(FILE_PART_NAME, new FileSystemResource(file));
        body.add(DESCRIPTION_PART_NAME, description);

        try {
            return this.restClient.post()
                                  .uri(MEDIA_URI)
                                  .header(HttpHeaders.AUTHORIZATION, authHeader)
                                  .contentType(MediaType.MULTIPART_FORM_DATA)
                                  .body(body)
                                  .retrieve()
                                  .requiredBody(Media.class);
        } catch (RestClientException | IllegalStateException e) {
            throw new MastodonException("Failed to upload media", e);
        }
    }

    public Status postStatus(String text, @Nullable String mediaId) {
        String accessToken = this.secretsClient.getSecret()
                                               .mastodon()
                                               .accessToken();

        var authHeader = AUTH_HEADER_TEMPLATE.formatted(accessToken);

        List<String> mediaIds = mediaId == null
            ? null
            : List.of(mediaId);

        var request = new CreateStatusRequest(text, mediaIds);

        try {
            return this.restClient.post()
                                  .uri(STATUSES_URI)
                                  .header(HttpHeaders.AUTHORIZATION, authHeader)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(request)
                                  .retrieve()
                                  .requiredBody(Status.class);
        } catch (RestClientException | IllegalStateException e) {
            throw new MastodonException("Failed to upload media", e);
        }
    }
}
