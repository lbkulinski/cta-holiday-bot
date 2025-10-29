package com.cta4j.runner;

import com.cta4j.twitter.service.TwitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TweetRunner implements CommandLineRunner {
    private final TwitterService twitterService;

    @Autowired
    public TweetRunner(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @Override
    public void run(String... args) {
        this.twitterService.postTweet("Testing... Getting ready for Holiday Train 2025!");
    }
}
