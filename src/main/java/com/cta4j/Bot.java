package com.cta4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCECodeChallengeMethod;
import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.auth.TwitterOAuth20Service;
import com.twitter.clientlib.model.TweetCreateRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.*;

/**
 * A Twitter bot used to track the CTA Holiday Train.
 *
 * @author Logan Kulinski, rashes_lineage02@icloud.com
 */
public final class Bot {
    /**
     * The {@link Logger} of the {@link Bot} class.
     */
    private static final Logger LOGGER;

    /**
     * The {@link Properties} of the {@link Bot} class.
     */
    private static final Properties PROPERTIES;

    static {
        LOGGER = LogManager.getLogger(Bot.class);

        PROPERTIES = new Properties();

        Bot.loadProperties();
    } //static

    /**
     * Loads the properties of this {@link Bot}.
     */
    private static void loadProperties() {
        String pathString = "src/main/resources/application.properties";

        Path path = Path.of(pathString);

        try (var reader = Files.newBufferedReader(path)) {
            Bot.PROPERTIES.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } //end try catch
    } //loadProperties

    /**
     * Saves the properties of this {@link Bot}.
     */
    private static void saveProperties() {
        String pathString = "src/main/resources/application.properties";

        Path path = Path.of(pathString);

        try (var writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            Bot.PROPERTIES.store(writer, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } //end try catch
    } //saveProperties

    /**
     * Returns the next {@link Train} associated with the run number that is being tracked.
     *
     * @return the next {@link Train} associated with the run number that is being tracked
     */
    public static Train getNextTrain() {
        String run = Bot.PROPERTIES.getProperty("cta_train_run");

        if (run == null) {
            return null;
        } //end if

        String uriString = "https://cta4j.com/api/follow?run=%s".formatted(run);

        URI uri = URI.create(uriString);

        HttpRequest request = HttpRequest.newBuilder(uri)
                                         .GET()
                                         .build();

        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response;

        try {
            response = client.send(request, bodyHandler);
        } catch (IOException | InterruptedException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();

            return null;
        } //end try catch

        String body = response.body();

        ObjectMapper mapper = new ObjectMapper();

        Train train;

        try {
            train = mapper.readValue(body, Train.class);
        } catch (JsonProcessingException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();

            return null;
        } //end try catch

        return train;
    } //getTrain

    /**
     * Returns the tweet text for the specified {@link Train}.
     *
     * @param train the {@link Train} to be used in the operation
     * @return the tweet text for the specified {@link Train}
     */
    public static String getTweetText(Train train) {
        Objects.requireNonNull(train, "the specified Train is null");

        String route = train.route();

        int run = train.run();

        String destination = train.destination();

        String station = train.station();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");

        String arrivalTime = train.arrivalTime()
                                  .format(formatter);

        return """
        %s-bound %s Run %d will be arriving at %s at %s""".formatted(destination, route, run, station, arrivalTime);
    } //getTweetText

    /**
     * Returns a random code verifier to be used with Twitter's OAuth process.
     *
     * @return a random code verifier to be used with Twitter's OAuth process
     */
    private static String getCodeVerifier() {
        SecureRandom random = new SecureRandom();

        byte[] codeVerifier = new byte[64];

        random.nextBytes(codeVerifier);

        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(codeVerifier);
    } //getCodeVerifier

    /**
     * Returns a random code challenge using the specified code verifier to be used with Twitter's OAuth process.
     *
     * @param codeVerifier the code verifier to be used in the operation
     * @return a random code challenge using the specified code verifier to be used with Twitter's OAuth process
     */
    private static String getCodeChallenge(String codeVerifier) {
        MessageDigest messageDigest;

        String algorithm = "SHA-256";

        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();

            return null;
        } //end try catch

        byte[] bytes = codeVerifier.getBytes();

        messageDigest.update(bytes);

        byte[] digest = messageDigest.digest();

        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(digest);
    } //getCodeChallenge

    /**
     * Returns a random state to be used with Twitter's OAuth process.
     *
     * @return a random state to be used with Twitter's OAuth process
     */
    private static String getState() {
        SecureRandom random = new SecureRandom();

        byte[] state = new byte[64];

        random.nextBytes(state);

        return Base64.getEncoder()
                     .encodeToString(state);
    } //getState

    /**
     * Returns an {@link OAuth2AccessToken} after the user completes Twitter's OAuth flow.
     *
     * @return an {@link OAuth2AccessToken} after the user completes Twitter's OAuth flow
     */
    private static OAuth2AccessToken getAccessToken() {
        String clientId = Bot.PROPERTIES.getProperty("client_id");

        String clientSecret = Bot.PROPERTIES.getProperty("client_secret");

        String callback = Bot.PROPERTIES.getProperty("callback");

        if ((clientId == null) || (clientSecret == null) || (callback == null)) {
            return null;
        } //end if

        String defaultScope = "offline.access tweet.read tweet.write users.read";

        String codeVerifier = Bot.getCodeVerifier();

        String codeChallenge = Bot.getCodeChallenge(codeVerifier);

        if (codeChallenge == null) {
            return null;
        } //end if

        PKCE pkce = new PKCE();

        pkce.setCodeVerifier(codeVerifier);

        pkce.setCodeChallenge(codeChallenge);

        pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.S256);

        String state = Bot.getState();

        OAuth2AccessToken accessToken;

        try (var service = new TwitterOAuth20Service(clientId, clientSecret, callback, defaultScope)) {
            String authorizationUrl = service.getAuthorizationUrl(pkce, state);

            System.out.println(authorizationUrl);

            Scanner scanner = new Scanner(System.in);

            String code = scanner.nextLine();

            scanner.close();

            accessToken = service.getAccessToken(pkce, code);
        } catch (IOException | ExecutionException | InterruptedException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();

            return null;
        } //end try catch

        return accessToken;
    } //getBearerToken

    /**
     * Returns a {@link TwitterCredentialsOAuth2} to be used with Twitter's API.
     *
     * @return a {@link TwitterCredentialsOAuth2} to be used with Twitter's API
     */
    private static TwitterCredentialsOAuth2 getCredentials() {
        String clientId = Bot.PROPERTIES.getProperty("client_id");

        String clientSecret = Bot.PROPERTIES.getProperty("client_secret");

        String accessToken = Bot.PROPERTIES.getProperty("access_token");

        String refreshToken = Bot.PROPERTIES.getProperty("refresh_token");

        if ((clientId == null) || (clientSecret == null) || (accessToken == null) || (refreshToken == null)) {
            return null;
        } //end if

        boolean automaticRefresh = true;

        return new TwitterCredentialsOAuth2(clientId, clientSecret, accessToken, refreshToken, automaticRefresh);
    } //getCredentials

    /**
     * Creates a tweet using the specified {@link TwitterApi} and text.
     *
     * @param twitterApi the {@link TwitterApi} to be used in the operation
     * @param text the text to be used in the operation
     */
    private static void createTweet(TwitterApi twitterApi, String text) {
        TweetCreateRequest request = new TweetCreateRequest();

        request.setText(text);

        try {
            twitterApi.tweets()
                      .createTweet(request)
                      .execute();
        } catch (ApiException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();
        } //end try catch
    } //createTweet

    /**
     * Runs the {@link Bot} class.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        TwitterCredentialsOAuth2 credentials = Bot.getCredentials();

        TwitterApi twitterApi = new TwitterApi(credentials);

        twitterApi.addCallback(token -> {
            String accessToken = token.getAccessToken();

            String refreshToken = token.getRefreshToken();

            Bot.PROPERTIES.setProperty("access_token", accessToken);

            Bot.PROPERTIES.setProperty("refresh_token", refreshToken);

            Bot.saveProperties();
        });

        try {
            twitterApi.refreshToken();
        } catch (ApiException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();

            return;
        } //end try catch

        Runnable runnable = () -> {
            Train train = Bot.getNextTrain();

            if (train == null) {
                return;
            } //end if

            String text = Bot.getTweetText(train);

            Bot.LOGGER.atInfo()
                      .log(text);

            Bot.createTweet(twitterApi, text);
        };

        long initialDelay = 0L;

        long period = 5L;

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(runnable, initialDelay, period,
            TimeUnit.MINUTES);

        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            Bot.LOGGER.atError()
                      .withThrowable(e)
                      .log();
        } //end try catch
    } //main
}