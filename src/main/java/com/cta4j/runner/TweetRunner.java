package com.cta4j.runner;

import com.cta4j.twitter.service.TokenRefreshService;
import com.cta4j.twitter.service.TwitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TweetRunner implements CommandLineRunner {
    private final TwitterService twitterService;
    private final TokenRefreshService tokenRefreshService;

    @Autowired
    public TweetRunner(TwitterService twitterService, TokenRefreshService tokenRefreshService) {
        this.twitterService = twitterService;
        this.tokenRefreshService = tokenRefreshService;
    }

    @Override
    public void run(String... args) {
        this.tokenRefreshService.refreshAccessToken();

        this.twitterService.postTweet("Testing... Getting ready for Holiday Train 2025!");
    }
}
