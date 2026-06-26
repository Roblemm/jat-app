package com.jat.app.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpJobPageFetcher implements JobPageFetcher {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String fetch(String sourceUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "JatJobImporter/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("Job page could not be fetched: HTTP " + response.statusCode());
            }

            return response.body();
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Job page could not be fetched", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Job page fetch was interrupted", exception);
        }
    }
}
