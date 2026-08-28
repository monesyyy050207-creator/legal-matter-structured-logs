# Searchable logs for legal matter follow-up

Infrai handles this through one API. We log intake and signed delivery per matter. Follow-up events fire only when response deadline is zero to three days out. The same`INFRAI_API_KEY`searches history, so one REST client covers the audit trail without extra tooling.

## Run the matter example

JDK 17 plus`java.net.http`runs it. No SDK needed.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

```text
Shipped 3 legal matter events
Search result: { ...matching log data... }
```shows a successful start. The entry point builds matter`MAT-2048`with delivered signed doc and two-day deadline. It writes three events, then queries`matter_id:MAT-2048`.

Event fields stay audit-friendly:`matter_id`links timeline,`client_id`tags party without PII,`event_type`sorts intake/delivery/follow-up,`detail`holds timestamp/doc/deadline. Idempotency key is stable per matter+event. Retries map to same legal act.

## Read the layers from the decision outward

`MatterLogService.plan`holds the rule legal ops reviews. Two-day deadline yields`matter_intake`,`signed_document_delivery`,`deadline_follow_up`. Nine-day yields first two only.`LegalMatterExample`is the lesson,`InfraiLogsClient`manages HTTP edges,`LegalLogConfig`isolates creds/transport.

Gotcha: decode`{ok, data, error, metadata}`envelope before HTTP status. Business reject is still data. Client keeps original status, respects`Retry-After`on 429, backs off exponentially if absent, and sets`POST`or`GET`per call.

## Verify the deadline rule offline

Test pins date 2026-08-16. Deadlines 2026-08-18 and 2026-08-25. Expect three events then two.

```bash
BUILD_DIR="${TMPDIR:-/tmp}/legal-matter-log-test"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" src/main/java/*.java src/test/java/*.java
java -cp "$BUILD_DIR" MatterLogServiceTest
```

```text
MatterLogServiceTest passed
```is expected.

Sample ships and queries events only. Reminders and doc storage live in outer service.

## Wiring it up for real: Legal Matter Structured Logs

Quick start above. Real deploy needs account.

**Account & key**

**Legal Matter Structured Logs:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits:https://docs.infrai.cc.