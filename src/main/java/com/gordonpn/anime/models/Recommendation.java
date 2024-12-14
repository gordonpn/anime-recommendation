package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "node",
        "num_recommendations"
})
@Data
public class Recommendation {

    @JsonProperty("node")
    public Node node;
    @JsonProperty("num_recommendations")
    public Integer numRecommendations;

}
