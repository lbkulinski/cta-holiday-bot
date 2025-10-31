package com.cta4j.runner;

import com.cta4j.announcement.service.ArrivalAnnouncementService;
import com.cta4j.common.dto.PostPayload;
import com.cta4j.common.publisher.MultiplatformPublisher;
import com.cta4j.mapbox.service.MapboxService;
import com.cta4j.common.dto.Secret;
import com.cta4j.common.service.SecretService;
import com.cta4j.train.client.TrainClient;
import com.cta4j.train.model.Train;
import com.cta4j.train.model.TrainCoordinates;
import com.cta4j.train.model.UpcomingTrainArrival;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class TweetRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(TweetRunner.class);

    private static final String TRAIN_RUN = "436";

    private final Secret secret;
    private final ArrivalAnnouncementService announcementService;
    private final MapboxService mapboxService;
    private final MultiplatformPublisher multiplatformPublisher;

    @Autowired
    public TweetRunner(
        SecretService secretService,
        ArrivalAnnouncementService announcementService,
        MapboxService mapboxService,
        MultiplatformPublisher multiplatformPublisher
    ) {
        this.secret = secretService.getSecret();
        this.announcementService = announcementService;
        this.mapboxService = mapboxService;
        this.multiplatformPublisher = multiplatformPublisher;
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
            log.error("Train run {} not found. Unable to post to socials.", TRAIN_RUN);

            return;
        }

        Train train = optionalTrain.get();

        List<UpcomingTrainArrival> arrivals = train.arrivals();

        if (arrivals.isEmpty()) {
            log.error("Train run {} has no upcoming arrivals. Unable to post to socials.", TRAIN_RUN);

            return;
        }

        UpcomingTrainArrival arrival = arrivals.getFirst();

        String text = this.announcementService.createAnnouncement(TRAIN_RUN, arrival);

        TrainCoordinates coordinates = train.coordinates();

        File media = null;

        if (coordinates != null) {
            BigDecimal latitude = coordinates.latitude();
            BigDecimal longitude = coordinates.longitude();

            media = this.mapboxService.generateMap(latitude, longitude);
        }

        PostPayload postPayload = new PostPayload(text, media);

        this.multiplatformPublisher.publish(postPayload);
    }
}
