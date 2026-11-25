import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * SWISS FRONT-DESK — 100% PURE JAVA HOTEL MANAGEMENT SYSTEM (MINI PROJECT)
 * ============================================================================
 * Built completely from scratch using Core Java (JDK 11+):
 *  - UI Toolkit   : Java Swing (javax.swing) & AWT (java.awt)
 *  - Architecture : Object-Oriented Programming (Room, Reservation, Bill, HotelService)
 *  - Persistence  : Java File I/O (BufferedWriter, FileWriter, BufferedReader)
 *                   saving records to ./hotel_records/ (.txt, .csv, .dat)
 * ============================================================================
 */
public class HotelManagementApp extends JFrame {

    // =========================================================================
    // 1. SWISS FRONT-DESK DESIGN TOKENS (COLORS & FONTS)
    // =========================================================================
    private static final Color BG_CANVAS      = new Color(0xF8, 0xFA, 0xFC); // #F8FAFC
    private static final Color BG_SURFACE     = new Color(0xFF, 0xFF, 0xFF); // #FFFFFF
    private static final Color BG_SUBTLE      = new Color(0xF1, 0xF5, 0xF9); // #F1F5F9
    private static final Color BORDER_COLOR   = new Color(0xE2, 0xE8, 0xF0); // #E2E8F0
    private static final Color TEXT_PRIMARY   = new Color(0x0F, 0x17, 0x2A); // #0F172A
    private static final Color TEXT_MUTED     = new Color(0x47, 0x55, 0x69); // #475569
    private static final Color COBALT_PRIMARY = new Color(0x25, 0x63, 0xEB); // #2563EB
    private static final Color COBALT_DARK    = new Color(0x1D, 0x4E, 0xD8); // #1D4ED8
    private static final Color COBALT_TINT    = new Color(0xEF, 0xF6, 0xFF); // #EFF6FF

    // Semantic Status Palette
    private static final Color EMERALD_FG     = new Color(0x05, 0x96, 0x69);
    private static final Color EMERALD_BG     = new Color(0xEC, 0xFD, 0xF5);
    private static final Color EMERALD_BORDER = new Color(0xA7, 0xF3, 0xD0);

    private static final Color ROSE_FG        = new Color(0xE1, 0x1D, 0x48);
    private static final Color ROSE_BG        = new Color(0xFF, 0xF1, 0xF2);
    private static final Color ROSE_BORDER    = new Color(0xFE, 0xCD, 0xD3);

    private static final Color SKY_FG         = new Color(0x03, 0x69, 0xA1);
    private static final Color SKY_BG         = new Color(0xF0, 0xF9, 0xFF);
    private static final Color SKY_BORDER     = new Color(0xBA, 0xE6, 0xFD);

    private static final Color AMBER_FG       = new Color(0xD9, 0x77, 0x06);
    private static final Color AMBER_BG       = new Color(0xFF, 0xFB, 0xEB);
    private static final Color AMBER_BORDER   = new Color(0xFD, 0xE6, 0x8A);

    // Typography (Segoe UI on Windows for crisp modern rendering)
    private static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 21);
    private static final Font FONT_SUBTITLE   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_CARD_HEAD  = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD       = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_KPI        = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_MONO       = new Font("Consolas", Font.BOLD, 13);
    // 2. OBJECT-ORIENTED DOMAIN MODELS
    // =========================================================================

    public enum RoomStatus {
        AVAILABLE("Available"),
        OCCUPIED("Occupied"),
        CLEANING("Cleaning"),
        MAINTENANCE("Maintenance");

        private final String label;
        RoomStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static class Room {
        private final int roomNumber;
        private final int floor;
        private final String type;
        private double nightlyRate;
        private RoomStatus status;

        public Room(int roomNumber, int floor, String type, double nightlyRate, RoomStatus status) {
            this.roomNumber = roomNumber;
            this.floor = floor;
            this.type = type;
            this.nightlyRate = nightlyRate;
            this.status = status;
        }

        public int getRoomNumber() { return roomNumber; }
        public int getFloor() { return floor; }
        public String getType() { return type; }
        public double getNightlyRate() { return nightlyRate; }
        public void setNightlyRate(double nightlyRate) { this.nightlyRate = nightlyRate; }
        public RoomStatus getStatus() { return status; }
        public void setStatus(RoomStatus status) { this.status = status; }
        public boolean isEmpty() { return status == RoomStatus.AVAILABLE; }

        @Override
        public String toString() {
            return String.format("Room %d — %s (₹%.0f/night)", roomNumber, type, nightlyRate);
        }
    }

    public static class Reservation {
        private final String bookingId;
        private final int roomNumber;
        private final String roomType;
        private final double nightlyRate;
        private final String guestName;
        private final String phone;
        private final String govId;
        private final int guestsCount;
        private final LocalDate checkIn;
        private final LocalDate checkOut;
        private final double advancePaid;
        private final String paymentMode;
        private String status; // Checked-In | Checked-Out | Cancelled

        public Reservation(String bookingId, int roomNumber, String roomType, double nightlyRate,
                           String guestName, String phone, String govId, int guestsCount,
                           LocalDate checkIn, LocalDate checkOut, double advancePaid,
                           String paymentMode, String status) {
            this.bookingId = bookingId;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.nightlyRate = nightlyRate;
            this.guestName = guestName;
            this.phone = phone;
            this.govId = govId;
            this.guestsCount = guestsCount;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.advancePaid = advancePaid;
            this.paymentMode = paymentMode;
            this.status = status;
        }

        public long getNights() {
            return Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut));
        }

        public String getBookingId() { return bookingId; }
        public int getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public double getNightlyRate() { return nightlyRate; }
        public String getGuestName() { return guestName; }
        public String getPhone() { return phone; }
        public String getGovId() { return govId; }
        public int getGuestsCount() { return guestsCount; }
        public LocalDate getCheckIn() { return checkIn; }
        public LocalDate getCheckOut() { return checkOut; }
        public double getAdvancePaid() { return advancePaid; }
        public String getPaymentMode() { return paymentMode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return String.format("%s | Room %d — %s (%d nights)", bookingId, roomNumber, guestName, getNights());
        }
    }

    public static class AddonItem {
        private final String description;
        private final double amount;

        public AddonItem(String description, double amount) {
            this.description = description;
            this.amount = amount;
        }

        public String getDescription() { return description; }
        public double getAmount() { return amount; }
    }

    public static class Bill {
        private final String invoiceId;
        private final String bookingId;
        private final String guestName;
        private final int roomNumber;
        private final String roomType;
        private final long nights;
        private final double roomCharge;
        private final double addonsTotal;
        private final double discountAmount;
        private final double gstPercent;
        private final double gstAmount;
        private final double grandTotal;
        private final double advancePaid;
        private final double balanceDue;
        private final String paymentMode;
        private final LocalDateTime issuedAt;

        public Bill(String invoiceId, String bookingId, String guestName, int roomNumber, String roomType,
                    long nights, double roomCharge, double addonsTotal, double discountAmount,
                    double gstPercent, double gstAmount, double grandTotal, double advancePaid,
                    double balanceDue, String paymentMode, LocalDateTime issuedAt) {
            this.invoiceId = invoiceId;
            this.bookingId = bookingId;
            this.guestName = guestName;
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.nights = nights;
            this.roomCharge = roomCharge;
            this.addonsTotal = addonsTotal;
            this.discountAmount = discountAmount;
            this.gstPercent = gstPercent;
            this.gstAmount = gstAmount;
            this.grandTotal = grandTotal;
            this.advancePaid = advancePaid;
            this.balanceDue = balanceDue;
            this.paymentMode = paymentMode;
            this.issuedAt = issuedAt;
        }

        public String getInvoiceId() { return invoiceId; }
        public String getBookingId() { return bookingId; }
        public String getGuestName() { return guestName; }
        public int getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public long getNights() { return nights; }
        public double getRoomCharge() { return roomCharge; }
        public double getAddonsTotal() { return addonsTotal; }
        public double getDiscountAmount() { return discountAmount; }
        public double getGstPercent() { return gstPercent; }
        public double getGstAmount() { return gstAmount; }
        public double getGrandTotal() { return grandTotal; }
        public double getAdvancePaid() { return advancePaid; }
        public double getBalanceDue() { return balanceDue; }
        public String getPaymentMode() { return paymentMode; }
        public LocalDateTime getIssuedAt() { return issuedAt; }

        public String formatReceiptText() {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
            return String.join("\n",
                "============================================================",
                "        SWISS FRONT-DESK HOTEL — OFFICIAL TAX INVOICE",
                "        MG Road, Bengaluru | GSTIN: 29AABCS1429B1Z5",
                "============================================================",
                String.format("Invoice ID     : %s", invoiceId),
                String.format("Booking Ref    : %s", bookingId),
                String.format("Issued On      : %s", issuedAt.format(fmt)),
                String.format("Guest Name     : %s", guestName),
                String.format("Room           : Room %d (%s)", roomNumber, roomType),
                String.format("Duration       : %d night(s)", nights),
                String.format("Payment Mode   : %s", paymentMode),
                "------------------------------------------------------------",
                String.format("Room Charges   : ₹%,10.2f", roomCharge),
                String.format("Add-On Extras  : ₹%,10.2f", addonsTotal),
                String.format("Discount       : -₹%,9.2f", discountAmount),
                String.format("GST (%.1f%%)    : ₹%,10.2f", gstPercent, gstAmount),
                "------------------------------------------------------------",
                String.format("GRAND TOTAL    : ₹%,10.2f", grandTotal),
                String.format("Advance Paid   : -₹%,9.2f", advancePaid),
                String.format("BALANCE DUE    : ₹%,10.2f", balanceDue),
                "============================================================"
            );
        }
    }

    // =========================================================================
    // 3. HOTEL SERVICE & JAVA FILE I/O PERSISTENCE ENGINE
    // =========================================================================

    public static class HotelService {
        private final List<Room> rooms = new ArrayList<>();
        private final List<Reservation> reservations = new ArrayList<>();
        private final List<Bill> bills = new ArrayList<>();
        private int nextResId = 1005;
        private int nextInvId = 1003;

        private final Path recordsDir = Paths.get("hotel_records");
        private final Path invoicesDir = recordsDir.resolve("invoices");
        private final Path databaseFile = recordsDir.resolve("hotel_database.txt");
        private final Path csvLedgerFile = recordsDir.resolve("billing_ledger.csv");

        public HotelService() {
            try {
                Files.createDirectories(recordsDir);
                Files.createDirectories(invoicesDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Files.exists(databaseFile)) {
                loadFromDisk();
            } else {
                seedInitialData();
                saveAllToDisk();
            }
        }

        public void seedInitialData() {
            rooms.clear();
            reservations.clear();
            bills.clear();
            nextResId = 1005;
            nextInvId = 1003;

            // 10 Rooms (101 - 110)
            rooms.add(new Room(101, 1, "Standard Room", 2500, RoomStatus.OCCUPIED));
            rooms.add(new Room(102, 1, "Standard Room", 2500, RoomStatus.AVAILABLE));
            rooms.add(new Room(103, 1, "Standard Room", 2500, RoomStatus.CLEANING));
            rooms.add(new Room(104, 1, "Standard Room", 2500, RoomStatus.MAINTENANCE));
            rooms.add(new Room(105, 2, "Deluxe Room", 4200, RoomStatus.OCCUPIED));
            rooms.add(new Room(106, 2, "Deluxe Room", 4200, RoomStatus.AVAILABLE));
            rooms.add(new Room(107, 2, "Deluxe Room", 4200, RoomStatus.AVAILABLE));
            rooms.add(new Room(108, 3, "Executive Suite", 6800, RoomStatus.OCCUPIED));
            rooms.add(new Room(109, 3, "Executive Suite", 6800, RoomStatus.AVAILABLE));
            rooms.add(new Room(110, 3, "Penthouse Suite", 9500, RoomStatus.AVAILABLE));

            LocalDate today = LocalDate.now();
            reservations.add(new Reservation("RES-1002", 101, "Standard Room", 2500,
                "Aarav Mehta", "+91 98450 21490", "IND-PASS-88412", 1,
                today.minusDays(2), today.plusDays(1), 2000, "UPI", "Checked-In"));
            reservations.add(new Reservation("RES-1003", 105, "Deluxe Room", 4200,
                "Clara Lindholm", "+41 79 442 18 90", "CH-7731904", 2,
                today.minusDays(1), today.plusDays(2), 4000, "Card", "Checked-In"));
            reservations.add(new Reservation("RES-1004", 108, "Executive Suite", 6800,
                "Rohan Deshmukh", "+91 99001 73215", "AADHAAR-9104", 2,
                today.minusDays(3), today, 6000, "Bank Transfer", "Checked-In"));
            reservations.add(new Reservation("RES-1001", 102, "Standard Room", 2500,
                "Priya Nair", "+91 97412 55091", "DL-KA01-4421", 1,
                today.minusDays(5), today.minusDays(3), 2000, "UPI", "Checked-Out"));

            bills.add(new Bill("INV-2026-1001", "RES-1001", "Priya Nair", 102, "Standard Room",
                2, 5000, 850, 0, 12.0, 702.0, 6552.0, 2000.0, 4552.0, "UPI",
                LocalDateTime.now().minusDays(3)));
            bills.add(new Bill("INV-2026-1002", "RES-1000", "Marcus Vance", 110, "Penthouse Suite",
                3, 28500, 1500, 1500, 18.0, 5130.0, 33630.0, 10000.0, 23630.0, "Card",
                LocalDateTime.now().minusDays(1)));
        }

        public List<Room> getRooms() { return rooms; }
        public List<Reservation> getReservations() { return reservations; }
        public List<Bill> getBills() { return bills; }
        public Path getRecordsDir() { return recordsDir; }

        public Room findRoom(int roomNumber) {
            for (Room r : rooms) {
                if (r.getRoomNumber() == roomNumber) return r;
            }
            return null;
        }

        public Reservation getActiveReservationForRoom(int roomNumber) {
            for (Reservation res : reservations) {
                if (res.getRoomNumber() == roomNumber && "Checked-In".equals(res.getStatus())) {
                    return res;
                }
            }
            return null;
        }

        public List<Room> getAvailableRooms() {
            List<Room> empty = new ArrayList<>();
            for (Room r : rooms) {
                if (r.isEmpty()) empty.add(r);
            }
            return empty;
        }

        public double getTotalRevenue() {
            double total = 0;
            for (Bill b : bills) total += b.getGrandTotal();
            return total;
        }

        public Reservation createReservation(int roomNumber, String guestName, String phone, String govId,
                                             int guestsCount, LocalDate checkIn, LocalDate checkOut,
                                             double advancePaid, String paymentMode) {
            Room room = findRoom(roomNumber);
            if (room == null || !room.isEmpty()) {
                throw new IllegalStateException("Room " + roomNumber + " is not available for check-in.");
            }
            if (!checkOut.isAfter(checkIn)) {
                throw new IllegalArgumentException("Check-out date must be after Check-in date.");
            }
            String id = "RES-" + (nextResId++);
            Reservation res = new Reservation(id, roomNumber, room.getType(), room.getNightlyRate(),
                    guestName, phone, govId, guestsCount, checkIn, checkOut, advancePaid, paymentMode, "Checked-In");
            room.setStatus(RoomStatus.OCCUPIED);
            reservations.add(0, res);
            saveAllToDisk();
            return res;
        }

        public void cancelReservation(String bookingId) {
            for (Reservation res : reservations) {
                if (res.getBookingId().equals(bookingId) && "Checked-In".equals(res.getStatus())) {
                    res.setStatus("Cancelled");
                    Room room = findRoom(res.getRoomNumber());
                    if (room != null) room.setStatus(RoomStatus.AVAILABLE);
                    saveAllToDisk();
                    return;
                }
            }
        }

        public Bill checkoutReservation(Reservation res, double addonsTotal, double discountAmount,
                                        double gstPercent, String paymentMode) {
            double roomCharge = res.getNightlyRate() * res.getNights();
            double subtotal = roomCharge + addonsTotal;
            double validDiscount = Math.min(subtotal, Math.max(0, discountAmount));
            double taxable = Math.max(0, subtotal - validDiscount);
            double gstAmount = (taxable * gstPercent) / 100.0;
            double grandTotal = taxable + gstAmount;
            double balanceDue = Math.max(0, grandTotal - res.getAdvancePaid());

            String invId = "INV-2026-" + (nextInvId++);
            Bill bill = new Bill(invId, res.getBookingId(), res.getGuestName(), res.getRoomNumber(),
                    res.getRoomType(), res.getNights(), roomCharge, addonsTotal, validDiscount,
                    gstPercent, gstAmount, grandTotal, res.getAdvancePaid(), balanceDue,
                    paymentMode, LocalDateTime.now());

            res.setStatus("Checked-Out");
            Room room = findRoom(res.getRoomNumber());
            if (room != null) {
                room.setStatus(RoomStatus.CLEANING); // Housekeeping workflow after checkout
            }

            bills.add(0, bill);
            saveAllToDisk();
            writeIndividualReceiptFile(bill);
            return bill;
        }

        /**
         * Persists all Rooms, Reservations, and Bills to plain-text & CSV files using BufferedWriter.
         */
        public void saveAllToDisk() {
            try {
                Files.createDirectories(recordsDir);
                Files.createDirectories(invoicesDir);

                // 1. Save structured pipe-delimited database file (hotel_records/hotel_database.txt)
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(databaseFile.toFile(), StandardCharsets.UTF_8))) {
                    bw.write("META|" + nextResId + "|" + nextInvId);
                    bw.newLine();
                    for (Room r : rooms) {
                        bw.write(String.format("ROOM|%d|%d|%s|%.2f|%s",
                                r.getRoomNumber(), r.getFloor(), r.getType(), r.getNightlyRate(), r.getStatus().name()));
                        bw.newLine();
                    }
                    for (Reservation res : reservations) {
                        bw.write(String.format("RES|%s|%d|%s|%.2f|%s|%s|%s|%d|%s|%s|%.2f|%s|%s",
                                res.getBookingId(), res.getRoomNumber(), res.getRoomType(), res.getNightlyRate(),
                                sanitize(res.getGuestName()), sanitize(res.getPhone()), sanitize(res.getGovId()),
                                res.getGuestsCount(), res.getCheckIn(), res.getCheckOut(),
                                res.getAdvancePaid(), sanitize(res.getPaymentMode()), res.getStatus()));
                        bw.newLine();
                    }
                    for (Bill b : bills) {
                        bw.write(String.format("BILL|%s|%s|%s|%d|%s|%d|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%s|%s",
                                b.getInvoiceId(), b.getBookingId(), sanitize(b.getGuestName()), b.getRoomNumber(),
                                b.getRoomType(), b.getNights(), b.getRoomCharge(), b.getAddonsTotal(),
                                b.getDiscountAmount(), b.getGstPercent(), b.getGstAmount(), b.getGrandTotal(),
                                b.getAdvancePaid(), b.getBalanceDue(), sanitize(b.getPaymentMode()), b.getIssuedAt()));
                        bw.newLine();
                    }
                }

                // 2. Save Excel-ready CSV ledger (hotel_records/billing_ledger.csv)
                try (BufferedWriter csv = new BufferedWriter(new FileWriter(csvLedgerFile.toFile(), StandardCharsets.UTF_8))) {
                    csv.write("Invoice ID,Booking ID,Guest Name,Room,Room Type,Nights,Room Charge,Add-Ons,Discount,GST %,GST Amount,Grand Total,Advance Paid,Balance Due,Payment Mode,Issued At");
                    csv.newLine();
                    for (Bill b : bills) {
                        csv.write(String.format("\"%s\",\"%s\",\"%s\",%d,\"%s\",%d,%.2f,%.2f,%.2f,%.1f,%.2f,%.2f,%.2f,%.2f,\"%s\",\"%s\"",
                                b.getInvoiceId(), b.getBookingId(), b.getGuestName().replace("\"", "\"\""),
                                b.getRoomNumber(), b.getRoomType(), b.getNights(), b.getRoomCharge(),
                                b.getAddonsTotal(), b.getDiscountAmount(), b.getGstPercent(), b.getGstAmount(),
                                b.getGrandTotal(), b.getAdvancePaid(), b.getBalanceDue(), b.getPaymentMode(), b.getIssuedAt()));
                        csv.newLine();
                    }
                }

                // 3. Ensure receipt files exist for all invoices
                for (Bill b : bills) {
                    writeIndividualReceiptFile(b);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeIndividualReceiptFile(Bill bill) {
            Path receiptFile = invoicesDir.resolve(bill.getInvoiceId() + ".txt");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(receiptFile.toFile(), StandardCharsets.UTF_8))) {
                bw.write(bill.formatReceiptText());
                bw.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void loadFromDisk() {
            rooms.clear();
            reservations.clear();
            bills.clear();
            try (BufferedReader br = new BufferedReader(new FileReader(databaseFile.toFile(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split("\\|", -1);
                    if (p.length == 0) continue;
                    switch (p[0]) {
                        case "META":
                            nextResId = Integer.parseInt(p[1]);
                            nextInvId = Integer.parseInt(p[2]);
                            break;
                        case "ROOM":
                            rooms.add(new Room(Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3],
                                    Double.parseDouble(p[4]), RoomStatus.valueOf(p[5])));
                            break;
                        case "RES":
                            reservations.add(new Reservation(p[1], Integer.parseInt(p[2]), p[3],
                                    Double.parseDouble(p[4]), p[5], p[6], p[7], Integer.parseInt(p[8]),
                                    LocalDate.parse(p[9]), LocalDate.parse(p[10]), Double.parseDouble(p[11]),
                                    p[12], p[13]));
                            break;
                        case "BILL":
                            bills.add(new Bill(p[1], p[2], p[3], Integer.parseInt(p[4]), p[5],
                                    Long.parseLong(p[6]), Double.parseDouble(p[7]), Double.parseDouble(p[8]),
                                    Double.parseDouble(p[9]), Double.parseDouble(p[10]), Double.parseDouble(p[11]),
                                    Double.parseDouble(p[12]), Double.parseDouble(p[13]), Double.parseDouble(p[14]),
                                    p[15], LocalDateTime.parse(p[16])));
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                seedInitialData();
            }
        }

        private String sanitize(String s) {
            return s == null ? "" : s.replace("|", "/").replace("\n", " ").trim();
        }
    }

    // =========================================================================
    // 4. SWING GUI COMPONENTS & VIEWS
    // =========================================================================

    private final HotelService hotelService = new HotelService();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);

    // Active Navigation Tracking
    private String activeNavKey = "OVERVIEW";
    private final List<NavItemButton> navButtons = new ArrayList<>();

    // Header KPI Labels
    private JLabel kpiTotalVal, kpiEmptyVal, kpiOccupiedVal, kpiRevenueVal;

    // View 1: Room Grid & Filter
    private JPanel roomsGridPanel;
    private final List<FilterPillButton> filterPills = new ArrayList<>();
    private String currentRoomFilter = "ALL"; // ALL | AVAILABLE | OCCUPIED | CLEANING | MAINTENANCE

    // View 2: Reservations Table
    private DefaultTableModel reservationsTableModel;
    private JTable reservationsTable;

    // View 3: Bill Calculator Components
    private JComboBox<Reservation> billResCombo;
    private DefaultTableModel addonsTableModel;
    private final List<AddonItem> currentAddons = new ArrayList<>();
    private JTextField gstField, discountField;
    private JComboBox<String> settlementModeCombo;
    private JTextArea liveInvoiceArea;

    // View 4: Records & File Export Table
    private DefaultTableModel recordsTableModel;
    private JTable recordsTable;

    public HotelManagementApp() {
        super("Swiss Front-Desk — Java Hotel Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_CANVAS);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(BG_CANVAS);
        rightWrapper.add(buildTopHeaderAndKpis(), BorderLayout.NORTH);

        contentCards.setBackground(BG_CANVAS);
        contentCards.add(buildRoomOverviewView(), "OVERVIEW");
        contentCards.add(buildReservationsView(), "RESERVATIONS");
        contentCards.add(buildBillingView(), "BILLING");
        contentCards.add(buildRecordsView(), "RECORDS");

        rightWrapper.add(contentCards, BorderLayout.CENTER);
        add(rightWrapper, BorderLayout.CENTER);

        updateNavSelection();
        refreshAllViews();
    }

    // -------------------------------------------------------------------------
    // Sidebar Navigation
    // -------------------------------------------------------------------------
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBackground(BG_SURFACE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR));
        sidebar.setLayout(new BorderLayout());

        // Brand Header with Cobalt Badge
        JPanel brandBox = new JPanel(new BorderLayout(12, 0));
        brandBox.setBackground(BG_SURFACE);
        brandBox.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JPanel logoBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COBALT_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        logoBadge.setOpaque(false);
        logoBadge.setPreferredSize(new Dimension(38, 38));
        JLabel logoTxt = new JLabel("SF");
        logoTxt.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoTxt.setForeground(Color.WHITE);
        logoBadge.add(logoTxt);

        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
        brandText.setBackground(BG_SURFACE);
        JLabel title = new JLabel("Swiss Front-Desk");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        JLabel sub = new JLabel("10-Room Java PMS");
        sub.setFont(FONT_SUBTITLE);
        sub.setForeground(TEXT_MUTED);
        brandText.add(title);
        brandText.add(sub);

        brandBox.add(logoBadge, BorderLayout.WEST);
        brandBox.add(brandText, BorderLayout.CENTER);
        sidebar.add(brandBox, BorderLayout.NORTH);

        // Navigation Menu Buttons
        JPanel navList = new JPanel();
        navList.setBackground(BG_SURFACE);
        navList.setBorder(new EmptyBorder(16, 14, 16, 14));
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));

        navButtons.clear();
        navList.add(createNavButton("Room Overview", "OVERVIEW"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Reservations Manager", "RESERVATIONS"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Bill Calculator", "BILLING"));
        navList.add(Box.createVerticalStrut(6));
        navList.add(createNavButton("Records & File Export", "RECORDS"));

        sidebar.add(navList, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 8));
        footer.setBackground(BG_SURFACE);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JButton saveBtn = createSecondaryButton("Save Records to Disk");
        saveBtn.addActionListener(e -> {
            hotelService.saveAllToDisk();
            JOptionPane.showMessageDialog(this,
                    "All room, reservation, and invoice records saved to:\n" + hotelService.getRecordsDir().toAbsolutePath(),
                    "Saved to Java File Storage", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton resetBtn = createSecondaryButton("Reset Demo Data");
        resetBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Restore all 10 rooms and sample bookings to initial defaults?",
                    "Confirm Reset", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                hotelService.seedInitialData();
                hotelService.saveAllToDisk();
                refreshAllViews();
            }
        });

        footer.add(saveBtn);
        footer.add(resetBtn);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        NavItemButton btn = new NavItemButton(text, cardName);
        btn.addActionListener(e -> {
            activeNavKey = cardName;
            updateNavSelection();
            cardLayout.show(contentCards, cardName);
            refreshAllViews();
        });
        navButtons.add(btn);
        return btn;
    }

    private void updateNavSelection() {
        for (NavItemButton btn : navButtons) {
            btn.setSelectedNav(btn.cardName.equals(activeNavKey));
        }
    }

    // -------------------------------------------------------------------------
    // Top Header & 4 KPI Summary Cards
    // -------------------------------------------------------------------------
    private JPanel buildTopHeaderAndKpis() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setBackground(BG_CANVAS);
        wrapper.setBorder(new EmptyBorder(18, 22, 10, 22));

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_CANVAS);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 2));
        titleGroup.setBackground(BG_CANVAS);
        JLabel headerTitle = new JLabel("Hotel Operations Console");
        headerTitle.setFont(FONT_TITLE);
        headerTitle.setForeground(TEXT_PRIMARY);
        JLabel headerSub = new JLabel("Manage up to 10 rooms, check empty rooms, calculate GST bills & archive records");
        headerSub.setFont(FONT_SUBTITLE);
        headerSub.setForeground(TEXT_MUTED);
        titleGroup.add(headerTitle);
        titleGroup.add(headerSub);

        JButton newResBtn = createPrimaryButton("+  New Reservation");
        newResBtn.setPreferredSize(new Dimension(175, 40));
        newResBtn.addActionListener(e -> openNewReservationDialog(null));

        JPanel rightBtnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        rightBtnWrap.setBackground(BG_CANVAS);
        rightBtnWrap.add(newResBtn);

        topBar.add(titleGroup, BorderLayout.WEST);
        topBar.add(rightBtnWrap, BorderLayout.EAST);
        wrapper.add(topBar, BorderLayout.NORTH);

        // 4 KPI Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setBackground(BG_CANVAS);

        kpiTotalVal = new JLabel("10");
        kpiEmptyVal = new JLabel("5");
        kpiOccupiedVal = new JLabel("3");
        kpiRevenueVal = new JLabel("₹40,182");

        kpiGrid.add(createKpiCard("TOTAL ROOMS", kpiTotalVal, "10-Room Inventory", TEXT_PRIMARY, BORDER_COLOR));
        kpiGrid.add(createKpiCard("EMPTY / AVAILABLE", kpiEmptyVal, "Ready for Check-In", EMERALD_FG, EMERALD_BORDER));
        kpiGrid.add(createKpiCard("OCCUPIED ROOMS", kpiOccupiedVal, "Active Checked-In Guests", ROSE_FG, BORDER_COLOR));
        kpiGrid.add(createKpiCard("COLLECTED REVENUE", kpiRevenueVal, "Finalized Tax Invoices", COBALT_PRIMARY, BORDER_COLOR));

        wrapper.add(kpiGrid, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createKpiCard(String label, JLabel valueLabel, String subtext, Color accentColor, Color borderColor) {
        JPanel card = new RoundedPanel(12, BG_SURFACE, borderColor);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel top = new JLabel(label);
        top.setFont(new Font("Segoe UI", Font.BOLD, 11));
        top.setForeground(TEXT_MUTED);

        valueLabel.setFont(FONT_KPI);
        valueLabel.setForeground(accentColor);

        JLabel bot = new JLabel(subtext);
        bot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bot.setForeground(TEXT_MUTED);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bot, BorderLayout.SOUTH);
        return card;
    }

    // -------------------------------------------------------------------------
    // VIEW 1: 10-Room Overview & Empty Room Checker
    // -------------------------------------------------------------------------
    private JPanel buildRoomOverviewView() {
        JPanel view = new JPanel(new BorderLayout(0, 14));
        view.setBackground(BG_CANVAS);
        view.setBorder(new EmptyBorder(8, 22, 22, 22));

        // Filter Bar
        JPanel filterBar = new RoundedPanel(12, BG_SURFACE, BORDER_COLOR);
        filterBar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterBar.setBorder(new EmptyBorder(4, 10, 4, 10));

        filterPills.clear();
        filterBar.add(createFilterPill("All 10 Rooms", "ALL"));
        filterBar.add(createFilterPill("Empty / Available Only", "AVAILABLE"));
        filterBar.add(createFilterPill("Occupied", "OCCUPIED"));
        filterBar.add(createFilterPill("Cleaning", "CLEANING"));
        filterBar.add(createFilterPill("Maintenance", "MAINTENANCE"));
        updateFilterPillSelection();

        view.add(filterBar, BorderLayout.NORTH);

        // 3-Column Responsive Room Grid Container (prevents horizontal clipping)
        roomsGridPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        roomsGridPanel.setBackground(BG_CANVAS);
        roomsGridPanel.setBorder(new EmptyBorder(2, 0, 8, 4));

        JPanel gridTopAnchor = new JPanel(new BorderLayout());
        gridTopAnchor.setBackground(BG_CANVAS);
        gridTopAnchor.add(roomsGridPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(gridTopAnchor);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BG_CANVAS);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private JButton createFilterPill(String label, String filterKey) {
        FilterPillButton btn = new FilterPillButton(label, filterKey);
        btn.addActionListener(e -> {
            currentRoomFilter = filterKey;
            updateFilterPillSelection();
            rebuildRoomCards();
        });
        filterPills.add(btn);
        return btn;
    }

    private void updateFilterPillSelection() {
        for (FilterPillButton pill : filterPills) {
            pill.setActive(pill.filterKey.equals(currentRoomFilter));
        }
    }

    private void rebuildRoomCards() {
        roomsGridPanel.removeAll();
        for (Room room : hotelService.getRooms()) {
            if (!"ALL".equals(currentRoomFilter) && !room.getStatus().name().equals(currentRoomFilter)) {
                continue;
            }
            roomsGridPanel.add(createRoomCard(room));
        }
        roomsGridPanel.revalidate();
        roomsGridPanel.repaint();
    }

    private JPanel createRoomCard(Room room) {
        JPanel card = new RoundedPanel(12, BG_SURFACE, BORDER_COLOR);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Top Row: Room # + Status Pill
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(BG_SURFACE);

        JPanel roomTitleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        roomTitleBox.setBackground(BG_SURFACE);
        JLabel numLbl = new JLabel("Room " + room.getRoomNumber());
        numLbl.setFont(FONT_CARD_HEAD);
        numLbl.setForeground(TEXT_PRIMARY);
        JLabel typeLbl = new JLabel("Floor " + room.getFloor() + " · " + room.getType());
        typeLbl.setFont(FONT_SUBTITLE);
        typeLbl.setForeground(TEXT_MUTED);
        roomTitleBox.add(numLbl);
        roomTitleBox.add(typeLbl);

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrap.setBackground(BG_SURFACE);
        badgeWrap.add(createStatusBadge(room.getStatus()));

        header.add(roomTitleBox, BorderLayout.WEST);
        header.add(badgeWrap, BorderLayout.EAST);

        // Middle: Rate & Guest Context
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 6));
        center.setBackground(BG_SURFACE);
        center.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 0, 10, 0)
        ));

        JLabel rateLbl = new JLabel(String.format("Rate: ₹%,.0f / night", room.getNightlyRate()));
        rateLbl.setFont(FONT_MONO);
        rateLbl.setForeground(TEXT_PRIMARY);

        Reservation activeRes = hotelService.getActiveReservationForRoom(room.getRoomNumber());
        JLabel contextLbl = new JLabel();
        contextLbl.setFont(FONT_SUBTITLE);
        if (room.getStatus() == RoomStatus.OCCUPIED && activeRes != null) {
            contextLbl.setText("Guest: " + activeRes.getGuestName() + " (Out: " + activeRes.getCheckOut() + ")");
            contextLbl.setForeground(ROSE_FG);
        } else if (room.getStatus() == RoomStatus.AVAILABLE) {
            contextLbl.setText("Inspected & ready for check-in");
            contextLbl.setForeground(EMERALD_FG);
        } else if (room.getStatus() == RoomStatus.CLEANING) {
            contextLbl.setText("Housekeeping cleaning required");
            contextLbl.setForeground(SKY_FG);
        } else {
            contextLbl.setText("Offline for maintenance");
            contextLbl.setForeground(AMBER_FG);
        }

        center.add(rateLbl);
        center.add(contextLbl);

        // Bottom: Action Buttons
        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setBackground(BG_SURFACE);

        if (room.getStatus() == RoomStatus.AVAILABLE) {
            JButton bookBtn = createPrimaryButton("Reserve / Check-In");
            bookBtn.addActionListener(e -> openNewReservationDialog(room.getRoomNumber()));
            JButton maintBtn = createSecondaryButton("Maint.");
            maintBtn.setPreferredSize(new Dimension(76, 36));
            maintBtn.addActionListener(e -> {
                room.setStatus(RoomStatus.MAINTENANCE);
                hotelService.saveAllToDisk();
                refreshAllViews();
            });
            actions.add(bookBtn, BorderLayout.CENTER);
            actions.add(maintBtn, BorderLayout.EAST);
        } else if (room.getStatus() == RoomStatus.OCCUPIED && activeRes != null) {
            JButton billBtn = createPrimaryButton("Calculate Bill");
            billBtn.addActionListener(e -> {
                activeNavKey = "BILLING";
                updateNavSelection();
                cardLayout.show(contentCards, "BILLING");
                refreshAllViews();
                billResCombo.setSelectedItem(activeRes);
                updateLiveBillPreview();
            });
            actions.add(billBtn, BorderLayout.CENTER);
        } else if (room.getStatus() == RoomStatus.CLEANING) {
            JButton cleanBtn = createSecondaryButton("Mark Clean & Available");
            cleanBtn.addActionListener(e -> {
                room.setStatus(RoomStatus.AVAILABLE);
                hotelService.saveAllToDisk();
                refreshAllViews();
            });
            actions.add(cleanBtn, BorderLayout.CENTER);
        } else {
            JButton availBtn = createSecondaryButton("Mark Available");
            availBtn.addActionListener(e -> {
                room.setStatus(RoomStatus.AVAILABLE);
                hotelService.saveAllToDisk();
                refreshAllViews();
            });
            actions.add(availBtn, BorderLayout.CENTER);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JLabel createStatusBadge(RoomStatus status) {
        Color fg, bg, bdr;
        if (status == RoomStatus.AVAILABLE) {
            fg = EMERALD_FG; bg = EMERALD_BG; bdr = EMERALD_BORDER;
        } else if (status == RoomStatus.OCCUPIED) {
            fg = ROSE_FG; bg = ROSE_BG; bdr = ROSE_BORDER;
        } else if (status == RoomStatus.CLEANING) {
            fg = SKY_FG; bg = SKY_BG; bdr = SKY_BORDER;
        } else {
            fg = AMBER_FG; bg = AMBER_BG; bdr = AMBER_BORDER;
        }
        JLabel badge = new JLabel(status.getLabel()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(bdr);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(fg);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        return badge;
    }

    // -------------------------------------------------------------------------
    // VIEW 2: Reservations Manager & Check-In Dialog
    // -------------------------------------------------------------------------
    private JPanel buildReservationsView() {
        JPanel view = new JPanel(new BorderLayout(0, 12));
        view.setBackground(BG_CANVAS);
        view.setBorder(new EmptyBorder(8, 20, 20, 20));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_SURFACE);
        toolbar.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 14, 10, 14)));

        JLabel lbl = new JLabel("All Guest Reservations (Checked-In, Checked-Out & Cancelled)");
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(TEXT_PRIMARY);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setBackground(BG_SURFACE);

        JButton addBtn = createPrimaryButton("+ New Check-In");
        addBtn.addActionListener(e -> openNewReservationDialog(null));

        JButton cancelBtn = createDangerButton("Cancel Selected");
        cancelBtn.addActionListener(e -> {
            int row = reservationsTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select an active Checked-In reservation row first.");
                return;
            }
            String bookingId = (String) reservationsTableModel.getValueAt(row, 0);
            String status = (String) reservationsTableModel.getValueAt(row, 7);
            if (!"Checked-In".equals(status)) {
                JOptionPane.showMessageDialog(this, "Only 'Checked-In' reservations can be cancelled.");
                return;
            }
            hotelService.cancelReservation(bookingId);
            refreshAllViews();
        });

        btnGroup.add(addBtn);
        btnGroup.add(cancelBtn);

        toolbar.add(lbl, BorderLayout.WEST);
        toolbar.add(btnGroup, BorderLayout.EAST);
        view.add(toolbar, BorderLayout.NORTH);

        String[] cols = {"Booking ID", "Guest Name", "Phone", "Room", "Check-In", "Check-Out", "Advance (₹)", "Status"};
        reservationsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        reservationsTable = new JTable(reservationsTableModel);
        styleTable(reservationsTable);

        JScrollPane scroll = new JScrollPane(reservationsTable);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        view.add(scroll, BorderLayout.CENTER);

        return view;
    }

    private void openNewReservationDialog(Integer preselectedRoom) {
        List<Room> available = hotelService.getAvailableRooms();
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No empty rooms are currently available. Mark a room Clean or Available first.",
                    "No Empty Rooms", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "New Room Reservation & Check-In", true);
        dialog.setSize(480, 470);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_SURFACE);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(9, 2, 10, 10));
        form.setBackground(BG_SURFACE);
        form.setBorder(new EmptyBorder(18, 20, 18, 20));

        JComboBox<Room> roomCombo = new JComboBox<>(available.toArray(new Room[0]));
        if (preselectedRoom != null) {
            for (Room r : available) {
                if (r.getRoomNumber() == preselectedRoom) roomCombo.setSelectedItem(r);
            }
        }

        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField govIdField = new JTextField();
        JSpinner guestsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 6, 1));
        JTextField checkInField = new JTextField(LocalDate.now().toString());
        JTextField checkOutField = new JTextField(LocalDate.now().plusDays(2).toString());
        JTextField advanceField = new JTextField("0");
        JComboBox<String> payModeCombo = new JComboBox<>(new String[]{"UPI", "Card", "Cash", "Bank Transfer"});

        form.add(new JLabel("Select Empty Room:")); form.add(roomCombo);
        form.add(new JLabel("Guest Full Name *:")); form.add(nameField);
        form.add(new JLabel("Phone Number *:")); form.add(phoneField);
        form.add(new JLabel("Gov ID / Passport *:")); form.add(govIdField);
        form.add(new JLabel("Number of Guests:")); form.add(guestsSpinner);
        form.add(new JLabel("Check-In (YYYY-MM-DD):")); form.add(checkInField);
        form.add(new JLabel("Check-Out (YYYY-MM-DD):")); form.add(checkOutField);
        form.add(new JLabel("Advance Deposit (₹):")); form.add(advanceField);
        form.add(new JLabel("Payment Mode:")); form.add(payModeCombo);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        actions.setBackground(BG_CANVAS);
        actions.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton cancelBtn = createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton confirmBtn = createPrimaryButton("Confirm Check-In");
        confirmBtn.addActionListener(e -> {
            try {
                Room selected = (Room) roomCombo.getSelectedItem();
                String gName = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String govId = govIdField.getText().trim();
                if (gName.isEmpty() || phone.isEmpty() || govId.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please enter Guest Name, Phone, and Gov ID.");
                    return;
                }
                LocalDate inDate = LocalDate.parse(checkInField.getText().trim());
                LocalDate outDate = LocalDate.parse(checkOutField.getText().trim());
                double adv = Math.max(0, Double.parseDouble(advanceField.getText().trim()));
                int guests = (Integer) guestsSpinner.getValue();
                String mode = (String) payModeCombo.getSelectedItem();

                Reservation res = hotelService.createReservation(
                        selected.getRoomNumber(), gName, phone, govId, guests, inDate, outDate, adv, mode);
                dialog.dispose();
                refreshAllViews();
                JOptionPane.showMessageDialog(this,
                        "Checked in " + res.getGuestName() + " into Room " + res.getRoomNumber() + " (" + res.getBookingId() + ").");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input: " + ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        actions.add(cancelBtn);
        actions.add(confirmBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // -------------------------------------------------------------------------
    // VIEW 3: Itemized Bill Calculator & Checkout
    // -------------------------------------------------------------------------
    private JPanel buildBillingView() {
        JPanel view = new JPanel(new GridLayout(1, 2, 16, 0));
        view.setBackground(BG_CANVAS);
        view.setBorder(new EmptyBorder(8, 20, 20, 20));

        // Left Panel: Controls & Add-Ons
        JPanel leftCard = new JPanel(new BorderLayout(0, 12));
        leftCard.setBackground(BG_SURFACE);
        leftCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(16, 18, 16, 18)));

        JPanel topSelector = new JPanel(new GridLayout(2, 1, 0, 6));
        topSelector.setBackground(BG_SURFACE);
        JLabel selLbl = new JLabel("1. Select Checked-In Guest Reservation:");
        selLbl.setFont(FONT_BOLD);
        billResCombo = new JComboBox<>();
        billResCombo.addActionListener(e -> {
            Reservation r = (Reservation) billResCombo.getSelectedItem();
            if (r != null) {
                gstField.setText(r.getNightlyRate() >= 7500 ? "18" : "12");
                settlementModeCombo.setSelectedItem(r.getPaymentMode());
            }
            updateLiveBillPreview();
        });
        topSelector.add(selLbl);
        topSelector.add(billResCombo);

        // Add-ons Section
        JPanel addonsPanel = new JPanel(new BorderLayout(0, 8));
        addonsPanel.setBackground(BG_SURFACE);
        JLabel addonLbl = new JLabel("2. Itemized Add-On Charges (Room Service, Minibar, Laundry):");
        addonLbl.setFont(FONT_BOLD);

        JPanel addonInputRow = new JPanel(new BorderLayout(8, 0));
        addonInputRow.setBackground(BG_SURFACE);
        JTextField itemDescField = new JTextField("Room Service");
        JTextField itemAmtField = new JTextField("850");
        itemAmtField.setPreferredSize(new Dimension(95, 30));
        JButton addAddonBtn = createSecondaryButton("+ Add");
        addAddonBtn.addActionListener(e -> {
            try {
                String desc = itemDescField.getText().trim();
                double amt = Double.parseDouble(itemAmtField.getText().trim());
                if (!desc.isEmpty() && amt > 0) {
                    currentAddons.add(new AddonItem(desc, amt));
                    addonsTableModel.addRow(new Object[]{desc, String.format("₹%.2f", amt)});
                    itemDescField.setText("");
                    itemAmtField.setText("");
                    updateLiveBillPreview();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid numeric amount for add-on.");
            }
        });

        JPanel rightInputs = new JPanel(new BorderLayout(6, 0));
        rightInputs.setBackground(BG_SURFACE);
        rightInputs.add(itemAmtField, BorderLayout.WEST);
        rightInputs.add(addAddonBtn, BorderLayout.EAST);

        addonInputRow.add(itemDescField, BorderLayout.CENTER);
        addonInputRow.add(rightInputs, BorderLayout.EAST);

        addonsTableModel = new DefaultTableModel(new String[]{"Service Description", "Amount"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable addonsTable = new JTable(addonsTableModel);
        styleTable(addonsTable);
        JScrollPane addonScroll = new JScrollPane(addonsTable);
        addonScroll.setPreferredSize(new Dimension(0, 130));

        JButton clearAddonsBtn = createSecondaryButton("Clear Add-Ons");
        clearAddonsBtn.addActionListener(e -> {
            currentAddons.clear();
            addonsTableModel.setRowCount(0);
            updateLiveBillPreview();
        });

        JPanel addonTop = new JPanel(new BorderLayout(0, 6));
        addonTop.setBackground(BG_SURFACE);
        addonTop.add(addonLbl, BorderLayout.NORTH);
        addonTop.add(addonInputRow, BorderLayout.CENTER);

        addonsPanel.add(addonTop, BorderLayout.NORTH);
        addonsPanel.add(addonScroll, BorderLayout.CENTER);
        addonsPanel.add(clearAddonsBtn, BorderLayout.SOUTH);

        // Tax, Discount & Payment Controls
        JPanel taxGrid = new JPanel(new GridLayout(3, 2, 10, 8));
        taxGrid.setBackground(BG_SURFACE);
        gstField = new JTextField("12");
        discountField = new JTextField("0");
        settlementModeCombo = new JComboBox<>(new String[]{"UPI", "Card", "Cash", "Bank Transfer"});

        gstField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateLiveBillPreview());
        discountField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateLiveBillPreview());

        taxGrid.add(new JLabel("GST / Tax (%):")); taxGrid.add(gstField);
        taxGrid.add(new JLabel("Flat Discount (₹):")); taxGrid.add(discountField);
        taxGrid.add(new JLabel("Settlement Payment Mode:")); taxGrid.add(settlementModeCombo);

        leftCard.add(topSelector, BorderLayout.NORTH);
        leftCard.add(addonsPanel, BorderLayout.CENTER);
        leftCard.add(taxGrid, BorderLayout.SOUTH);

        // Right Panel: Live Tax Invoice Preview & Checkout Action
        JPanel rightCard = new JPanel(new BorderLayout(0, 12));
        rightCard.setBackground(BG_SURFACE);
        rightCard.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(16, 18, 16, 18)));

        JLabel previewHead = new JLabel("Live Tax Invoice Statement");
        previewHead.setFont(FONT_CARD_HEAD);
        previewHead.setForeground(TEXT_PRIMARY);

        liveInvoiceArea = new JTextArea();
        liveInvoiceArea.setEditable(false);
        liveInvoiceArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        liveInvoiceArea.setBackground(BG_CANVAS);
        liveInvoiceArea.setForeground(TEXT_PRIMARY);
        liveInvoiceArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JButton checkoutBtn = createPrimaryButton("Complete Check-Out & Save Receipt File");
        checkoutBtn.addActionListener(e -> handleFinalizeCheckout());

        rightCard.add(previewHead, BorderLayout.NORTH);
        rightCard.add(new JScrollPane(liveInvoiceArea), BorderLayout.CENTER);
        rightCard.add(checkoutBtn, BorderLayout.SOUTH);

        view.add(leftCard);
        view.add(rightCard);
        return view;
    }

    private void updateLiveBillPreview() {
        if (liveInvoiceArea == null || billResCombo == null) return;
        Reservation res = (Reservation) billResCombo.getSelectedItem();
        if (res == null) {
            liveInvoiceArea.setText("No active Checked-In reservations available for billing.\nCreate a new check-in first.");
            return;
        }
        double addonsSum = 0;
        for (AddonItem item : currentAddons) addonsSum += item.getAmount();

        double gst = parseSafeDouble(gstField.getText(), 12.0);
        double disc = parseSafeDouble(discountField.getText(), 0.0);
        double roomCharge = res.getNightlyRate() * res.getNights();
        double subtotal = roomCharge + addonsSum;
        double validDisc = Math.min(subtotal, Math.max(0, disc));
        double taxable = Math.max(0, subtotal - validDisc);
        double gstAmt = (taxable * gst) / 100.0;
        double grandTotal = taxable + gstAmt;
        double balanceDue = Math.max(0, grandTotal - res.getAdvancePaid());

        liveInvoiceArea.setText(String.join("\n",
            "------------------------------------------------------",
            "  SWISS FRONT-DESK HOTEL — LIVE BILL PREVIEW",
            "------------------------------------------------------",
            "Booking Ref   : " + res.getBookingId(),
            "Guest Name    : " + res.getGuestName(),
            "Room          : Room " + res.getRoomNumber() + " (" + res.getRoomType() + ")",
            "Stay Period   : " + res.getCheckIn() + " to " + res.getCheckOut() + " (" + res.getNights() + " nights)",
            "------------------------------------------------------",
            String.format("Room Charges  : ₹%,10.2f", roomCharge),
            String.format("Add-On Extras : ₹%,10.2f (%d item(s))", addonsSum, currentAddons.size()),
            String.format("Subtotal      : ₹%,10.2f", subtotal),
            String.format("Discount      : -₹%,9.2f", validDisc),
            String.format("GST (%.1f%%)   : ₹%,10.2f", gst, gstAmt),
            "------------------------------------------------------",
            String.format("GRAND TOTAL   : ₹%,10.2f", grandTotal),
            String.format("Less Advance  : -₹%,9.2f", res.getAdvancePaid()),
            String.format("BALANCE DUE   : ₹%,10.2f", balanceDue),
            "------------------------------------------------------"
        ));
    }

    private void handleFinalizeCheckout() {
        Reservation res = (Reservation) billResCombo.getSelectedItem();
        if (res == null) return;

        double addonsSum = 0;
        for (AddonItem item : currentAddons) addonsSum += item.getAmount();
        double gst = parseSafeDouble(gstField.getText(), 12.0);
        double disc = parseSafeDouble(discountField.getText(), 0.0);
        String mode = (String) settlementModeCombo.getSelectedItem();

        Bill bill = hotelService.checkoutReservation(res, addonsSum, disc, gst, mode);
        currentAddons.clear();
        addonsTableModel.setRowCount(0);
        discountField.setText("0");
        refreshAllViews();

        JOptionPane.showMessageDialog(this,
                "Checkout completed for " + bill.getGuestName() + "!\n"
                + "Invoice " + bill.getInvoiceId() + " saved to hotel_records/invoices/" + bill.getInvoiceId() + ".txt\n"
                + "Room " + bill.getRoomNumber() + " moved to Housekeeping (Cleaning).",
                "Checkout & File Archive Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // VIEW 4: Saved Records & File Ledger
    // -------------------------------------------------------------------------
    private JPanel buildRecordsView() {
        JPanel view = new JPanel(new BorderLayout(0, 12));
        view.setBackground(BG_CANVAS);
        view.setBorder(new EmptyBorder(8, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_SURFACE);
        topBar.setBorder(new CompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 14, 10, 14)));

        JLabel infoLbl = new JLabel("Finalized Invoices & Archived File Records (./hotel_records/)");
        infoLbl.setFont(FONT_BOLD);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(BG_SURFACE);

        JButton viewReceiptBtn = createSecondaryButton("View Selected Receipt");
        viewReceiptBtn.addActionListener(e -> {
            int row = recordsTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select an invoice row to inspect its formatted receipt.");
                return;
            }
            String invId = (String) recordsTableModel.getValueAt(row, 0);
            for (Bill b : hotelService.getBills()) {
                if (b.getInvoiceId().equals(invId)) {
                    JTextArea area = new JTextArea(b.formatReceiptText());
                    area.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    area.setEditable(false);
                    JOptionPane.showMessageDialog(this, new JScrollPane(area), invId, JOptionPane.PLAIN_MESSAGE);
                    break;
                }
            }
        });

        JButton exportBtn = createPrimaryButton("Export & Sync CSV/TXT Files");
        exportBtn.addActionListener(e -> {
            hotelService.saveAllToDisk();
            JOptionPane.showMessageDialog(this,
                    "Exported:\n1. hotel_records/billing_ledger.csv\n2. hotel_records/hotel_database.txt\n3. Individual receipts in hotel_records/invoices/",
                    "Files Exported Successfully", JOptionPane.INFORMATION_MESSAGE);
        });

        actions.add(viewReceiptBtn);
        actions.add(exportBtn);

        topBar.add(infoLbl, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        view.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Invoice ID", "Booking Ref", "Guest Name", "Room", "Nights", "Grand Total (₹)", "Balance Due (₹)", "Mode"};
        recordsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        recordsTable = new JTable(recordsTableModel);
        styleTable(recordsTable);

        JScrollPane scroll = new JScrollPane(recordsTable);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        view.add(scroll, BorderLayout.CENTER);

        return view;
    }

    // -------------------------------------------------------------------------
    // Master Refresh & Helper Styling Methods
    // -------------------------------------------------------------------------
    private void refreshAllViews() {
        int total = hotelService.getRooms().size();
        int empty = hotelService.getAvailableRooms().size();
        int occupied = 0;
        for (Room r : hotelService.getRooms()) {
            if (r.getStatus() == RoomStatus.OCCUPIED) occupied++;
        }

        kpiTotalVal.setText(String.valueOf(total));
        kpiEmptyVal.setText(String.valueOf(empty));
        kpiOccupiedVal.setText(String.valueOf(occupied));
        kpiRevenueVal.setText(String.format("₹%,.0f", hotelService.getTotalRevenue()));

        rebuildRoomCards();

        // Refresh Reservations Table
        reservationsTableModel.setRowCount(0);
        for (Reservation r : hotelService.getReservations()) {
            reservationsTableModel.addRow(new Object[]{
                    r.getBookingId(), r.getGuestName(), r.getPhone(),
                    "Room " + r.getRoomNumber(), r.getCheckIn(), r.getCheckOut(),
                    String.format("₹%,.0f", r.getAdvancePaid()), r.getStatus()
            });
        }

        // Refresh Billing ComboBox
        billResCombo.removeAllItems();
        for (Reservation r : hotelService.getReservations()) {
            if ("Checked-In".equals(r.getStatus())) {
                billResCombo.addItem(r);
            }
        }
        updateLiveBillPreview();

        // Refresh Records Table
        recordsTableModel.setRowCount(0);
        for (Bill b : hotelService.getBills()) {
            recordsTableModel.addRow(new Object[]{
                    b.getInvoiceId(), b.getBookingId(), b.getGuestName(),
                    "Room " + b.getRoomNumber(), b.getNights(),
                    String.format("₹%,.2f", b.getGrandTotal()),
                    String.format("₹%,.2f", b.getBalanceDue()), b.getPaymentMode()
            });
        }
    }

    private void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_SURFACE);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(new Color(0xEF, 0xF6, 0xFF));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(BG_CANVAS);
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
    }

    private JButton createPrimaryButton(String text) {
        return new StyledButton(text, COBALT_PRIMARY, COBALT_DARK, Color.WHITE, null);
    }

    private JButton createSecondaryButton(String text) {
        return new StyledButton(text, BG_SURFACE, BG_SUBTLE, TEXT_PRIMARY, BORDER_COLOR);
    }

    private JButton createDangerButton(String text) {
        return new StyledButton(text, ROSE_BG, new Color(0xFF, 0xE4, 0xE6), ROSE_FG, ROSE_BORDER);
    }

    // =========================================================================
    // CUSTOM ANTI-ALIASED SWING COMPONENTS (FIXES WINDOWS L&F BUTTON CLIPPING)
    // =========================================================================

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fillColor;
        private final Color borderColor;

        public RoundedPanel(int radius, Color fillColor, Color borderColor) {
            this.radius = radius;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class StyledButton extends JButton {
        private final Color baseBg;
        private final Color hoverBg;
        private final Color borderColor;
        private boolean hovered = false;

        public StyledButton(String text, Color baseBg, Color hoverBg, Color fg, Color borderColor) {
            super(text);
            this.baseBg = baseBg;
            this.hoverBg = hoverBg;
            this.borderColor = borderColor;
            setFont(FONT_BOLD);
            setForeground(fg);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 14, 8, 14));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? hoverBg : baseBg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class NavItemButton extends JButton {
        public final String cardName;
        private boolean selectedNav = false;
        private boolean hovered = false;

        public NavItemButton(String text, String cardName) {
            super(text);
            this.cardName = cardName;
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(FONT_BOLD);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 14, 10, 14));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        public void setSelectedNav(boolean selectedNav) {
            this.selectedNav = selectedNav;
            setForeground(selectedNav ? COBALT_PRIMARY : TEXT_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selectedNav) {
                g2.setColor(COBALT_TINT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            } else if (hovered) {
                g2.setColor(BG_SUBTLE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class FilterPillButton extends JButton {
        public final String filterKey;
        private boolean active = false;
        private boolean hovered = false;

        public FilterPillButton(String text, String filterKey) {
            super(text);
            this.filterKey = filterKey;
            setFont(FONT_BOLD);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(7, 14, 7, 14));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : TEXT_PRIMARY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(COBALT_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            } else {
                g2.setColor(hovered ? BG_SUBTLE : BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    @FunctionalInterface
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            HotelManagementApp app = new HotelManagementApp();
            app.setVisible(true);
        });
    }
}
