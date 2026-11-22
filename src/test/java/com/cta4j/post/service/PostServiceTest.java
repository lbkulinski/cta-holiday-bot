package com.cta4j.post.service;

import com.cta4j.common.geo.Coordinates;
import com.cta4j.mapbox.client.MapboxClient;
import com.cta4j.mapbox.exception.MapboxException;
import com.cta4j.post.model.Post;
import com.cta4j.train.TrainApi;
import com.cta4j.train.common.model.Arrival;
import com.cta4j.train.common.model.ArrivalMetadata;
import com.cta4j.train.common.model.TrainDirection;
import com.cta4j.train.common.model.TrainLine;
import com.cta4j.train.follow.FollowApi;
import com.cta4j.train.follow.model.FollowTrain;
import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    private static final String RUN = "417";

    // 2026-12-01T18:30:00Z is 12:30 PM in America/Chicago (CST, UTC-6)
    private static final Instant NOON_THIRTY_CHICAGO = Instant.parse("2026-12-01T18:30:00Z");

    private static final Coordinates COORDINATES =
        new Coordinates(new BigDecimal("41.88"), new BigDecimal("-87.63"), 90);

    private static final ArrivalMetadata METADATA =
        new ArrivalMetadata(RUN, TrainDirection.NORTHBOUND, COORDINATES, "");

    @Mock
    private TrainApi trainApi;

    @Mock
    private FollowApi followApi;

    @Mock
    private MapboxClient mapboxClient;

    @Mock
    private Rollbar rollbar;

    private PostService postService;

    @BeforeEach
    void setUp() {
        this.postService = new PostService(this.trainApi, this.mapboxClient, this.rollbar);
    }

    private void givenFollowTrain(FollowTrain followTrain) {
        givenFollowApi();

        when(this.followApi.findByRun(RUN)).thenReturn(Optional.of(followTrain));
    }

    private void givenNoFollowTrain() {
        givenFollowApi();

        when(this.followApi.findByRun(RUN)).thenReturn(Optional.empty());
    }

    private void givenFollowApi() {
        when(this.trainApi.follow()).thenReturn(this.followApi);
    }

    private static Arrival arrival(TrainLine line, String station, String destination, Instant arrivalTime) {
        return new Arrival(
            "station-id", station, "stop-id", "stop-desc", line, "dest-id", destination,
            arrivalTime.minusSeconds(60), arrivalTime, false, false, false, false, METADATA);
    }

    @Test
    void rejectsNullRun() {
        assertThatNullPointerException().isThrownBy(() -> this.postService.buildPost(null));
    }

    @Test
    void returnsNullWhenTrainNotFound() {
        givenNoFollowTrain();

        assertThat(this.postService.buildPost(RUN)).isNull();

        verifyNoInteractions(this.mapboxClient, this.rollbar);
    }

    @Test
    void returnsNullWhenThereAreNoArrivals() {
        givenFollowTrain(new FollowTrain(COORDINATES, List.of()));

        assertThat(this.postService.buildPost(RUN)).isNull();

        verifyNoInteractions(this.mapboxClient, this.rollbar);
    }

    @Test
    void buildsPostWithMapAndAltText() {
        var file = new File("map.png");

        var arrival = arrival(TrainLine.RED, "Howard", "95th/Dan Ryan", NOON_THIRTY_CHICAGO);

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(file);

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.text())
            .isEqualTo("Red Line train to 95th/Dan Ryan will be arriving at Howard at 12:30 PM 🎅🏻");
        assertThat(post.image()).isNotNull();
        assertThat(post.image().file()).isSameAs(file);
        assertThat(post.image().altText()).isEqualTo(
            "Map showing the current location of the Red Line train to 95th/Dan Ryan, marked with a pin. "
                + "It is heading to Howard.");
        verifyNoInteractions(this.rollbar);
    }

    @Test
    void usesEarliestArrivalRegardlessOfListOrder() {
        var later = arrival(TrainLine.BLUE, "Later", "LaterDest", NOON_THIRTY_CHICAGO.plusSeconds(600));
        var earliest = arrival(TrainLine.GREEN, "Earliest", "EarliestDest", NOON_THIRTY_CHICAGO);
        var middle = arrival(TrainLine.PINK, "Middle", "MiddleDest", NOON_THIRTY_CHICAGO.plusSeconds(300));

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(later, earliest, middle)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(new File("map.png"));

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.text()).contains("Green Line").contains("EarliestDest").contains("Earliest at");
    }

    @Test
    void doesNotMutateTheSourceArrivalList() {
        var later = arrival(TrainLine.BLUE, "Later", "LaterDest", NOON_THIRTY_CHICAGO.plusSeconds(600));
        var earliest = arrival(TrainLine.GREEN, "Earliest", "EarliestDest", NOON_THIRTY_CHICAGO);

        // An immutable list would throw if buildPost sorted it in place.
        var arrivals = List.of(later, earliest);

        givenFollowTrain(new FollowTrain(COORDINATES, arrivals));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(new File("map.png"));

        this.postService.buildPost(RUN);

        assertThat(arrivals).containsExactly(later, earliest);
    }

    @Test
    void usesHolidayLabelWhenLineIsNull() {
        var arrival = arrival(null, "Howard", "Loop", NOON_THIRTY_CHICAGO);

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(new File("map.png"));

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.text()).startsWith("Holiday train to Loop");
        assertThat(post.image()).isNotNull();
        assertThat(post.image().altText()).startsWith("Map showing the current location of the Holiday train to Loop");
    }

    @ParameterizedTest
    @CsvSource({
        "RED, Red",
        "BLUE, Blue",
        "BROWN, Brown",
        "GREEN, Green",
        "ORANGE, Orange",
        "PURPLE, Purple",
        "PINK, Pink",
        "YELLOW, Yellow"
    })
    void formatsEveryLine(TrainLine line, String expectedName) {
        var arrival = arrival(line, "Station", "Dest", NOON_THIRTY_CHICAGO);

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(new File("map.png"));

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.text()).startsWith(expectedName + " Line train to Dest");
    }

    @ParameterizedTest
    @CsvSource({
        "2026-12-01T06:05:00Z, 12:05 AM",   // midnight hour, CST
        "2026-12-01T18:30:00Z, 12:30 PM",
        "2026-12-02T03:59:00Z, 9:59 PM",
        "2026-07-01T17:00:00Z, 12:00 PM"    // daylight time (UTC-5)
    })
    void formatsTimeInChicagoZone(Instant instant, String expected) {
        var arrival = arrival(TrainLine.RED, "Howard", "Dest", instant);

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(new File("map.png"));

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.text()).contains("Howard at " + expected);
    }

    @Test
    void returnsTextOnlyPostWhenCoordinatesAreMissing() {
        var arrival = arrival(TrainLine.RED, "Howard", "Dest", NOON_THIRTY_CHICAGO);

        givenFollowTrain(new FollowTrain(null, List.of(arrival)));

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.image()).isNull();
        assertThat(post.text()).startsWith("Red Line train to Dest");
        verifyNoInteractions(this.mapboxClient, this.rollbar);
    }

    @Test
    void passesCoordinatesToMapboxClient() {
        var arrival = arrival(TrainLine.RED, "Howard", "Dest", NOON_THIRTY_CHICAGO);

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenReturn(new File("map.png"));

        this.postService.buildPost(RUN);

        verify(this.mapboxClient).generateMap(COORDINATES);
    }

    @Test
    void fallsBackToTextOnlyPostAndReportsToRollbarWhenMapGenerationFails() {
        var arrival = arrival(TrainLine.RED, "Howard", "95th/Dan Ryan", NOON_THIRTY_CHICAGO);
        var failure = new MapboxException("Failed to download Mapbox image to temporary file");

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenThrow(failure);

        Post post = this.postService.buildPost(RUN);

        assertThat(post).isNotNull();
        assertThat(post.text())
            .isEqualTo("Red Line train to 95th/Dan Ryan will be arriving at Howard at 12:30 PM 🎅🏻");
        assertThat(post.image()).isNull();
        verify(this.rollbar).error(failure, "Failed to generate Mapbox image");
    }

    @Test
    void propagatesNonMapboxExceptionsWithoutReportingToRollbar() {
        var arrival = arrival(TrainLine.RED, "Howard", "Dest", NOON_THIRTY_CHICAGO);

        givenFollowTrain(new FollowTrain(COORDINATES, List.of(arrival)));
        when(this.mapboxClient.generateMap(COORDINATES)).thenThrow(new IllegalStateException("boom"));

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> this.postService.buildPost(RUN))
            .withMessage("boom");

        verifyNoInteractions(this.rollbar);
    }
}
