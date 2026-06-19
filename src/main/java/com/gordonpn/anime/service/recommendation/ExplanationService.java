package com.gordonpn.anime.service.recommendation;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExplanationService {

    public String generateExplanation(double occurrence, List<String> sourceTitles) {
        if (sourceTitles == null || sourceTitles.isEmpty()) {
            return String.format("Recommended with occurrence frequency: %.1f.", occurrence);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Recommended because it is suggested by %.1f completed title(s) on your list: ", occurrence));
        
        int showLimit = 3;
        int count = Math.min(sourceTitles.size(), showLimit);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(sourceTitles.get(i)).append("\"");
        }
        
        if (sourceTitles.size() > showLimit) {
            sb.append(String.format(" and %d more", sourceTitles.size() - showLimit));
        }
        sb.append(".");
        return sb.toString();
    }
}
