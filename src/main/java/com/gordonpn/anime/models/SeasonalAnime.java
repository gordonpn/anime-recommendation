package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data",
        "paging",
        "season"
})
@Data
public class SeasonalAnime {

    @JsonProperty("data")
    public List<Datum> data;
    @JsonProperty("paging")
    public Paging paging;
    @JsonProperty("season")
    public Season season;

}
