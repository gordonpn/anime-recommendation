package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ListStatusType {
    WATCHING("watching"),
    COMPLETED("completed"),
    ON_HOLD("on_hold"),
    DROPPED("dropped"),
    PLAN_TO_WATCH("plan_to_watch");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ListStatusType fromValue(String value) {
        for (ListStatusType status : ListStatusType.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}