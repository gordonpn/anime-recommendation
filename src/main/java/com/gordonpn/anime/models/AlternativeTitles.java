package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "synonyms",
        "en",
        "ja"
})
@Data
public class AlternativeTitles {

    @JsonProperty("synonyms")
    public List<String> synonyms;
    @JsonProperty("en")
    public String en;
    @JsonProperty("ja")
    public String ja;

}
