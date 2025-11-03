package app.cta4j.common.service;

import app.cta4j.common.dto.Post;
import app.cta4j.mapbox.service.MapboxService;
import com.cta4j.train.client.TrainClient;
import com.cta4j.train.model.Route;
import com.cta4j.train.model.Train;
import com.cta4j.train.model.TrainCoordinates;
import com.cta4j.train.model.UpcomingTrainArrival;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public final class PostService {
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final TrainClient trainClient;
    private final MapboxService mapboxService;

    @Autowired
    public PostService(
        TrainClient trainClient,
        MapboxService mapboxService
    ) {
        this.trainClient = trainClient;
        this.mapboxService = mapboxService;
    }

    private String toTitleCase(Route route) {
        if (route == null) {
            return null;
        }

        String string = route.toString();

        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    private String buildText(String run, UpcomingTrainArrival arrival) {
        String route = this.toTitleCase(arrival.route());

        String arrivalTime = arrival.arrivalTime()
                                    .atZone(ZONE)
                                    .toLocalDateTime()
                                    .format(TIME_FORMAT);

        return String.format(
            "%s-bound %s Line Run %s will be arriving at %s at %s",
            arrival.destinationName(),
            route,
            run,
            arrival.stationName(),
            arrivalTime
        );
    }

    private File generateMap(TrainCoordinates coordinates) {
        if (coordinates == null) {
            return null;
        }

        if ((coordinates.latitude() == null) || (coordinates.longitude() == null)) {
            return null;
        }

        return this.mapboxService.generateMap(coordinates.latitude(), coordinates.longitude());
    }

    private Post buildPost(String run, Train train) {
        List<UpcomingTrainArrival> arrivals = train.arrivals();

        if (arrivals.isEmpty()) {
            return null;
        }

        List<UpcomingTrainArrival> copy = new ArrayList<>(arrivals);

        copy.sort(Comparator.comparing(UpcomingTrainArrival::arrivalTime));

        UpcomingTrainArrival arrival = copy.getFirst();

        String text = this.buildText(run, arrival);

        TrainCoordinates coordinates = train.coordinates();

        File media = this.generateMap(coordinates);

        return new Post(text, media);
    }

    public Optional<Post> buildPost(String run) {
        Objects.requireNonNull(run);

        Optional<Train> optionalTrain = this.trainClient.getTrain(run);

        if (optionalTrain.isEmpty()) {
            return Optional.empty();
        }

        Train train = optionalTrain.get();

        Post post = this.buildPost(run, train);

        return Optional.ofNullable(post);
    }
}
