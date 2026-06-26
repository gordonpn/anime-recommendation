package com.gordonpn.anime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.component.flow.ComponentFlow;

@Configuration
public class TuiComponentConfiguration {

    @Bean
    public ComponentFlow.Builder componentFlowBuilder() {
        return ComponentFlow.builder();
    }
}
