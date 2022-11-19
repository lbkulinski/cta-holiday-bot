package com.cta4j;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;

@JsonDeserialize(using = TrainDeserializer.class)
public record Train(String route, int run, String station, String destination, LocalDateTime predictionTime,
    LocalDateTime arrivalTime) {
}
