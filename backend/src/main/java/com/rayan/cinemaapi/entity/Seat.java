package com.rayan.cinemaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "seats")
public class Seat {

    @EmbeddedId
    private SeatId seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @NotNull
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @OneToMany(mappedBy = "seat", fetch = FetchType.LAZY)
    private Set<Booking> bookings = new HashSet<>();

    public SeatId getSeatId() {
        return seatId;
    }

    public void setSeatId(SeatId seatId) {
        this.seatId = seatId;
    }

    public Set<Booking> getBookings() {
        return bookings;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        if (this.room != null) {
            this.room.getSeats().remove(this);
        }
        this.room = room;
        room.getSeats().add(this);
    }
}
