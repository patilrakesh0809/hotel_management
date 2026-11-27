package com.hotel.model;

import com.hotel.util.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a guest room reservation and its billing / lifecycle state.
 */
public class Reservation {
    private String id;
    private int roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String checkInDate;
    private String checkOutDate;
    private int nights;
    private int guests;
    private List<String> extras = new ArrayList<>();
    private double customFee;
    private String customFeeLabel;
    private double discountPercent;
    private String promoCode;
    private ReservationStatus status = ReservationStatus.RESERVED;
    private BillBreakdown bill;
    private boolean paid;
    private String notes;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public void setGuestPhone(String guestPhone) {
        this.guestPhone = guestPhone;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public List<String> getExtras() {
        return extras;
    }

    public void setExtras(List<String> extras) {
        this.extras = extras != null ? new ArrayList<>(extras) : new ArrayList<>();
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

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public BillBreakdown getBill() {
        return bill;
    }

    public void setBill(BillBreakdown bill) {
        this.bill = bill;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("roomNumber", roomNumber);
        map.put("guestName", guestName);
        map.put("guestEmail", guestEmail);
        map.put("guestPhone", guestPhone);
        map.put("checkInDate", checkInDate);
        map.put("checkOutDate", checkOutDate);
        map.put("nights", nights);
        map.put("guests", guests);
        map.put("extras", new ArrayList<>(extras));
        map.put("customFee", customFee);
        map.put("customFeeLabel", customFeeLabel);
        map.put("discountPercent", discountPercent);
        map.put("promoCode", promoCode);
        map.put("status", status.name());
        map.put("bill", bill != null ? bill.toMap() : null);
        map.put("paid", paid);
        map.put("notes", notes);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Reservation fromMap(Map<String, Object> map) {
        Reservation res = new Reservation();
        res.setId(JsonUtil.asString(map.get("id")));
        res.setRoomNumber(JsonUtil.asInt(map.get("roomNumber"), 0));
        res.setGuestName(JsonUtil.asString(map.get("guestName")));
        res.setGuestEmail(JsonUtil.asString(map.get("guestEmail")));
        res.setGuestPhone(JsonUtil.asString(map.get("guestPhone")));
        res.setCheckInDate(JsonUtil.asString(map.get("checkInDate")));
        res.setCheckOutDate(JsonUtil.asString(map.get("checkOutDate")));
        res.setNights(JsonUtil.asInt(map.get("nights"), 1));
        res.setGuests(JsonUtil.asInt(map.get("guests"), 1));
        res.setExtras(JsonUtil.asStringList(map.get("extras")));
        res.setCustomFee(JsonUtil.asDouble(map.get("customFee"), 0.0));
        res.setCustomFeeLabel(JsonUtil.asString(map.get("customFeeLabel")));
        res.setDiscountPercent(JsonUtil.asDouble(map.get("discountPercent"), 0.0));
        res.setPromoCode(JsonUtil.asString(map.get("promoCode")));
        String statusStr = JsonUtil.asString(map.get("status"));
        res.setStatus(statusStr != null ? ReservationStatus.valueOf(statusStr) : ReservationStatus.RESERVED);
        Object billObj = map.get("bill");
        if (billObj instanceof Map<?, ?> m) {
            res.setBill(BillBreakdown.fromMap((Map<String, Object>) m));
        }
        res.setPaid(JsonUtil.asBoolean(map.get("paid"), false));
        res.setNotes(JsonUtil.asString(map.get("notes")));
        res.setCreatedAt(JsonUtil.asString(map.get("createdAt")));
        res.setUpdatedAt(JsonUtil.asString(map.get("updatedAt")));
        return res;
    }
}
