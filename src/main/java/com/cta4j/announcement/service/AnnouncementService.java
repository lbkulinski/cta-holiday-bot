package com.cta4j.announcement.service;

import com.cta4j.train.model.UpcomingTrainArrival;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public final class AnnouncementService {
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    public String buildAnnouncement(String run, UpcomingTrainArrival arrival) {
        Objects.requireNonNull(run);
        Objects.requireNonNull(arrival);

        String destination = arrival.destinationName();

        String route = arrival.route().toString();

        String titleCaseRoute = route.substring(0, 1).toUpperCase() + route.substring(1).toLowerCase();

        String station = arrival.stationName();

        String arrivalTime = arrival.arrivalTime()
                                    .atZone(ZONE)
                                    .toLocalDateTime()
                                    .format(TIME_FORMAT);

        return String.format(
            "%s-bound %s Line Run %s will be arriving at %s at %s",
            destination,
            titleCaseRoute,
            run,
            station,
            arrivalTime
        );
    }
}
