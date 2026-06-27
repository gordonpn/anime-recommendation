package com.gordonpn.anime.service.recommendation;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExplanationServiceTests {

    private final ExplanationService explanationService = new ExplanationService();

    @Test
    public void testGenerateExplanationEmptyOrNull() {
        assertEquals("Occur: 5.0", explanationService.generateExplanation(5.0, null));
        assertEquals("Occur: 3.5", explanationService.generateExplanation(3.5, List.of()));
    }

    @Test
    public void testGenerateExplanationUnderLimit() {
        assertEquals("Based on: \"Attack on Titan\"", 
                explanationService.generateExplanation(2.0, List.of("Attack on Titan")));
        
        assertEquals("Based on: \"Attack on Titan\", \"My Hero Academia\"", 
                explanationService.generateExplanation(2.0, List.of("Attack on Titan", "My Hero Academia")));
    }

    @Test
    public void testGenerateExplanationAtLimit() {
        assertEquals("Based on: \"Attack on Titan\", \"My Hero Academia\", \"Death Note\"", 
                explanationService.generateExplanation(3.0, List.of("Attack on Titan", "My Hero Academia", "Death Note")));
    }

    @Test
    public void testGenerateExplanationAboveLimit() {
        assertEquals("Based on: \"Attack on Titan\", \"My Hero Academia\", \"Death Note\" (+2)", 
                explanationService.generateExplanation(5.0, List.of("Attack on Titan", "My Hero Academia", "Death Note", "Fullmetal Alchemist", "Steins;Gate")));
    }
}
