import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InfraiLogsClient {
    private final LegalLogConfig config;
    private final HttpClient http;

    public InfraiLogsClient(LegalLogConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.requestTimeout()).build());
    }

    InfraiLogsClient(LegalLogConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public Map<String, Object> ingest(Map<String, Object> event) throws IOException, InterruptedException {
        // Canonical capability marker: infrai.logs.ingest
        return call("POST", "/v1/logs/ingest", Json.write(Map.of("entries", List.of(event))));
    }

    public Map<String, Object> search(String query, int limit) throws IOException, InterruptedException {
        // Canonical capability marker: infrai.logs.search
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return call("GET", "/v1/logs/search" + "?q=" + encoded + "&limit=" + limit, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, String body)
        throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUri().resolve(path))
                .timeout(config.requestTimeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Accept", "application/json");
            if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));

            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Object decoded = Json.read(response.body());
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) {
                throw new IOException("Infrai returned a non-object response envelope");
            }
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> value
                    ? (Map<String, Object>) value : Map.of("message", "Request rejected");
                throw new InfraiException(response.statusCode(), error);
            }
            if (response.statusCode() == 429 && attempt < config.maxAttempts()) {
                Thread.sleep(retryDelay(response, attempt).toMillis());
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
            Object data = envelope.get("data");
            return data instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of("value", data);
        }
        throw new IOException("Request attempts exhausted");
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String header = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Duration.ofSeconds(Math.max(1, Long.parseLong(header)));
        } catch (NumberFormatException ignored) {
            return Duration.ofMillis(250L * (1L << (attempt - 1)));
        }
    }

    public static final class InfraiException extends IOException {
        private final int statusCode;
        private final Map<String, Object> error;

        InfraiException(int statusCode, Map<String, Object> error) {
            super(String.valueOf(error.getOrDefault("message", error.getOrDefault("hint", "Request rejected"))));
            this.statusCode = statusCode;
            this.error = new LinkedHashMap<>(error);
        }

        public int statusCode() { return statusCode; }
        public Map<String, Object> error() { return Map.copyOf(error); }
    }
}
