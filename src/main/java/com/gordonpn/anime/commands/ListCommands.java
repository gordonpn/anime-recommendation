package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.userlist.Status;
import com.gordonpn.anime.service.BackgroundFetchService;
import com.gordonpn.anime.service.UserListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class ListCommands {
    private final UserListService userListService;
    private final BackgroundFetchService backgroundFetchService;

    @ShellMethod(key = "list", value = "List all completed animes")
    public AnimeList list(@ShellOption(defaultValue = "@me") String userName) throws JsonProcessingException {
        AnimeList listByUserName = userListService.getListByUserName(userName, Status.COMPLETED);
        log.info("List length fetched: {}", listByUserName.getData().size());
        return listByUserName;
    }

    @ShellMethod(key = "sync", value = "Synchronize MyAnimeList lists to local SQLite database")
    public String sync() throws JsonProcessingException {
        log.info("Triggering list synchronization");
        backgroundFetchService.fetchList();
        return "List sync completed successfully!";
    }
}