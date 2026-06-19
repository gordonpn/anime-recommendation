package com.gordonpn.anime.service.recommendation;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilteringServiceTests {

    private final FilteringService filteringService = new FilteringService();

    @Test
    public void testShouldInclude() {
        Set<Integer> completed = Set.of(1, 2, 3);
        Set<Integer> dropped = Set.of(4, 5);

        // Not in completed or dropped -> include
        assertTrue(filteringService.shouldInclude(6, completed, dropped));

        // In completed -> exclude
        assertFalse(filteringService.shouldInclude(1, completed, dropped));

        // In dropped -> exclude
        assertFalse(filteringService.shouldInclude(4, completed, dropped));

        // Null -> exclude
        assertFalse(filteringService.shouldInclude(null, completed, dropped));
    }
}
