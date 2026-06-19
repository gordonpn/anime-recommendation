package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "title",
        "main_picture",
        "alternative_titles",
        "start_date",
        "end_date",
        "synopsis",
        "mean",
        "rank",
        "popularity",
        "num_list_users",
        "num_scoring_users",
        "nsfw",
        "created_at",
        "updated_at",
        "media_type",
        "status",
        "genres",
        "my_list_status",
        "num_episodes",
        "start_season",
        "broadcast",
        "source",
        "average_episode_duration",
        "rating",
        "pictures",
        "background",
        "related_anime",
        "related_manga",
        "recommendations",
        "studios",
        "statistics"
})
@Data
public class AnimeDetails {

    @JsonProperty("id")
    public Integer id;
    @JsonProperty("title")
    public String title;
    @JsonProperty("main_picture")
    public MainPicture mainPicture;
    @JsonProperty("alternative_titles")
    public AlternativeTitles alternativeTitles;
    @JsonProperty("start_date")
    public String startDate;
    @JsonProperty("end_date")
    public String endDate;
    @JsonProperty("synopsis")
    public String synopsis;
    @JsonProperty("mean")
    public Double mean;
    @JsonProperty("rank")
    public Integer rank;
    @JsonProperty("popularity")
    public Integer popularity;
    @JsonProperty("num_list_users")
    public Integer numListUsers;
    @JsonProperty("num_scoring_users")
    public Integer numScoringUsers;
    @JsonProperty("nsfw")
    public String nsfw;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("updated_at")
    public String updatedAt;
    @JsonProperty("media_type")
    public String mediaType;
    @JsonProperty("status")
    public String status;
    @JsonProperty("genres")
    public List<Genre> genres;
    @JsonProperty("my_list_status")
    public MyListStatus myListStatus;
    @JsonProperty("num_episodes")
    public Integer numEpisodes;
    @JsonProperty("start_season")
    public StartSeason startSeason;
    @JsonProperty("broadcast")
    public Broadcast broadcast;
    @JsonProperty("source")
    public String source;
    @JsonProperty("average_episode_duration")
    public Integer averageEpisodeDuration;
    @JsonProperty("rating")
    public String rating;
    @JsonProperty("pictures")
    public List<Picture> pictures;
    @JsonProperty("background")
    public String background;
    @JsonProperty("related_anime")
    public List<RelatedAnime> relatedAnime;
    @JsonProperty("related_manga")
    public List<Object> relatedManga;
    @JsonProperty("recommendations")
    public List<Recommendation> recommendations;
    @JsonProperty("studios")
    public List<Studio> studios;
    @JsonProperty("statistics")
    public Statistics statistics;

    public String getDisplayTitle() {
        if (alternativeTitles != null && alternativeTitles.getEn() != null && !alternativeTitles.getEn().trim().isEmpty()) {
            return alternativeTitles.getEn();
        }
        return title;
    }
}
