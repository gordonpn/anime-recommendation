package com.gordonpn.anime.service.recommendation;

import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.Node;
import com.gordonpn.anime.models.dto.RecommendationDetailedResult;
import com.gordonpn.anime.models.dto.SeasonalAnimeResult;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TableFormatter {

    private static final AttributedStyle BORDER_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLACK | AttributedStyle.BRIGHT);
    private static final AttributedStyle HEADER_STYLE = AttributedStyle.DEFAULT.bold();
    private static final AttributedStyle HIGH_SCORE_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold();
    private static final AttributedStyle DEFAULT_STYLE = AttributedStyle.DEFAULT;

    public String formatRecommendations(String userName, List<RecommendationDetailedResult> recommendations) {
        AttributedStringBuilder sb = new AttributedStringBuilder();

        sb.append(String.format("\n=== Recommendations for %s ===\n", userName));

        // Format header
        sb.style(HEADER_STYLE).append(String.format("%-4s", "Rank"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-40s", "Title"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-6s", "Score"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-6s", "Occur"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-6s", "Rating"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-6s", "Pop"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(" Explanation\n");

        // Separator line
        sb.style(BORDER_STYLE);
        sb.append("─".repeat(5));
        sb.append("┼");
        sb.append("─".repeat(42));
        sb.append("┼");
        sb.append("─".repeat(8));
        sb.append("┼");
        sb.append("─".repeat(8));
        sb.append("┼");
        sb.append("─".repeat(8));
        sb.append("┼");
        sb.append("─".repeat(8));
        sb.append("┼");
        sb.append("─".repeat(26));
        sb.append("\n");

        for (int i = 0; i < recommendations.size(); i++) {
            RecommendationDetailedResult res = recommendations.get(i);
            String title = res.getTitle() != null ? res.getTitle() : "Unknown";
            if (title.length() > 40) {
                title = title.substring(0, 37) + "...";
            }

            // High recommendation score or user rating threshold (e.g. score > 8.0)
            boolean isHighScore = res.getScore() != null && res.getScore() > 8.0;
            AttributedStyle style = isHighScore ? HIGH_SCORE_STYLE : DEFAULT_STYLE;

            sb.style(style).append(String.format("%-4d", i + 1));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-40s", title));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-6.2f", res.getScore() != null ? res.getScore() : 0.0));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-6.1f", res.getOccurrence() != null ? res.getOccurrence() : 0.0));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-6.1f", res.getMeanRating() != null ? res.getMeanRating() : 0.0));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-6s", res.getPopularity() != null ? String.valueOf(res.getPopularity()) : "N/A"));
            appendSeparator(sb);
            sb.style(style).append(" ").append(res.getExplanation() != null ? res.getExplanation() : "").append("\n");
        }

        sb.style(BORDER_STYLE).append("==============================================================================================================\n");
        return sb.toAnsi();
    }

    public String formatSeasonal(String numYears, Set<SeasonalAnimeResult> sortedResult, int limit) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(String.format("\n=== Top Seasonal Anime (Past %s Years) ===\n", numYears));

        sb.style(HEADER_STYLE).append(String.format("%-4s", "Rank"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-50s", "Title"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(" Rating\n");

        sb.style(BORDER_STYLE);
        sb.append("─".repeat(5));
        sb.append("┼");
        sb.append("─".repeat(52));
        sb.append("┼");
        sb.append("─".repeat(8));
        sb.append("\n");

        int rank = 1;
        for (SeasonalAnimeResult res : sortedResult) {
            if (rank > limit) {
                break;
            }
            String title = res.getTitle() != null ? res.getTitle() : "Unknown";
            if (title.length() > 50) {
                title = title.substring(0, 47) + "...";
            }

            boolean isHighScore = res.getMean() != null && res.getMean() > 8.0;
            AttributedStyle style = isHighScore ? HIGH_SCORE_STYLE : DEFAULT_STYLE;

            sb.style(style).append(String.format("%-4d", rank));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-50s", title));
            appendSeparator(sb);
            sb.style(style).append(String.format(" %-6.2f\n", res.getMean() != null ? res.getMean() : 0.0));
            rank++;
        }

        sb.style(BORDER_STYLE).append("====================================================================\n");
        return sb.toAnsi();
    }

    public String formatSuggestions(List<Datum> data, int limit) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("\n=== Personalized Suggestions from MyAnimeList ===\n");

        sb.style(HEADER_STYLE).append(String.format("%-4s", "Rank"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(String.format(" %-10s", "ID"));
        appendSeparator(sb);
        sb.style(HEADER_STYLE).append(" Title\n");

        sb.style(BORDER_STYLE);
        sb.append("─".repeat(5));
        sb.append("┼");
        sb.append("─".repeat(12));
        sb.append("┼");
        sb.append("─".repeat(44));
        sb.append("\n");

        int count = Math.min(data.size(), limit);
        for (int i = 0; i < count; i++) {
            Datum d = data.get(i);
            Node node = d.getNode();
            if (node != null) {
                sb.style(DEFAULT_STYLE).append(String.format("%-4d", i + 1));
                appendSeparator(sb);
                sb.style(DEFAULT_STYLE).append(String.format(" %-10d", node.getId()));
                appendSeparator(sb);
                sb.style(DEFAULT_STYLE).append(" ").append(node.getDisplayTitle() != null ? node.getDisplayTitle() : "Unknown").append("\n");
            }
        }

        sb.style(BORDER_STYLE).append("==============================================================\n");
        return sb.toAnsi();
    }

    private void appendSeparator(AttributedStringBuilder sb) {
        sb.style(BORDER_STYLE).append(" │ ");
    }
}
