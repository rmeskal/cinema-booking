package com.rayan.cinemaapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.HashSet;
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
    private Set<Booking> bookings = new HashSet<>();

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
        if (this.movie != null) {
            this.movie.getScreenings().remove(this);
        }
        this.movie = movie;
        movie.getScreenings().add(this);
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        if (this.room != null) {
            this.room.getScreenings().remove(this);
        }
        this.room = room;
        room.getScreenings().add(this);
    }

    public Set<Booking> getBookings() {
        return bookings;
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
