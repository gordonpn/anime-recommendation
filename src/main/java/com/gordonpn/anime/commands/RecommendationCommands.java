package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.ObjectProvider;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.Node;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.RelatedAnime;
import com.gordonpn.anime.models.SeasonalAnime;
import com.gordonpn.anime.models.SeasonalAnimeResult;
import com.gordonpn.anime.models.MalResourceNotFoundException;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
    private final ObjectProvider<LineReader> lineReaderProvider;

    @ShellMethod(key = "recommendation", value = "Generate personalized anime recommendations based on completed shows. Groups series, applies user rating weights, and averages candidate franchise scores.")
    public String recommendation(
            @ShellOption(defaultValue = ShellOption.NULL, help = "The MyAnimeList username, or '@me' to use the local SQLite database") String userName,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Output the recommendations in JSON format") Boolean json,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum number of recommendations to return") Integer limit,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Minimum release year filter") Integer minYear,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum release year filter") Integer maxYear,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Decay parameter in months for temporal decay weighting (half-life)") Integer decayMonths) throws JsonProcessingException {
        
        String finalUserName = userName != null ? userName : promptString("Enter MyAnimeList username", "@me");
        boolean finalJson = json != null ? json : promptBoolean("Output in JSON format?", false);
        int finalLimit = limit != null ? limit : promptInt("Enter maximum number of recommendations to return", 10);
        Integer finalMinYear = minYear != null ? minYear : promptOptionalInt("Enter minimum release year (optional, leave blank for none)", null);
        Integer finalMaxYear = maxYear != null ? maxYear : promptOptionalInt("Enter maximum release year (optional, leave blank for none)", null);
        Integer finalDecayMonths = decayMonths != null ? decayMonths : promptOptionalInt("Enter temporal decay half-life in months (optional, leave blank for none)", null);

        List<RecommendationDetailedResult> recommendations = recommendationFacade.getRecommendations(finalUserName, finalLimit, finalMinYear, finalMaxYear, finalDecayMonths);
        
        if (finalJson) {
            return objectMapper.writeValueAsString(recommendations);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== Recommendations for %s ===\n", finalUserName));
        sb.append(String.format("%-4s | %-40s | %-6s | %-6s | %-6s | %-6s | %s\n", 
                "Rank", "Title", "Score", "Occur", "Rating", "Pop", "Explanation"));
        sb.append("-".repeat(110)).append("\n");
        for (int i = 0; i < recommendations.size(); i++) {
            RecommendationDetailedResult res = recommendations.get(i);
            String title = res.getTitle() != null ? res.getTitle() : "Unknown";
            if (title.length() > 40) {
                title = title.substring(0, 37) + "...";
            }
            sb.append(String.format("%-4d | %-40s | %-6.2f | %-6.1f | %-6.1f | %-6s | %s\n",
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

    @ShellMethod(key = "seasonal", value = "Recommend top-rated seasonal anime from past years, grouped by franchise component.")
    public String seasonal(
            @ShellOption(defaultValue = ShellOption.NULL, help = "Number of past years of seasonal anime to search") String numYears,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Output the seasonal rankings in JSON format") Boolean json,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum number of results to display") Integer limit) throws JsonProcessingException {
        
        String finalNumYears = numYears != null ? numYears : promptString("Enter number of past years of seasonal anime to search", "5");
        boolean finalJson = json != null ? json : promptBoolean("Output the seasonal rankings in JSON format?", false);
        int finalLimit = limit != null ? limit : promptInt("Enter maximum number of results to display", 15);

        var seasons = List.of("winter", "spring", "summer", "fall");
        var years = Integer.parseInt(finalNumYears);

        var allData = new ArrayList<Datum>();
        var currentYear = Year.now().getValue();
        for (var i = 0; i < years; i++) {
            for (String season : seasons) {
                SeasonalAnime seasonalAnime = animeService.getSeasonalAnime(season, currentYear - i);
                List<Datum> data = seasonalAnime.getData();
                if (data != null) {
                    allData.addAll(data);
                }
            }
        }

        // 1. Sort the raw seasonal data by mean score descending to find candidate items
        List<Datum> topSeasonal = allData.stream()
                .filter(d -> d.getNode() != null && d.getNode().getId() != null)
                .sorted((d1, d2) -> {
                    Double m1 = d1.getNode().getMean();
                    Double m2 = d2.getNode().getMean();
                    return Double.compare(m2 != null ? m2 : 0.0, m1 != null ? m1 : 0.0);
                })
                .limit(Math.max(100, finalLimit * 3))
                .toList();

        // 2. Fetch full details (with related_anime) and build relationship graph
        Map<Integer, Set<Integer>> graph = new HashMap<>();
        Map<Integer, AnimeDetails> detailsMap = new HashMap<>();

        for (Datum datum : topSeasonal) {
            Integer id = datum.getNode().getId();
            try {
                AnimeDetails details = animeService.getAnimeById(id);
                detailsMap.put(id, details);

                graph.putIfAbsent(id, new HashSet<>());

                List<RelatedAnime> related = details.getRelatedAnime();
                if (related != null) {
                    for (RelatedAnime rel : related) {
                        if (rel.getNode() != null && rel.getNode().getId() != null) {
                            Integer relId = rel.getNode().getId();
                            graph.putIfAbsent(relId, new HashSet<>());
                            graph.get(id).add(relId);
                            graph.get(relId).add(id);
                        }
                    }
                }
            } catch (MalResourceNotFoundException e) {
                log.warn("Seasonal anime ID {} not found on MyAnimeList, skipping: {}", id, e.getMessage());
            } catch (Exception e) {
                log.error("Failed to fetch relations for seasonal anime ID: {}", id, e);
                throw e;
            }
        }

        // 3. Find connected components of the graph (each represents a franchise/series)
        Set<Integer> visited = new HashSet<>();
        List<Set<Integer>> components = new ArrayList<>();

        for (Integer node : graph.keySet()) {
            if (!visited.contains(node)) {
                Set<Integer> component = new HashSet<>();
                Queue<Integer> queue = new LinkedList<>();
                queue.add(node);
                visited.add(node);

                while (!queue.isEmpty()) {
                    Integer curr = queue.poll();
                    component.add(curr);
                    for (Integer neighbor : graph.getOrDefault(curr, Collections.emptySet())) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                components.add(component);
            }
        }

        // 4. Consolidate seasonal items belonging to the same component
        List<SeasonalAnimeResult> consolidatedResults = new ArrayList<>();
        for (Set<Integer> component : components) {
            List<Integer> componentSeasonalIds = new ArrayList<>();
            for (Integer id : component) {
                if (detailsMap.containsKey(id)) {
                    componentSeasonalIds.add(id);
                }
            }

            if (componentSeasonalIds.isEmpty()) {
                continue;
            }

            // Calculate combined mean rating (arithmetic average of elements present in list)
            double sum = 0.0;
            int count = 0;
            for (Integer id : componentSeasonalIds) {
                AnimeDetails d = detailsMap.get(id);
                if (d != null && d.getMean() != null) {
                    sum += d.getMean();
                    count++;
                }
            }
            double combinedMean = count > 0 ? (sum / count) : 0.0;

            // Pick the title of the entry with the highest popularity (lowest popularity rank) in the ENTIRE component
            AnimeDetails representative = null;
            for (Integer id : component) {
                AnimeDetails d = null;
                if (detailsMap.containsKey(id)) {
                    d = detailsMap.get(id);
                } else {
                    try {
                        d = animeService.getAnimeById(id);
                    } catch (MalResourceNotFoundException e) {
                        log.debug("Skipping relation graph bridge detail fetch failure for ID (Not Found): {}", id);
                    } catch (Exception e) {
                        log.error("Failed to fetch relation graph bridge detail for ID: {}", id, e);
                        throw e;
                    }
                }
                if (d == null) continue;
                if (representative == null) {
                    representative = d;
                } else {
                    Integer pRep = representative.getPopularity();
                    Integer pD = d.getPopularity();
                    if (pD != null && (pRep == null || pD < pRep)) {
                        representative = d;
                    } else if ((pD == null && pRep == null) || (pD != null && pD.equals(pRep))) {
                        Double mRep = representative.getMean();
                        Double mD = d.getMean();
                        if (mD != null && (mRep == null || mD > mRep)) {
                            representative = d;
                        }
                    }
                }
            }

            String finalTitle = representative != null ? representative.getDisplayTitle() : "Unknown Series";
            Integer repId = representative != null ? representative.getId() : componentSeasonalIds.get(0);

            consolidatedResults.add(SeasonalAnimeResult.builder()
                    .id(repId)
                    .title(finalTitle)
                    .mean(combinedMean)
                    .build());
        }

        // 5. Sort consolidated results by score descending, resolving duplicates via title and id comparison
        Set<SeasonalAnimeResult> sortedResult = new TreeSet<>(
                Comparator.<SeasonalAnimeResult>comparingDouble(a -> a.getMean() == null ? Double.MIN_VALUE : a.getMean()).reversed()
                        .thenComparing(SeasonalAnimeResult::getTitle)
                        .thenComparingInt(SeasonalAnimeResult::getId)
        );
        sortedResult.addAll(consolidatedResults);

        if (finalJson) {
            return objectMapper.writeValueAsString(sortedResult.stream().limit(finalLimit).toList());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== Top Seasonal Anime (Past %s Years) ===\n", finalNumYears));
        sb.append(String.format("%-4s | %-50s | %-6s\n", "Rank", "Title", "Rating"));
        sb.append("-".repeat(66)).append("\n");
        int rank = 1;
        for (SeasonalAnimeResult res : sortedResult) {
            if (rank > finalLimit) {
                break;
            }
            String title = res.getTitle() != null ? res.getTitle() : "Unknown";
            if (title.length() > 50) {
                title = title.substring(0, 47) + "...";
            }
            sb.append(String.format("%-4d | %-50s | %-6.2f\n",
                    rank,
                    title,
                    res.getMean() != null ? res.getMean() : 0.0));
            rank++;
        }
        sb.append("==========================================\n");
        return sb.toString();
    }

    @ShellMethod(key = "suggestions", value = "Retrieve personalized anime suggestions directly from MyAnimeList's native recommendations engine.")
    public String suggestions(
            @ShellOption(defaultValue = ShellOption.NULL, help = "Output the suggestions in JSON format") Boolean json,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum number of suggestions to return") Integer limit) throws JsonProcessingException {
        
        boolean finalJson = json != null ? json : promptBoolean("Output the suggestions in JSON format?", false);
        int finalLimit = limit != null ? limit : promptInt("Enter maximum number of suggestions to return", 10);

        AnimeList suggestionsList = recommendationService.getSuggestions();
        List<Datum> data = suggestionsList != null ? suggestionsList.getData() : Collections.emptyList();

        if (finalJson) {
            return objectMapper.writeValueAsString(data.stream().limit(finalLimit).toList());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Personalized Suggestions from MyAnimeList ===\n");
        sb.append(String.format("%-4s | %-10s | %s\n", "Rank", "ID", "Title"));
        sb.append("-".repeat(60)).append("\n");

        int count = Math.min(data.size(), finalLimit);
        for (int i = 0; i < count; i++) {
            Datum d = data.get(i);
            Node node = d.getNode();
            if (node != null) {
                sb.append(String.format("%-4d | %-10d | %s\n",
                        i + 1,
                        node.getId(),
                        node.getDisplayTitle() != null ? node.getDisplayTitle() : "Unknown"));
            }
        }
        sb.append("==================================================\n");
        return sb.toString();
    }

    private String promptString(String promptText, String defaultValue) {
        if (lineReaderProvider == null) {
            return defaultValue;
        }
        LineReader lineReader = lineReaderProvider.getIfAvailable();
        if (lineReader == null) {
            return defaultValue;
        }
        try {
            String line = lineReader.readLine(promptText + " (default: " + defaultValue + "): ");
            if (line == null || line.trim().isEmpty()) {
                return defaultValue;
            }
            return line.trim();
        } catch (Exception e) {
            log.debug("Failed to read interactive input, using default", e);
            return defaultValue;
        }
    }

    private boolean promptBoolean(String promptText, boolean defaultValue) {
        String defaultStr = defaultValue ? "y" : "n";
        String response = promptString(promptText + " (y/n)", defaultStr);
        return response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes") || response.equalsIgnoreCase("true");
    }

    private int promptInt(String promptText, int defaultValue) {
        String response = promptString(promptText, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(response);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private Integer promptOptionalInt(String promptText, Integer defaultValue) {
        String defaultStr = defaultValue != null ? String.valueOf(defaultValue) : "";
        String response = promptString(promptText, defaultStr);
        if (response.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(response);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Using default: " + (defaultValue != null ? defaultValue : "none"));
            return defaultValue;
        }
    }
}
