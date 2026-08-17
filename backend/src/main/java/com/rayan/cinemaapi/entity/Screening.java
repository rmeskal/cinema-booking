package com.rayan.cinemaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"room_id", "start_time"}
        ),
        name = "screenings"
)

public class Screening {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="movie_id", nullable = false)
    @NotNull
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="room_id", nullable = false)
    @NotNull
    private Room room;

    @OneToMany(mappedBy = "screening", fetch = FetchType.LAZY)
    private Set<Booking> bookings;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer priceInCents;

    @Transient
    public LocalDateTime getEndTime() {
        return startTime.plusMinutes(movie.getDurationInMinutes());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Set<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(Set<Booking> bookings) {
        this.bookings = bookings;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getPriceInCents() {
        return priceInCents;
    }

    public void setPriceInCents(Integer priceInCents) {
        this.priceInCents = priceInCents;
    }
}
