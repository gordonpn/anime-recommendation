# Recommendation Engine Expansion & Research Plan

This document serves as a reference for expanding the anime recommendation engine's capabilities. It contains brainstorming details for probabilistic ranking models, CLI app expansions, and MyAnimeList API data integration opportunities.

---

## 1. Probabilistic Concepts for Recommendations

Applying statistics and probability can shift the recommendation logic from a basic heuristic calculator to a predictive model. Below are four key concepts that can be introduced:

### A. Bayesian Average for Candidate Ratings (Bayesian Shrinkage)
*   **The Problem:** Candidate ratings on MyAnimeList can be highly volatile for obscure shows (e.g., a show rated `10.0` by only 1 voter will outrank a classic rated `9.0` by 100,000 voters).
*   **The Probabilistic Formula:**
    $$R_{\text{Bayes}} = \frac{v \cdot R + c \cdot C}{v + c}$$
    *   $v$: The number of users who rated the show (`num_scoring_users` from MAL API)
    *   $R$: The raw average rating of the show (`mean` from MAL API)
    *   $c$: A smoothing constant representing the threshold number of votes (e.g., the average vote count across the database)
    *   $C$: The global average rating across all anime in the database
*   **Educational Value:** Demonstrates how to use a *prior probability distribution* (the global mean) to correct for small sample sizes.

### B. Naive Bayes Preference Classifier
*   **The Problem:** We currently suggest similar items, but we do not predict how likely a user is to enjoy a candidate show based on its attributes.
*   **The Probabilistic Formula:** Using Bayes' theorem to calculate the probability that the user likes a candidate show given its attributes (e.g., genres, studios):
    $$P(\text{Like} \mid \text{Attributes}) \propto P(\text{Like}) \cdot \prod_{a \in \text{Attributes}} P(\text{Attribute}_a \mid \text{Like})$$
    *   Calculate $P(\text{Genre}_g \mid \text{Like})$ using the proportion of completed shows in that genre.
    *   Calculate $P(\text{Genre}_g \mid \text{Dislike})$ using the proportion of dropped/low-rated shows in that genre.
*   **Educational Value:** Illustrates joint probabilities, independence assumptions, and classic classification algorithms.

### C. Joint Occurrence Probability (Collaborative Filtering)
*   **The Problem:** We treat all recommendation signals with equal weight prior to rating scaling.
*   **The Probabilistic Formula:** Calculate the conditional probability $P(\text{Candidate} \mid \text{Completed})$ based on global co-occurrence:
    $$P(\text{Show B} \mid \text{Show A}) = \frac{\text{Users who completed both } A \text{ and } B}{\text{Users who completed } A}$$
*   **Educational Value:** Explains joint events, conditional probabilities, and recommendation filtering matrices.

### D. Expected Utility and Drop Risk Modeling
*   **The Problem:** Recommended shows might match the user's high-level taste but belong to a genre/format that the user frequently drops.
*   **The Probabilistic Formula:** Treat the recommendation score as the *expected utility* of starting a show, discounted by the probability of dropping it:
    $$\text{Expected Score} = \text{Raw Score} \times (1 - P(\text{Drop} \mid \text{Media Type, Episode Count}))$$
*   **Educational Value:** Explains Decision Theory and expected value calculations under uncertainty.

---

## 2. CLI Application Feature Expansions

Here are several specific ways we can expand the command-line interface itself to increase usability and recommendation control:

1. **Niche vs. Mainstream Tuner (`--min-similarity`):**
   * **Concept:** MAL's recommendations contain an agreement count of how many users voted for the link. Filter out weak connections by specifying a minimum agreement threshold.
   * **CLI Command:** `recommendation --min-similarity 10` (strict, high-consensus classics) or `recommendation --min-similarity 1` (adventurous, niche/rare suggestions).
2. **Targeted Search (`--seed`):**
   * **Concept:** Generate recommendations starting from a single specific show (or franchise component) rather than scanning the entire completed watchlist.
   * **CLI Command:** `recommendation --seed "Steins;Gate"` or `recommendation --seed 9253` (ideal for finding similar shows right after finishing one).
3. **Advanced Genre & Studio Filters (`--genres` / `--exclude-genres`):**
   * **Concept:** Filter the candidate recommendations list by genre tags or animation studios.
   * **CLI Command:** `recommendation --genres Action,Sci-Fi --exclude-genres Romance`
4. **Format & Length Constraints (`--type` / `--max-episodes`):**
   * **Concept:** Limit recommendations based on the runtime format (e.g. movies only) or maximum series duration.
   * **CLI Command:** `recommendation --type movie` or `recommendation --max-episodes 13` (short-form shows).
5. **Release Era Selector (`--min-year` / `--max-year`):**
   * **Concept:** Filter suggestions by release decade to target retro/classic anime or focus only on recent releases.
   * **CLI Command:** `recommendation --max-year 2005` (classic era discovery).
6. **Interactive Detail Explainer (`recommendation-detail <id>`):**
   * **Concept:** A separate command that displays details for a recommended show (synopsis, studio, MAL score) and prints the specific user recommendation comments showing why it matches your watched history.
   * **CLI Command:** `recommendation-detail 30015`
7. **User List Analytics (`analytics` Command):**
   * **Concept:** Analyze the local SQLite database to print user statistics: favorite studios, favorite genres, score distribution histograms, and total watch time.
8. **Pagination and Interactive TUI (Text User Interface):**
   * **Concept:** Use a terminal interface library (like Lanterna or JLine widgets) to support interactive scrolling and collapsible explanation panels.

---

## 3. High-Value MyAnimeList API Data Opportunities

Looking at the MyAnimeList API v2 specs, we can leverage additional fields in our requests to enrich the SQLite cache (`anime_cache`) and improve recommendations:

| Field Name | Type | Recommended Use in App |
| :--- | :--- | :--- |
| `num_scoring_users` | Integer | Used as the vote count ($v$) to calculate the **Bayesian Average** rating. |
| `genres` | Array | Enables **Naive Bayes** taste classification and CLI genre filtering. |
| `studios` | Array | Adds studio preferences to recommendation signals and user list analytics. |
| `source` | String | Identifies source material (manga, light novel, original) to align recommendations with adaptation preferences. |
| `media_type` | String | Identifies format (tv, ova, movie, special, ona, music) to support format filtering. |
| `num_episodes` | Integer | Enables filtering recommendations by show length (e.g. short-form series vs long running shonen). |
| `start_date` / `end_date` | String | Identifies release eras, enabling decade/era-specific recommendations. |
| `rating` | String | Content rating (e.g., pg13, r, r+). Enables filtering out or highlighting mature titles. |
| `average_episode_duration`| Integer | Computes precise watch times in the `analytics` command. |
| `statistics` | Object | Detailed completion statuses globally (completed, planning, dropped count) to compute popularity trends. |
