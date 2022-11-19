package com.cta4j;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class TrainDeserializer extends StdDeserializer<Train> {
    public TrainDeserializer(Class<?> clazz) {
        super(clazz);
    } //TrainDeserializer

    public TrainDeserializer() {
        this(null);
    } //TrainDeserializer

    private String getRoute(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode routeNode = jsonNode.get("rt");

        if ((routeNode == null) || !routeNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"rt\" does not exist or is not a string in the specified content");
        } //end if

        return routeNode.asText();
    } //getRoute

    private int getRun(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode runNode = jsonNode.get("rn");

        if ((runNode == null) || !runNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"rt\" does not exist or is not a string in the specified content");
        } //end if

        String runString = runNode.asText();

        int run;

        try {
            run = Integer.parseInt(runString);
        } catch (NumberFormatException e) {
            throw new JsonMappingException(jsonParser,
                "the field \"rt\" in the specified content is not a valid int", e);
        } //end try catch

        return run;
    } //getRun

    private String getStation(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode stationNode = jsonNode.get("staNm");

        if ((stationNode == null) || !stationNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"staNm\" does not exist or is not a string in the specified content");
        } //end if

        return stationNode.asText();
    } //getStation

    private String getDestination(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode destinationNode = jsonNode.get("destNm");

        if ((destinationNode == null) || !destinationNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"destNm\" does not exist or is not a string in the specified content");
        } //end if

        return destinationNode.asText();
    } //getDestination

    private LocalDateTime getPredictionTime(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode predictionTimeNode = jsonNode.get("prdt");

        if ((predictionTimeNode == null) || !predictionTimeNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"prdt\" does not exist or is not a string in the specified content");
        } //end if

        String predictionTimeString = predictionTimeNode.asText();

        LocalDateTime predictionTime;

        try {
            predictionTime = LocalDateTime.parse(predictionTimeString);
        } catch (DateTimeParseException e) {
            throw new JsonMappingException(jsonParser,
                "the field \"prdt\" in the specified content is not a valid date", e);
        } //end try catch

        return predictionTime;
    } //getPredictionTime

    private LocalDateTime getArrivalTime(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode arrivalTimeNode = jsonNode.get("arrT");

        if ((arrivalTimeNode == null) || !arrivalTimeNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"arrT\" does not exist or is not a string in the specified content");
        } //end if

        String arrivalTimeString = arrivalTimeNode.asText();

        LocalDateTime arrivalTime;

        try {
            arrivalTime = LocalDateTime.parse(arrivalTimeString);
        } catch (DateTimeParseException e) {
            throw new JsonMappingException(jsonParser,
                "the field \"arrT\" in the specified content is not a valid date", e);
        } //end try catch

        return arrivalTime;
    } //getArrivalTime

    @Override
    public Train deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode rootNode = jsonParser.getCodec()
                                      .readTree(jsonParser);

        JsonNode ctattNode = rootNode.get("ctatt");

        if ((ctattNode == null) || !ctattNode.isObject()) {
            throw new JsonMappingException(jsonParser,
                "the field \"ctatt\" does not exist or is not an object in the specified content");
        } //end if

        JsonNode etasNode = ctattNode.get("eta");

        if ((etasNode == null) || !etasNode.isArray()) {
            throw new JsonMappingException(jsonParser,
                "the field \"eta\" does not exist or is not an array in the specified content");
        } //end if

        if (etasNode.isEmpty()) {
            throw new JsonMappingException(jsonParser,
                "the field \"eta\" does not contain any elements in the specified content");
        } //end if

        JsonNode etaNode = etasNode.get(0);

        String route = this.getRoute(jsonParser, etaNode);

        int run = this.getRun(jsonParser, etaNode);

        String station = this.getStation(jsonParser, etaNode);

        String destination = this.getDestination(jsonParser, etaNode);

        LocalDateTime predictionTime = this.getPredictionTime(jsonParser, etaNode);

        LocalDateTime arrivalTime = this.getArrivalTime(jsonParser, etaNode);

        return new Train(route, run, station, destination, predictionTime, arrivalTime);
    } //deserialize
}
