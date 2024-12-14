package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.userlist.Status;
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

    @ShellMethod(key = "list", value = "Test command")
    public AnimeList list(@ShellOption(defaultValue = "@me") String userName) throws JsonProcessingException {
        AnimeList listByUserName = userListService.getListByUserName(userName, Status.COMPLETED);
        log.info("List length fetched: {}", listByUserName.getData().size());
        return listByUserName;
    }
}