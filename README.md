# Grand Horizon Hotel Management Web App (Java 21)

A zero-dependency, pure **Java 21** web application for managing hotel operations across **10 rooms (Rooms 101–110)**, featuring live empty room checks, room reservations & lifecycle management, itemized bill calculation, and persistent file storage in both JSON and CSV formats.

## Features

- **Manage Room Reservations (Up to 10 Rooms)**:
  - Pre-configured 10-room inventory (`101–110`) across Standard, Deluxe, Executive Suite, and Presidential Penthouse tiers.
  - Book rooms, prevent double-booking, validate guest capacities & stay dates, edit active reservations, check in, check out, and cancel reservations.
- **Check Empty Rooms**:
  - Real-time availability dashboard (`X / 10 Empty Rooms`) and dedicated filterable empty-room checker (by room type, minimum guests, and maximum nightly rate).
- **Calculate Bills**:
  - Itemized server-side billing engine (`BillingService`) supporting nightly room rates, add-on services (Breakfast, Extra Bed, Airport Pickup, Spa Access, Late Checkout), custom fees, discounts / promo codes (`WEEKEND10`, `VIP20`, `DIRECT5`), and 12% hospitality tax.
  - Includes an interactive Bill Calculator tab and printable guest invoices.
- **Save Record Data to a File**:
  - Automatic atomic persistence to `data/hotel-records.json` and `data/reservations-log.csv` on every state change, plus on-demand **Save Records Now**, **Download JSON**, and **Download CSV** actions.
- **Zero External Dependencies**:
  - Built using standard JDK 21 APIs (`com.sun.net.httpserver.HttpServer`, `java.nio.file`, `java.net.http`)—no Maven downloads or third-party JARs required. Includes `pom.xml` and GitHub Actions CI for standard Java tooling compatibility.

## Project Structure

```text
hotel-management-java/
├── .github/workflows/ci.yml        # GitHub Actions CI build & test workflow
├── data/                           # Saved JSON & CSV hotel records
├── src/
│   ├── main/
│   │   ├── java/com/hotel/
│   │   │   ├── Main.java
│   │   │   ├── model/              # Room, Reservation, BillBreakdown, Status enums
│   │   │   ├── service/            # HotelService, BillingService
│   │   │   ├── storage/            # FileStorageService (JSON & CSV persistence)
│   │   │   ├── util/               # Pure Java JsonUtil parser & serializer
│   │   │   └── web/                # Embedded HttpServer & REST API
│   │   └── resources/public/       # Responsive HTML/CSS/JS frontend
│   └── test/java/com/hotel/
│       └── HotelApplicationTest.java
├── pom.xml
├── run.bat / run.sh                # One-command compile & start scripts
├── test.bat / test.sh              # One-command automated test runners
├── LICENSE
└── .gitignore
```

## Quick Start

### Prerequisites
- **Java 21** (or newer JDK)

### Run the Web App

**Windows:**
```bat
run.bat
```

**macOS / Linux:**
```bash
chmod +x run.sh
./run.sh
```

Then open **http://localhost:8080** in your browser.

Optional flags:
```bash
./run.sh --port=9090 --data-dir=./my-hotel-data
```

### Run Automated Tests

**Windows:**
```bat
test.bat
```

**macOS / Linux:**
```bash
chmod +x test.sh
./test.sh
```

## REST API Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/health` | Health check |
| `GET` | `/api/summary` | Occupancy, revenue, and storage metadata |
| `GET` | `/api/rooms` | List all 10 rooms (optional `?status=` & `?type=`) |
| `GET` | `/api/rooms/empty` | Check empty rooms (optional `?type=`, `?minGuests=`, `?maxRate=`) |
| `GET` | `/api/reservations` | List reservations (`?status=`, `?q=`) |
| `POST` | `/api/reservations` | Create a new reservation and save to disk |
| `PUT` | `/api/reservations/{id}` | Update reservation details and recalculate bill |
| `POST` | `/api/reservations/{id}/checkin` | Check in a reserved guest |
| `POST` | `/api/reservations/{id}/checkout` | Check out guest, mark paid, and free room |
| `POST` | `/api/reservations/{id}/cancel` | Cancel reservation and free room |
| `GET` | `/api/reservations/{id}/bill` | Get itemized bill for a reservation |
| `POST` | `/api/bills/calculate` | Calculate itemized bill preview |
| `POST` | `/api/records/save` | Force-save records to JSON & CSV files |
| `GET` | `/api/records/export/json` | Download `hotel-records.json` |
| `GET` | `/api/records/export/csv` | Download `reservations-log.csv` |

## Pushing to GitHub

This project already has Git initialized and an initial commit ready. To push to your GitHub repository:

```bash
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo-name>.git
git push -u origin main
```
