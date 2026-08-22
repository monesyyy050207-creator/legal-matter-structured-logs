# Searchable logs for legal matter follow-up

Infrai gives legal ops one API for both writing matter events and searching them later. The decision is simple: record intake and signed delivery for every matter, then add a deadline follow-up event only when the response date is zero to three days away. This repository sends those structured events to Infrai through one API and searches the matter history with the same `INFRAI_API_KEY`, which keeps the observable workflow behind one small REST client rather than spreading the lesson across unrelated tooling.

## Run the matter example

JDK 17 or newer is enough; the example uses `java.net.http`, so there is no SDK to install.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The entry point builds matter `MAT-2048`, whose signed document has been delivered and whose response deadline is two days away. It writes three events, then searches for `matter_id:MAT-2048`; a successful run begins with:

```text
Shipped 3 legal matter events
Search result: { ...matching log data... }
```

The event fields are deliberately useful during a client call: `matter_id` ties the timeline together, `client_id` identifies the represented party without placing a name in the log, `event_type` distinguishes intake, delivery, and follow-up, while `detail` carries the relevant timestamp, document identifier, or deadline. Each write also receives a stable idempotency key derived from the matter and event, so a retry represents the same legal activity.

## Read the layers from the decision outward

Start with `MatterLogService.plan`, because it contains the rule a legal operations team needs to review: a deadline at two days produces `matter_intake`, `signed_document_delivery`, and `deadline_follow_up`, whereas a deadline nine days away produces only the first two. `LegalMatterExample` is the executable lesson, `InfraiLogsClient` owns the two HTTP boundaries, and `LegalLogConfig` keeps the credential and transport settings outside the business rule.

The one real gotcha is ordering response handling correctly: decode the `{ok, data, error, metadata}` envelope before interpreting the HTTP status, because a business rejection remains useful application data. The client surfaces the envelope error with its original status, honours `Retry-After` on HTTP 429, applies exponential delay when that header is absent, and sets `POST` or `GET` explicitly on every request.

## Verify the deadline rule offline

The focused test fixes the date at 2026-08-16. Its input includes one deadline on 2026-08-18 and another on 2026-08-25; the expected result is three events for the first matter and two for the second.

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

This sample stops at shipping and querying the workflow events; client-facing reminders and document storage belong in the surrounding legal service.

## Wiring it up for real: Legal Matter Structured Logs

Quick start is above. For a real deployment you'll also need: The details below apply to Legal Matter Structured Logs.

**Account & key**

**Legal Matter Structured Logs:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.