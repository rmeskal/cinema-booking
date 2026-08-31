package com.rayan.cinemaapi;

import com.rayan.cinemaapi.entity.*;

import java.time.LocalDateTime;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Movie createMovie() {
        return createMovie("Test Movie", "A test movie", 120);
    }

    public static Movie createMovie(
            String title,
            String description,
            int durationInMinutes
    ) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDurationInMinutes(durationInMinutes);
        movie.setThumbnailUrl("https://example.com/movie.jpg");
        return movie;
    }

    public static RoomType createRoomType() {
        return createRoomType("Standard");
    }

    public static RoomType createRoomType(String name) {
        RoomType roomType = new RoomType();
        roomType.setName(name);
        return roomType;
    }

    public static Room createRoom(RoomType roomType) {
        return createRoom("Test Room", roomType);
    }

    public static Room createRoom(
            String name,
            RoomType roomType
    ) {
        Room room = new Room();
        room.setName(name);
        room.setRoomType(roomType);
        return room;
    }

    public static Screening createScreening(
            Movie movie,
            Room room
    ) {
        return createScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 17, 20, 0)
        );
    }

    public static Screening createScreening(
            Movie movie,
            Room room,
            LocalDateTime startTime
    ) {
        Screening screening = new Screening();
        screening.setMovie(movie);
        screening.setRoom(room);
        screening.setStartTime(startTime);
        screening.setPriceInCents(1000);
        return screening;
    }

    public static Seat createSeat(
            Room room,
            String seatLabel
    ) {
        Seat seat = new Seat();
        seat.setSeatId(new SeatId(
                seatLabel,
                room.getId()
        ));
        seat.setRoom(room);
        return seat;
    }

    public static User createUser() {
        return createUser(
                "Test",
                "User",
                "test@example.com",
                "hashed-password",
                Role.USER
        );
    }

    public static User createUser(
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            Role role
    ) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }

    public static Booking createBooking(
            Screening screening,
            User user,
            Seat seat
    ) {
        Booking booking = new Booking();
        booking.setScreening(screening);
        booking.setUser(user);
        booking.setSeat(seat);
        return booking;
    }
}