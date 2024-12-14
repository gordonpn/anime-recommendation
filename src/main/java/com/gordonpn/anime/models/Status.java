package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "watching",
        "completed",
        "on_hold",
        "dropped",
        "plan_to_watch"
})
@Data
public class Status {

    @JsonProperty("watching")
    public String watching;
    @JsonProperty("completed")
    public String completed;
    @JsonProperty("on_hold")
    public String onHold;
    @JsonProperty("dropped")
    public String dropped;
    @JsonProperty("plan_to_watch")
    public String planToWatch;

}
