package com.gordonpn.anime.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpStatusCode;

import java.io.IOException;

@Slf4j
public class MalRetryInterceptor implements ClientHttpRequestInterceptor {

    private final int maxAttempts;
    private final long initialBackoffMs;
    private final double backoffMultiplier;

    public MalRetryInterceptor(int maxAttempts, long initialBackoffMs, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long backoffMs = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                HttpStatusCode status = response.getStatusCode();

                if (status.isError()) {
                    // Fail fast on non-transient errors (404, 401, 403, etc.)
                    if (status.value() == 404 || status.value() == 401 || status.value() == 403) {
                        return response;
                    }

                    // For transient errors (e.g., 429, 5xx), retry if attempts remain
                    if (attempt < maxAttempts && (status.value() == 429 || status.is5xxServerError())) {
                        log.warn("Transient HTTP error {} on attempt {} for URI: {}. Retrying in {}ms...",
                                status.value(), attempt, request.getURI(), backoffMs);
                        response.close(); // Clean up response resources
                        sleep(backoffMs);
                        backoffMs = (long) (backoffMs * backoffMultiplier);
                        continue;
                    }
                }
                return response;
            } catch (IOException e) {
                if (attempt < maxAttempts) {
                    log.warn("IOException on attempt {} for URI: {} - {}. Retrying in {}ms...",
                            attempt, request.getURI(), e.getMessage(), backoffMs);
                    sleep(backoffMs);
                    backoffMs = (long) (backoffMs * backoffMultiplier);
                } else {
                    throw e;
                }
            }
        }
        throw new IOException("Retry limit exceeded for request to " + request.getURI());
    }

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry wait interrupted", ie);
        }
    }
}
