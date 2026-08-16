import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class MatterLogServiceTest {
    private MatterLogServiceTest() {}

    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-08-16T09:00:00Z"), ZoneOffset.UTC);
        MatterLogService service = new MatterLogService(null, clock);

        MatterActivity urgent = new MatterActivity(
            "MAT-2048", "CLIENT-731", Instant.parse("2026-08-16T08:30:00Z"),
            "DOC-SIGNED-908", LocalDate.parse("2026-08-18")
        );
        List<MatterLogService.LegalEvent> urgentEvents = service.plan(urgent);
        assertEquals(3, urgentEvents.size(), "an intake due in two days includes follow-up");
        assertEquals("deadline_follow_up", urgentEvents.get(2).eventType(), "follow-up is the final event");

        MatterActivity later = new MatterActivity(
            "MAT-2049", "CLIENT-732", Instant.parse("2026-08-16T08:45:00Z"),
            "DOC-SIGNED-909", LocalDate.parse("2026-08-25")
        );
        assertEquals(2, service.plan(later).size(), "a later deadline does not create early follow-up");
        System.out.println("MatterLogServiceTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
