package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.SeasonalAnime;
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
import java.util.List;
import java.util.Map;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class RecommendationCommands {
    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RecommendationService recommendationService;
    private final AnimeService animeService;

    @ShellMethod(key = "recommendation", value = "Recommendation command")
    public String recommendation(@ShellOption(defaultValue = "@me") String userName) throws JsonProcessingException {
        Map<String, Integer> recommendationOccurrence = recommendationService.recommendationOccurrence(userName);
        return objectMapper.writeValueAsString(recommendationOccurrence);
    }

    @ShellMethod(key = "seasonal", value = "Seasonal recommendation command")
    public String seasonal(@ShellOption(defaultValue = "5") String numYears) throws JsonProcessingException {
        var seasons = List.of("winter", "spring", "summer", "fall");
        var years = Integer.parseInt(numYears);

        var allData = new ArrayList<>();
        var currentYear = Year.now().getValue();
        for (var i = 0; i < years; i++) {
            for (String season : seasons) {
                SeasonalAnime seasonalAnime = animeService.getSeasonalAnime(season, currentYear - i);
                List<Datum> data = seasonalAnime.getData();
                allData.addAll(data);
            }
        }
        return objectMapper.writeValueAsString(allData);
    }

    @ShellMethod(key = "suggestions", value = "Suggestions command")
    public void suggestions() throws JsonProcessingException {
        recommendationService.getSuggestions();
    }
}
