package app.cta4j.common.service;

import app.cta4j.common.dto.Post;
import app.cta4j.mapbox.service.MapboxService;
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

    private static final String NO_LINE_TEXT_TEMPLATE =
        "Holiday train to %s will be arriving at %s at %s \uD83C\uDF85\uD83C\uDFFB";
    private static final String LINE_TEXT_TEMPLATE =
        "%s Line train to %s will be arriving at %s at %s \uD83C\uDF85\uD83C\uDFFB";

    private final TrainApi trainApi;
    private final MapboxService mapboxService;

    @Autowired
    public PostService(
        TrainApi trainApi,
        MapboxService mapboxService
    ) {
        this.trainApi = trainApi;
        this.mapboxService = mapboxService;
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
        File media = this.generateMap(coordinates);

        return new Post(text, media);
    }

    private String buildText(Arrival arrival) {
        TrainLine line = arrival.line();

        String arrivalTime = arrival.arrivalTime()
                                    .atZone(ZONE)
                                    .toLocalDateTime()
                                    .format(TIME_FORMAT);

        if (line == null) {
            return NO_LINE_TEXT_TEMPLATE.formatted(arrival.destinationName(), arrival.stationName(), arrivalTime);
        }

        var lineString = switch (line) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case BROWN -> "Brown";
            case GREEN -> "Green";
            case ORANGE -> "Orange";
            case PURPLE -> "Purple";
            case PINK -> "Pink";
            case YELLOW -> "Yellow";
        };

        return LINE_TEXT_TEMPLATE.formatted(lineString, arrival.destinationName(), arrival.stationName(), arrivalTime);
    }

    private @Nullable File generateMap(@Nullable Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }

        return this.mapboxService.generateMap(coordinates.latitude(), coordinates.longitude());
    }
}
