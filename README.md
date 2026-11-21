[project_readme.md](https://github.com/user-attachments/files/35208804/project_readme.md)
# Swiss Front-Desk — Hotel Management Application

A clean, responsive, single-file **Hotel Management System (PMS)** built with a **Minimalism & Swiss Grid** interface. Designed for managing a 10-room boutique property, tracking real-time room availability, handling immediate and future reservations, calculating itemized guest bills, and exporting/importing persistent record files.

---

## Key Features

### 1. 10-Room Overview & Empty Room Checker
- **Live KPI Strip:** Displays Total Rooms (`10`), Empty / Available Rooms, Occupied Rooms, and Total Revenue Collected.
- **Instant Filtering:** Filter rooms by status (`All 10`, `Empty Only`, `Occupied`, `Cleaning`, `Maintenance`), room category (`Standard`, `Deluxe`, `Executive Suite`, `Penthouse Suite`), or search by room number and guest name.
- **Housekeeping Workflow:** Checking out a guest automatically transitions the room to **Cleaning** before returning it to **Available**, preventing uncleaned rooms from being assigned.
- **7-Day Analytics & Shift Log:** Built-in 7-day revenue and occupancy bar chart paired with a timestamped shift handover audit trail.

### 2. Reservations Manager
- **Immediate & Future Bookings:** Book available rooms for same-day check-in or schedule **Upcoming** future reservations with automatic date-range overlap detection.
- **Advance Deposit & Payment Tracking:** Record advance/security deposits and payment methods (`UPI`, `Card`, `Cash`, `Bank Transfer`) at check-in.
- **Booking Ledger:** Filter and search active, upcoming, checked-out, and cancelled reservations with one-click check-in, billing, and cancellation actions.

### 3. Itemized Bill Calculator & Printable Invoice
- **Automatic Stay & Tariff Calculation:** Computes nightly room charges plus quick-add or custom extra services (Room Service, Minibar, Laundry, Airport Transfer, Extra Bed).
- **Smart GST Slabs & Discounts:** Automatically applies configurable GST slabs based on room rate (default `12%` / `18%`), supports percentage or flat discounts, and deducts advance deposits to show the exact **Balance Due**.
- **Print / Save PDF Receipt:** Dedicated `@media print` stylesheet outputs a clean, formal tax invoice.

### 4. Records & File Export / Import
- **Multi-Format File Export:** Download billing ledgers and property snapshots as **CSV** (Excel-compatible), **JSON** (full backup), or **TXT** (formatted summary report).
- **JSON Restore & Persistence:** Automatically saves state in `localStorage` and supports restoring full data from exported `.json` backup files.
- **Property & Rate Settings:** Customize hotel name, address, GSTIN, tax thresholds, display currency (`INR ₹` / `USD $`), and individual nightly rates for all 10 rooms.

---

## Tech Stack

- **Frontend:** HTML5, Vanilla JavaScript (ES6+), Tailwind CSS (CDN)
- **Typography:** Plus Jakarta Sans, Inter, JetBrains Mono (Google Fonts)
- **Icons:** Lucide Icons
- **Storage & File I/O:** Browser `localStorage` + Blob API (`CSV`, `JSON`, `TXT` export & `FileReader` import)

---

## Getting Started

Because the entire application is self-contained in a single HTML file, no build tools or package installations are required:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YOUR-USERNAME/hotel-management-app.git
   cd hotel-management-app
   ```
2. **Run locally:**
   Open `index.html` directly in any modern web browser (Chrome, Edge, Firefox, Safari).

---

## Project Structure

```text
hotel-management-app/
├── index.html      # Complete self-contained Hotel Management Application
└── README.md       # Project documentation
```

---

## Default Room Configuration

| Room Numbers | Category | Floor | Default Rate (INR) |
| :--- | :--- | :---: | :---: |
| `101` – `104` | Standard Room | 1 | ₹2,500 / night |
| `105` – `107` | Deluxe Room | 2 | ₹4,200 / night |
| `108` – `109` | Executive Suite | 3 | ₹6,800 / night |
| `110` | Penthouse Suite | 3 | ₹9,500 / night |

*(All room rates, GST percentages, and currency display settings can be edited live inside the **Rates & Settings** panel.)*
