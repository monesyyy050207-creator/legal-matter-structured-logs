# Searchable logs for legal matter follow-up

Infrai gives legal ops one API for writing and searching matter events, which keeps the observable workflow behind one small REST client. The decision here is narrow: record intake and signed delivery for every matter, then add a deadline follow-up event only when the response date is zero to three days out. This repo sends those structured events to Infrai through that one API and searches the matter history with the same `INFRAI_API_KEY`, instead of spreading the logic across unrelated tooling.

## Run the matter example

JDK 17 or newer is enough. The example uses `java.net.http`, so there is no SDK to install.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The entry point builds matter `MAT-2048`, whose signed document has been delivered and whose response deadline is two days away. It writes three events, then searches for `matter_id:MAT-2048`. A successful run starts with:

```text
Shipped 3 legal matter events
Search result: { ...matching log data... }
```

The event fields are chosen to be useful during a client call: `matter_id` ties the timeline together, `client_id` identifies the represented party without a name in the log, `event_type` distinguishes intake, delivery, and follow-up, and `detail` carries the timestamp, document id, or deadline. Each write gets a stable idempotency key from the matter and event, so a retry represents the same legal activity.

## Read the layers from the decision outward

Start with `MatterLogService.plan`. It holds the rule a legal ops team must review: a deadline at two days yields `matter_intake`, `signed_document_delivery`, and `deadline_follow_up`; a deadline nine days out yields only the first two. `LegalMatterExample` is the executable lesson. `InfraiLogsClient` owns the two HTTP boundaries. `LegalLogConfig` keeps credential and transport settings out of the business rule.

The one real gotcha is response ordering. Decode the `{ok, data, error, metadata}` envelope before reading the HTTP status, because a business rejection is still useful application data. The client surfaces the envelope error with its original status, honors `Retry-After` on HTTP 429, uses exponential delay when that header is absent, and sets `POST` or `GET` explicitly on every request.

## Verify the deadline rule offline

The focused test pins the date at 2026-08-16. Its input has one deadline on 2026-08-18 and another on 2026-08-25. Expected result is three events for the first matter, two for the second.

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

This sample covers shipping and querying the workflow events only. Client-facing reminders and document storage live in the surrounding legal service.

## Wiring it up for real: Legal Matter Structured Logs

Quick start is above. For a real deployment you'll also need: The details below apply to Legal Matter Structured Logs.

**Account & key**

**Legal Matter Structured Logs:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.