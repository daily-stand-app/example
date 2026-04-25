package dev.lindau.cfp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProposalHttpHandler implements HttpHandler {
    private final ProposalStore proposalStore;

    public ProposalHttpHandler(ProposalStore proposalStore) {
        this.proposalStore = proposalStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod()) && isCollectionRequest(exchange)) {
                handleList(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCreate(exchange);
                return;
            }

            if ("PATCH".equalsIgnoreCase(exchange.getRequestMethod()) && isStatusRequest(exchange)) {
                handleStatusUpdate(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleDetail(exchange);
                return;
            }

            send(exchange, 405, "Method not allowed", "text/plain; charset=utf-8");
        } catch (IllegalArgumentException exception) {
            send(exchange, 400, exception.getMessage(), "text/plain; charset=utf-8");
        }
    }

    private void handleDetail(HttpExchange exchange) throws IOException {
        long proposalId = parseProposalId(exchange);
        Proposal proposal = proposalStore.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown proposal id: " + proposalId));
        send(exchange, 200, JsonSupport.write(proposal), "application/json; charset=utf-8");
    }

    private void handleStatusUpdate(HttpExchange exchange) throws IOException {
        long proposalId = parseProposalId(exchange);
        StatusUpdateRequest statusUpdateRequest = JsonSupport.read(readRequestBody(exchange), StatusUpdateRequest.class);
        ProposalStatus status = ProposalStatus.valueOf(statusUpdateRequest.status().trim().toUpperCase());
        Proposal proposal = proposalStore.updateStatus(proposalId, status);
        send(exchange, 200, JsonSupport.write(proposal), "application/json; charset=utf-8");
    }

    private void handleList(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange);
        List<Proposal> proposals = proposalStore.search(query.get("speakerEmail"), query.get("tag"));
        send(exchange, 200, JsonSupport.write(proposals), "application/json; charset=utf-8");
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        ProposalSubmission submission = JsonSupport.read(requestBody, ProposalSubmission.class);
        validate(submission);
        Proposal proposal = proposalStore.save(
                submission.title(),
                submission.speakerName(),
                submission.speakerEmail(),
                submission.abstractText(),
                submission.tags() == null ? List.of() : submission.tags()
        );

        send(exchange, 201, JsonSupport.write(proposal), "application/json; charset=utf-8");
    }

    private static void validate(ProposalSubmission submission) {
        requireText(submission.title(), "title");
        requireText(submission.speakerName(), "speakerName");
        requireText(submission.speakerEmail(), "speakerEmail");
        requireText(submission.abstractText(), "abstractText");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean isCollectionRequest(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return "/proposals".equals(path) || "/proposals/".equals(path);
    }

    private static Map<String, String> parseQuery(HttpExchange exchange) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        Map<String, String> values = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return values;
        }

        for (String pair : rawQuery.split("&")) {
            String[] entry = pair.split("=", 2);
            String key = URLDecoder.decode(entry[0], StandardCharsets.UTF_8);
            String value = entry.length > 1 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static long parseProposalId(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] segments = path.split("/");
        if (segments.length < 3 || segments[2].isBlank()) {
            throw new IllegalArgumentException("Missing proposal id in request path");
        }
        return Long.parseLong(segments[2]);
    }

    private static boolean isStatusRequest(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        return path.endsWith("/status");
    }

    private static void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    public record ProposalSubmission(
            String title,
            String speakerName,
            String speakerEmail,
            String abstractText,
            List<String> tags
    ) {
    }

    public record StatusUpdateRequest(String status) {
    }
}
