package com.rayan.cinemaapi.persistence;

import com.rayan.cinemaapi.entity.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidirectionalRelationshipTests {

    @Test
    void settingMovieUpdatesBothSides() {
        Movie movie = new Movie();
        Screening screening = new Screening();

        screening.setMovie(movie);

        assertSame(movie, screening.getMovie());
        assertTrue(movie.getScreenings().contains(screening));
    }

    @Test
    void changingMovieUpdatesBothSides() {
        Movie oldMovie = new Movie();
        Movie newMovie = new Movie();
        Screening screening = new Screening();

        screening.setMovie(oldMovie);
        screening.setMovie(newMovie);

        assertSame(newMovie, screening.getMovie());
        assertFalse(oldMovie.getScreenings().contains(screening));
        assertTrue(newMovie.getScreenings().contains(screening));
    }

    @Test
    void settingRoomOnScreeningUpdatesBothSides() {
        Room room = new Room();
        Screening screening = new Screening();

        screening.setRoom(room);

        assertSame(room, screening.getRoom());
        assertTrue(room.getScreenings().contains(screening));
    }

    @Test
    void changingRoomOnScreeningUpdatesBothSides() {
        Room oldRoom = new Room();
        Room newRoom = new Room();
        Screening screening = new Screening();

        screening.setRoom(oldRoom);
        screening.setRoom(newRoom);

        assertSame(newRoom, screening.getRoom());
        assertFalse(oldRoom.getScreenings().contains(screening));
        assertTrue(newRoom.getScreenings().contains(screening));
    }

    @Test
    void settingRoomOnSeatUpdatesBothSides() {
        Room room = new Room();
        Seat seat = new Seat();

        seat.setRoom(room);

        assertSame(room, seat.getRoom());
        assertTrue(room.getSeats().contains(seat));
    }

    @Test
    void changingRoomOnSeatUpdatesBothSides() {
        Room oldRoom = new Room();
        Room newRoom = new Room();
        Seat seat = new Seat();

        seat.setRoom(oldRoom);
        seat.setRoom(newRoom);

        assertSame(newRoom, seat.getRoom());
        assertFalse(oldRoom.getSeats().contains(seat));
        assertTrue(newRoom.getSeats().contains(seat));
    }

    @Test
    void settingScreeningOnBookingUpdatesBothSides() {
        Screening screening = new Screening();
        Booking booking = new Booking();

        booking.setScreening(screening);

        assertSame(screening, booking.getScreening());
        assertTrue(screening.getBookings().contains(booking));
    }

    @Test
    void changingScreeningOnBookingUpdatesBothSides() {
        Screening oldScreening = new Screening();
        Screening newScreening = new Screening();
        Booking booking = new Booking();

        booking.setScreening(oldScreening);
        booking.setScreening(newScreening);

        assertSame(newScreening, booking.getScreening());
        assertFalse(oldScreening.getBookings().contains(booking));
        assertTrue(newScreening.getBookings().contains(booking));
    }

    @Test
    void settingUserOnBookingUpdatesBothSides() {
        User user = new User();
        Booking booking = new Booking();

        booking.setUser(user);

        assertSame(user, booking.getUser());
        assertTrue(user.getBookings().contains(booking));
    }

    @Test
    void changingUserOnBookingUpdatesBothSides() {
        User oldUser = new User();
        User newUser = new User();
        Booking booking = new Booking();

        booking.setUser(oldUser);
        booking.setUser(newUser);

        assertSame(newUser, booking.getUser());
        assertFalse(oldUser.getBookings().contains(booking));
        assertTrue(newUser.getBookings().contains(booking));
    }

    @Test
    void settingSeatOnBookingUpdatesBothSides() {
        Seat seat = new Seat();
        Booking booking = new Booking();

        booking.setSeat(seat);

        assertSame(seat, booking.getSeat());
        assertTrue(seat.getBookings().contains(booking));
    }

    @Test
    void changingSeatOnBookingUpdatesBothSides() {
        Seat oldSeat = new Seat();
        Seat newSeat = new Seat();
        Booking booking = new Booking();

        booking.setSeat(oldSeat);
        booking.setSeat(newSeat);

        assertSame(newSeat, booking.getSeat());
        assertFalse(oldSeat.getBookings().contains(booking));
        assertTrue(newSeat.getBookings().contains(booking));
    }
}