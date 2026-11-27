package com.hotel.service;

import com.hotel.model.BillBreakdown;
import com.hotel.model.Room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calculates itemized hotel stay bills including room charges, add-on services,
 * promotional discounts, and hospitality taxes.
 */
public class BillingService {

    public static final double DEFAULT_TAX_RATE = 0.12;

    public record ExtraServiceDef(String code, String label, double price, boolean perNight) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", code);
            map.put("label", label);
            map.put("price", price);
            map.put("perNight", perNight);
            map.put("pricingType", perNight ? "PER_NIGHT" : "FLAT");
            return map;
        }
    }

    private static final Map<String, ExtraServiceDef> EXTRAS_CATALOG = new LinkedHashMap<>();
    private static final Map<String, Double> PROMO_CODES = Map.of(
            "WEEKEND10", 10.0,
            "VIP20", 20.0,
            "DIRECT5", 5.0
    );

    static {
        registerExtra("BREAKFAST", "Breakfast Package", 25.0, true);
        registerExtra("EXTRA_BED", "Extra Rollaway Bed", 35.0, true);
        registerExtra("AIRPORT_PICKUP", "Airport Chauffeur Pickup", 45.0, false);
        registerExtra("SPA_ACCESS", "Thermal Spa & Wellness Pass", 60.0, false);
        registerExtra("LATE_CHECKOUT", "Late Checkout (4:00 PM)", 40.0, false);
    }

    private static void registerExtra(String code, String label, double price, boolean perNight) {
        EXTRAS_CATALOG.put(code, new ExtraServiceDef(code, label, price, perNight));
    }

    public List<Map<String, Object>> getExtrasCatalog() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ExtraServiceDef def : EXTRAS_CATALOG.values()) {
            list.add(def.toMap());
        }
        return list;
    }

    public Map<String, Double> getPromoCodes() {
        return PROMO_CODES;
    }

    public BillBreakdown calculateBill(
            Room room,
            int nights,
            List<String> extras,
            double customFee,
            String customFeeLabel,
            double discountPercent,
            String promoCode
    ) {
        if (room == null) {
            throw new IllegalArgumentException("Room is required for bill calculation");
        }
        if (nights < 1 || nights > 365) {
            throw new IllegalArgumentException("Nights must be between 1 and 365");
        }
        if (customFee < 0) {
            throw new IllegalArgumentException("Custom fee cannot be negative");
        }
        if (discountPercent < 0 || discountPercent > 50) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 50");
        }

        double effectiveDiscount = discountPercent;
        String normalizedPromo = promoCode != null ? promoCode.trim().toUpperCase(Locale.ROOT) : "";
        if (effectiveDiscount == 0 && !normalizedPromo.isEmpty()) {
            Double promoDiscount = PROMO_CODES.get(normalizedPromo);
            if (promoDiscount == null) {
                throw new IllegalArgumentException("Unknown promo code: " + normalizedPromo);
            }
            effectiveDiscount = promoDiscount;
        }

        double roomCharge = round2(room.getNightlyRate() * nights);
        List<Map<String, Object>> extraItems = new ArrayList<>();
        double extrasCharge = 0.0;

        if (extras != null) {
            for (String rawCode : extras) {
                if (rawCode == null || rawCode.isBlank()) {
                    continue;
                }
                String code = rawCode.trim().toUpperCase(Locale.ROOT);
                ExtraServiceDef def = EXTRAS_CATALOG.get(code);
                if (def == null) {
                    throw new IllegalArgumentException("Unsupported extra service: " + code);
                }
                double amount = round2(def.perNight() ? def.price() * nights : def.price());
                extrasCharge = round2(extrasCharge + amount);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", def.code());
                item.put("label", def.label());
                item.put("unitPrice", def.price());
                item.put("pricingType", def.perNight() ? "PER_NIGHT" : "FLAT");
                item.put("quantity", def.perNight() ? nights : 1);
                item.put("amount", amount);
                extraItems.add(item);
            }
        }

        double roundedCustomFee = round2(customFee);
        double subtotal = round2(roomCharge + extrasCharge + roundedCustomFee);
        double discountAmount = round2(subtotal * (effectiveDiscount / 100.0));
        double taxableAmount = round2(Math.max(0.0, subtotal - discountAmount));
        double taxAmount = round2(taxableAmount * DEFAULT_TAX_RATE);
        double totalAmount = round2(taxableAmount + taxAmount);

        BillBreakdown bill = new BillBreakdown();
        bill.setRoomNumber(room.getRoomNumber());
        bill.setRoomType(room.getType());
        bill.setNightlyRate(room.getNightlyRate());
        bill.setNights(nights);
        bill.setRoomCharge(roomCharge);
        bill.setExtraItems(extraItems);
        bill.setExtrasCharge(extrasCharge);
        bill.setCustomFee(roundedCustomFee);
        bill.setCustomFeeLabel(customFeeLabel != null && !customFeeLabel.isBlank() ? customFeeLabel.trim() : "Misc Charges");
        bill.setSubtotal(subtotal);
        bill.setDiscountPercent(round2(effectiveDiscount));
        bill.setPromoCode(normalizedPromo.isEmpty() ? null : normalizedPromo);
        bill.setDiscountAmount(discountAmount);
        bill.setTaxableAmount(taxableAmount);
        bill.setTaxRate(DEFAULT_TAX_RATE);
        bill.setTaxAmount(taxAmount);
        bill.setTotalAmount(totalAmount);
        return bill;
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
