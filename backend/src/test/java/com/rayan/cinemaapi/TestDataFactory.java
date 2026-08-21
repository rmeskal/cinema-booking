package com.rayan.cinemaapi;

import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;

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

    public static Room createRoom(String name, RoomType roomType) {
        Room room = new Room();
        room.setName(name);
        room.setRoomType(roomType);
        return room;
    }
}