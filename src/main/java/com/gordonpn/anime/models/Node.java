package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "title",
        "main_picture"
})
@Data
public class Node {

    @JsonProperty("id")
    public Integer id;
    @JsonProperty("title")
    public String title;
    @JsonProperty("main_picture")
    public MainPicture mainPicture;
    @JsonProperty("mean")
    public Double mean;
    @JsonProperty("alternative_titles")
    public AlternativeTitles alternativeTitles;

    public String getDisplayTitle() {
        if (alternativeTitles != null && alternativeTitles.getEn() != null && !alternativeTitles.getEn().trim().isEmpty()) {
            return alternativeTitles.getEn();
        }
        return title;
    }

}
