package com.gordonpn.anime.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.*;
import com.gordonpn.anime.models.dto.RecommendationDetailedResult;
import com.gordonpn.anime.models.dto.SeasonalAnimeResult;
import com.gordonpn.anime.service.AnimeService;
import com.gordonpn.anime.service.RecommendationService;
import com.gordonpn.anime.service.recommendation.RecommendationFacade;
import com.gordonpn.anime.service.recommendation.explanation.TableFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.shell.component.flow.ComponentFlow;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class RecommendationCommandsTests {

    @Mock
    private RecommendationService recommendationService;
    @Mock
    private AnimeService animeService;
    @Mock
    private RecommendationFacade recommendationFacade;
    @Mock
    private ComponentFlow.Builder componentFlowBuilder;
    @Mock
    private Terminal terminal;

    private final TableFormatter tableFormatter = new TableFormatter();
    private RecommendationCommands recommendationCommands;

    @BeforeEach
    public void setUp() {
        // Stub terminal.writer() to avoid NullPointerException in TerminalSpinner
        lenient().when(terminal.writer()).thenReturn(new PrintWriter(new StringWriter()));

        recommendationCommands = new RecommendationCommands(
                new ObjectMapper(), // Use real objectMapper for formatting output
                recommendationService,
                animeService,
                recommendationFacade,
                null,
                componentFlowBuilder,
                terminal,
                tableFormatter
        );
    }

    @Test
    public void testSeasonalGrouping() throws Exception {
        // Prepare mock seasonal anime list payload
        SeasonalAnime seasonalAnime = new SeasonalAnime();
        List<Datum> seasonalData = new ArrayList<>();

        Datum datum1 = new Datum();
        Node node1 = new Node();
        node1.setId(1);
        node1.setTitle("Attack on Titan Season 4 Part 1");
        node1.setMean(9.0);
        datum1.setNode(node1);
        seasonalData.add(datum1);

        Datum datum2 = new Datum();
        Node node2 = new Node();
        node2.setId(2);
        node2.setTitle("Attack on Titan Season 4 Part 2");
        node2.setMean(8.8);
        datum2.setNode(node2);
        seasonalData.add(datum2);

        seasonalAnime.setData(seasonalData);

        // Mock getSeasonalAnime calls
        when(animeService.getSeasonalAnime(anyString(), anyInt())).thenReturn(seasonalAnime);

        // Mock details lookup for node 1
        AnimeDetails details1 = new AnimeDetails();
        details1.setId(1);
        details1.setTitle("Attack on Titan Season 4 Part 1");
        details1.setMean(9.0);
        details1.setPopularity(100);
        
        RelatedAnime relTo2 = new RelatedAnime();
        Node relNode2 = new Node();
        relNode2.setId(2);
        relNode2.setTitle("Attack on Titan Season 4 Part 2");
        relTo2.setNode(relNode2);
        details1.setRelatedAnime(List.of(relTo2));

        // Mock details lookup for node 2
        AnimeDetails details2 = new AnimeDetails();
        details2.setId(2);
        details2.setTitle("Attack on Titan Season 4 Part 2");
        details2.setMean(8.8);
        details2.setPopularity(50); // More popular than part 1 (rank 50 < 100)

        RelatedAnime relTo1 = new RelatedAnime();
        Node relNode1 = new Node();
        relNode1.setId(1);
        relNode1.setTitle("Attack on Titan Season 4 Part 1");
        relTo1.setNode(relNode1);
        details2.setRelatedAnime(List.of(relTo1));

        when(animeService.getAnimeById(1)).thenReturn(details1);
        when(animeService.getAnimeById(2)).thenReturn(details2);

        // Run (limit 5) - should group them under the representative title ("Attack on Titan Season 4 Part 2" due to popularity 50 vs 100)
        // Combined rating: (9.0 + 8.8) / 2 = 8.90
        String output = recommendationCommands.seasonal("1", false, 5);

        assertNotNull(output);
        assertTrue(output.contains("Attack on Titan Season 4 Part 2"));
        assertTrue(output.contains("8.90"));
    }

    @Test
    public void testRecommendationInteractivePrompts() throws Exception {
        var cloneMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        var resetMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        
        var userNameSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var jsonSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var limitSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var minYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var maxYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var decayMonthsSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var decayMonthsCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var freshnessBoostSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var freshnessBoostCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);

        when(componentFlowBuilder.clone()).thenReturn(cloneMock);
        when(cloneMock.reset()).thenReturn(resetMock);

        when(resetMock.withStringInput("userName")).thenReturn(userNameSpecMock);
        when(userNameSpecMock.name(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.defaultValue(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("json")).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.name(anyString())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.selectItems(anyList())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("limit")).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.name(anyString())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.selectItems(anyList())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("minYear")).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.name(anyString())).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("maxYear")).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.name(anyString())).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("decayMonthsSelect")).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.name(anyString())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.selectItems(anyList())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.next(any())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("decayMonthsCustom")).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.name(anyString())).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("freshnessBoostSelect")).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.name(anyString())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.selectItems(anyList())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.next(any())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("freshnessBoostCustom")).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.name(anyString())).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.and()).thenReturn(resetMock);

        var flowMock = org.mockito.Mockito.mock(ComponentFlow.class);
        when(resetMock.build()).thenReturn(flowMock);

        var resultMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult.class);
        when(flowMock.run()).thenReturn(resultMock);

        var contextMock = org.mockito.Mockito.mock(org.springframework.shell.component.context.ComponentContext.class);
        when(resultMock.getContext()).thenReturn(contextMock);

        when(contextMock.get("userName")).thenReturn("@me");
        when(contextMock.get("json")).thenReturn("true");
        when(contextMock.get("limit")).thenReturn("5");
        when(contextMock.get("minYear")).thenReturn("");
        when(contextMock.get("maxYear")).thenReturn("");
        when(contextMock.get("decayMonthsSelect")).thenReturn("none");
        when(contextMock.get("freshnessBoostSelect")).thenReturn("0.5");

        List<RecommendationDetailedResult> recs = List.of(
                RecommendationDetailedResult.builder().id(12).title("Test").score(10.0).build()
        );
        when(recommendationFacade.getRecommendations("@me", 5, null, null, null, 0.5)).thenReturn(recs);

        String result = recommendationCommands.recommendation(null, null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":12"));
        assertTrue(result.contains("\"title\":\"Test\""));
    }

    @Test
    public void testRecommendationInteractivePromptsCustom() throws Exception {
        var cloneMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        var resetMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        
        var userNameSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var jsonSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var limitSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var minYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var maxYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var decayMonthsSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var decayMonthsCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var freshnessBoostSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var freshnessBoostCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);

        when(componentFlowBuilder.clone()).thenReturn(cloneMock);
        when(cloneMock.reset()).thenReturn(resetMock);

        when(resetMock.withStringInput("userName")).thenReturn(userNameSpecMock);
        when(userNameSpecMock.name(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.defaultValue(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("json")).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.name(anyString())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.selectItems(anyList())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("limit")).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.name(anyString())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.selectItems(anyList())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("minYear")).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.name(anyString())).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("maxYear")).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.name(anyString())).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("decayMonthsSelect")).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.name(anyString())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.selectItems(anyList())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.next(any())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("decayMonthsCustom")).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.name(anyString())).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("freshnessBoostSelect")).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.name(anyString())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.selectItems(anyList())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.next(any())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("freshnessBoostCustom")).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.name(anyString())).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.and()).thenReturn(resetMock);

        var flowMock = org.mockito.Mockito.mock(ComponentFlow.class);
        when(resetMock.build()).thenReturn(flowMock);

        var resultMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult.class);
        when(flowMock.run()).thenReturn(resultMock);

        var contextMock = org.mockito.Mockito.mock(org.springframework.shell.component.context.ComponentContext.class);
        when(resultMock.getContext()).thenReturn(contextMock);

        when(contextMock.get("userName")).thenReturn("@me");
        when(contextMock.get("json")).thenReturn("true");
        when(contextMock.get("limit")).thenReturn("5");
        when(contextMock.get("minYear")).thenReturn("");
        when(contextMock.get("maxYear")).thenReturn("");
        when(contextMock.get("decayMonthsSelect")).thenReturn("none");
        when(contextMock.get("freshnessBoostSelect")).thenReturn("custom");
        when(contextMock.get("freshnessBoostCustom")).thenReturn("0.3");

        List<RecommendationDetailedResult> recs = List.of(
                RecommendationDetailedResult.builder().id(12).title("Test").score(10.0).build()
        );
        when(recommendationFacade.getRecommendations("@me", 5, null, null, null, 0.3)).thenReturn(recs);

        String result = recommendationCommands.recommendation(null, null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":12"));
        assertTrue(result.contains("\"title\":\"Test\""));
    }

    @Test
    public void testRecommendationInteractivePromptsDecayCustom() throws Exception {
        var cloneMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        var resetMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        
        var userNameSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var jsonSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var limitSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var minYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var maxYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var decayMonthsSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var decayMonthsCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var freshnessBoostSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var freshnessBoostCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);

        when(componentFlowBuilder.clone()).thenReturn(cloneMock);
        when(cloneMock.reset()).thenReturn(resetMock);

        when(resetMock.withStringInput("userName")).thenReturn(userNameSpecMock);
        when(userNameSpecMock.name(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.defaultValue(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("json")).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.name(anyString())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.selectItems(anyList())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("limit")).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.name(anyString())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.selectItems(anyList())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("minYear")).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.name(anyString())).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("maxYear")).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.name(anyString())).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("decayMonthsSelect")).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.name(anyString())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.selectItems(anyList())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.next(any())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("decayMonthsCustom")).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.name(anyString())).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("freshnessBoostSelect")).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.name(anyString())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.selectItems(anyList())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.next(any())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("freshnessBoostCustom")).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.name(anyString())).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.and()).thenReturn(resetMock);

        var flowMock = org.mockito.Mockito.mock(ComponentFlow.class);
        when(resetMock.build()).thenReturn(flowMock);

        var resultMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult.class);
        when(flowMock.run()).thenReturn(resultMock);

        var contextMock = org.mockito.Mockito.mock(org.springframework.shell.component.context.ComponentContext.class);
        when(resultMock.getContext()).thenReturn(contextMock);

        when(contextMock.get("userName")).thenReturn("@me");
        when(contextMock.get("json")).thenReturn("true");
        when(contextMock.get("limit")).thenReturn("5");
        when(contextMock.get("minYear")).thenReturn("");
        when(contextMock.get("maxYear")).thenReturn("");
        when(contextMock.get("decayMonthsSelect")).thenReturn("custom");
        when(contextMock.get("decayMonthsCustom")).thenReturn("18");
        when(contextMock.get("freshnessBoostSelect")).thenReturn("0.5");

        List<RecommendationDetailedResult> recs = List.of(
                RecommendationDetailedResult.builder().id(12).title("Test").score(10.0).build()
        );
        when(recommendationFacade.getRecommendations("@me", 5, null, null, 18, 0.5)).thenReturn(recs);

        String result = recommendationCommands.recommendation(null, null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":12"));
        assertTrue(result.contains("\"title\":\"Test\""));
    }

    @Test
    public void testRecommendationInteractivePromptsWithFilters() throws Exception {
        var cloneMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        var resetMock = org.mockito.Mockito.mock(ComponentFlow.Builder.class);
        
        var userNameSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var jsonSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var limitSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var minYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var maxYearSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var decayMonthsSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var decayMonthsCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);
        var freshnessBoostSelectSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.SingleItemSelectorSpec.class);
        var freshnessBoostCustomSpecMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.StringInputSpec.class);

        when(componentFlowBuilder.clone()).thenReturn(cloneMock);
        when(cloneMock.reset()).thenReturn(resetMock);

        when(resetMock.withStringInput("userName")).thenReturn(userNameSpecMock);
        when(userNameSpecMock.name(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.defaultValue(anyString())).thenReturn(userNameSpecMock);
        when(userNameSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("json")).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.name(anyString())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.selectItems(anyList())).thenReturn(jsonSpecMock);
        lenient().when(jsonSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("limit")).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.name(anyString())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.selectItems(anyList())).thenReturn(limitSpecMock);
        lenient().when(limitSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("minYear")).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.name(anyString())).thenReturn(minYearSpecMock);
        lenient().when(minYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withStringInput("maxYear")).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.name(anyString())).thenReturn(maxYearSpecMock);
        lenient().when(maxYearSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("decayMonthsSelect")).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.name(anyString())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.selectItems(anyList())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.next(any())).thenReturn(decayMonthsSelectSpecMock);
        lenient().when(decayMonthsSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("decayMonthsCustom")).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.name(anyString())).thenReturn(decayMonthsCustomSpecMock);
        lenient().when(decayMonthsCustomSpecMock.and()).thenReturn(resetMock);

        when(resetMock.withSingleItemSelector("freshnessBoostSelect")).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.name(anyString())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.selectItems(anyList())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.next(any())).thenReturn(freshnessBoostSelectSpecMock);
        lenient().when(freshnessBoostSelectSpecMock.and()).thenReturn(resetMock);

        lenient().when(resetMock.withStringInput("freshnessBoostCustom")).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.name(anyString())).thenReturn(freshnessBoostCustomSpecMock);
        lenient().when(freshnessBoostCustomSpecMock.and()).thenReturn(resetMock);

        var flowMock = org.mockito.Mockito.mock(ComponentFlow.class);
        when(resetMock.build()).thenReturn(flowMock);

        var resultMock = org.mockito.Mockito.mock(org.springframework.shell.component.flow.ComponentFlow.ComponentFlowResult.class);
        when(flowMock.run()).thenReturn(resultMock);

        var contextMock = org.mockito.Mockito.mock(org.springframework.shell.component.context.ComponentContext.class);
        when(resultMock.getContext()).thenReturn(contextMock);

        when(contextMock.get("userName")).thenReturn("@me");
        when(contextMock.get("json")).thenReturn("true");
        when(contextMock.get("limit")).thenReturn("5");
        when(contextMock.get("minYear")).thenReturn("2020");
        when(contextMock.get("maxYear")).thenReturn("2025");
        when(contextMock.get("decayMonthsSelect")).thenReturn("12");
        when(contextMock.get("freshnessBoostSelect")).thenReturn("0.2");

        List<RecommendationDetailedResult> recs = List.of(
                RecommendationDetailedResult.builder().id(12).title("Test").score(10.0).build()
        );
        when(recommendationFacade.getRecommendations("@me", 5, 2020, 2025, 12, 0.2)).thenReturn(recs);

        String result = recommendationCommands.recommendation(null, null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":12"));
        assertTrue(result.contains("\"title\":\"Test\""));
    }
}
