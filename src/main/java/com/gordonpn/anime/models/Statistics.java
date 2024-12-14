package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "status",
        "num_list_users"
})
@Data
public class Statistics {

    @JsonProperty("status")
    public Status status;
    @JsonProperty("num_list_users")
    public Integer numListUsers;

}
