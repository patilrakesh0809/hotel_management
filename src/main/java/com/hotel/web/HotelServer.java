package com.hotel.web;

import com.hotel.model.BillBreakdown;
import com.hotel.model.Reservation;
import com.hotel.service.HotelService;
import com.hotel.util.JsonUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Embedded Java HTTP server exposing REST endpoints and serving the static web UI.
 */
public class HotelServer {

    private final HotelService hotelService;
    private final HttpServer server;
    private final Path diskPublicDir;

    public HotelServer(HotelService hotelService, int port) throws IOException {
        this.hotelService = hotelService;
        this.diskPublicDir = Path.of("src", "main", "resources", "public").toAbsolutePath().normalize();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(8));
        registerRoutes();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    private void registerRoutes() {
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
    }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());

            if ("OPTIONS".equals(method)) {
                sendHeadersOnly(exchange, 204);
                return;
            }

            if ("GET".equals(method) && "/api/health".equals(path)) {
                sendJson(exchange, 200, Map.of("status", "UP", "service", "Grand Horizon Hotel API"));
                return;
            }
            if ("GET".equals(method) && "/api/summary".equals(path)) {
                sendJson(exchange, 200, hotelService.getSummary());
                return;
            }
            if ("GET".equals(method) && "/api/rooms".equals(path)) {
                List<Map<String, Object>> rooms = hotelService.getRooms(query.get("status"), query.get("type"));
                sendJson(exchange, 200, Map.of("rooms", rooms, "count", rooms.size(), "maxRooms", HotelService.MAX_ROOMS));
                return;
            }
            if ("GET".equals(method) && "/api/rooms/empty".equals(path)) {
                Integer minGuests = parseNullableInt(query.get("minGuests"));
                Double maxRate = parseNullableDouble(query.get("maxRate"));
                List<Map<String, Object>> empty = hotelService.getEmptyRooms(query.get("type"), minGuests, maxRate);
                sendJson(exchange, 200, Map.of("emptyRooms", empty, "availableCount", empty.size(), "totalRooms", HotelService.MAX_ROOMS));
                return;
            }
            if ("GET".equals(method) && "/api/reservations".equals(path)) {
                List<Map<String, Object>> list = hotelService.getReservations(query.get("status"), query.get("q"));
                sendJson(exchange, 200, Map.of("reservations", list, "count", list.size()));
                return;
            }
            if ("POST".equals(method) && "/api/reservations".equals(path)) {
                Map<String, Object> body = readJsonBody(exchange);
                Reservation created = hotelService.createReservation(body);
                sendJson(exchange, 201, Map.of("reservation", created.toMap(), "message", "Reservation created and saved to file"));
                return;
            }
            if ("POST".equals(method) && "/api/bills/calculate".equals(path)) {
                Map<String, Object> body = readJsonBody(exchange);
                BillBreakdown bill = hotelService.previewBill(body);
                sendJson(exchange, 200, Map.of("bill", bill.toMap()));
                return;
            }
            if ("POST".equals(method) && "/api/records/save".equals(path)) {
                Map<String, Object> saveInfo = hotelService.saveRecordsNow();
                sendJson(exchange, 200, Map.of("saved", true, "details", saveInfo));
                return;
            }
            if ("GET".equals(method) && "/api/records/export/json".equals(path)) {
                String json = hotelService.exportJson();
                sendDownload(exchange, 200, "application/json; charset=utf-8", "hotel-records.json", json);
                return;
            }
            if ("GET".equals(method) && "/api/records/export/csv".equals(path)) {
                String csv = hotelService.exportCsv();
                sendDownload(exchange, 200, "text/csv; charset=utf-8", "reservations-log.csv", csv);
                return;
            }

            if (path.startsWith("/api/reservations/")) {
                handleReservationSubroute(exchange, method, path);
                return;
            }

            sendJson(exchange, 404, Map.of("error", "API endpoint not found: " + method + " " + path));
        } catch (HotelService.NoSuchReservationException ex) {
            sendJson(exchange, 404, Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            sendJson(exchange, 500, Map.of("error", "Internal server error: " + ex.getMessage()));
        }
    }

    private void handleReservationSubroute(HttpExchange exchange, String method, String path) throws IOException {
        String remainder = path.substring("/api/reservations/".length());
        String[] parts = remainder.split("/");
        if (parts.length == 0 || parts[0].isBlank()) {
            sendJson(exchange, 404, Map.of("error", "Missing reservation ID"));
            return;
        }
        String id = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);

        if (parts.length == 1 && "PUT".equals(method)) {
            Map<String, Object> body = readJsonBody(exchange);
            Reservation updated = hotelService.updateReservation(id, body);
            sendJson(exchange, 200, Map.of("reservation", updated.toMap(), "message", "Reservation updated and saved"));
            return;
        }
        if (parts.length == 2 && "POST".equals(method)) {
            switch (parts[1]) {
                case "checkin" -> {
                    Reservation res = hotelService.checkIn(id);
                    sendJson(exchange, 200, Map.of("reservation", res.toMap(), "message", "Guest checked in"));
                    return;
                }
                case "checkout" -> {
                    Reservation res = hotelService.checkOut(id);
                    sendJson(exchange, 200, Map.of("reservation", res.toMap(), "message", "Guest checked out and bill settled"));
                    return;
                }
                case "cancel" -> {
                    Reservation res = hotelService.cancelReservation(id);
                    sendJson(exchange, 200, Map.of("reservation", res.toMap(), "message", "Reservation cancelled"));
                    return;
                }
                default -> {
                }
            }
        }
        if (parts.length == 2 && "GET".equals(method) && "bill".equals(parts[1])) {
            BillBreakdown bill = hotelService.getReservationBill(id);
            sendJson(exchange, 200, Map.of("bill", bill != null ? bill.toMap() : null));
            return;
        }

        sendJson(exchange, 404, Map.of("error", "Reservation route not found: " + method + " " + path));
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            sendText(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed");
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        if (rawPath == null || "/".equals(rawPath)) {
            rawPath = "/index.html";
        }

        if (rawPath.contains("..")) {
            sendText(exchange, 403, "text/plain; charset=utf-8", "Forbidden");
            return;
        }

        String normalized = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        byte[] bytes = loadResourceBytes(normalized);
        if (bytes == null) {
            sendText(exchange, 404, "text/plain; charset=utf-8", "Not Found: " + rawPath);
            return;
        }

        String contentType = detectContentType(normalized);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-cache");

        if ("HEAD".equals(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private byte[] loadResourceBytes(String relativePath) throws IOException {
        try (InputStream is = HotelServer.class.getClassLoader().getResourceAsStream("public/" + relativePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        }
        Path diskCandidate = diskPublicDir.resolve(relativePath).normalize();
        if (diskCandidate.startsWith(diskPublicDir) && Files.isRegularFile(diskCandidate)) {
            return Files.readAllBytes(diskCandidate);
        }
        return null;
    }

    private String detectContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return Map.of();
        }
        return JsonUtil.parseObject(body);
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] bytes = JsonUtil.stringify(payload).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendDownload(HttpExchange exchange, int status, String contentType, String filename, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String contentType, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendHeadersOnly(HttpExchange exchange, int status) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(k, v);
            } else if (!pair.isBlank()) {
                params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    private Integer parseNullableInt(String s) {
        if (s == null || s.isBlank()) return null;
        return Integer.parseInt(s.trim());
    }

    private Double parseNullableDouble(String s) {
        if (s == null || s.isBlank()) return null;
        return Double.parseDouble(s.trim());
    }
}
