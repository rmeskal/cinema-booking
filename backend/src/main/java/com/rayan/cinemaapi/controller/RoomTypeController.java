package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.roomtype.RoomTypeRequest;
import com.rayan.cinemaapi.dto.roomtype.RoomTypeResponse;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public List<RoomTypeResponse> getRoomTypes() {
        return roomTypeService.getRoomTypes()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public RoomTypeResponse getRoomType(@PathVariable Long id) {
        return toResponse(roomTypeService.getRoomType(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponse createRoomType(
            @Valid @RequestBody RoomTypeRequest request
    ) {
        return toResponse(
                roomTypeService.createRoomType(toEntity(request))
        );
    }

    @PutMapping("/{id}")
    public RoomTypeResponse updateRoomType(
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        RoomType roomType = toEntity(request);
        roomType.setId(id);

        return toResponse(roomTypeService.updateRoomType(roomType));
    }

    @DeleteMapping("/{id}")
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