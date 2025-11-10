package app.cta4j.mapbox.service;

import app.cta4j.mapbox.exception.MapboxException;
import app.cta4j.common.dto.Secret;
import app.cta4j.common.service.SecretService;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

@Service
public final class MapboxService {
    private static final String MAPBOX_API_URL_TEMPLATE = """
    https://api.mapbox.com/styles/v1/mapbox/streets-v12/static/\
    pin-s+ff0000(%f,%f)/%f,%f,15,0,0/600x400@2x?access_token=%s""";

    private final Secret secret;

    @Autowired
    public MapboxService(SecretService secretService) {
        this.secret = secretService.getSecret();
    }

    public File generateMap(BigDecimal latitude, BigDecimal longitude) {
        Objects.requireNonNull(latitude);
        Objects.requireNonNull(longitude);

        String accessToken = this.secret.mapbox()
                                        .accessToken();

        String uriString = String.format(
            MAPBOX_API_URL_TEMPLATE,
            longitude,
            latitude,
            longitude,
            latitude,
            accessToken
        );

        URI uri = URI.create(uriString);

        URL url;

        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new MapboxException("Failed to convert Mapbox URI to URL", e);
        }

        File tempFile;

        try {
            tempFile = File.createTempFile("mapbox-", ".png");
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
