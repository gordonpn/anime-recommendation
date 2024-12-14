package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "status",
        "score",
        "num_watched_episodes",
        "is_rewatching",
        "updated_at",
        "num_episodes_watched"
})
@Data
public class ListStatus {
    @JsonProperty("status")
    private String status;
    @JsonProperty("score")
    private Integer score;
    @JsonProperty("num_watched_episodes")
    private Integer numWatchedEpisodes;
    @JsonProperty("is_rewatching")
    private Boolean isRewatching;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("num_episodes_watched")
    private Integer numEpisodesWatched;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();
}
