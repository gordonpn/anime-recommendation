package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.SeasonalAnime;
import com.gordonpn.anime.models.SeasonalAnimeResult;
import com.gordonpn.anime.service.AnimeService;
import com.gordonpn.anime.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.gordonpn.anime.models.RecommendationDetailedResult;
import com.gordonpn.anime.service.recommendation.RecommendationFacade;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class RecommendationCommands {
    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RecommendationService recommendationService;
    private final AnimeService animeService;
    private final RecommendationFacade recommendationFacade;

    @ShellMethod(key = "recommendation", value = "Recommendation command")
    public String recommendation(
            @ShellOption(defaultValue = "@me") String userName,
            @ShellOption(defaultValue = "false") boolean json,
            @ShellOption(defaultValue = "10") int limit) throws JsonProcessingException {
        
        List<RecommendationDetailedResult> recommendations = recommendationFacade.getRecommendations(userName, limit);
        
        if (json) {
            return objectMapper.writeValueAsString(recommendations);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== Recommendations for %s ===\n", userName));
        sb.append(String.format("%-4s | %-40s | %-6s | %-6s | %-6s | %-6s | %s\n", 
                "Rank", "Title", "Score", "Occur", "Rating", "Pop", "Explanation"));
        sb.append("-".repeat(110)).append("\n");
        for (int i = 0; i < recommendations.size(); i++) {
            RecommendationDetailedResult res = recommendations.get(i);
            String title = res.getTitle() != null ? res.getTitle() : "Unknown";
            if (title.length() > 40) {
                title = title.substring(0, 37) + "...";
            }
            sb.append(String.format("%-4d | %-40s | %-6.2f | %-6d | %-6.1f | %-6s | %s\n",
                    i + 1,
                    title,
                    res.getScore(),
                    res.getOccurrence(),
                    res.getMeanRating() != null ? res.getMeanRating() : 0.0,
                    res.getPopularity() != null ? String.valueOf(res.getPopularity()) : "N/A",
                    res.getExplanation()));
        }
        sb.append("=============================================\n");
        return sb.toString();
    }

    @ShellMethod(key = "seasonal", value = "Seasonal recommendation command")
    public String seasonal(@ShellOption(defaultValue = "5") String numYears) throws JsonProcessingException {
        var seasons = List.of("winter", "spring", "summer", "fall");
        var years = Integer.parseInt(numYears);

        var allData = new ArrayList<Datum>();
        var currentYear = Year.now().getValue();
        for (var i = 0; i < years; i++) {
            for (String season : seasons) {
                SeasonalAnime seasonalAnime = animeService.getSeasonalAnime(season, currentYear - i);
                List<Datum> data = seasonalAnime.getData();
                allData.addAll(data);
            }
        }

        Set<SeasonalAnimeResult> sortedResult = new TreeSet<>(Comparator.<SeasonalAnimeResult>comparingDouble(a ->
                a.getMean() == null ? Double.MIN_VALUE : a.getMean()).reversed());

        for (Datum datum : allData) {
            Double meanScore = datum.getNode().getMean();
            SeasonalAnimeResult seasonalAnimeResult = SeasonalAnimeResult.builder()
                    .id(datum.getNode().getId())
                    .title(datum.getNode().getTitle())
                    .mean(meanScore)
                    .build();

            sortedResult.add(seasonalAnimeResult);
        }

        log.info("Sorted: {}", sortedResult);

        return objectMapper.writeValueAsString(sortedResult);
    }

    @ShellMethod(key = "suggestions", value = "Suggestions command")
    public void suggestions() throws JsonProcessingException {
        recommendationService.getSuggestions();
    }
}
