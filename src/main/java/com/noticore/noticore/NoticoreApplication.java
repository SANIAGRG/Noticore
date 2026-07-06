package com.noticore.noticore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
// @EnableRetry turns on Spring's processing of @Retryable / @Recover
// annotations anywhere in the app -- without this, those annotations would
// just be silently ignored and methods would run normally, once, with no
// retry behavior at all.
@EnableRetry
public class NoticoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoticoreApplication.class, args);
    }
}