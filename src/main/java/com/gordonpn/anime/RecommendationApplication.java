package com.gordonpn.anime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecommendationApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(RecommendationApplication.class, args);
        context.registerShutdownHook();
    }
}
