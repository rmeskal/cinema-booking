package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.roomtype.RoomTypeRequest;
import com.rayan.cinemaapi.dto.roomtype.RoomTypeResponse;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-types")
@Tag(name = "Room Types", description = "Manage room types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    @Operation(summary = "Get all room types")
    public List<RoomTypeResponse> getRoomTypes() {
        return roomTypeService.getRoomTypes()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a room type by ID")
    public RoomTypeResponse getRoomType(@PathVariable Long id) {
        return toResponse(roomTypeService.getRoomType(id));
    }

    @PostMapping
    @Operation(summary = "Create a room type")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponse createRoomType(
            @Valid @RequestBody RoomTypeRequest request
    ) {
        return toResponse(
                roomTypeService.createRoomType(toEntity(request))
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a room type")
    public RoomTypeResponse updateRoomType(
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        RoomType roomType = toEntity(request);
        roomType.setId(id);

        return toResponse(roomTypeService.updateRoomType(roomType));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a room type")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoomType(@PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
    }

    private RoomType toEntity(RoomTypeRequest request) {
        RoomType roomType = new RoomType();
        roomType.setName(request.name());

        return roomType;
    }

    private RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getName()
        );
    }
}