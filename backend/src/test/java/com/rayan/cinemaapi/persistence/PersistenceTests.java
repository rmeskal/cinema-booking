package com.rayan.cinemaapi.persistence;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PersistenceTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Validator validator;

    @Test
    @Transactional
    void shouldPersistAndRetrieveRoomType() {
        RoomType roomType = TestDataFactory.createRoomType("IMAX");

        entityManager.persist(roomType);
        entityManager.flush();
        entityManager.clear();

        RoomType retrieved = entityManager.find(RoomType.class, roomType.getId());

        assertEquals("IMAX", retrieved.getName());
    }

    @Test
    @Transactional
    void shouldPersistRoomWithRoomType() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room = TestDataFactory.createRoom(roomType);

        entityManager.persist(room);
        entityManager.flush();
        entityManager.clear();

        Room retrievedRoom = entityManager.find(Room.class, room.getId());

        assertEquals(roomType.getId(), retrievedRoom.getRoomType().getId());
        assertEquals("Standard", retrievedRoom.getRoomType().getName());
    }

    @Test
    @Transactional
    void shouldPersistSeatWithRoom() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room = TestDataFactory.createRoom(roomType);
        entityManager.persist(room);

        SeatId seatId = new SeatId();
        seatId.setSeatLabel("A1");
        seatId.setRoomId(room.getId());

        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setRoom(room);

        entityManager.persist(seat);
        entityManager.flush();
        entityManager.clear();

        Seat retrievedSeat = entityManager.find(Seat.class, seatId);

        assertEquals("A1", retrievedSeat.getSeatId().getSeatLabel());
        assertEquals(room.getId(), retrievedSeat.getSeatId().getRoomId());
        assertEquals(room.getId(), retrievedSeat.getRoom().getId());
    }

    @Test
    @Transactional
    void shouldPersistMovie() {
        Movie movie = TestDataFactory.createMovie(
                "Test Movie",
                "A test movie",
                120
        );

        entityManager.persist(movie);
        entityManager.flush();
        entityManager.clear();

        Movie retrieved = entityManager.find(Movie.class, movie.getId());

        assertEquals("A test movie", retrieved.getDescription());
        assertEquals(120, retrieved.getDurationInMinutes());
        assertEquals(
                "https://example.com/movie.jpg",
                retrieved.getThumbnailUrl()
        );
    }

    @Test
    @Transactional
    void shouldPersistScreeningWithMovieAndRoom() {
        Movie movie = TestDataFactory.createMovie(
                "Test Movie",
                "Test Movie",
                120
        );
        entityManager.persist(movie);

        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room = TestDataFactory.createRoom(roomType);
        entityManager.persist(room);

        Screening screening = TestDataFactory.createScreening(movie, room);
        screening.setStartTime(
                LocalDateTime.of(2026, 8, 12, 20, 0)
        );
        screening.setPriceInCents(1200);

        entityManager.persist(screening);
        entityManager.flush();
        entityManager.clear();

        Screening retrieved = entityManager.find(
                Screening.class,
                screening.getId()
        );

        assertEquals(movie.getId(), retrieved.getMovie().getId());
        assertEquals(room.getId(), retrieved.getRoom().getId());
        assertEquals(
                LocalDateTime.of(2026, 8, 12, 20, 0),
                retrieved.getStartTime()
        );
        assertEquals(1200, retrieved.getPriceInCents());
    }

    @Test
    @Transactional
    void shouldPersistUser() {
        User user = TestDataFactory.createUser(
                "John",
                "Doe",
                "john.doe@test.com",
                "hashed-password",
                Role.USER
        );

        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        User retrieved = entityManager.find(User.class, user.getId());

        assertEquals("John", retrieved.getFirstName());
        assertEquals("Doe", retrieved.getLastName());
        assertEquals("john.doe@test.com", retrieved.getEmail());
        assertEquals("hashed-password", retrieved.getPasswordHash());
        assertEquals(Role.USER, retrieved.getRole());
    }

    @Test
    @Transactional
    void shouldPersistBookingWithUserScreeningAndSeat() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room = TestDataFactory.createRoom(roomType);
        entityManager.persist(room);

        SeatId seatId = new SeatId();
        seatId.setSeatLabel("A1");
        seatId.setRoomId(room.getId());

        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setRoom(room);
        entityManager.persist(seat);

        Movie movie = TestDataFactory.createMovie(
                "Test Movie",
                "Test Movie",
                120
        );
        entityManager.persist(movie);

        Screening screening = TestDataFactory.createScreening(movie, room);
        screening.setStartTime(
                LocalDateTime.of(2026, 8, 12, 20, 0)
        );
        screening.setPriceInCents(1200);
        entityManager.persist(screening);

        User user = TestDataFactory.createUser(
                "John",
                "Doe",
                "john.booking@test.com",
                "hashed-password",
                Role.USER
        );
        entityManager.persist(user);

        Booking booking = TestDataFactory.createBooking(
                screening,
                user,
                seat
        );

        entityManager.persist(booking);
        entityManager.flush();
        entityManager.clear();

        Booking retrieved = entityManager.find(
                Booking.class,
                booking.getId()
        );

        assertEquals(screening.getId(), retrieved.getScreening().getId());
        assertEquals(user.getId(), retrieved.getUser().getId());
        assertEquals(
                "A1",
                retrieved.getSeat().getSeatId().getSeatLabel()
        );
        assertEquals(
                room.getId(),
                retrieved.getSeat().getSeatId().getRoomId()
        );
    }

    @Test
    @Transactional
    void shouldAllowSameSeatLabelInDifferentRooms() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room1 = TestDataFactory.createRoom("Room 1", roomType);
        entityManager.persist(room1);

        Room room2 = TestDataFactory.createRoom("Room 2", roomType);
        entityManager.persist(room2);

        SeatId seatId1 = new SeatId();
        seatId1.setSeatLabel("A1");
        seatId1.setRoomId(room1.getId());

        Seat seat1 = new Seat();
        seat1.setSeatId(seatId1);
        seat1.setRoom(room1);
        entityManager.persist(seat1);

        SeatId seatId2 = new SeatId();
        seatId2.setSeatLabel("A1");
        seatId2.setRoomId(room2.getId());

        Seat seat2 = new Seat();
        seat2.setSeatId(seatId2);
        seat2.setRoom(room2);
        entityManager.persist(seat2);

        entityManager.flush();

        Seat retrieved1 = entityManager.find(Seat.class, seatId1);
        Seat retrieved2 = entityManager.find(Seat.class, seatId2);

        assertNotNull(retrieved1);
        assertNotNull(retrieved2);

        assertEquals(room1.getId(), retrieved1.getRoom().getId());
        assertEquals(room2.getId(), retrieved2.getRoom().getId());
    }

    @Test
    @Transactional
    void shouldRejectDuplicateRoomTypeName() {
        RoomType first = TestDataFactory.createRoomType("IMAX");

        entityManager.persist(first);
        entityManager.flush();
        entityManager.clear();

        RoomType duplicate = TestDataFactory.createRoomType("IMAX");

        assertThrows(
                PersistenceException.class,
                () -> {
                    entityManager.persist(duplicate);
                    entityManager.flush();
                }
        );
    }

    @Test
    @Transactional
    void shouldRejectDuplicateUserEmail() {
        User first = TestDataFactory.createUser(
                "John",
                "Doe",
                "duplicate@test.com",
                "hashed-password",
                Role.USER
        );

        entityManager.persist(first);
        entityManager.flush();
        entityManager.clear();

        User duplicate = TestDataFactory.createUser(
                "Jane",
                "Doe",
                "duplicate@test.com",
                "hashed-password",
                Role.USER
        );

        assertThrows(
                PersistenceException.class,
                () -> {
                    entityManager.persist(duplicate);
                    entityManager.flush();
                }
        );
    }

    @Test
    @Transactional
    void shouldRejectTwoScreeningsInSameRoomAtSameTime() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room = TestDataFactory.createRoom(roomType);
        entityManager.persist(room);

        Movie movie1 = TestDataFactory.createMovie(
                "Movie 1",
                "Movie 1",
                120
        );
        entityManager.persist(movie1);

        Movie movie2 = TestDataFactory.createMovie(
                "Movie 2",
                "Movie 2",
                100
        );
        entityManager.persist(movie2);

        LocalDateTime startTime =
                LocalDateTime.of(2026, 8, 12, 20, 0);

        Screening first = TestDataFactory.createScreening(
                movie1,
                room,
                startTime
        );
        first.setPriceInCents(1000);

        entityManager.persist(first);
        entityManager.flush();

        Screening duplicate = TestDataFactory.createScreening(
                movie2,
                room,
                startTime
        );
        duplicate.setPriceInCents(1200);

        assertThrows(
                PersistenceException.class,
                () -> {
                    entityManager.persist(duplicate);
                    entityManager.flush();
                }
        );
    }

    @Test
    @Transactional
    void shouldRejectDoubleBooking() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        entityManager.persist(roomType);

        Room room = TestDataFactory.createRoom(roomType);
        entityManager.persist(room);

        SeatId seatId = new SeatId();
        seatId.setSeatLabel("A1");
        seatId.setRoomId(room.getId());

        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setRoom(room);
        entityManager.persist(seat);

        Movie movie = TestDataFactory.createMovie();
        entityManager.persist(movie);

        Screening screening = TestDataFactory.createScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 12, 20, 0)
        );
        screening.setPriceInCents(1200);
        entityManager.persist(screening);

        User user1 = TestDataFactory.createUser(
                "John",
                "Doe",
                "user1@test.com",
                "hashed-password",
                Role.USER
        );
        entityManager.persist(user1);

        User user2 = TestDataFactory.createUser(
                "Jane",
                "Doe",
                "user2@test.com",
                "hashed-password",
                Role.USER
        );
        entityManager.persist(user2);

        Booking first = TestDataFactory.createBooking(
                screening,
                user1,
                seat
        );

        entityManager.persist(first);
        entityManager.flush();

        Booking duplicate = TestDataFactory.createBooking(
                screening,
                user2,
                seat
        );

        assertThrows(
                PersistenceException.class,
                () -> {
                    entityManager.persist(duplicate);
                    entityManager.flush();
                }
        );
    }

    @Test
    void shouldRejectMovieWithNullDuration() {
        Movie movie = new Movie();
        movie.setDurationInMinutes(null);

        Set<ConstraintViolation<Movie>> violations =
                validator.validate(movie);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("durationInMinutes"))
        );
    }

    @Test
    void shouldRejectMovieWithZeroDuration() {
        Movie movie = new Movie();
        movie.setDurationInMinutes(0);

        Set<ConstraintViolation<Movie>> violations =
                validator.validate(movie);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("durationInMinutes"))
        );
    }

    @Test
    void shouldRejectMovieWithNegativeDuration() {
        Movie movie = new Movie();
        movie.setDurationInMinutes(-10);

        Set<ConstraintViolation<Movie>> violations =
                validator.validate(movie);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("durationInMinutes"))
        );
    }

    @Test
    void shouldRejectRoomWithNullRoomType() {
        Room room = new Room();

        Set<ConstraintViolation<Room>> violations =
                validator.validate(room);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("roomType"))
        );
    }

    @Test
    void shouldRejectScreeningWithNullStartTime() {
        Screening screening = new Screening();

        Set<ConstraintViolation<Screening>> violations =
                validator.validate(screening);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("startTime"))
        );
    }

    @Test
    void shouldRejectScreeningWithNullPrice() {
        Screening screening = new Screening();

        Set<ConstraintViolation<Screening>> violations =
                validator.validate(screening);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("priceInCents"))
        );
    }

    @Test
    void shouldRejectScreeningWithNegativePrice() {
        Screening screening = new Screening();
        screening.setPriceInCents(-1);

        Set<ConstraintViolation<Screening>> violations =
                validator.validate(screening);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("priceInCents"))
        );
    }

    @Test
    void shouldRejectRoomTypeWithBlankName() {
        RoomType roomType = new RoomType();
        roomType.setName("");

        Set<ConstraintViolation<RoomType>> violations =
                validator.validate(roomType);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("name"))
        );
    }

    @Test
    void shouldRejectUserWithBlankFirstName() {
        User user = TestDataFactory.createUser();
        user.setFirstName("");

        Set<ConstraintViolation<User>> violations =
                validator.validate(user);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("firstName"))
        );
    }

    @Test
    void shouldRejectUserWithBlankLastName() {
        User user = TestDataFactory.createUser();
        user.setLastName("");

        Set<ConstraintViolation<User>> violations =
                validator.validate(user);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("lastName"))
        );
    }

    @Test
    void shouldRejectUserWithBlankEmail() {
        User user = TestDataFactory.createUser();
        user.setEmail("");

        Set<ConstraintViolation<User>> violations =
                validator.validate(user);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("email"))
        );
    }

    @Test
    void shouldRejectUserWithInvalidEmail() {
        User user = TestDataFactory.createUser();
        user.setEmail("not-an-email");

        Set<ConstraintViolation<User>> violations =
                validator.validate(user);

        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getPropertyPath().toString()
                                .equals("email"))
        );
    }

    @Test
    void shouldAcceptValidMovieDuration() {
        Movie movie = TestDataFactory.createMovie();

        Set<ConstraintViolation<Movie>> violations =
                validator.validate(movie);

        assertTrue(
                violations.stream()
                        .noneMatch(v -> v.getPropertyPath().toString()
                                .equals("durationInMinutes"))
        );
    }

    @Test
    void shouldAcceptValidUserEmail() {
        User user = TestDataFactory.createUser();
        user.setEmail("john.doe@example.com");

        Set<ConstraintViolation<User>> violations =
                validator.validate(user);

        assertTrue(
                violations.stream()
                        .noneMatch(v -> v.getPropertyPath().toString()
                                .equals("email"))
        );
    }
}