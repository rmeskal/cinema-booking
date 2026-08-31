package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.room.RoomRequest;
import com.rayan.cinemaapi.dto.room.RoomResponse;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Manage rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @Operation(summary = "Get all rooms")
    public List<RoomResponse> getRooms() {
        return roomService.getRooms()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a room by ID")
    public RoomResponse getRoom(@PathVariable Long id) {
        return toResponse(roomService.getRoom(id));
    }

    @PostMapping
    @Operation(summary = "Create a room")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody RoomRequest request) {
        return toResponse(roomService.createRoom(toEntity(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a room")
    public RoomResponse updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        Room room = toEntity(request);
        room.setId(id);

        return toResponse(roomService.updateRoom(room));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a room")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
    }

    private Room toEntity(RoomRequest request) {
        Room room = new Room();
        room.setName(request.name());

        RoomType roomType = new RoomType();
        roomType.setId(request.roomTypeId());
        room.setRoomType(roomType);

        return room;
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getRoomType().getName()
        );
    }
}