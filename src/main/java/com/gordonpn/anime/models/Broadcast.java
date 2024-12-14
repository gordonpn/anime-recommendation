package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "day_of_the_week",
        "start_time"
})
@Data
public class Broadcast {

    @JsonProperty("day_of_the_week")
    public String dayOfTheWeek;
    @JsonProperty("start_time")
    public String startTime;

}
