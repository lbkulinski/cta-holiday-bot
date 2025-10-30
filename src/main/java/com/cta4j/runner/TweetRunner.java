package com.cta4j.runner;

import com.cta4j.mapbox.service.MapboxService;
import com.cta4j.secretsmanager.dto.Secret;
import com.cta4j.secretsmanager.service.SecretService;
import com.cta4j.train.client.TrainClient;
import com.cta4j.train.model.Train;
import com.cta4j.train.model.TrainCoordinates;
import com.cta4j.train.model.UpcomingTrainArrival;
import com.cta4j.twitter.dto.Media;
import com.cta4j.twitter.dto.Tweet;
import com.cta4j.twitter.service.MediaService;
import com.cta4j.twitter.service.TweetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class TweetRunner implements CommandLineRunner {
    private static final String TRAIN_RUN = "412";

    private final Secret secret;
    private final TweetService tweetService;
    private final MapboxService mapboxService;
    private final MediaService mediaService;

    @Autowired
    public TweetRunner(
        SecretService secretService,
        TweetService tweetService,
        MapboxService mapboxService,
        MediaService mediaService
    ) {
        this.secret = secretService.getSecret();
        this.tweetService = tweetService;
        this.mapboxService = mapboxService;
        this.mediaService = mediaService;
    }

    @Override
    public void run(String... args) {
        String trainApiKey = this.secret.cta()
                                        .trainApiKey();

        TrainClient trainClient = TrainClient.builder()
                                             .apiKey(trainApiKey)
                                             .build();

        Optional<Train> optionalTrain = trainClient.getTrain(TRAIN_RUN);

        if (optionalTrain.isEmpty()) {
            System.out.println("Train not found.");

            return;
        }

        Train train = optionalTrain.get();

        List<UpcomingTrainArrival> arrivals = train.arrivals();

        if (arrivals.isEmpty()) {
            System.out.println("No upcoming arrivals for the train.");

            return;
        }

        TrainCoordinates coordinates = train.coordinates();
        BigDecimal latitude = coordinates.latitude();
        BigDecimal longitude = coordinates.longitude();

        File file = this.mapboxService.generateMap(latitude, longitude);

        Media media = this.mediaService.uploadMedia(file);

        String mediaId = media.id();

        UpcomingTrainArrival arrival = train.arrivals()
                                            .getFirst();

        //Loop-bound Purple Line Run 1225 will be arriving at Wilson at 7:26 PM

        String destination = arrival.destinationName();

        String route = arrival.route().toString();

        String titleCaseRoute = route.substring(0, 1).toUpperCase() + route.substring(1).toLowerCase();

        String station = arrival.stationName();

        ZoneId zoneId = ZoneId.of("America/Chicago");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");

        String arrivalTime = arrival.arrivalTime()
                                    .atZone(zoneId)
                                    .toLocalDateTime()
                                    .format(formatter);

        String tweetText = String.format(
            "%s-bound %s Line Run 1225 will be arriving at %s at %s",
            destination,
            titleCaseRoute,
            station,
            arrivalTime
        );

        Tweet tweet = this.tweetService.postTweet(tweetText, mediaId);

        System.out.printf("Tweet posted successfully! Tweet ID: %s%n", tweet.id());
    }
}
