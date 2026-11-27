package com.hotel;

import com.hotel.service.BillingService;
import com.hotel.service.HotelService;
import com.hotel.storage.FileStorageService;
import com.hotel.web.HotelServer;

import java.nio.file.Path;

/**
 * Entry point for the Grand Horizon Hotel Management Web Application.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = resolvePort(args);
        Path dataDir = resolveDataDir(args);

        BillingService billingService = new BillingService();
        FileStorageService storageService = new FileStorageService(dataDir);
        HotelService hotelService = new HotelService(billingService, storageService);
        HotelServer server = new HotelServer(hotelService, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                hotelService.saveRecordsNow();
            } catch (Exception ignored) {
            }
            server.stop(1);
        }));

        server.start();
        System.out.println("===================================================");
        System.out.println("  Grand Horizon Hotel Management Web App (Java 21)");
        System.out.println("  URL:      http://localhost:" + server.getPort());
        System.out.println("  Data Dir: " + storageService.getDataDir());
        System.out.println("===================================================");
    }

    private static int resolvePort(String[] args) {
        String envPort = System.getenv("PORT");
        int port = (envPort != null && !envPort.isBlank()) ? Integer.parseInt(envPort.trim()) : 8080;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--port=")) {
                port = Integer.parseInt(args[i].substring("--port=".length()));
            } else if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }
        return port;
    }

    private static Path resolveDataDir(String[] args) {
        Path dir = Path.of("data");
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--data-dir=")) {
                dir = Path.of(args[i].substring("--data-dir=".length()));
            } else if ("--data-dir".equals(args[i]) && i + 1 < args.length) {
                dir = Path.of(args[++i]);
            }
        }
        return dir;
    }
}
