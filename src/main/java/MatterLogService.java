import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MatterLogService {
    private final InfraiLogsClient logs;
    private final Clock clock;

    public MatterLogService(InfraiLogsClient logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    public List<LegalEvent> plan(MatterActivity activity) {
        List<LegalEvent> events = new ArrayList<>();
        events.add(new LegalEvent("matter_intake", "info", activity.matterId(), activity.clientId(),
            "Matter intake recorded", activity.receivedAt().toString()));
        events.add(new LegalEvent("signed_document_delivery", "info", activity.matterId(), activity.clientId(),
            "Signed document delivered", activity.signedDocumentId()));

        LocalDate today = LocalDate.now(clock);
        long daysRemaining = ChronoUnit.DAYS.between(today, activity.responseDeadline());
        if (daysRemaining >= 0 && daysRemaining <= 3) {
            events.add(new LegalEvent("deadline_follow_up", "warn", activity.matterId(), activity.clientId(),
                "Response deadline needs follow-up", activity.responseDeadline().toString()));
        }
        return List.copyOf(events);
    }

    public int ship(MatterActivity activity) throws IOException, InterruptedException {
        List<LegalEvent> events = plan(activity);
        for (LegalEvent event : events) logs.ingest(event.asLog());
        return events.size();
    }

    public Map<String, Object> findMatterHistory(String matterId) throws IOException, InterruptedException {
        return logs.search("matter_id:" + matterId, 25);
    }

    public record LegalEvent(String eventType, String level, String matterId, String clientId,
                             String message, String detail) {
        Map<String, Object> asLog() {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("matter_id", matterId);
            attributes.put("client_id", clientId);
            attributes.put("event_type", eventType);
            attributes.put("detail", detail);

            Map<String, Object> log = new LinkedHashMap<>();
            log.put("level", level);
            log.put("message", message);
            log.put("service", "legal-matter-job");
            log.put("attributes", attributes);
            log.put("idempotency_key", matterId + ":" + eventType + ":" + detail);
            return log;
        }
    }
}
