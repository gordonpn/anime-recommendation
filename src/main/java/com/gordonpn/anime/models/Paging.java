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
        "next"
})
@Data
public class Paging {
    @JsonProperty("next")
    private String next;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();
}
