package com.cta4j;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * A deserializer for the {@link Train} class.
 *
 * @author Logan Kulinski, rashes_lineage02@icloud.com
 */
public final class TrainDeserializer extends StdDeserializer<Train> {
    /**
     * Constructs an instance of the {@link TrainDeserializer} class.
     *
     * @param clazz the {@link Class} to be used in the operation
     */
    public TrainDeserializer(Class<?> clazz) {
        super(clazz);
    } //TrainDeserializer

    /**
     * Constructs an instance of the {@link TrainDeserializer} class.
     */
    public TrainDeserializer() {
        this(null);
    } //TrainDeserializer

    /**
     * Returns a {@link Train}'s route using the specified {@link JsonParser} and {@link JsonNode}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param jsonNode the {@link JsonNode} to be used in the operation
     * @return a {@link Train}'s route using the specified {@link JsonParser} and {@link JsonNode}
     * @throws JsonMappingException if the specified {@link JsonNode} cannot be mapped to a route
     */
    private String getRoute(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode routeNode = jsonNode.get("route");

        if ((routeNode == null) || !routeNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"route\" does not exist or is not a string in the specified content");
        } //end if

        String route = routeNode.asText();

        return switch (route) {
            case "RED" -> "Red Line";
            case "BLUE" -> "Blue Line";
            case "BROWN" -> "Brown Line";
            case "GREEN" -> "Green Line";
            case "ORANGE" -> "Orange Line";
            case "PURPLE" -> "Purple Line";
            case "PINK" -> "Pink Line";
            case "YELLOW" -> "Yellow Line";
            default -> throw new JsonMappingException(jsonParser,
                "the field \"route\" is malformed in the specified content");
        };
    } //getRoute

    /**
     * Returns a {@link Train}'s run using the specified {@link JsonParser} and {@link JsonNode}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param jsonNode the {@link JsonNode} to be used in the operation
     * @return a {@link Train}'s run using the specified {@link JsonParser} and {@link JsonNode}
     * @throws JsonMappingException if the specified {@link JsonNode} cannot be mapped to a run
     */
    private int getRun(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode runNode = jsonNode.get("run");

        if ((runNode == null) || !runNode.isInt()) {
            throw new JsonMappingException(jsonParser,
                "the field \"run\" does not exist or is not an int in the specified content");
        } //end if

        return runNode.asInt();
    } //getRun

    /**
     * Returns a {@link Train}'s station using the specified {@link JsonParser} and {@link JsonNode}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param jsonNode the {@link JsonNode} to be used in the operation
     * @return a {@link Train}'s station using the specified {@link JsonParser} and {@link JsonNode}
     * @throws JsonMappingException if the specified {@link JsonNode} cannot be mapped to a station
     */
    private String getStation(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode stationNode = jsonNode.get("station");

        if ((stationNode == null) || !stationNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"station\" does not exist or is not a string in the specified content");
        } //end if

        return stationNode.asText();
    } //getStation

    /**
     * Returns a {@link Train}'s destination using the specified {@link JsonParser} and {@link JsonNode}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param jsonNode the {@link JsonNode} to be used in the operation
     * @return a {@link Train}'s destination using the specified {@link JsonParser} and {@link JsonNode}
     * @throws JsonMappingException if the specified {@link JsonNode} cannot be mapped to a destination
     */
    private String getDestination(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode destinationNode = jsonNode.get("destination");

        if ((destinationNode == null) || !destinationNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"destination\" does not exist or is not a string in the specified content");
        } //end if

        return destinationNode.asText();
    } //getDestination

    /**
     * Returns a {@link Train}'s prediction time using the specified {@link JsonParser} and {@link JsonNode}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param jsonNode the {@link JsonNode} to be used in the operation
     * @return a {@link Train}'s prediction time using the specified {@link JsonParser} and {@link JsonNode}
     * @throws JsonMappingException if the specified {@link JsonNode} cannot be mapped to a prediction time
     */
    private LocalDateTime getPredictionTime(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode predictionTimeNode = jsonNode.get("predictionTime");

        if ((predictionTimeNode == null) || !predictionTimeNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"predictionTime\" does not exist or is not a string in the specified content");
        } //end if

        String predictionTimeString = predictionTimeNode.asText();

        LocalDateTime predictionTime;

        try {
            predictionTime = LocalDateTime.parse(predictionTimeString);
        } catch (DateTimeParseException e) {
            throw new JsonMappingException(jsonParser,
                "the field \"predictionTime\" in the specified content is not a valid date", e);
        } //end try catch

        return predictionTime;
    } //getPredictionTime

    /**
     * Returns a {@link Train}'s arrival time using the specified {@link JsonParser} and {@link JsonNode}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param jsonNode the {@link JsonNode} to be used in the operation
     * @return a {@link Train}'s arrival time using the specified {@link JsonParser} and {@link JsonNode}
     * @throws JsonMappingException if the specified {@link JsonNode} cannot be mapped to an arrival time
     */
    private LocalDateTime getArrivalTime(JsonParser jsonParser, JsonNode jsonNode) throws JsonMappingException {
        JsonNode arrivalTimeNode = jsonNode.get("arrivalTime");

        if ((arrivalTimeNode == null) || !arrivalTimeNode.isTextual()) {
            throw new JsonMappingException(jsonParser,
                "the field \"arrivalTime\" does not exist or is not a string in the specified content");
        } //end if

        String arrivalTimeString = arrivalTimeNode.asText();

        LocalDateTime arrivalTime;

        try {
            arrivalTime = LocalDateTime.parse(arrivalTimeString);
        } catch (DateTimeParseException e) {
            throw new JsonMappingException(jsonParser,
                "the field \"arrivalTime\" in the specified content is not a valid date", e);
        } //end try catch

        return arrivalTime;
    } //getArrivalTime

    /**
     * Returns a {@link Train} that is deserialized using the specified {@link JsonParser} and
     * {@link DeserializationContext}.
     *
     * @param jsonParser the {@link JsonParser} to be used in the operation
     * @param deserializationContext the {@link DeserializationContext} to be used in the operation
     * @return a {@link Train} that is deserialized using the specified {@link JsonParser} and
     * {@link DeserializationContext}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public Train deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode rootNode = jsonParser.getCodec()
                                      .readTree(jsonParser);

        JsonNode contentNode = rootNode.get("content");

        if ((contentNode == null) || !contentNode.isObject()) {
            throw new JsonMappingException(jsonParser,
                "the field \"content\" does not exist or is not an object in the specified content");
        } //end if

        JsonNode trainsNode = contentNode.get("trains");

        if ((trainsNode == null) || !trainsNode.isArray()) {
            throw new JsonMappingException(jsonParser,
                "the field \"trains\" does not exist or is not an array in the specified content");
        } //end if

        if (trainsNode.isEmpty()) {
            throw new JsonMappingException(jsonParser,
                "the field \"trains\" does not contain any elements in the specified content");
        } //end if

        JsonNode trainNode = trainsNode.get(0);

        String route = this.getRoute(jsonParser, trainNode);

        int run = this.getRun(jsonParser, trainNode);

        String station = this.getStation(jsonParser, trainNode);

        String destination = this.getDestination(jsonParser, trainNode);

        LocalDateTime predictionTime = this.getPredictionTime(jsonParser, trainNode);

        LocalDateTime arrivalTime = this.getArrivalTime(jsonParser, trainNode);

        return new Train(route, run, station, destination, predictionTime, arrivalTime);
    } //deserialize
}