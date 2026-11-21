package com.cta4j.post.service;

import com.cta4j.post.model.Post;
import com.cta4j.mapbox.service.MapboxClient;
import com.cta4j.common.geo.Coordinates;
import com.cta4j.train.TrainApi;
import com.cta4j.train.common.model.Arrival;
import com.cta4j.train.common.model.TrainLine;
import com.cta4j.train.follow.model.FollowTrain;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@NullMarked
public final class PostService {
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private static final String TEXT_TEMPLATE = "%s train to %s will be arriving at %s at %s \uD83C\uDF85\uD83C\uDFFB";
    private static final String ALT_TEMPLATE =
        "Map showing the current location of the %s train to %s, marked with a pin. It is heading to %s.";

    private final TrainApi trainApi;
    private final MapboxClient mapboxClient;

    @Autowired
    public PostService(
        TrainApi trainApi,
        MapboxClient mapboxClient
    ) {
        this.trainApi = trainApi;
        this.mapboxClient = mapboxClient;
    }

    public @Nullable Post buildPost(String run) {
        Objects.requireNonNull(run);

        FollowTrain followTrain = this.trainApi.follow()
                                               .findByRun(run)
                                               .orElse(null);

        if (followTrain == null) {
            return null;
        }

        List<Arrival> arrivals = followTrain.arrivals();

        if (arrivals.isEmpty()) {
            return null;
        }

        var copy = new ArrayList<>(arrivals);

        copy.sort(Comparator.comparing(Arrival::arrivalTime));

        var arrival = copy.getFirst();

        String text = this.buildText(arrival);

        Coordinates coordinates = followTrain.coordinates();

        if (coordinates == null) {
            return new Post(text, null);
        }

        File file = this.mapboxClient.generateMap(coordinates);
        String alt = this.buildAlt(arrival);

        var image = new Post.Image(file, alt);

        return new Post(text, image);
    }

    private String buildText(Arrival arrival) {
        String trainLabel = trainLabel(arrival.line());

        String arrivalTime = arrival.arrivalTime()
                                    .atZone(ZONE)
                                    .toLocalDateTime()
                                    .format(TIME_FORMAT);

        return TEXT_TEMPLATE.formatted(trainLabel, arrival.destinationName(), arrival.stationName(), arrivalTime);
    }

    private String buildAlt(Arrival arrival) {
        String trainLabel = trainLabel(arrival.line());

        return ALT_TEMPLATE.formatted(trainLabel, arrival.destinationName(), arrival.stationName());
    }

    private static String trainLabel(@Nullable TrainLine line) {
        return line == null
            ? "Holiday"
            : "%s Line".formatted(formatLine(line));
    }

    private static String formatLine(TrainLine line) {
        return switch (line) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case BROWN -> "Brown";
            case GREEN -> "Green";
            case ORANGE -> "Orange";
            case PURPLE -> "Purple";
            case PINK -> "Pink";
            case YELLOW -> "Yellow";
        };
    }
}
