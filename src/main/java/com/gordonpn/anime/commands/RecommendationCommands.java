package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.RelatedAnime;
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
    public String seasonal(
            @ShellOption(defaultValue = "5") String numYears,
            @ShellOption(defaultValue = "false") boolean json,
            @ShellOption(defaultValue = "15") int limit) throws JsonProcessingException {
        var seasons = List.of("winter", "spring", "summer", "fall");
        var years = Integer.parseInt(numYears);

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
                .limit(Math.max(100, limit * 3))
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
            } catch (Exception e) {
                log.error("Failed to fetch relations for seasonal anime ID: {}", id, e);
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
                    } catch (Exception e) {
                        log.debug("Skipping relation graph bridge detail fetch failure for ID: {}", id);
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

            String finalTitle = representative != null ? representative.getTitle() : "Unknown Series";
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

        if (json) {
            return objectMapper.writeValueAsString(sortedResult.stream().limit(limit).toList());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n=== Top Seasonal Anime (Past %s Years) ===\n", numYears));
        sb.append(String.format("%-4s | %-50s | %-6s\n", "Rank", "Title", "Rating"));
        sb.append("-".repeat(66)).append("\n");
        int rank = 1;
        for (SeasonalAnimeResult res : sortedResult) {
            if (rank > limit) {
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

    @ShellMethod(key = "suggestions", value = "Suggestions command")
    public void suggestions() throws JsonProcessingException {
        recommendationService.getSuggestions();
    }
}
