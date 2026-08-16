import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

public final class LegalMatterExample {
    private LegalMatterExample() {}

    public static void main(String[] args) throws Exception {
        LegalLogConfig config = LegalLogConfig.fromEnvironment();
        MatterLogService service = new MatterLogService(new InfraiLogsClient(config), Clock.systemUTC());
        MatterActivity intake = new MatterActivity(
            "MAT-2048", "CLIENT-731", Instant.now(), "DOC-SIGNED-908", LocalDate.now(Clock.systemUTC()).plusDays(2)
        );

        int shipped = service.ship(intake);
        System.out.println("Shipped " + shipped + " legal matter events");
        System.out.println("Search result: " + Json.write(service.findMatterHistory(intake.matterId())));
    }
}
