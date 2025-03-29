package com.gordonpn.anime.config;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;

import java.io.PrintWriter;
import java.io.StringWriter;

@Configuration
public class ShellConfig {

    @Bean
    public CommandExceptionResolver commandExceptionResolver() {
        return (exception) -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String stackTrace = sw.toString();

            return CommandHandlingResult.of(stackTrace, 1);
        };
    }

    @Bean
    public ResultHandler<CommandHandlingResult> resultHandler() {
        return new ResultHandler<CommandHandlingResult>() {
            @Override
            public void handleResult(CommandHandlingResult result) {
                if (result.exitCode() != 0 && result.message() != null) {
                    System.err.println(new AttributedString("Error: ",
                            AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).toAnsi());
                    System.err.println(result.message());
                } else if (result.message() != null) {
                    System.out.println(result.message());
                }
            }
        };
    }
}