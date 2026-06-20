# Refactoring Opportunities

Comprehensive code scan analysis completed: 2026-06-19 14:00 UTC

## Quick Summary

The codebase has good architectural foundation (CLI/API separation, service layer, recommendation facade) but has **18 refactoring opportunities** across clean code, SOLID compliance, and architectural alignment.

**Estimated effort**: 15-20 days for full compliance

---

## HIGH SEVERITY ISSUES (5)

### 1. JsonProcessingException Propagation (9+ files)
**Problem**: Methods throw `JsonProcessingException` directly, violating encapsulation and forcing callers to handle Jackson implementation details.

**Files**: 
- `src/main/java/com/gordonpn/anime/service/RecommendationService.java:40`
- `src/main/java/com/gordonpn/anime/service/AnimeService.java:32,80`
- `src/main/java/com/gordonpn/anime/service/UserListService.java:31`
- `src/main/java/com/gordonpn/anime/commands/RecommendationCommands.java:50,86`
- Plus 5+ other files

**Impact**: 
- Pollutes API signatures with Jackson implementation details
- Makes code hard to test (requires handling Jackson exceptions)
- Violates the dependency inversion principle (high-level modules depend on low-level Jackson details)
- Breaks encapsulation of the service layer

**Suggested refactoring**:
```java
// Create domain exceptions:
public class AnimeDataProcessingException extends RuntimeException {
    public AnimeDataProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Update service layer:
public List<RecommendationDetailedResult> getRecommendations(String userName, int limit) {
    try {
        // ... existing logic
    } catch (JsonProcessingException e) {
        throw new AnimeDataProcessingException("Failed to process recommendation data", e);
    }
}
```

---

### 2. Cache Handling Duplication (60+ lines)
**Problem**: Identical cache-check-fetch-fallback logic repeated in:
- `src/main/java/com/gordonpn/anime/service/AnimeService.java:32-78,80-133`
- `src/main/java/com/gordonpn/anime/service/UserListService.java:31-82`

Pattern repeats 3 times with identical structure:
1. Check cache, return if valid (with TTL check)
2. Fetch from API
3. Update cache
4. Fallback to expired cache on error

**Impact**: 
- 60+ lines of duplicated logic across 3 files
- Bug fixes need to be applied in 3 places
- Hard to maintain TTL strategy consistently
- Violates DRY principle

**Suggested refactoring**:
```java
// Create generic cache service:
@Service
public class CacheService {
    public <T> T getOrFetch(
        String cacheKey, 
        Supplier<Optional<? extends CachedEntity>> cacheLoader,
        Supplier<String> fetcher,
        Function<String, T> deserializer,
        int ttlDays,
        GenericCacheRepository repo,
        Class<T> clazz
    ) throws JsonProcessingException {
        // Centralized caching logic
    }
}

// Usage in AnimeService:
public AnimeDetails getAnimeById(int animeId) {
    return cacheService.getOrFetch(
        "anime:" + animeId,
        () -> animeCacheRepository.findById(animeId),
        () -> fetchFromMalApi(animeId),
        json -> objectMapper.readValue(json, AnimeDetails.class),
        7,
        animeCacheRepository,
        AnimeDetails.class
    );
}
```

---

### 3. RecommendationFacade is God Object (330 lines)
**File**: `src/main/java/com/gordonpn/anime/service/recommendation/RecommendationFacade.java`

**Problem**: The class handles too many concerns:
- Fetching user lists (lines 33-75)
- Building relationship graphs (lines 77-127)
- Component traversal/BFS (lines 104-127)
- Representative selection logic (lines 129-179)
- Recommendation extraction (lines 181-226)
- Sorting and final ranking (lines 228-268)
- Franchise rating calculation (lines 270-328)

**Impact**: 
- Violates Single Responsibility Principle
- Hard to test individual concerns (e.g., ranking vs. filtering)
- Difficult to reuse logic (e.g., graph traversal)
- 330 lines is too large for maintainability

**Suggested refactoring**: Extract existing services further:

```java
// Create GraphBuilder service:
@Service
public class AnimeGraphBuilder {
    public Map<Integer, Set<Integer>> buildRelationshipGraph(List<Integer> animeIds) { ... }
}

// Create ComponentAnalyzer service:
@Service
public class ComponentAnalyzer {
    public List<Set<Integer>> findConnectedComponents(Map<Integer, Set<Integer>> graph) { ... }
}

// Create FranchiseRepresentative service:
@Service
public class FranchiseRepresentativeSelector {
    public String selectRepresentativeTitle(Set<Integer> component, Map<Integer, AnimeDetails> details) { ... }
}

// Updated RecommendationFacade becomes orchestrator:
@Service
@RequiredArgsConstructor
public class RecommendationFacade {
    private final AnimeGraphBuilder graphBuilder;
    private final ComponentAnalyzer componentAnalyzer;
    private final FranchiseRepresentativeSelector repSelector;
    private final RankingService rankingService;
    
    public List<RecommendationDetailedResult> getRecommendations(String userName, int limit) {
        // Clean orchestration without implementation details
    }
}
```

---

### 4. TokenResponse Mixed Concerns (Entity + DTO)
**File**: `src/main/java/com/gordonpn/anime/models/TokenResponse.java`

**Problem**: `TokenResponse` is used for THREE separate purposes:
1. API response DTO from MAL OAuth endpoint (has `@JsonProperty` annotations)
2. JPA entity (has `@Entity`, `@Table`, `@Id`)
3. In-memory state holder (see `AuthorizationState`)

**Impact**: 
- Mixing API contracts with persistence models violates architectural separation
- If we add new API fields, they pollute the database
- Hard to version API independently from persistence
- Persistence logic (`@PrePersist`) embedded in DTO

**Suggested refactoring**:
```java
// 1. Create pure DTO for MAL API response:
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MalTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private Integer expiresIn;
}

// 2. Create JPA entity for persistence:
@Entity
@Table(name = "tokens")
@Data
@NoArgsConstructor
public class TokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

// 3. Update TokenRepository to use TokenEntity
// 4. Update services to convert MalTokenResponse → TokenEntity
```

---

### 5. Missing REST Controllers (Phase B Incomplete)
**Problem**: Only `HealthController` exists. Missing:
- `AuthController` (auth/status, auth/refresh)
- `UserListController` (GET /api/v1/users/{userName}/list)
- `RecommendationController` (recommendations endpoints)

**Impact**: 
- API parity not achieved, architecture incomplete
- ARCHITECTURE.md Phase B not implemented
- Users cannot access features via REST API

**Suggested refactoring**: Implement remaining 3 controllers, reusing existing services:

```java
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final MalAuthService authService;
    
    @GetMapping("/status")
    public AuthStatusResponse status() {
        // Delegate to MalAuthService
    }
    
    @PostMapping("/refresh")
    public AuthRefreshResponse refresh() {
        // Delegate to TokenExchangeService
    }
}

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserListController {
    private final UserListService userListService;
    
    @GetMapping("/{userName}/list")
    public UserListResponse getList(@PathVariable String userName, @RequestParam(defaultValue = "completed") String status) {
        // Delegate to UserListService
    }
}

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationFacade recommendationFacade;
    
    @GetMapping("/{userName}")
    public RecommendationResponse getRecommendations(@PathVariable String userName, @RequestParam(defaultValue = "50") int limit) {
        // Delegate to RecommendationFacade
    }
    
    @GetMapping("/seasonal")
    public SeasonalResponse getSeasonal(@RequestParam(defaultValue = "5") int years) {
        // Delegate to RecommendationFacade
    }
}
```

---

## MEDIUM SEVERITY ISSUES (7)

### 6. RecommendationCommands Has Business Logic
**Files**: `src/main/java/com/gordonpn/anime/commands/RecommendationCommands.java`, `src/main/java/com/gordonpn/anime/commands/ListCommands.java`

**Problem**: Command classes call services directly without clear orchestration, mixing CLI concerns with business logic.

**Impact**: Difficult to test commands independently, logic duplication with future API controllers

**Fix**: Use RecommendationFacade consistently for all coordination

---

### 7. No Response Format Consistency
**Problem**: Commands use hardcoded table formatting; no unified response model

**Files**: `src/main/java/com/gordonpn/anime/commands/RecommendationCommands.java:58-79,245-265`

**Impact**: Duplicated formatting logic, inconsistent user experience

**Fix**: Create `TableFormatter` service, standardize API response DTOs in `/dto/` package

---

### 8. Long Methods in RecommendationFacade
**Problem**: 
- Lines 33-75 (user list fetch) could be extracted
- Lines 129-179 (representative selection) could be extracted

**Impact**: Method too large for easy comprehension and testing

**Fix**: Extract to dedicated helper methods or services

---

### 9. Tight Coupling in HTTP Clients
**Problem**: RestClient calls embedded in services without clear abstraction

**Files**: `src/main/java/com/gordonpn/anime/service/AnimeService.java`, `src/main/java/com/gordonpn/anime/service/UserListService.java`

**Impact**: Hard to mock/test, difficult to switch HTTP clients, implementation details leak

**Fix**: Create `MalApiClient` wrapper service with consistent error handling

---

### 10. No Input Validation at Service Boundaries
**Problem**: Services accept parameters without validation (null checks, bounds)

**Impact**: Runtime errors instead of clear validation failures, hard to trace bugs

**Fix**: Add `@Validated` annotations, custom validators (e.g., `@NotBlank`, `@Positive`)

---

### 11. RankingService Weights Not Validated
**File**: `src/main/java/com/gordonpn/anime/service/recommendation/RankingService.java:9-16`

**Problem**: Configuration weights loaded but never checked for NaN, positive values, or reasonable ranges

**Impact**: Bad configuration could silently produce weird ranking results, no visibility into actual values

**Fix**: Add @PostConstruct validation + structured logging:

```java
@Service
@Validated
public class RankingService {
    
    @Value("${recommendation.weights.occurrence:1.0}")
    @Positive(message = "Weight occurrence must be positive")
    private double weightOccurrence;
    
    @PostConstruct
    public void validateWeights() {
        log.info("Ranking weights: occurrence={}, rating={}, popularity={}",
            weightOccurrence, weightRating, weightPopularity);
        if (Double.isNaN(weightOccurrence) || Double.isNaN(weightRating) || Double.isNaN(weightPopularity)) {
            throw new ConfigurationException("Invalid ranking weights: NaN detected");
        }
    }
}
```

---

### 12. Inconsistent Exception Handling
**Problem**: Some methods return null, others throw, some log, some don't

**Impact**: Unpredictable error behavior, difficult debugging

**Fix**: Define exception handling strategy, use GlobalExceptionHandler consistently

---

## LOW SEVERITY ISSUES (6)

### 13. Package Structure Under-organized
**File structure**: `src/main/java/com/gordonpn/anime/service/recommendation/`

**Problem**: All recommendation services in one directory:
- RecommendationFacade
- RankingService
- FilteringService
- ExplanationService

**Impact**: Could become cluttered as codebase grows

**Suggested refactoring**:
```
service/
├── recommendation/
│   ├── RecommendationFacade.java
│   ├── ranking/
│   │   ├── RankingService.java
│   │   └── RankingWeightsValidator.java
│   ├── filtering/
│   │   └── FilteringService.java
│   └── explanation/
│       └── ExplanationService.java
```

---

### 14. String Formatting Logic in Commands
**File**: `src/main/java/com/gordonpn/anime/commands/RecommendationCommands.java:58-79,245-265`

**Problem**: Table formatting logic hardcoded in command classes (lines of string concatenation, padding calculations)

**Impact**: Duplicated if multiple commands format similar tables, commands bloated with non-business logic

**Suggested refactoring**:
```java
@Service
public class TableFormatter {
    public String formatRecommendations(List<RecommendationDetailedResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s | %-40s | %-6s | ...\n", "Rank", "Title", "Score"));
        // Table formatting
        return sb.toString();
    }
    
    public String formatSeasonalAnime(List<SeasonalAnimeResult> results) {
        // Seasonal formatting
    }
}

// Usage in command:
@ShellMethod
public String recommendation(...) {
    List<RecommendationDetailedResult> results = recommendationFacade.getRecommendations(userName, limit);
    return json ? objectMapper.writeValueAsString(results) : tableFormatter.formatRecommendations(results);
}
```

---

### 15. Hardcoded Magic Numbers and Constants
**Files affected**:
- `src/main/java/com/gordonpn/anime/service/UserListService.java:23` (MAX = 1000)
- `src/main/java/com/gordonpn/anime/service/recommendation/RankingService.java:24-25` (10000.0 normalization)
- `src/main/java/com/gordonpn/anime/service/recommendation/ExplanationService.java:18` (showLimit = 3)
- `src/main/java/com/gordonpn/anime/service/recommendation/RecommendationFacade.java:232,285` (100, 10)

**Problem**: Magic numbers scattered without clear purpose or configuration

**Impact**: Hard to understand intent, difficult to adjust behavior without code changes

**Suggested refactoring**:
```java
@Component
@ConfigurationProperties(prefix = "app.constants")
@Data
public class ApplicationConstants {
    private int maxListSize = 1000;
    private int recommendationCacheSize = 100;
    private int franchiseGroupLimit = 10;
    private int explanationSourceShowLimit = 3;
    private double rankingNormalizationFactor = 10000.0;
}
```

---

### 16. No API Versioning Strategy
**Files affected**: All controllers

**Problem**: API endpoints hardcoded to `/api/v1/` but no version upgrade strategy defined

**Impact**: When breaking changes needed, no clear path forward, ARCHITECTURE.md doesn't address it

**Suggested refactoring**: Define and document API versioning strategy in ARCHITECTURE.md

---

### 17. Missing Integration Tests for API
**Problem**: No tests for API endpoints, only service + command tests

**Impact**: Cannot verify API contracts, controller logic untested, regression risk

**Suggested refactoring**: Add RestAssured/MockMvc integration tests:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RecommendationControllerIntegrationTests {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void getRecommendations_returnsValidResponse() {
        ResponseEntity<RecommendationResponse> response = 
            restTemplate.getForEntity("/api/v1/recommendations/{userName}", RecommendationResponse.class, "testUser");
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
```

---

### 18. No Request/Response Logging and Correlation IDs
**Problem**: Hard to debug API issues; no correlation IDs for tracing requests end-to-end

**Impact**: Difficult debugging in production, no visibility into request flow

**Suggested refactoring**: Add structured logging with correlation IDs to GlobalExceptionHandler:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        
        log.error("Request {} failed with correlation ID {}", 
            request.getRequestURI(), correlationId, e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(e.getMessage(), correlationId));
    }
}
```

---

## ARCHITECTURAL ALIGNMENT vs. ARCHITECTURE.MD

### ✅ Implemented Well
- ✓ CLI (Spring Shell) and API (REST) separation
- ✓ Service layer exists and properly organized
- ✓ Recommendation facade pattern
- ✓ Recommendation sub-services (Filtering, Ranking, Explanation)
- ✓ Global exception handling for API
- ✓ Health checks implemented

### ⚠️ Partially Implemented / Missing
1. **Phase B**: Only `HealthController` exists (need 3 more)
   - Missing: AuthController, UserListController, RecommendationController
2. **Phase C**: RecommendationFacade too large (needs decomposition)
   - Missing: GraphBuilder, ComponentAnalyzer services
3. **Phase D**: Credentials not Spring-externalized (.env loading is manual)
   - Should use `@ConfigurationProperties` or Spring Cloud Config
4. **API response DTOs**: No `/dto/` package for API request/response DTOs
   - Using models directly, should separate API contracts from persistence
5. **Entity/DTO separation**: TokenResponse is both entity and DTO (should be separate)

### ❌ Not Implemented
- API versioning strategy (no upgrade path defined)
- Correlation ID / request logging
- Integration tests for API endpoints
- CLI vs. API feature parity testing

---

---

## ADDITIONAL ARCHITECTURAL FINDINGS (June 2026)

### 19. Models Package Organization (30+ mixed concerns)
**File**: `src/main/java/com/gordonpn/anime/models/` (30+ files)

**Problem**: The `models` directory contains 30+ files with mixed concerns:
- API request/response objects (no DTO separation)
- Domain models (MAL data structures)
- Custom exceptions (MalResourceNotFoundException, AuthCodeParsingException)
- Utility classes (RandomState, PKCEUtil)

**Impact**:
- No clear separation between API contracts and domain models
- Difficult to navigate and understand the role of each class
- Violates single responsibility principle at package level
- Hard to version API independently from domain

**Suggested refactoring**:
```
models/
├── dto/
│   ├── request/
│   │   ├── RecommendationRequest.java
│   │   └── UserListRequest.java
│   └── response/
│       ├── RecommendationResponse.java
│       ├── UserListResponse.java
│       └── AuthStatusResponse.java
├── domain/
│   ├── AnimeDetails.java
│   ├── Recommendation.java
│   └── UserAnimeListEntry.java
├── mal/
│   ├── Node.java
│   ├── Datum.java
│   ├── AnimeList.java
│   └── TokenResponse.java (migrate to dto/ or entity/)
└── exception/
    ├── MalResourceNotFoundException.java
    └── AuthCodeParsingException.java

# Move utilities to util/ package:
util/
├── RandomState.java
├── PKCEUtil.java
├── Browser.java
└── CallbackHandler.java
```

---

### 20. Missing HTTP Client Abstraction Layer
**Files**: `AnimeService.java`, `UserListService.java`, `RecommendationService.java`, `TokenExchangeService.java`

**Problem**: Multiple services directly use injected `RestClient` instances without abstraction. Each service duplicates API call patterns:
- Error handling differs between services
- Endpoint construction logic spread across files
- No centralized MAL API contract enforcement

**Impact**:
- Hard to change HTTP client implementation (e.g., switch to WebClient)
- Inconsistent error handling across services
- API versioning strategy not enforced
- Testing requires mocking RestClient in each service test

**Suggested refactoring**:
```java
// Create MalApiClient abstraction:
@Service
public class MalApiClient {
    private final RestClient apiKeyClient;
    private final String baseUrl = "https://api.myanimelist.net/v2";
    
    public <T> T getAnimeById(int animeId, Class<T> responseType) {
        return get("/anime/{id}", responseType, animeId);
    }
    
    public <T> List<T> getUserList(String userName, String status, Class<T> responseType) {
        return getList("/users/{userName}/animelist", responseType, userName);
    }
    
    private <T> T get(String endpoint, Class<T> responseType, Object... pathVars) {
        try {
            return apiKeyClient.get()
                .uri(baseUrl + endpoint, pathVars)
                .retrieve()
                .body(responseType);
        } catch (HttpClientErrorException e) {
            throw new MalApiException("API call failed", e);
        }
    }
}

// Services become simpler:
@Service
public class AnimeService {
    private final MalApiClient malApiClient;
    
    public AnimeDetails getAnimeById(int animeId) {
        return malApiClient.getAnimeById(animeId, AnimeDetails.class);
    }
}
```

---

### 21. RecommendationFacade Size Growing (396 lines, approaching cognitive load limit)
**File**: `src/main/java/com/gordonpn/anime/service/recommendation/RecommendationFacade.java`

**Problem**: Facade is now 396 lines (measured June 2026), which exceeds maintainability threshold. Further growth will compound Issue #3.

**Impact**:
- Difficult to read and understand all responsibilities in one sitting
- Hard to add new features without breaking existing logic
- Slower unit test execution (all dependencies must be mocked)
- Higher risk of regression when modifying ranking/filtering logic

**Suggested refactoring** (extends Issue #3):
- Extract franchise grouping logic to `FranchiseGroupingService`
- Move recommendation filtering logic to dedicated `RecommendationFilterService`
- Move scoring calculation to `RecommendationScoringService`
- Keep facade as orchestrator, limit to 150 lines maximum

---

### 22. Missing DTO/Request Validation Layer
**Problem**: No centralized request validation. Commands and controllers accept parameters without bounds checking.

**Examples**:
- `recommendation --limit 10000` (should cap at 500)
- No validation for invalid user names (should fail early, not at API call)
- No validation for year ranges in `--min-year` / `--max-year` filters

**Impact**:
- Wasted API calls for invalid requests
- Poor error messages (user gets HTTP 400 instead of validation error)
- No clear contract for what requests are acceptable

**Suggested refactoring**:
```java
@Data
public class RecommendationRequest {
    @NotBlank(message = "userName is required")
    private String userName;
    
    @Positive(message = "limit must be positive")
    @Max(value = 500, message = "limit cannot exceed 500")
    private int limit = 50;
    
    @Min(value = 1917, message = "minYear must be >= 1917")
    @Max(value = 2100, message = "minYear must be <= 2100")
    private Integer minYear;
}

// In controller:
@PostMapping
public ResponseEntity<RecommendationResponse> getRecommendations(
    @Valid @RequestBody RecommendationRequest request) {
    // request is guaranteed to be valid
}
```

---

### 23. No Circuit Breaker Pattern for Resilience
**Problem**: Application currently retries MAL API calls via `MalRetryInterceptor`, but has no circuit breaker to stop calling a failing API.

**Scenario**: If MAL API is down for 10 minutes, every recommendation request will hit the retry limit (3 × exponential backoff ≈ 4 seconds per request). With no circuit breaker, requests keep failing slowly.

**Impact**:
- Poor user experience during MAL outages
- Wasted resources on guaranteed-to-fail API calls
- No automatic recovery signaling

**Suggested refactoring**:
```java
@Configuration
public class ResilienceConfig {
    @Bean
    public CircuitBreaker malApiCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0) // Open after 50% failures
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slowCallRateThreshold(50.0)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .build();
        
        return CircuitBreaker.of("malApiCircuitBreaker", config);
    }
}

// Apply to MalApiClient:
@Service
public class MalApiClient {
    private final CircuitBreaker circuitBreaker;
    
    public <T> T get(String endpoint, Class<T> responseType) {
        return Decorators.ofSupplier(() -> restClient.get(...))
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retryConfig)
            .get()
            .get();
    }
}
```

---

### 24. Test Coverage Gap (16 tests for 3548 LOC)
**Problem**: Test directory contains only 16 test files, covering ~0.5% of codebase (estimated 3548 LOC).

**Critical gaps**:
- No tests for `RecommendationFacade` (core logic)
- No tests for recommendation ranking algorithm
- No tests for API controllers
- No integration tests for MAL API client
- Manual testing likely carries all the risk

**Impact**:
- High regression risk on refactoring
- No confidence in ranking behavior changes
- Difficult to onboard new contributors (unclear expected behavior)
- Difficult to debug in production

**Suggested refactoring** (multi-phase):
1. Add unit tests for `RankingService` (scoring logic)
2. Add unit tests for `FilteringService` (filter logic)
3. Add integration tests for `RecommendationFacade`
4. Add API endpoint tests for controllers
5. Add test fixtures and builders for common test data

**Example test structure**:
```java
@SpringBootTest
class RecommendationFacadeIntegrationTest {
    @Test
    void getRecommendations_withValidUser_returnsRankedResults() {
        // Arrange
        UserAnimeListTestBuilder userList = UserAnimeListTestBuilder.create()
            .withCompleted("Steins;Gate", 9.0)
            .withCompleted("Death Note", 8.5);
        
        // Act
        List<RecommendationDetailedResult> results = 
            facade.getRecommendations("testUser", 10);
        
        // Assert
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getScore()).isGreater(results.get(1).getScore());
    }
}
```

---

### 25. Configuration Management Fragmentation
**Problem**: Configuration spread across multiple locations:
- `application.properties` (Spring config)
- `AnimeClientConfig.java` (RestClient bean config)
- `RankingService.java` (@Value annotations)
- `MalRetryInterceptor.java` (retry config hardcoded)
- `BackgroundFetchService.java` (ONE_HOUR_IN_MILLIS constant)
- `ApplicationConstants` (suggested in Issue #15)

**Impact**:
- Hard to understand all configuration options
- No centralized config schema
- Difficult to manage environment-specific configs
- Risk of config defaults being inconsistent

**Suggested refactoring**:
```yaml
# application.properties
app:
  mode: cli
  api:
    base-url: /api/v1
  constants:
    max-list-size: 1000
    recommendation-cache-size: 100

mal:
  api:
    base-url: https://api.myanimelist.net/v2
    timeout-ms: 10000
  retry:
    max-attempts: 3
    initial-backoff-ms: 500
    backoff-multiplier: 2.0
  circuit-breaker:
    failure-rate-threshold: 50
    wait-duration-seconds: 30
  cache:
    anime-ttl-days: 30
    user-list-ttl-days: 1
    seasonal-ttl-days: 1

recommendation:
  ranking:
    weights:
      occurrence: 1.0
      rating: 0.5
      popularity: 0.3
  temporal:
    completion-decay-months: 12
    release-freshness-boost: 0.5
```

Then use `@ConfigurationProperties` to load:
```java
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationConfig {
    private String mode;
    private Api api;
    private Constants constants;
}

@Configuration
@ConfigurationProperties(prefix = "mal")
@Data
public class MalConfig {
    private Api api;
    private Retry retry;
    private CircuitBreaker circuitBreaker;
    private Cache cache;
}
```

---

### 26. No Observability Layer (Metrics, Distributed Tracing)
**Problem**: Application lacks observability for production monitoring.

**Missing**:
- Metrics collection (API latency, error rates, cache hit/miss)
- Structured logging (no correlation IDs across logs)
- Distributed tracing support
- Health check metrics endpoint

**Impact**:
- Cannot diagnose performance issues in production
- Hard to detect when MAL API is slow
- Difficult to debug end-to-end request flow
- No alerting capability for degradation

**Suggested refactoring**:
```java
// Add Micrometer metrics:
@Service
public class RecommendationFacade {
    private final MeterRegistry meterRegistry;
    
    public List<RecommendationDetailedResult> getRecommendations(String userName, int limit) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<RecommendationDetailedResult> results = ...; // existing logic
            meterRegistry.counter("recommendations.generated", "user", userName).increment();
            return results;
        } catch (Exception e) {
            meterRegistry.counter("recommendations.error", "user", userName).increment();
            throw e;
        } finally {
            sample.stop(Timer.builder("recommendations.latency")
                .description("Time to generate recommendations")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        }
    }
}

// Add structured logging:
log.info("Recommendations generated", 
    Map.of("userName", userName, "count", results.size(), "latencyMs", elapsed));
```

---

## RECOMMENDED REFACTORING ORDER

Priority should be **High** issues first (blocks architecture compliance), then **Medium** (improves maintainability), then **Low** (technical debt).

### **Phase 1: Exception Handling & Error Encapsulation** (2-3 days)
- Issue #1: Create domain exceptions
- Issue #12: Define exception handling strategy
- Impact: Improves testability and API contracts

### **Phase 2: DTO/Entity Separation** (1-2 days)
- Issue #4: Separate TokenResponse into DTO + Entity
- Issue #7: Create `/dto/` package with response models
- Issue #19: Reorganize models package
- Impact: Clean architectural separation, better API versioning

### **Phase 3: HTTP Client Abstraction** (2-3 days)
- Issue #20: Create MalApiClient wrapper service
- Impact: Easier to test, consistent API patterns, easier to change implementation

### **Phase 4: REST API Completion** (2-3 days)
- Issue #5: Create AuthController, UserListController, RecommendationController
- Issue #18: Add correlation ID logging
- Issue #24: Add API integration tests
- Impact: Achieves Phase B of ARCHITECTURE.md

### **Phase 5: Decompose RecommendationFacade & Add Validation** (4-5 days)
- Issue #3: Extract GraphBuilder, ComponentAnalyzer, FranchiseRepresentativeSelector
- Issue #21: Extract scoring and filtering services
- Issue #22: Add DTO validation layer
- Issue #8: Break long methods
- Impact: Achieves Phase C, improves testability

### **Phase 6: Centralize Cache Logic & Configuration** (2-3 days)
- Issue #2: Create CacheService with generic getOrFetch()
- Issue #25: Consolidate configuration management
- Impact: Reduces duplication, consistent TTL strategy, centralized config

### **Phase 7: Resilience & Observability** (3-4 days)
- Issue #23: Add circuit breaker pattern
- Issue #26: Add metrics and structured logging
- Issue #11: Validate RankingService weights
- Impact: Production-ready reliability and debugging

### **Phase 8: Configuration & Test Coverage** (3-4 days)
- Issue #15: Extract magic numbers to ApplicationConstants
- Issue #10: Add service input validation
- Issue #24: Improve test coverage (unit + integration)
- Impact: Improves reliability and maintainability

### **Phase 9: Code Quality & Structure** (2-3 days)
- Issue #6: Consolidate command/service separation
- Issue #13: Reorganize package structure (if needed after Phase 2)
- Issue #14: Extract TableFormatter service
- Issue #16: Document API versioning strategy
- Impact: Improves navigability and consistency

**Total effort**: ~25-30 days for full architectural compliance + production readiness

---

## Next Steps

1. **Prioritize High issues first** (blocks architecture compliance)
2. **Each refactoring should be a single atomic commit** following conventional commits
3. **Update ARCHITECTURE.md as patterns emerge** (e.g., new services, new packages)
4. **Add integration tests alongside controller work** (Phase 3 & beyond)
5. **Run linters/formatters after each phase** to maintain code quality
6. **Review against this document** periodically as new code is written
