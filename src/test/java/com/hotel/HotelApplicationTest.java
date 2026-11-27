package com.hotel;

import com.hotel.model.BillBreakdown;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.service.BillingService;
import com.hotel.service.HotelService;
import com.hotel.storage.FileStorageService;
import com.hotel.util.JsonUtil;
import com.hotel.web.HotelServer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-contained automated unit and HTTP API integration test suite for the Hotel Management app.
 */
public class HotelApplicationTest {

    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        testJsonRoundTrip();
        testTenRoomsAndEmptyRoomChecks();
        testBillingCalculation();
        testReservationLifecycleAndFilePersistence();
        testLiveHttpServerAndRestApi();
        System.out.println("\nALL TESTS PASSED (" + passed + " test suites verified).");
    }

    private static void testJsonRoundTrip() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("name", "Grand \"Horizon\"\nHotel");
        input.put("rooms", 10);
        input.put("rate", 175.5);
        input.put("open", true);
        input.put("tags", List.of("Wi-Fi", "Spa"));

        String json = JsonUtil.stringify(input);
        Map<String, Object> parsed = JsonUtil.parseObject(json);

        assertEquals("Grand \"Horizon\"\nHotel", parsed.get("name"), "JSON string round-trip");
        assertEquals(10, JsonUtil.asInt(parsed.get("rooms"), 0), "JSON int round-trip");
        assertClose(175.5, JsonUtil.asDouble(parsed.get("rate"), 0), "JSON double round-trip");
        assertTrue(JsonUtil.asBoolean(parsed.get("open"), false), "JSON boolean round-trip");
        assertEquals(2, JsonUtil.asStringList(parsed.get("tags")).size(), "JSON array round-trip");
        pass("testJsonRoundTrip");
    }

    private static void testTenRoomsAndEmptyRoomChecks() throws IOException {
        Path tmp = Files.createTempDirectory("hotel-test-rooms-");
        try {
            HotelService service = new HotelService(new BillingService(), new FileStorageService(tmp));
            List<Map<String, Object>> allRooms = service.getRooms("ALL", "ALL");
            assertEquals(10, allRooms.size(), "Hotel must initialize exactly 10 rooms");

            List<Map<String, Object>> empty = service.getEmptyRooms(null, null, null);
            assertEquals(10, empty.size(), "All 10 rooms initially empty");

            List<Map<String, Object>> suites = service.getEmptyRooms("Executive Suite", 4, 250.0);
            assertEquals(1, suites.size(), "Filter empty Executive Suite <= $250");
            assertEquals(108, JsonUtil.asInt(suites.get(0).get("roomNumber"), 0), "Room 108 matches filter");
            pass("testTenRoomsAndEmptyRoomChecks");
        } finally {
            deleteDir(tmp);
        }
    }

    private static void testBillingCalculation() throws IOException {
        Path tmp = Files.createTempDirectory("hotel-test-bill-");
        try {
            HotelService service = new HotelService(new BillingService(), new FileStorageService(tmp));
            // Room 105 = Deluxe ($160/night), 2 nights = $320
            // BREAKFAST ($25 * 2 = $50) + SPA_ACCESS ($60 flat) + customFee $10 => subtotal $440
            // Promo WEEKEND10 = 10% => discount $44, taxable $396, 12% tax = $47.52, total = $443.52
            BillBreakdown bill = service.previewBill(Map.of(
                    "roomNumber", 105,
                    "nights", 2,
                    "extras", List.of("BREAKFAST", "SPA_ACCESS"),
                    "customFee", 10.0,
                    "promoCode", "WEEKEND10"
            ));
            assertClose(320.0, bill.getRoomCharge(), "Room charge");
            assertClose(110.0, bill.getExtrasCharge(), "Extras charge");
            assertClose(440.0, bill.getSubtotal(), "Subtotal");
            assertClose(44.0, bill.getDiscountAmount(), "Promo discount");
            assertClose(47.52, bill.getTaxAmount(), "12% tax");
            assertClose(443.52, bill.getTotalAmount(), "Grand total");
            pass("testBillingCalculation");
        } finally {
            deleteDir(tmp);
        }
    }

    private static void testReservationLifecycleAndFilePersistence() throws IOException {
        Path tmp = Files.createTempDirectory("hotel-test-persist-");
        try {
            FileStorageService storage = new FileStorageService(tmp);
            HotelService service = new HotelService(new BillingService(), storage);

            Reservation res1 = service.createReservation(Map.of(
                    "roomNumber", 101,
                    "guestName", "Priya Patel",
                    "guestEmail", "priya@example.com",
                    "checkInDate", "2026-12-01",
                    "checkOutDate", "2026-12-04",
                    "guests", 2,
                    "extras", List.of("BREAKFAST")
            ));
            assertEquals(ReservationStatus.RESERVED, res1.getStatus(), "Initial reservation status");
            assertEquals(9, service.getEmptyRooms(null, null, null).size(), "9 empty rooms after booking 101");

            // Double booking must fail
            boolean conflictThrown = false;
            try {
                service.createReservation(Map.of(
                        "roomNumber", 101,
                        "guestName", "Other Guest",
                        "checkInDate", "2026-12-02",
                        "checkOutDate", "2026-12-03",
                        "guests", 1
                ));
            } catch (IllegalStateException expected) {
                conflictThrown = true;
            }
            assertTrue(conflictThrown, "Double booking room 101 must be rejected");

            // Check in & verify persistence across restart
            service.checkIn(res1.getId());
            assertTrue(Files.exists(storage.getJsonFile()), "JSON file created");
            assertTrue(Files.exists(storage.getCsvFile()), "CSV file created");

            HotelService reloaded = new HotelService(new BillingService(), new FileStorageService(tmp));
            assertEquals(9, reloaded.getEmptyRooms(null, null, null).size(), "Reloaded service keeps occupancy");
            List<Map<String, Object>> reloadedRes = reloaded.getReservations("CHECKED_IN", "Priya");
            assertEquals(1, reloadedRes.size(), "Reloaded service preserves checked-in reservation");

            // Check out frees room back to 10 empty
            Reservation checkedOut = reloaded.checkOut(res1.getId());
            assertEquals(ReservationStatus.CHECKED_OUT, checkedOut.getStatus(), "Checkout status");
            assertTrue(checkedOut.isPaid(), "Paid after checkout");
            assertEquals(10, reloaded.getEmptyRooms(null, null, null).size(), "Room becomes empty after checkout");

            // Reserve & cancel frees room
            Reservation res2 = reloaded.createReservation(Map.of(
                    "roomNumber", 110,
                    "guestName", "Alex Rivera",
                    "checkInDate", "2026-12-10",
                    "checkOutDate", "2026-12-12",
                    "guests", 2
            ));
            assertEquals(9, reloaded.getEmptyRooms(null, null, null).size(), "Room 110 reserved");
            reloaded.cancelReservation(res2.getId());
            assertEquals(10, reloaded.getEmptyRooms(null, null, null).size(), "Room 110 freed after cancel");
            pass("testReservationLifecycleAndFilePersistence");
        } finally {
            deleteDir(tmp);
        }
    }

    private static void testLiveHttpServerAndRestApi() throws Exception {
        Path tmp = Files.createTempDirectory("hotel-test-http-");
        HotelService service = new HotelService(new BillingService(), new FileStorageService(tmp));
        HotelServer server = new HotelServer(service, 0);
        server.start();
        try {
            int port = server.getPort();
            String base = "http://127.0.0.1:" + port;
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> htmlResp = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertEquals(200, htmlResp.statusCode(), "GET / returns 200");
            assertTrue(htmlResp.body().contains("Grand Horizon Hotel"), "Index HTML contains title");

            HttpResponse<String> emptyResp = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/rooms/empty")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertEquals(200, emptyResp.statusCode(), "GET /api/rooms/empty returns 200");
            Map<String, Object> emptyMap = JsonUtil.parseObject(emptyResp.body());
            assertEquals(10, JsonUtil.asInt(emptyMap.get("availableCount"), 0), "10 empty rooms via HTTP");

            String createBody = """
                    {"roomNumber":108,"guestName":"Jordan Lee","checkInDate":"2026-12-05","checkOutDate":"2026-12-07","guests":2,"extras":["AIRPORT_PICKUP"]}
                    """;
            HttpResponse<String> createResp = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/reservations"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(createBody))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertEquals(201, createResp.statusCode(), "POST /api/reservations returns 201");

            HttpResponse<String> saveResp = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/records/save"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertEquals(200, saveResp.statusCode(), "POST /api/records/save returns 200");

            HttpResponse<String> csvResp = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/api/records/export/csv")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            assertEquals(200, csvResp.statusCode(), "GET /api/records/export/csv returns 200");
            assertTrue(csvResp.body().contains("Jordan Lee"), "CSV export contains booked guest");
            pass("testLiveHttpServerAndRestApi");
        } finally {
            server.stop(0);
            deleteDir(tmp);
        }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError("Assertion failed: " + msg);
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(msg + " — expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static void assertClose(double expected, double actual, String msg) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(msg + " — expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }
}
