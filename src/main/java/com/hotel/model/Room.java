package com.hotel.model;

import com.hotel.util.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents one of the hotel's rooms (up to 10 rooms supported).
 */
public class Room {
    private int roomNumber;
    private String type;
    private double nightlyRate;
    private int maxGuests;
    private List<String> amenities;
    private RoomStatus status;
    private String activeReservationId;

    public Room() {
        this.amenities = new ArrayList<>();
        this.status = RoomStatus.AVAILABLE;
    }

    public Room(int roomNumber, String type, double nightlyRate, int maxGuests, List<String> amenities) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.nightlyRate = nightlyRate;
        this.maxGuests = maxGuests;
        this.amenities = new ArrayList<>(amenities);
        this.status = RoomStatus.AVAILABLE;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities != null ? new ArrayList<>(amenities) : new ArrayList<>();
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public String getActiveReservationId() {
        return activeReservationId;
    }

    public void setActiveReservationId(String activeReservationId) {
        this.activeReservationId = activeReservationId;
    }

    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roomNumber", roomNumber);
        map.put("type", type);
        map.put("nightlyRate", nightlyRate);
        map.put("maxGuests", maxGuests);
        map.put("amenities", new ArrayList<>(amenities));
        map.put("status", status.name());
        map.put("activeReservationId", activeReservationId);
        return map;
    }

    public static Room fromMap(Map<String, Object> map) {
        Room room = new Room();
        room.setRoomNumber(JsonUtil.asInt(map.get("roomNumber"), 0));
        room.setType(JsonUtil.asString(map.get("type")));
        room.setNightlyRate(JsonUtil.asDouble(map.get("nightlyRate"), 0.0));
        room.setMaxGuests(JsonUtil.asInt(map.get("maxGuests"), 1));
        room.setAmenities(JsonUtil.asStringList(map.get("amenities")));
        String statusStr = JsonUtil.asString(map.get("status"));
        room.setStatus(statusStr != null ? RoomStatus.valueOf(statusStr) : RoomStatus.AVAILABLE);
        room.setActiveReservationId(JsonUtil.asString(map.get("activeReservationId")));
        return room;
    }
}
