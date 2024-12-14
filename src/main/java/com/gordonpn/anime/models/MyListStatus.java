package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "status",
        "score",
        "num_episodes_watched",
        "is_rewatching",
        "updated_at"
})
@Data
public class MyListStatus {

    @JsonProperty("status")
    public String status;
    @JsonProperty("score")
    public Integer score;
    @JsonProperty("num_episodes_watched")
    public Integer numEpisodesWatched;
    @JsonProperty("is_rewatching")
    public Boolean isRewatching;
    @JsonProperty("updated_at")
    public String updatedAt;

}
