package com.hotel.model;

import com.hotel.util.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Itemized bill breakdown for a hotel stay.
 */
public class BillBreakdown {
    private int roomNumber;
    private String roomType;
    private double nightlyRate;
    private int nights;
    private double roomCharge;
    private List<Map<String, Object>> extraItems = new ArrayList<>();
    private double extrasCharge;
    private double customFee;
    private String customFeeLabel;
    private double subtotal;
    private double discountPercent;
    private String promoCode;
    private double discountAmount;
    private double taxableAmount;
    private double taxRate;
    private double taxAmount;
    private double totalAmount;

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public double getRoomCharge() {
        return roomCharge;
    }

    public void setRoomCharge(double roomCharge) {
        this.roomCharge = roomCharge;
    }

    public List<Map<String, Object>> getExtraItems() {
        return extraItems;
    }

    public void setExtraItems(List<Map<String, Object>> extraItems) {
        this.extraItems = extraItems != null ? new ArrayList<>(extraItems) : new ArrayList<>();
    }

    public double getExtrasCharge() {
        return extrasCharge;
    }

    public void setExtrasCharge(double extrasCharge) {
        this.extrasCharge = extrasCharge;
    }

    public double getCustomFee() {
        return customFee;
    }

    public void setCustomFee(double customFee) {
        this.customFee = customFee;
    }

    public String getCustomFeeLabel() {
        return customFeeLabel;
    }

    public void setCustomFeeLabel(String customFeeLabel) {
        this.customFeeLabel = customFeeLabel;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(double taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roomNumber", roomNumber);
        map.put("roomType", roomType);
        map.put("nightlyRate", nightlyRate);
        map.put("nights", nights);
        map.put("roomCharge", roomCharge);
        map.put("extraItems", extraItems);
        map.put("extrasCharge", extrasCharge);
        map.put("customFee", customFee);
        map.put("customFeeLabel", customFeeLabel);
        map.put("subtotal", subtotal);
        map.put("discountPercent", discountPercent);
        map.put("promoCode", promoCode);
        map.put("discountAmount", discountAmount);
        map.put("taxableAmount", taxableAmount);
        map.put("taxRate", taxRate);
        map.put("taxAmount", taxAmount);
        map.put("totalAmount", totalAmount);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static BillBreakdown fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        BillBreakdown bill = new BillBreakdown();
        bill.setRoomNumber(JsonUtil.asInt(map.get("roomNumber"), 0));
        bill.setRoomType(JsonUtil.asString(map.get("roomType")));
        bill.setNightlyRate(JsonUtil.asDouble(map.get("nightlyRate"), 0.0));
        bill.setNights(JsonUtil.asInt(map.get("nights"), 1));
        bill.setRoomCharge(JsonUtil.asDouble(map.get("roomCharge"), 0.0));
        Object rawItems = map.get("extraItems");
        if (rawItems instanceof List<?> list) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    items.add((Map<String, Object>) m);
                }
            }
            bill.setExtraItems(items);
        }
        bill.setExtrasCharge(JsonUtil.asDouble(map.get("extrasCharge"), 0.0));
        bill.setCustomFee(JsonUtil.asDouble(map.get("customFee"), 0.0));
        bill.setCustomFeeLabel(JsonUtil.asString(map.get("customFeeLabel")));
        bill.setSubtotal(JsonUtil.asDouble(map.get("subtotal"), 0.0));
        bill.setDiscountPercent(JsonUtil.asDouble(map.get("discountPercent"), 0.0));
        bill.setPromoCode(JsonUtil.asString(map.get("promoCode")));
        bill.setDiscountAmount(JsonUtil.asDouble(map.get("discountAmount"), 0.0));
        bill.setTaxableAmount(JsonUtil.asDouble(map.get("taxableAmount"), 0.0));
        bill.setTaxRate(JsonUtil.asDouble(map.get("taxRate"), 0.12));
        bill.setTaxAmount(JsonUtil.asDouble(map.get("taxAmount"), 0.0));
        bill.setTotalAmount(JsonUtil.asDouble(map.get("totalAmount"), 0.0));
        return bill;
    }
}
