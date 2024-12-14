package com.gordonpn.anime.models.userlist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Status {
    WATCHING("watching"),
    PLAN_TO_WATCH("plan_to_watch"),
    COMPLETED("completed"),
    DROPPED("dropped"),
    ON_HOLD("on_hold");

    private final String type;
}
