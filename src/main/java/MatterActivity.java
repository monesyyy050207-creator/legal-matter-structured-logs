import java.time.Instant;
import java.time.LocalDate;

public record MatterActivity(
    String matterId,
    String clientId,
    Instant receivedAt,
    String signedDocumentId,
    LocalDate responseDeadline
) {}
