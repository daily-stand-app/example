package dev.lindau.cfp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ProposalHttpHandler implements HttpHandler {
    private final ProposalStore proposalStore;

    public ProposalHttpHandler(ProposalStore proposalStore) {
        this.proposalStore = proposalStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleList(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCreate(exchange);
            return;
        }

        send(exchange, 405, "Method not allowed", "text/plain; charset=utf-8");
    }

    private void handleList(HttpExchange exchange) throws IOException {
        send(exchange, 200, JsonSupport.write(proposalStore.findAll()), "application/json; charset=utf-8");
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        ProposalSubmission submission = JsonSupport.read(requestBody, ProposalSubmission.class);
        Proposal proposal = proposalStore.save(
                submission.title(),
                submission.speakerName(),
                submission.speakerEmail(),
                submission.abstractText(),
                submission.tags() == null ? List.of() : submission.tags()
        );

        send(exchange, 201, JsonSupport.write(proposal), "application/json; charset=utf-8");
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
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
}
