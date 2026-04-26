# Conference CfP API

Tiny Java REST API for managing conference call-for-paper submissions.

## Run

```bash
mvn package
java -jar target/cfp-api-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## API Examples

Create a proposal:

```bash
curl -X POST http://localhost:8080/proposals \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Modern Java HTTP Clients",
    "speakerName": "Alice Becker",
    "speakerEmail": "alice@example.com",
    "abstractText": "A practical tour through modern Java REST integrations.",
    "tags": ["java", "http", "api"]
  }'
```

List proposals:

```bash
curl http://localhost:8080/proposals
```

Filter proposals by reviewer focus:

```bash
curl "http://localhost:8080/proposals?tag=java&speakerEmail=alice@example.com"
```

Mark a proposal as in review:

```bash
curl -X PATCH http://localhost:8080/proposals/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_REVIEW"}'
```
