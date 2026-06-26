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
import com.gordonpn.anime.models.dto.SeasonalAnimeResult;
import com.gordonpn.anime.models.exception.MalResourceNotFoundException;
import com.gordonpn.anime.models.exception.AnimeDataProcessingException;
import com.gordonpn.anime.service.AnimeService;
import com.gordonpn.anime.service.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.SelectItem;
import org.jline.terminal.Terminal;
import com.gordonpn.anime.service.recommendation.TableFormatter;
import com.gordonpn.anime.util.TerminalSpinner;

import java.io.PrintWriter;
import java.io.IOException;
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

import com.gordonpn.anime.models.dto.RecommendationDetailedResult;
import com.gordonpn.anime.service.recommendation.RecommendationFacade;

@ShellComponent
@Slf4j
public class RecommendationCommands {
    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RecommendationService recommendationService;
    private final AnimeService animeService;
    private final RecommendationFacade recommendationFacade;
    private final ObjectProvider<LineReader> lineReaderProvider;
    private final ComponentFlow.Builder componentFlowBuilder;
    private final Terminal terminal;
    private final TableFormatter tableFormatter;

    public RecommendationCommands(
            @Qualifier("objectMapper") ObjectMapper objectMapper,
            RecommendationService recommendationService,
            AnimeService animeService,
            RecommendationFacade recommendationFacade,
            ObjectProvider<LineReader> lineReaderProvider,
            ComponentFlow.Builder componentFlowBuilder,
            Terminal terminal,
            TableFormatter tableFormatter
    ) {
        this.objectMapper = objectMapper;
        this.recommendationService = recommendationService;
        this.animeService = animeService;
        this.recommendationFacade = recommendationFacade;
        this.lineReaderProvider = lineReaderProvider;
        this.componentFlowBuilder = componentFlowBuilder;
        this.terminal = terminal;
        this.tableFormatter = tableFormatter;
    }

    @ShellMethod(key = "recommendation", value = "Generate personalized anime recommendations based on completed shows. Groups series, applies user rating weights, and averages candidate franchise scores.")
    public String recommendation(
            @ShellOption(defaultValue = ShellOption.NULL, help = "The MyAnimeList username, or '@me' to use the local SQLite database") String userName,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Output the recommendations in JSON format") Boolean json,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum number of recommendations to return") Integer limit,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Minimum release year filter") Integer minYear,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum release year filter") Integer maxYear,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Decay parameter in months for temporal decay weighting (half-life)") Integer decayMonths,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Freshness boost parameter for release date weighting (decay rate)") Double freshnessBoost) {
        
        String finalUserName = userName;
        Boolean finalJson = json;
        Integer finalLimit = limit;
        Integer finalMinYear = minYear;
        Integer finalMaxYear = maxYear;
        Integer finalDecayMonths = decayMonths;
        Double finalFreshnessBoost = freshnessBoost;

        if (userName == null || json == null || limit == null || minYear == null || maxYear == null || decayMonths == null || freshnessBoost == null) {
            ComponentFlow.Builder flowBuilder = componentFlowBuilder.clone().reset();
            boolean hasFlow = false;

            if (userName == null) {
                flowBuilder.withStringInput("userName")
                        .name("Enter MyAnimeList username")
                        .defaultValue("@me")
                        .and();
                hasFlow = true;
            }
            if (json == null) {
                flowBuilder.withSingleItemSelector("json")
                        .name("Output in JSON format?")
                        .selectItems(List.of(
                                SelectItem.of("No (standard table)", "false"),
                                SelectItem.of("Yes (JSON text)", "true")
                        ))
                        .and();
                hasFlow = true;
            }
            if (limit == null) {
                flowBuilder.withSingleItemSelector("limit")
                        .name("Select maximum number of recommendations")
                        .selectItems(List.of(
                                SelectItem.of("5", "5"),
                                SelectItem.of("10", "10"),
                                SelectItem.of("20", "20"),
                                SelectItem.of("50", "50")
                        ))
                        .and();
                hasFlow = true;
            }
            if (minYear == null) {
                flowBuilder.withStringInput("minYear")
                        .name("Enter minimum release year (optional, press Enter for none)")
                        .and();
                hasFlow = true;
            }
            if (maxYear == null) {
                flowBuilder.withStringInput("maxYear")
                        .name("Enter maximum release year (optional, press Enter for none)")
                        .and();
                hasFlow = true;
            }
            if (decayMonths == null) {
                flowBuilder.withStringInput("decayMonths")
                        .name("Enter temporal decay half-life in months (optional, press Enter for none)")
                        .and();
                hasFlow = true;
            }
            if (freshnessBoost == null) {
                flowBuilder.withStringInput("freshnessBoost")
                        .name("Enter freshness boost parameter (optional, press Enter for none)")
                        .and();
                hasFlow = true;
            }

            if (hasFlow) {
                var flowResult = flowBuilder.build().run();
                var context = flowResult.getContext();

                if (userName == null) {
                    finalUserName = getSafe(context, "userName");
                }
                if (json == null) {
                    String jsonVal = getSafe(context, "json");
                    finalJson = "true".equalsIgnoreCase(jsonVal);
                }
                if (limit == null) {
                    String limitVal = getSafe(context, "limit");
                    try {
                        finalLimit = limitVal != null ? Integer.parseInt(limitVal) : 10;
                    } catch (NumberFormatException e) {
                        finalLimit = 10;
                    }
                }
                if (minYear == null) {
                    String minYearVal = getSafe(context, "minYear");
                    try {
                        finalMinYear = (minYearVal != null && !minYearVal.trim().isEmpty()) ? Integer.parseInt(minYearVal.trim()) : null;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format for minYear. Ignoring filter.");
                        finalMinYear = null;
                    }
                }
                if (maxYear == null) {
                    String maxYearVal = getSafe(context, "maxYear");
                    try {
                        finalMaxYear = (maxYearVal != null && !maxYearVal.trim().isEmpty()) ? Integer.parseInt(maxYearVal.trim()) : null;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format for maxYear. Ignoring filter.");
                        finalMaxYear = null;
                    }
                }
                if (decayMonths == null) {
                    String decayVal = getSafe(context, "decayMonths");
                    try {
                        finalDecayMonths = (decayVal != null && !decayVal.trim().isEmpty()) ? Integer.parseInt(decayVal.trim()) : null;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format for decay months. Ignoring decay.");
                        finalDecayMonths = null;
                    }
                }
                if (freshnessBoost == null) {
                    String freshVal = getSafe(context, "freshnessBoost");
                    try {
                        finalFreshnessBoost = (freshVal != null && !freshVal.trim().isEmpty()) ? Double.parseDouble(freshVal.trim()) : null;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format for freshness boost. Ignoring boost.");
                        finalFreshnessBoost = null;
                    }
                }
            }
        }

        if (finalUserName == null) {
            finalUserName = "@me";
        }
        if (finalJson == null) {
            finalJson = false;
        }
        if (finalLimit == null) {
            finalLimit = 10;
        }

        List<RecommendationDetailedResult> recommendations;
        try (TerminalSpinner spinner = new TerminalSpinner(terminal, "Generating recommendations for " + finalUserName + "...")) {
            recommendations = recommendationFacade.getRecommendations(finalUserName, finalLimit, finalMinYear, finalMaxYear, finalDecayMonths, finalFreshnessBoost);
        }
        
        if (finalJson) {
            try {
                return objectMapper.writeValueAsString(recommendations);
            } catch (JsonProcessingException e) {
                throw new AnimeDataProcessingException("Failed to format recommendations as JSON", e);
            }
        }

        return tableFormatter.formatRecommendations(finalUserName, recommendations);
    }

    @ShellMethod(key = "seasonal", value = "Recommend top-rated seasonal anime from past years, grouped by franchise component.")
    public String seasonal(
            @ShellOption(defaultValue = ShellOption.NULL, help = "Number of past years of seasonal anime to search") String numYears,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Output the seasonal rankings in JSON format") Boolean json,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum number of results to display") Integer limit) {
        
        String finalNumYears = numYears;
        Boolean finalJson = json;
        Integer finalLimit = limit;

        if (numYears == null || json == null || limit == null) {
            ComponentFlow.Builder flowBuilder = componentFlowBuilder.clone().reset();
            boolean hasFlow = false;

            if (numYears == null) {
                flowBuilder.withStringInput("numYears")
                        .name("Enter number of past years of seasonal anime to search")
                        .defaultValue("5")
                        .and();
                hasFlow = true;
            }
            if (json == null) {
                flowBuilder.withSingleItemSelector("json")
                        .name("Output the seasonal rankings in JSON format?")
                        .selectItems(List.of(
                                SelectItem.of("No (standard table)", "false"),
                                SelectItem.of("Yes (JSON text)", "true")
                        ))
                        .and();
                hasFlow = true;
            }
            if (limit == null) {
                flowBuilder.withSingleItemSelector("limit")
                        .name("Enter maximum number of results to display")
                        .selectItems(List.of(
                                SelectItem.of("5", "5"),
                                SelectItem.of("15", "15"),
                                SelectItem.of("30", "30"),
                                SelectItem.of("50", "50")
                        ))
                        .and();
                hasFlow = true;
            }

            if (hasFlow) {
                var flowResult = flowBuilder.build().run();
                var context = flowResult.getContext();

                if (numYears == null) {
                    finalNumYears = getSafe(context, "numYears");
                }
                if (json == null) {
                    String jsonVal = getSafe(context, "json");
                    finalJson = "true".equalsIgnoreCase(jsonVal);
                }
                if (limit == null) {
                    String limitVal = getSafe(context, "limit");
                    try {
                        finalLimit = limitVal != null ? Integer.parseInt(limitVal) : 15;
                    } catch (NumberFormatException e) {
                        finalLimit = 15;
                    }
                }
            }
        }

        if (finalNumYears == null) {
            finalNumYears = "5";
        }
        if (finalJson == null) {
            finalJson = false;
        }
        if (finalLimit == null) {
            finalLimit = 15;
        }

        var seasons = List.of("winter", "spring", "summer", "fall");
        var years = 5;
        try {
            years = Integer.parseInt(finalNumYears);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number of years format. Using default: 5");
        }

        var allData = new ArrayList<Datum>();
        var currentYear = Year.now().getValue();

        final int finalYears = years;
        final List<Datum> finalAllData = allData;

        try (TerminalSpinner spinner = new TerminalSpinner(terminal, "Fetching seasonal anime lists...")) {
            for (var i = 0; i < finalYears; i++) {
                for (String season : seasons) {
                    SeasonalAnime seasonalAnime = animeService.getSeasonalAnime(season, currentYear - i);
                    List<Datum> data = seasonalAnime.getData();
                    if (data != null) {
                        finalAllData.addAll(data);
                    }
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

        try (TerminalSpinner spinner = new TerminalSpinner(terminal, "Building relation graph for top seasonal titles...")) {
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
        
        try (TerminalSpinner spinner = new TerminalSpinner(terminal, "Consolidating franchise results...")) {
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
        }

        // 5. Sort consolidated results by score descending, resolving duplicates via title and id comparison
        Set<SeasonalAnimeResult> sortedResult = new TreeSet<>(
                Comparator.<SeasonalAnimeResult>comparingDouble(a -> a.getMean() == null ? Double.MIN_VALUE : a.getMean()).reversed()
                        .thenComparing(SeasonalAnimeResult::getTitle)
                        .thenComparingInt(SeasonalAnimeResult::getId)
        );
        sortedResult.addAll(consolidatedResults);

        if (finalJson) {
            try {
                return objectMapper.writeValueAsString(sortedResult.stream().limit(finalLimit).toList());
            } catch (JsonProcessingException e) {
                throw new AnimeDataProcessingException("Failed to format seasonal anime as JSON", e);
            }
        }

        return tableFormatter.formatSeasonal(finalNumYears, sortedResult, finalLimit);
    }

    @ShellMethod(key = "suggestions", value = "Retrieve personalized anime suggestions directly from MyAnimeList's native recommendations engine.")
    public String suggestions(
            @ShellOption(defaultValue = ShellOption.NULL, help = "Output the suggestions in JSON format") Boolean json,
            @ShellOption(defaultValue = ShellOption.NULL, help = "Maximum number of suggestions to return") Integer limit) {
        
        Boolean finalJson = json;
        Integer finalLimit = limit;

        if (json == null || limit == null) {
            ComponentFlow.Builder flowBuilder = componentFlowBuilder.clone().reset();
            boolean hasFlow = false;

            if (json == null) {
                flowBuilder.withSingleItemSelector("json")
                        .name("Output the suggestions in JSON format?")
                        .selectItems(List.of(
                                SelectItem.of("No (standard table)", "false"),
                                SelectItem.of("Yes (JSON text)", "true")
                        ))
                        .and();
                hasFlow = true;
            }
            if (limit == null) {
                flowBuilder.withSingleItemSelector("limit")
                        .name("Enter maximum number of suggestions to return")
                        .selectItems(List.of(
                                SelectItem.of("5", "5"),
                                SelectItem.of("10", "10"),
                                SelectItem.of("20", "20"),
                                SelectItem.of("50", "50")
                        ))
                        .and();
                hasFlow = true;
            }

            if (hasFlow) {
                var flowResult = flowBuilder.build().run();
                var context = flowResult.getContext();

                if (json == null) {
                    String jsonVal = getSafe(context, "json");
                    finalJson = "true".equalsIgnoreCase(jsonVal);
                }
                if (limit == null) {
                    String limitVal = getSafe(context, "limit");
                    try {
                        finalLimit = limitVal != null ? Integer.parseInt(limitVal) : 10;
                    } catch (NumberFormatException e) {
                        finalLimit = 10;
                    }
                }
            }
        }

        if (finalJson == null) {
            finalJson = false;
        }
        if (finalLimit == null) {
            finalLimit = 10;
        }

        AnimeList suggestionsList;
        try (TerminalSpinner spinner = new TerminalSpinner(terminal, "Retrieving suggestions from MyAnimeList...")) {
            suggestionsList = recommendationService.getSuggestions();
        }
        List<Datum> data = suggestionsList != null ? suggestionsList.getData() : Collections.emptyList();

        if (finalJson) {
            try {
                return objectMapper.writeValueAsString(data.stream().limit(finalLimit).toList());
            } catch (JsonProcessingException e) {
                throw new AnimeDataProcessingException("Failed to format suggestions as JSON", e);
            }
        }

        return tableFormatter.formatSuggestions(data, finalLimit);
    }

    @ShellMethod(key = "dashboard", value = "Show a distraction-free full-screen analytics dashboard of your anime recommendations.")
    public void dashboard(
            @ShellOption(defaultValue = ShellOption.NULL, help = "The MyAnimeList username, or '@me'") String userName
    ) {
        String finalUserName = userName;
        if (finalUserName == null) {
            var flowResult = componentFlowBuilder.clone().reset()
                    .withStringInput("userName")
                    .name("Enter MyAnimeList username")
                    .defaultValue("@me")
                    .and()
                    .build()
                    .run();
            finalUserName = getSafe(flowResult.getContext(), "userName");
        }
        if (finalUserName == null || finalUserName.trim().isEmpty()) {
            finalUserName = "@me";
        }

        org.jline.terminal.Attributes originalAttributes = terminal.enterRawMode();
        PrintWriter writer = terminal.writer();
        try {
            writer.print("\033[?1049h");
            writer.print("\033[?25l");
            writer.flush();

            boolean running = true;
            boolean needsRefresh = true;
            List<RecommendationDetailedResult> recommendations = null;

            while (running) {
                if (needsRefresh) {
                    writer.print("\033[H\033[2J");
                    writer.println("Loading recommendations for " + finalUserName + "...");
                    writer.flush();
                    try {
                        recommendations = recommendationFacade.getRecommendations(finalUserName, 10, null, null, null, null);
                    } catch (Exception e) {
                        writer.print("\033[H\033[2J");
                        writer.println("Error fetching recommendations: " + e.getMessage());
                        writer.println("\nPress [q] to exit, [r] to retry.");
                        writer.flush();
                        recommendations = null;
                    }
                    needsRefresh = false;
                }

                if (recommendations != null) {
                    writer.print("\033[H\033[2J");
                    writer.println("==============================================================================================================");
                    writer.println("                               ANIME RECOMMENDATIONS ANALYTICS DASHBOARD");
                    writer.println("==============================================================================================================");

                    String formattedTable = tableFormatter.formatRecommendations(finalUserName, recommendations);
                    writer.print(formattedTable);

                    writer.println("\n [r] Refresh Data  |  [q] Exit Dashboard");
                    writer.flush();
                }

                try {
                    int ch = terminal.reader().read(200); // 200 ms timeout
                    if (ch == 'q' || ch == 'Q') {
                        running = false;
                    } else if (ch == 'r' || ch == 'R') {
                        needsRefresh = true;
                    }
                } catch (IOException e) {
                    log.error("Error reading terminal input", e);
                    running = false;
                }
            }
        } finally {
            terminal.setAttributes(originalAttributes);
            writer.print("\033[?25h");
            writer.print("\033[?1049l");
            writer.flush();
        }
    }

    private String getSafe(org.springframework.shell.component.context.ComponentContext<?> context, String key) {
        if (context == null) {
            return null;
        }
        try {
            return context.get(key);
        } catch (Exception e) {
            return null;
        }
    }
}
