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

## RECOMMENDED REFACTORING ORDER

Priority should be **High** issues first (blocks architecture compliance), then **Medium** (improves maintainability), then **Low** (technical debt).

### **Phase 1: Exception Handling & Error Encapsulation** (2-3 days)
- Issue #1: Create domain exceptions
- Issue #12: Define exception handling strategy
- Impact: Improves testability and API contracts

### **Phase 2: DTO/Entity Separation** (1-2 days)
- Issue #4: Separate TokenResponse into DTO + Entity
- Issue #7: Create `/dto/` package with response models
- Impact: Clean architectural separation, better API versioning

### **Phase 3: REST API Completion** (2-3 days)
- Issue #5: Create AuthController, UserListController, RecommendationController
- Issue #18: Add correlation ID logging
- Issue #17: Add integration tests
- Impact: Achieves Phase B of ARCHITECTURE.md

### **Phase 4: Decompose RecommendationFacade** (3-5 days)
- Issue #3: Extract GraphBuilder, ComponentAnalyzer, FranchiseRepresentativeSelector
- Issue #8: Break long methods
- Impact: Achieves Phase C, improves testability

### **Phase 5: Centralize Cache Logic** (2-3 days)
- Issue #2: Create CacheService with generic getOrFetch()
- Impact: Reduces duplication, consistent TTL strategy

### **Phase 6: Configuration & Validation** (2-3 days)
- Issue #11: Validate RankingService weights
- Issue #15: Extract magic numbers to ApplicationConstants
- Issue #10: Add service input validation
- Issue #9: Create MalApiClient wrapper
- Impact: Improves reliability and maintainability

### **Phase 7: Code Quality & Structure** (2-3 days)
- Issue #6: Consolidate command/service separation
- Issue #13: Reorganize package structure
- Issue #14: Extract TableFormatter service
- Issue #16: Document API versioning strategy
- Impact: Improves navigability and consistency

**Total effort**: ~15-20 days for full architectural compliance + clean code standards

---

## Next Steps

1. **Prioritize High issues first** (blocks architecture compliance)
2. **Each refactoring should be a single atomic commit** following conventional commits
3. **Update ARCHITECTURE.md as patterns emerge** (e.g., new services, new packages)
4. **Add integration tests alongside controller work** (Phase 3 & beyond)
5. **Run linters/formatters after each phase** to maintain code quality
6. **Review against this document** periodically as new code is written
