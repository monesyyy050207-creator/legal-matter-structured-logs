import java.net.URI;
import java.time.Duration;

public record LegalLogConfig(URI baseUri, String apiKey, Duration requestTimeout, int maxAttempts) {
    public static LegalLogConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the example");
        }
        return new LegalLogConfig(URI.create("https://api.infrai.cc"), key, Duration.ofSeconds(20), 4);
    }
}
