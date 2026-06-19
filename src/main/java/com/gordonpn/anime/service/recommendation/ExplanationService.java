package com.gordonpn.anime.service.recommendation;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExplanationService {

    public String generateExplanation(double occurrence, List<String> sourceTitles) {
        if (sourceTitles == null || sourceTitles.isEmpty()) {
            return String.format("Occur: %.1f", occurrence);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Based on: ");

        int showLimit = 3;
        int count = Math.min(sourceTitles.size(), showLimit);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(sourceTitles.get(i)).append("\"");
        }

        if (sourceTitles.size() > showLimit) {
            sb.append(String.format(" (+%d)", sourceTitles.size() - showLimit));
        }
        return sb.toString();
    }
}
