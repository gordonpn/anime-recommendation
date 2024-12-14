package com.gordonpn.anime.util;

import com.gordonpn.anime.models.AuthCodeParsingException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public final class CallbackHandler {
    private CallbackHandler() {
    }

    public static String extractCodeFromCallback(ServerSocket serverSocket, String state) throws IOException {
        try (Socket socket = serverSocket.accept()) {
            log.info("Received connection from {}", socket.getInetAddress());

            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String requestLine = bufferedReader.readLine();
            if (requestLine == null) {
                log.error("Received empty request");
                throw new AuthCodeParsingException("Empty request");
            }
            log.info("Received request: {}", requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                log.error("Invalid request format: {}", requestLine);
                throw new AuthCodeParsingException("Invalid request format");
            }

            URI uri = new URI(parts[1]);
            String query = uri.getQuery();
            Map<String, String> params = parseQueryString(query);

            sendResponse(socket);
            log.info("Sent success response to browser");

            if (!state.equals(params.get("state"))) {
                log.error("State mismatch. Expected: {}, Received: {}", state, params.get("state"));
                throw new AuthCodeParsingException("State mismatch");
            }

            String code = params.get("code");
            if (code == null) {
                log.error("No authorization code found in response");
                throw new AuthCodeParsingException("No code parameter found");
            }

            log.info("Successfully extracted authorization code");
            return code;
        } catch (URISyntaxException e) {
            log.error("Failed to parse redirect URI", e);
            throw new AuthCodeParsingException("Failed to parse redirect URI", e);
        }
    }

    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    params.put(pair[0], pair[1]);
                }
            }
        }
        return params;
    }

    private static void sendResponse(Socket socket) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html");
        out.println();
        out.println("<html><body><h1>Authentication successful!</h1><p>You can close this window.</p></body></html>");
        out.flush();
    }
}
