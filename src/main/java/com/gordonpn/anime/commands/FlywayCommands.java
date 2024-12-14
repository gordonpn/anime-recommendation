package com.gordonpn.anime.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class FlywayCommands {
    private final Flyway flyway;

    @ShellMethod(key = "flyway-info", value = "Show Flyway migration info")
    public void flywayInfo() {
        MigrationInfo[] migrations = flyway.info().all();
        for (MigrationInfo info : migrations) {
            log.info("Version: {}, Description: {}, State: {}",
                    info.getVersion(),
                    info.getDescription(),
                    info.getState());
        }
    }

    @ShellMethod(key = "flyway-repair", value = "Repair Flyway migrations")
    public void repairFlyway() {
        flyway.repair();
    }
}
