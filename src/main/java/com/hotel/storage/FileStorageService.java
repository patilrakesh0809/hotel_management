package com.hotel.storage;

import com.hotel.model.Reservation;
import com.hotel.model.Room;
import com.hotel.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists hotel rooms and reservation records to JSON and CSV files on disk.
 */
public class FileStorageService {

    public static final String JSON_FILE_NAME = "hotel-records.json";
    public static final String CSV_FILE_NAME = "reservations-log.csv";

    private final Path dataDir;
    private final Path jsonFile;
    private final Path csvFile;

    public record LoadedData(List<Room> rooms, List<Reservation> reservations, int nextReservationSeq, String lastSavedAt) {
    }

    public FileStorageService(Path dataDir) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.jsonFile = this.dataDir.resolve(JSON_FILE_NAME);
        this.csvFile = this.dataDir.resolve(CSV_FILE_NAME);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getJsonFile() {
        return jsonFile;
    }

    public Path getCsvFile() {
        return csvFile;
    }

    public synchronized LoadedData load() throws IOException {
        if (!Files.exists(jsonFile)) {
            return null;
        }
        String raw = Files.readString(jsonFile, StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            return null;
        }
        Map<String, Object> root = JsonUtil.parseObject(raw);

        List<Room> rooms = new ArrayList<>();
        Object rawRooms = root.get("rooms");
        if (rawRooms instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> roomMap = (Map<String, Object>) m;
                    rooms.add(Room.fromMap(roomMap));
                }
            }
        }

        List<Reservation> reservations = new ArrayList<>();
        Object rawRes = root.get("reservations");
        if (rawRes instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resMap = (Map<String, Object>) m;
                    reservations.add(Reservation.fromMap(resMap));
                }
            }
        }

        int nextSeq = JsonUtil.asInt(root.get("nextReservationSeq"), 1001);
        String lastSavedAt = JsonUtil.asString(root.get("lastSavedAt"));
        return new LoadedData(rooms, reservations, nextSeq, lastSavedAt);
    }

    public synchronized Map<String, Object> saveAll(
            List<Room> rooms,
            List<Reservation> reservations,
            int nextReservationSeq
    ) throws IOException {
        Files.createDirectories(dataDir);
        String savedAt = Instant.now().toString();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("hotelName", "Grand Horizon Hotel");
        root.put("maxRooms", 10);
        root.put("lastSavedAt", savedAt);
        root.put("nextReservationSeq", nextReservationSeq);

        List<Map<String, Object>> roomMaps = new ArrayList<>();
        for (Room room : rooms) {
            roomMaps.add(room.toMap());
        }
        root.put("rooms", roomMaps);

        List<Map<String, Object>> resMaps = new ArrayList<>();
        for (Reservation res : reservations) {
            resMaps.add(res.toMap());
        }
        root.put("reservations", resMaps);

        String jsonContent = JsonUtil.stringify(root) + System.lineSeparator();
        writeAtomic(jsonFile, jsonContent);

        String csvContent = buildCsv(reservations);
        writeAtomic(csvFile, csvContent);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("savedAt", savedAt);
        result.put("jsonFile", jsonFile.toString());
        result.put("csvFile", csvFile.toString());
        result.put("jsonBytes", Files.size(jsonFile));
        result.put("csvBytes", Files.size(csvFile));
        result.put("roomCount", rooms.size());
        result.put("reservationCount", reservations.size());
        return result;
    }

    public synchronized String readJsonContent() throws IOException {
        if (!Files.exists(jsonFile)) {
            return "{}";
        }
        return Files.readString(jsonFile, StandardCharsets.UTF_8);
    }

    public synchronized String readCsvContent() throws IOException {
        if (!Files.exists(csvFile)) {
            return buildCsv(List.of());
        }
        return Files.readString(csvFile, StandardCharsets.UTF_8);
    }

    private void writeAtomic(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String buildCsv(List<Reservation> reservations) {
        StringBuilder sb = new StringBuilder();
        sb.append("ReservationID,RoomNumber,RoomType,GuestName,GuestEmail,GuestPhone,CheckInDate,CheckOutDate,Nights,Guests,Extras,Subtotal,Discount,Tax,TotalAmount,Paid,Status,UpdatedAt\n");
        for (Reservation r : reservations) {
            double subtotal = r.getBill() != null ? r.getBill().getSubtotal() : 0.0;
            double discount = r.getBill() != null ? r.getBill().getDiscountAmount() : 0.0;
            double tax = r.getBill() != null ? r.getBill().getTaxAmount() : 0.0;
            double total = r.getBill() != null ? r.getBill().getTotalAmount() : 0.0;
            String roomType = r.getBill() != null ? r.getBill().getRoomType() : "";
            String extrasJoined = r.getExtras() != null ? String.join(";", r.getExtras()) : "";

            sb.append(csvCell(r.getId())).append(',')
              .append(r.getRoomNumber()).append(',')
              .append(csvCell(roomType)).append(',')
              .append(csvCell(r.getGuestName())).append(',')
              .append(csvCell(r.getGuestEmail())).append(',')
              .append(csvCell(r.getGuestPhone())).append(',')
              .append(csvCell(r.getCheckInDate())).append(',')
              .append(csvCell(r.getCheckOutDate())).append(',')
              .append(r.getNights()).append(',')
              .append(r.getGuests()).append(',')
              .append(csvCell(extrasJoined)).append(',')
              .append(String.format("%.2f", subtotal)).append(',')
              .append(String.format("%.2f", discount)).append(',')
              .append(String.format("%.2f", tax)).append(',')
              .append(String.format("%.2f", total)).append(',')
              .append(r.isPaid()).append(',')
              .append(csvCell(r.getStatus().name())).append(',')
              .append(csvCell(r.getUpdatedAt())).append('\n');
        }
        return sb.toString();
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
