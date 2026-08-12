package com.rayan.cinemaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

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
    private Set<Booking> bookings;

    public SeatId getSeatId() {
        return seatId;
    }

    public void setSeatId(SeatId seatId) {
        this.seatId = seatId;
    }

    public Set<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(Set<Booking> bookings) {
        this.bookings = bookings;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
