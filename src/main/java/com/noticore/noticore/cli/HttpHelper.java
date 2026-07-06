package com.noticore.noticore.cli;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// A tiny shared helper so SendCommand/CancelCommand/ReplayCommand don't each
// repeat the same "build an HTTP request, send it, print the result" code.
// Notice this class knows NOTHING about Kafka, Redis, or Supabase -- as far
// as the CLI is concerned, NotiCore is just a REST API at this base URL,
// exactly the same view Postman or a browser would have.
final class HttpHelper {

    static final String BASE_URL = "http://localhost:8080/api/notifications";

    private static final HttpClient client = HttpClient.newHttpClient();

    private HttpHelper() {}

    static int postJson(String url, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return send(request);
    }

    static int patch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        return send(request);
    }

    static int postNoBody(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return send(request);
    }

    private static int send(HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        // Treat any 2xx status as success (exit code 0), anything else
        // (like the 409 Conflict our @ExceptionHandler returns) as failure.
        boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
        return ok ? 0 : 1;
    }
}
