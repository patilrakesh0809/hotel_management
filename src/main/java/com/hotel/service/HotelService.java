package com.hotel.service;

import com.hotel.model.BillBreakdown;
import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.storage.FileStorageService;
import com.hotel.util.JsonUtil;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Core thread-safe service managing up to 10 hotel rooms, reservations,
 * empty room availability checks, billing calculations, and file persistence.
 */
public class HotelService {

    public static final int MAX_ROOMS = 10;

    private final BillingService billingService;
    private final FileStorageService storageService;
    private final Map<Integer, Room> rooms = new LinkedHashMap<>();
    private final Map<String, Reservation> reservations = new LinkedHashMap<>();
    private int nextReservationSeq = 1001;
    private String lastSavedAt;

    public HotelService(BillingService billingService, FileStorageService storageService) throws IOException {
        this.billingService = billingService;
        this.storageService = storageService;
        initialize();
    }

    private synchronized void initialize() throws IOException {
        FileStorageService.LoadedData loaded = storageService.load();
        if (loaded != null && !loaded.rooms().isEmpty()) {
            if (loaded.rooms().size() > MAX_ROOMS) {
                throw new IllegalStateException("Saved room count exceeds maximum of " + MAX_ROOMS);
            }
            for (Room room : loaded.rooms()) {
                rooms.put(room.getRoomNumber(), room);
            }
            for (Reservation res : loaded.reservations()) {
                reservations.put(res.getId(), res);
            }
            this.nextReservationSeq = Math.max(1001, loaded.nextReservationSeq());
            this.lastSavedAt = loaded.lastSavedAt();
        } else {
            seedDefaultRooms();
            persistInternal();
        }
    }

    private void seedDefaultRooms() {
        addDefaultRoom(new Room(101, "Standard", 100.0, 2, List.of("Queen Bed", "High-Speed Wi-Fi", "City View")));
        addDefaultRoom(new Room(102, "Standard", 100.0, 2, List.of("Queen Bed", "High-Speed Wi-Fi", "Workspace")));
        addDefaultRoom(new Room(103, "Standard", 110.0, 2, List.of("Twin Beds", "High-Speed Wi-Fi", "Courtyard View")));
        addDefaultRoom(new Room(104, "Standard", 110.0, 2, List.of("Queen Bed", "Smart TV", "Coffee Bar")));
        addDefaultRoom(new Room(105, "Deluxe", 160.0, 3, List.of("King Bed", "Private Balcony", "Breakfast Nook")));
        addDefaultRoom(new Room(106, "Deluxe", 160.0, 3, List.of("King Bed", "Soaking Tub", "Garden View")));
        addDefaultRoom(new Room(107, "Deluxe", 175.0, 3, List.of("King Bed", "Corner Balcony", "Espresso Machine")));
        addDefaultRoom(new Room(108, "Executive Suite", 240.0, 4, List.of("King Bed + Sofa", "Executive Lounge", "Dining Area")));
        addDefaultRoom(new Room(109, "Executive Suite", 260.0, 4, List.of("King Bed + Study", "Panoramic Windows", "Lounge Access")));
        addDefaultRoom(new Room(110, "Presidential Penthouse", 400.0, 4, List.of("Skyline Terrace", "Private Butler", "Spa Suite")));
    }

    private void addDefaultRoom(Room room) {
        if (rooms.size() >= MAX_ROOMS) {
            throw new IllegalStateException("Cannot exceed maximum room capacity of " + MAX_ROOMS);
        }
        rooms.put(room.getRoomNumber(), room);
    }

    public synchronized List<Map<String, Object>> getRooms(String statusFilter, String typeFilter) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                if (!room.getStatus().name().equalsIgnoreCase(statusFilter.trim())) {
                    continue;
                }
            }
            if (typeFilter != null && !typeFilter.isBlank() && !"ALL".equalsIgnoreCase(typeFilter)) {
                if (!room.getType().equalsIgnoreCase(typeFilter.trim())) {
                    continue;
                }
            }
            Map<String, Object> roomData = room.toMap();
            if (room.getActiveReservationId() != null) {
                Reservation active = reservations.get(room.getActiveReservationId());
                if (active != null) {
                    roomData.put("activeReservation", active.toMap());
                }
            }
            result.add(roomData);
        }
        return result;
    }

    public synchronized List<Map<String, Object>> getEmptyRooms(String typeFilter, Integer minGuests, Double maxRate) {
        List<Map<String, Object>> empty = new ArrayList<>();
        for (Room room : rooms.values()) {
            if (!room.isAvailable()) {
                continue;
            }
            if (typeFilter != null && !typeFilter.isBlank() && !"ALL".equalsIgnoreCase(typeFilter)) {
                if (!room.getType().equalsIgnoreCase(typeFilter.trim())) {
                    continue;
                }
            }
            if (minGuests != null && room.getMaxGuests() < minGuests) {
                continue;
            }
            if (maxRate != null && room.getNightlyRate() > maxRate) {
                continue;
            }
            empty.add(room.toMap());
        }
        return empty;
    }

    public synchronized List<Map<String, Object>> getReservations(String statusFilter, String query) {
        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        List<Reservation> list = new ArrayList<>(reservations.values());
        list.sort(Comparator.comparing(Reservation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> out = new ArrayList<>();
        for (Reservation r : list) {
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                if (!r.getStatus().name().equalsIgnoreCase(statusFilter.trim())) {
                    continue;
                }
            }
            if (!normalizedQuery.isEmpty()) {
                boolean matches = (r.getId() != null && r.getId().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                        || (r.getGuestName() != null && r.getGuestName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                        || (r.getGuestEmail() != null && r.getGuestEmail().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                        || String.valueOf(r.getRoomNumber()).contains(normalizedQuery);
                if (!matches) {
                    continue;
                }
            }
            out.add(r.toMap());
        }
        return out;
    }

    public synchronized Map<String, Object> getSummary() {
        int available = 0;
        int reserved = 0;
        int occupied = 0;
        for (Room room : rooms.values()) {
            switch (room.getStatus()) {
                case AVAILABLE -> available++;
                case RESERVED -> reserved++;
                case OCCUPIED -> occupied++;
            }
        }

        double collectedRevenue = 0.0;
        double pendingRevenue = 0.0;
        int activeReservations = 0;
        for (Reservation r : reservations.values()) {
            if (r.getStatus() == ReservationStatus.RESERVED || r.getStatus() == ReservationStatus.CHECKED_IN) {
                activeReservations++;
                if (r.getBill() != null) {
                    pendingRevenue = BillingService.round2(pendingRevenue + r.getBill().getTotalAmount());
                }
            } else if (r.getStatus() == ReservationStatus.CHECKED_OUT && r.isPaid() && r.getBill() != null) {
                collectedRevenue = BillingService.round2(collectedRevenue + r.getBill().getTotalAmount());
            }
        }

        double occupancyRate = rooms.isEmpty() ? 0.0 : BillingService.round2(((reserved + occupied) * 100.0) / rooms.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hotelName", "Grand Horizon Hotel");
        summary.put("maxRooms", MAX_ROOMS);
        summary.put("totalRooms", rooms.size());
        summary.put("availableRooms", available);
        summary.put("reservedRooms", reserved);
        summary.put("occupiedRooms", occupied);
        summary.put("occupancyPercent", occupancyRate);
        summary.put("activeReservations", activeReservations);
        summary.put("totalReservations", reservations.size());
        summary.put("collectedRevenue", collectedRevenue);
        summary.put("pendingRevenue", pendingRevenue);
        summary.put("lastSavedAt", lastSavedAt);
        summary.put("jsonFilePath", storageService.getJsonFile().toString());
        summary.put("csvFilePath", storageService.getCsvFile().toString());
        summary.put("extrasCatalog", billingService.getExtrasCatalog());
        summary.put("promoCodes", billingService.getPromoCodes());
        return summary;
    }

    public synchronized Reservation createReservation(Map<String, Object> payload) throws IOException {
        int roomNumber = JsonUtil.asInt(payload.get("roomNumber"), -1);
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " does not exist (valid rooms: 101-110)");
        }
        if (!room.isAvailable()) {
            throw new IllegalStateException("Room " + roomNumber + " is currently " + room.getStatus() + " and not available");
        }

        String guestName = JsonUtil.asString(payload.get("guestName"));
        if (guestName == null || guestName.isBlank()) {
            throw new IllegalArgumentException("Guest name is required");
        }

        int guests = JsonUtil.asInt(payload.get("guests"), 1);
        if (guests < 1 || guests > room.getMaxGuests()) {
            throw new IllegalArgumentException("Guests must be between 1 and " + room.getMaxGuests() + " for Room " + roomNumber);
        }

        DateRange dates = resolveDates(payload);
        List<String> extras = JsonUtil.asStringList(payload.get("extras"));
        double customFee = JsonUtil.asDouble(payload.get("customFee"), 0.0);
        String customFeeLabel = JsonUtil.asString(payload.get("customFeeLabel"));
        double discountPercent = JsonUtil.asDouble(payload.get("discountPercent"), 0.0);
        String promoCode = JsonUtil.asString(payload.get("promoCode"));
        boolean immediateCheckIn = JsonUtil.asBoolean(payload.get("immediateCheckIn"), false);

        BillBreakdown bill = billingService.calculateBill(
                room, dates.nights(), extras, customFee, customFeeLabel, discountPercent, promoCode
        );

        String id = "RES-" + nextReservationSeq++;
        String now = Instant.now().toString();

        Reservation res = new Reservation();
        res.setId(id);
        res.setRoomNumber(roomNumber);
        res.setGuestName(guestName.trim());
        res.setGuestEmail(trimOrEmpty(JsonUtil.asString(payload.get("guestEmail"))));
        res.setGuestPhone(trimOrEmpty(JsonUtil.asString(payload.get("guestPhone"))));
        res.setCheckInDate(dates.checkIn().toString());
        res.setCheckOutDate(dates.checkOut().toString());
        res.setNights(dates.nights());
        res.setGuests(guests);
        res.setExtras(extras);
        res.setCustomFee(bill.getCustomFee());
        res.setCustomFeeLabel(bill.getCustomFeeLabel());
        res.setDiscountPercent(bill.getDiscountPercent());
        res.setPromoCode(bill.getPromoCode());
        res.setStatus(immediateCheckIn ? ReservationStatus.CHECKED_IN : ReservationStatus.RESERVED);
        res.setBill(bill);
        res.setPaid(false);
        res.setNotes(trimOrEmpty(JsonUtil.asString(payload.get("notes"))));
        res.setCreatedAt(now);
        res.setUpdatedAt(now);

        room.setStatus(immediateCheckIn ? RoomStatus.OCCUPIED : RoomStatus.RESERVED);
        room.setActiveReservationId(id);
        reservations.put(id, res);

        persistInternal();
        return res;
    }

    public synchronized Reservation updateReservation(String id, Map<String, Object> payload) throws IOException {
        Reservation res = requireReservation(id);
        if (res.getStatus() == ReservationStatus.CHECKED_OUT || res.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot edit a " + res.getStatus() + " reservation");
        }
        Room room = rooms.get(res.getRoomNumber());

        if (payload.containsKey("guestName")) {
            String name = JsonUtil.asString(payload.get("guestName"));
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Guest name cannot be blank");
            }
            res.setGuestName(name.trim());
        }
        if (payload.containsKey("guestEmail")) {
            res.setGuestEmail(trimOrEmpty(JsonUtil.asString(payload.get("guestEmail"))));
        }
        if (payload.containsKey("guestPhone")) {
            res.setGuestPhone(trimOrEmpty(JsonUtil.asString(payload.get("guestPhone"))));
        }
        if (payload.containsKey("guests")) {
            int guests = JsonUtil.asInt(payload.get("guests"), res.getGuests());
            if (guests < 1 || guests > room.getMaxGuests()) {
                throw new IllegalArgumentException("Guests must be between 1 and " + room.getMaxGuests() + " for Room " + room.getRoomNumber());
            }
            res.setGuests(guests);
        }
        if (payload.containsKey("checkInDate") || payload.containsKey("checkOutDate") || payload.containsKey("nights")) {
            Map<String, Object> merged = new LinkedHashMap<>();
            merged.put("checkInDate", payload.containsKey("checkInDate") ? payload.get("checkInDate") : res.getCheckInDate());
            merged.put("checkOutDate", payload.containsKey("checkOutDate") ? payload.get("checkOutDate") : res.getCheckOutDate());
            if (payload.containsKey("nights") && !payload.containsKey("checkOutDate")) {
                merged.remove("checkOutDate");
                merged.put("nights", payload.get("nights"));
            }
            DateRange dates = resolveDates(merged);
            res.setCheckInDate(dates.checkIn().toString());
            res.setCheckOutDate(dates.checkOut().toString());
            res.setNights(dates.nights());
        }
        if (payload.containsKey("extras")) {
            res.setExtras(JsonUtil.asStringList(payload.get("extras")));
        }
        if (payload.containsKey("customFee")) {
            res.setCustomFee(JsonUtil.asDouble(payload.get("customFee"), 0.0));
        }
        if (payload.containsKey("customFeeLabel")) {
            res.setCustomFeeLabel(JsonUtil.asString(payload.get("customFeeLabel")));
        }
        if (payload.containsKey("discountPercent")) {
            res.setDiscountPercent(JsonUtil.asDouble(payload.get("discountPercent"), 0.0));
        }
        if (payload.containsKey("promoCode")) {
            res.setPromoCode(JsonUtil.asString(payload.get("promoCode")));
        }
        if (payload.containsKey("notes")) {
            res.setNotes(trimOrEmpty(JsonUtil.asString(payload.get("notes"))));
        }

        BillBreakdown recalculated = billingService.calculateBill(
                room,
                res.getNights(),
                res.getExtras(),
                res.getCustomFee(),
                res.getCustomFeeLabel(),
                res.getDiscountPercent(),
                res.getPromoCode()
        );
        res.setBill(recalculated);
        res.setDiscountPercent(recalculated.getDiscountPercent());
        res.setPromoCode(recalculated.getPromoCode());
        res.setCustomFee(recalculated.getCustomFee());
        res.setCustomFeeLabel(recalculated.getCustomFeeLabel());
        res.setUpdatedAt(Instant.now().toString());

        persistInternal();
        return res;
    }

    public synchronized Reservation checkIn(String id) throws IOException {
        Reservation res = requireReservation(id);
        if (res.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED bookings can check in (current: " + res.getStatus() + ")");
        }
        Room room = rooms.get(res.getRoomNumber());
        res.setStatus(ReservationStatus.CHECKED_IN);
        res.setUpdatedAt(Instant.now().toString());
        room.setStatus(RoomStatus.OCCUPIED);
        room.setActiveReservationId(res.getId());
        persistInternal();
        return res;
    }

    public synchronized Reservation checkOut(String id) throws IOException {
        Reservation res = requireReservation(id);
        if (res.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only CHECKED_IN guests can check out (current: " + res.getStatus() + ")");
        }
        Room room = rooms.get(res.getRoomNumber());
        BillBreakdown finalBill = billingService.calculateBill(
                room,
                res.getNights(),
                res.getExtras(),
                res.getCustomFee(),
                res.getCustomFeeLabel(),
                res.getDiscountPercent(),
                res.getPromoCode()
        );
        res.setBill(finalBill);
        res.setPaid(true);
        res.setStatus(ReservationStatus.CHECKED_OUT);
        res.setUpdatedAt(Instant.now().toString());

        room.setStatus(RoomStatus.AVAILABLE);
        room.setActiveReservationId(null);

        persistInternal();
        return res;
    }

    public synchronized Reservation cancelReservation(String id) throws IOException {
        Reservation res = requireReservation(id);
        if (res.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED bookings can be cancelled (current: " + res.getStatus() + ")");
        }
        Room room = rooms.get(res.getRoomNumber());
        res.setStatus(ReservationStatus.CANCELLED);
        res.setUpdatedAt(Instant.now().toString());
        room.setStatus(RoomStatus.AVAILABLE);
        room.setActiveReservationId(null);

        persistInternal();
        return res;
    }

    public synchronized BillBreakdown getReservationBill(String id) {
        Reservation res = requireReservation(id);
        return res.getBill();
    }

    public synchronized BillBreakdown previewBill(Map<String, Object> payload) {
        int roomNumber = JsonUtil.asInt(payload.get("roomNumber"), 101);
        Room room = rooms.get(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " does not exist (valid rooms: 101-110)");
        }
        int nights = JsonUtil.asInt(payload.get("nights"), 1);
        List<String> extras = JsonUtil.asStringList(payload.get("extras"));
        double customFee = JsonUtil.asDouble(payload.get("customFee"), 0.0);
        String customFeeLabel = JsonUtil.asString(payload.get("customFeeLabel"));
        double discountPercent = JsonUtil.asDouble(payload.get("discountPercent"), 0.0);
        String promoCode = JsonUtil.asString(payload.get("promoCode"));

        return billingService.calculateBill(room, nights, extras, customFee, customFeeLabel, discountPercent, promoCode);
    }

    public synchronized Map<String, Object> saveRecordsNow() throws IOException {
        return persistInternal();
    }

    public synchronized String exportJson() throws IOException {
        persistInternal();
        return storageService.readJsonContent();
    }

    public synchronized String exportCsv() throws IOException {
        persistInternal();
        return storageService.readCsvContent();
    }

    private Map<String, Object> persistInternal() throws IOException {
        List<Room> roomList = new ArrayList<>(rooms.values());
        List<Reservation> resList = new ArrayList<>(reservations.values());
        Map<String, Object> result = storageService.saveAll(roomList, resList, nextReservationSeq);
        this.lastSavedAt = JsonUtil.asString(result.get("savedAt"));
        return result;
    }

    private Reservation requireReservation(String id) {
        Reservation res = reservations.get(id);
        if (res == null) {
            throw new NoSuchReservationException("Reservation not found: " + id);
        }
        return res;
    }

    private record DateRange(LocalDate checkIn, LocalDate checkOut, int nights) {
    }

    private DateRange resolveDates(Map<String, Object> payload) {
        String inStr = JsonUtil.asString(payload.get("checkInDate"));
        String outStr = JsonUtil.asString(payload.get("checkOutDate"));
        LocalDate checkIn;
        if (inStr == null || inStr.isBlank()) {
            checkIn = LocalDate.now();
        } else {
            try {
                checkIn = LocalDate.parse(inStr.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid check-in date format (expected YYYY-MM-DD)");
            }
        }

        LocalDate checkOut;
        int nights;
        if (outStr != null && !outStr.isBlank()) {
            try {
                checkOut = LocalDate.parse(outStr.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid check-out date format (expected YYYY-MM-DD)");
            }
            long diff = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (diff < 1) {
                throw new IllegalArgumentException("Check-out date must be at least 1 day after check-in date");
            }
            if (diff > 365) {
                throw new IllegalArgumentException("Stay cannot exceed 365 nights");
            }
            nights = (int) diff;
        } else {
            nights = JsonUtil.asInt(payload.get("nights"), 1);
            if (nights < 1 || nights > 365) {
                throw new IllegalArgumentException("Nights must be between 1 and 365");
            }
            checkOut = checkIn.plusDays(nights);
        }
        return new DateRange(checkIn, checkOut, nights);
    }

    private String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    public static class NoSuchReservationException extends IllegalArgumentException {
        public NoSuchReservationException(String message) {
            super(message);
        }
    }
}
