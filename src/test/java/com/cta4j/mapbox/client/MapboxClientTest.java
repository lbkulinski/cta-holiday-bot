package com.cta4j.mapbox.client;

import com.cta4j.aws.client.SecretsClient;
import com.cta4j.common.geo.Coordinates;
import com.cta4j.mapbox.exception.MapboxException;
import com.cta4j.testsupport.TestSecrets;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The download goes through {@link FileUtils#copyURLToFile(URL, File, int, int)}, which is mocked statically so that
 * these tests never touch the network.
 */
@ExtendWith(MockitoExtension.class)
class MapboxClientTest {
    private static final Coordinates COORDINATES =
        new Coordinates(new BigDecimal("41.8781"), new BigDecimal("-87.6298"), 90);

    @Mock
    private SecretsClient secretsClient;

    private MapboxClient client;

    @BeforeEach
    void setUp() {
        this.client = new MapboxClient(this.secretsClient);
    }

    @Test
    void downloadsMapFromExpectedUrlIntoTempPngFile() {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());

        File file = null;

        try (MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            file = this.client.generateMap(COORDINATES);

            var urlCaptor = ArgumentCaptor.forClass(URL.class);
            var fileCaptor = ArgumentCaptor.forClass(File.class);

            fileUtils.verify(() -> FileUtils.copyURLToFile(
                urlCaptor.capture(),
                fileCaptor.capture(),
                eq(5_000),
                eq(10_000)));

            URL url = urlCaptor.getValue();

            assertThat(url.getProtocol()).isEqualTo("https");
            assertThat(url.getHost()).isEqualTo("api.mapbox.com");
            assertThat(url.getPath()).contains("/styles/v1/mapbox/streets-v12/static/");
            assertThat(url.getPath()).contains("(-87.6298,41.8781)");
            assertThat(url.getPath()).endsWith("/-87.6298,41.8781,15,0,0/600x400@2x");
            assertThat(url.getQuery()).isEqualTo("access_token=" + TestSecrets.MAPBOX_TOKEN);

            assertThat(fileCaptor.getValue()).isSameAs(file);
            assertThat(file.getName()).startsWith("mapbox-").endsWith(".png");
            assertThat(file).exists();
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    void wrapsDownloadFailureAndDeletesTempFile() {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());

        var fileCaptor = ArgumentCaptor.forClass(File.class);

        try (MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.copyURLToFile(any(URL.class), fileCaptor.capture(), anyInt(), anyInt()))
                     .thenThrow(new IOException("network down"));

            assertThatThrownBy(() -> this.client.generateMap(COORDINATES))
                .isInstanceOf(MapboxException.class)
                .hasMessage("Failed to download Mapbox image to temporary file")
                .hasNoCause();
        }

        assertThat(fileCaptor.getValue()).doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Connect timed out", "Read timed out"})
    void wrapsTimeoutsAndDeletesTempFile(String timeoutMessage) {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());

        var fileCaptor = ArgumentCaptor.forClass(File.class);

        try (MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.copyURLToFile(any(URL.class), fileCaptor.capture(), anyInt(), anyInt()))
                     .thenThrow(new SocketTimeoutException(timeoutMessage));

            assertThatThrownBy(() -> this.client.generateMap(COORDINATES))
                .isInstanceOf(MapboxException.class)
                .hasMessage("Failed to download Mapbox image to temporary file")
                .hasNoCause();
        }

        assertThat(fileCaptor.getValue()).doesNotExist();
    }

    @Test
    void doesNotLeakAccessTokenThroughTheException() {
        when(this.secretsClient.getSecret()).thenReturn(TestSecrets.secret());

        var leakyMessage = "Server returned HTTP response code: 401 for URL: "
            + "https://api.mapbox.com/x?access_token=" + TestSecrets.MAPBOX_TOKEN;

        try (MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.copyURLToFile(any(URL.class), any(File.class), anyInt(), anyInt()))
                     .thenThrow(new IOException(leakyMessage));

            assertThatThrownBy(() -> this.client.generateMap(COORDINATES))
                .isInstanceOf(MapboxException.class)
                .satisfies(e -> {
                    assertThat(e).hasNoCause();
                    assertThat(e.getMessage()).doesNotContain(TestSecrets.MAPBOX_TOKEN);
                    assertThat(e.getStackTrace()).isNotEmpty();
                });
        }
    }

    @Test
    void rejectsNullCoordinatesBeforeTouchingSecrets() {
        assertThatNullPointerException().isThrownBy(() -> this.client.generateMap(null));

        verifyNoInteractions(this.secretsClient);
    }
}
