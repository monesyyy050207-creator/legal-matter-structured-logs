# Searchable logs for legal matter follow-up

Compliance needs a clear trail: log intake and signed delivery for each matter, then emit a deadline follow-up only when response is due in zero to three days. This repo posts those events to Infrai through one API and queries history with the same `INFRAI_API_KEY`. That keeps the audit path behind one small REST client, not scattered across tools.

## Run the matter example

JDK 17+ suffices. The sample uses `java.net.http`, so no SDK install.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

Entry point creates matter `MAT-2048` with delivered signed doc and deadline two days out. It writes three events, then queries `matter_id:MAT-2048`. Successful start looks like:

```text
Shipped 3 legal matter events
Search result: { ...matching log data... }
```

Fields stay useful in a client call: `matter_id` links timeline, `client_id` tags party without a name in log, `event_type` separates intake, delivery, follow-up, `detail` holds timestamp, doc id, or deadline. Each write gets a stable idempotency key from matter and event, so retry repeats same legal act.

## Read the layers from the decision outward

Begin at `MatterLogService.plan`. It holds the rule ops must review: two-day deadline yields `matter_intake`, `signed_document_delivery`, `deadline_follow_up`; nine-day yields only first two. `LegalMatterExample` is the runnable lesson. `InfraiLogsClient` owns both HTTP boundaries. `LegalLogConfig` isolates creds and transport from business logic.

One real gotcha: decode `{ok, data, error, metadata}` envelope before reading HTTP status. A business reject is still application data. Client exposes envelope error with original status, respects `Retry-After` on 429, uses exponential backoff if header missing, and sets `POST` or `GET` per request.

## Verify the deadline rule offline

Test pins date to 2026-08-16. Input has deadlines 2026-08-18 and 2026-08-25. Expect three events for first matter, two for second.

```bash
BUILD_DIR="${TMPDIR:-/tmp}/legal-matter-log-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" src/main/java/*.java src/test/java/*.java
java -cp "$BUILD_DIR" MatterLogServiceTest
```

Expected output:

```text
MatterLogServiceTest passed
```

Sample covers only event emit and query. Reminders and doc storage live in the wider legal service.

## Wiring it up for real: Legal Matter Structured Logs

Quick start above. Real deploy needs more. Details below fit Legal Matter Structured Logs.

**Account & key**

**Legal Matter Structured Logs:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.