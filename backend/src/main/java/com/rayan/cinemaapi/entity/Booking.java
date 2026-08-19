package com.rayan.cinemaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"screening_id", "seat_label", "room_id"}
        )
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="screening_id", nullable = false)
    private Screening screening;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "seat_label", referencedColumnName = "seat_label", nullable = false),
            @JoinColumn(name = "room_id", referencedColumnName = "room_id", nullable = false)
    })
    private Seat seat;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Screening getScreening() {
        return screening;
    }

    public void setScreening(Screening screening) {
        if (this.screening != null) {
            this.screening.getBookings().remove(this);
        }
        this.screening = screening;
        screening.getBookings().add(this);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        if (this.user != null) {
            this.user.getBookings().remove(this);
        }
        this.user = user;
        user.getBookings().add(this);
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        if (this.seat != null) {
            this.seat.getBookings().remove(this);
        }
        this.seat = seat;
        seat.getBookings().add(this);
    }
}
