package com.gordonpn.anime.service.recommendation;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class FilteringService {

    /**
     * Filters out candidate anime IDs that are in the user's completed or dropped lists,
     * or any other excluded IDs.
     */
    public boolean shouldInclude(Integer animeId, Set<Integer> completedIds, Set<Integer> droppedIds) {
        if (animeId == null) {
            return false;
        }
        return !completedIds.contains(animeId) && !droppedIds.contains(animeId);
    }
}
