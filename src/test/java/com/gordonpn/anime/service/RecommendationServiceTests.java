package com.gordonpn.anime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.*;
import com.gordonpn.anime.models.userlist.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTests {

    @Mock
    private UserListService userListService;

    @Mock
    private AnimeService animeService;

    @Mock
    private RestClient apiKeyClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RecommendationService recommendationService;

    @BeforeEach
    public void setUp() {
        recommendationService = new RecommendationService(objectMapper, userListService, animeService, apiKeyClient);
    }

    @Test
    public void testRecommendationOccurrence() throws Exception {
        // Setup completed list
        AnimeList completedList = new AnimeList();
        List<Datum> completedData = new ArrayList<>();

        Datum completedItem = new Datum();
        Node completedNode = new Node();
        completedNode.setId(1);
        completedNode.setTitle("Completed Anime");
        completedItem.setNode(completedNode);
        completedData.add(completedItem);
        completedList.setData(completedData);

        // Setup dropped list
        AnimeList droppedList = new AnimeList();
        List<Datum> droppedData = new ArrayList<>();

        Datum droppedItem = new Datum();
        Node droppedNode = new Node();
        droppedNode.setId(2);
        droppedNode.setTitle("Dropped Anime");
        droppedItem.setNode(droppedNode);
        droppedData.add(droppedItem);
        droppedList.setData(droppedData);

        // Mock calls to UserListService
        when(userListService.getListByUserName("testUser", Status.COMPLETED)).thenReturn(completedList);
        when(userListService.getListByUserName("testUser", Status.DROPPED)).thenReturn(droppedList);

        // Setup recommendation details
        AnimeDetails animeDetails = new AnimeDetails();
        animeDetails.title = "Completed Anime";

        List<Recommendation> recommendations = new ArrayList<>();
        Recommendation rec = new Recommendation();
        rec.setNumRecommendations(5);
        Node recNode = new Node();
        recNode.setId(3); // Not in completed/dropped list
        recNode.setTitle("Recommended Anime");
        rec.setNode(recNode);
        recommendations.add(rec);
        animeDetails.setRecommendations(recommendations);

        // Mock call to AnimeService
        when(animeService.getAnimeById(1)).thenReturn(animeDetails);

        // Run
        Map<String, Integer> result = recommendationService.recommendationOccurrence("testUser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get("Recommended Anime"));
    }
}
