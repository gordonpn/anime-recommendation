package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "node",
        "relation_type",
        "relation_type_formatted"
})
@Data
public class RelatedAnime {

    @JsonProperty("node")
    public Node node;
    @JsonProperty("relation_type")
    public String relationType;
    @JsonProperty("relation_type_formatted")
    public String relationTypeFormatted;

}
