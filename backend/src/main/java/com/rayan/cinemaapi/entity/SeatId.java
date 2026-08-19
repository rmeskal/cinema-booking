package com.rayan.cinemaapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@Embeddable
public class SeatId {

    @NotBlank
    @Column(name = "seat_label")
    private String seatLabel;

    @NotNull
    @Column(name = "room_id")
    private Long roomId;

    public SeatId() {
    }

    public SeatId(String seatLabel, Long roomId) {
        this.seatLabel = seatLabel;
        this.roomId = roomId;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SeatId seatId)) return false;
        return Objects.equals(seatLabel, seatId.seatLabel) && Objects.equals(roomId, seatId.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatLabel, roomId);
    }

    @Override
    public String toString() {
        return seatLabel + " in room " + roomId;
    }
}
