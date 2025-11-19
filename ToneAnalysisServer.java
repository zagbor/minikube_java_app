import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ToneAnalysisServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Обработчик анализа текста
        server.createContext("/text-tone", new TextToneHandler());

        // Проверка живости сервиса
        server.createContext("/health", new HealthHandler());

        // Метрики в формате, совместимом с Prometheus
        server.createContext("/metrics", new MetricsHandler());

        server.setExecutor(null); // стандартный executor
        server.start();
    }

    // --------- Handlers ---------

    static class TextToneHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = readRequestBody(exchange);
                String text = extractJsonField(body, "text");

                if (text == null || text.isEmpty()) {
                    text = "no_text";
                }

                String jsonResponse = String.format(
                        "{\"received_text\": \"%s\", \"mood\": \"happy\", \"score\": 0.95}",
                        escapeForJson(text)
                );

                sendJson(exchange, 200, jsonResponse);
            } catch (Exception e) {
                String errorResponse = "{\"error\": \"bad_request\", \"mood\": \"neutral\", \"score\": 0.5}";
                sendJson(exchange, 400, errorResponse);
            }
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String jsonResponse = "{\"status\": \"OK\"}";
            sendJson(exchange, 200, jsonResponse);
        }
    }

    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            StringBuilder sb = new StringBuilder();
            sb.append("# HELP memory_usage_bytes Memory usage\n");
            sb.append("# TYPE memory_usage_bytes gauge\n");
            sb.append("memory_usage_bytes ").append(usedMemory).append("\n");

            sendText(exchange, 200, sb.toString(), "text/plain");
        }
    }

    // --------- Utils ---------

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Очень простенький парсер: ищет значение поля "key" в JSON-строке вида {"text": "something"}
     * Без претензий на полноценный JSON-парсер.
     */
    private static String extractJsonField(String json, String key) {
        if (json == null) {
            return null;
        }
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return null;
        }

        int firstQuote = json.indexOf("\"", colonIndex);
        if (firstQuote == -1) {
            return null;
        }

        int secondQuote = json.indexOf("\"", firstQuote + 1);
        if (secondQuote == -1) {
            return null;
        }

        return json.substring(firstQuote + 1, secondQuote);
    }

    private static String escapeForJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        sendText(exchange, statusCode, body, "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
