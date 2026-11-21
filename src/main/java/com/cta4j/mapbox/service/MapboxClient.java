package com.cta4j.mapbox.service;

import com.cta4j.mapbox.exception.MapboxException;
import com.cta4j.aws.client.SecretsClient;
import com.cta4j.common.geo.Coordinates;
import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

@Component
@NullMarked
public final class MapboxClient {
    private static final String SCHEME = "https";
    private static final String HOST = "api.mapbox.com";

    private static final String LONGITUDE_PATH_VARIABLE = "lon";
    private static final String LATITUDE_PATH_VARIABLE = "lat";

    private static final String MAP_ENDPOINT =
        "/styles/v1/mapbox/streets-v12/static/pin-s+ff0000({%s},{%s})/{%s},{%s},15,0,0/600x400@2x".formatted(
            LONGITUDE_PATH_VARIABLE,
            LATITUDE_PATH_VARIABLE,
            LONGITUDE_PATH_VARIABLE,
            LATITUDE_PATH_VARIABLE);

    private static final String ACCESS_TOKEN_PARAM = "access_token";

    private static final String FILENAME_PREFIX = "mapbox-";
    private static final String FILENAME_SUFFIX = ".png";

    private final SecretsClient secretsClient;

    @Autowired
    public MapboxClient(SecretsClient secretsClient) {
        this.secretsClient = secretsClient;
    }

    public File generateMap(Coordinates coordinates) {
        Objects.requireNonNull(coordinates);

        String accessToken = this.secretsClient.getSecret()
                                               .mapbox()
                                               .accessToken();

        Map<String, String> uriVariables = Map.of(
            LONGITUDE_PATH_VARIABLE, String.valueOf(coordinates.longitude()),
            LATITUDE_PATH_VARIABLE, String.valueOf(coordinates.latitude()));

        URI uri = UriComponentsBuilder.newInstance()
                                      .scheme(SCHEME)
                                      .host(HOST)
                                      .path(MAP_ENDPOINT)
                                      .queryParam(ACCESS_TOKEN_PARAM, accessToken)
                                      .buildAndExpand(uriVariables)
                                      .toUri();

        URL url;

        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new MapboxException("Failed to convert Mapbox URI to URL", e);
        }

        File tempFile;

        try {
            tempFile = File.createTempFile(FILENAME_PREFIX, FILENAME_SUFFIX);
        } catch (IOException e) {
            throw new MapboxException("Failed to create temporary file for Mapbox image", e);
        }

        try {
            FileUtils.copyURLToFile(url, tempFile);
        } catch (IOException e) {
            throw new MapboxException("Failed to download Mapbox image to temporary file", e);
        }

        tempFile.deleteOnExit();

        return tempFile;
    }
}
