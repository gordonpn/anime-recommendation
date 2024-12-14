package com.gordonpn.anime.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
@Slf4j
public class GracefulShutdownHandler {
    private final DataSource dataSource;

    public GracefulShutdownHandler(DataSource dataSource) {
        this.dataSource = dataSource;
        registerShutdownHook();
    }

    private void run() throws SQLException {
        dataSource.getConnection().close();
        log.info("Executing shutdown hook");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                run();
            } catch (SQLException e) {
                throw new RuntimeException("Error while closing the connection", e);
            }
        }));
    }
}
