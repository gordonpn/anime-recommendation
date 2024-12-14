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
        "node",
        "list_status"
})
@Data
public class Datum {
    @JsonProperty("node")
    private Node node;
    @JsonProperty("list_status")
    private ListStatus listStatus;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();
}
