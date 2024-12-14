package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "medium",
        "large"
})
@Data
public class MainPicture {

    @JsonProperty("medium")
    public String medium;
    @JsonProperty("large")
    public String large;

}
