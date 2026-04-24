package dev.lindau.cfp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class CfpApiApplication {
    private static final ProposalStore PROPOSAL_STORE = new ProposalStore();

    private CfpApiApplication() {
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/proposals", new ProposalHttpHandler(PROPOSAL_STORE));
        server.start();

        System.out.println("Conference CfP API listening on http://localhost:8080");
        System.out.println("Proposal store initialized with " + PROPOSAL_STORE.count() + " entries.");
    }

    static final class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = "cfp-api-ready".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }
}
