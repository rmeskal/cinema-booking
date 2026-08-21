package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
class RoomControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    @Test
    void getRooms_returnsRooms() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        Room room = new Room();
        room.setId(1L);
        room.setName("Room 1");
        room.setRoomType(roomType);

        when(roomService.getRooms())
                .thenReturn(List.of(room));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Room 1"))
                .andExpect(jsonPath("$[0].roomTypeName").value("IMAX"));
    }

    @Test
    void getRoom_returnsRoomWhenFound() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        Room room = new Room();
        room.setId(1L);
        room.setName("Room 1");
        room.setRoomType(roomType);

        when(roomService.getRoom(1L))
                .thenReturn(room);

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Room 1"))
                .andExpect(jsonPath("$.roomTypeName").value("IMAX"));
    }

    @Test
    void getRoom_returns404WhenNotFound() throws Exception {
        when(roomService.getRoom(1L))
                .thenThrow(new EntityNotFoundException("Room", 1L));

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRoom_returns201() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        Room room = new Room();
        room.setId(1L);
        room.setName("Room 1");
        room.setRoomType(roomType);

        when(roomService.createRoom(any(Room.class)))
                .thenReturn(room);

        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": 1
                }
                """;

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Room 1"))
                .andExpect(jsonPath("$.roomTypeName").value("IMAX"));

        verify(roomService).createRoom(argThat(roomArg ->
                roomArg.getName().equals("Room 1")
                        && roomArg.getRoomType().getId().equals(1L)
        ));
    }

    @Test
    void createRoom_returns400WhenNameIsBlank() throws Exception {
        String request = """
                {
                    "name": "",
                    "roomTypeId": 1
                }
                """;

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).createRoom(any());
    }

    @Test
    void createRoom_returns400WhenRoomTypeIdIsNull() throws Exception {
        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": null
                }
                """;

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).createRoom(any());
    }

    @Test
    void createRoom_returns404WhenRoomTypeDoesNotExist() throws Exception {
        when(roomService.createRoom(any(Room.class)))
                .thenThrow(new EntityNotFoundException("RoomType", 999L));

        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": 999
                }
                """;

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRoom_returnsUpdatedRoom() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        Room room = new Room();
        room.setId(1L);
        room.setName("Room 1");
        room.setRoomType(roomType);

        when(roomService.updateRoom(any(Room.class)))
                .thenReturn(room);

        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": 1
                }
                """;

        mockMvc.perform(put("/api/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Room 1"))
                .andExpect(jsonPath("$.roomTypeName").value("IMAX"));

        verify(roomService).updateRoom(argThat(roomArg ->
                roomArg.getId().equals(1L)
                        && roomArg.getName().equals("Room 1")
                        && roomArg.getRoomType().getId().equals(1L)
        ));
    }

    @Test
    void updateRoom_returns400WhenNameIsBlank() throws Exception {
        String request = """
                {
                    "name": "",
                    "roomTypeId": 1
                }
                """;

        mockMvc.perform(put("/api/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).updateRoom(any());
    }

    @Test
    void updateRoom_returns400WhenRoomTypeIdIsNull() throws Exception {
        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": null
                }
                """;

        mockMvc.perform(put("/api/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(roomService, never()).updateRoom(any());
    }

    @Test
    void updateRoom_returns404WhenRoomDoesNotExist() throws Exception {
        when(roomService.updateRoom(any(Room.class)))
                .thenThrow(new EntityNotFoundException("Room", 999L));

        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": 1
                }
                """;

        mockMvc.perform(put("/api/rooms/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRoom_returns404WhenRoomTypeDoesNotExist() throws Exception {
        when(roomService.updateRoom(any(Room.class)))
                .thenThrow(new EntityNotFoundException("RoomType", 999L));

        String request = """
                {
                    "name": "Room 1",
                    "roomTypeId": 999
                }
                """;

        mockMvc.perform(put("/api/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRoom_returns204() throws Exception {
        mockMvc.perform(delete("/api/rooms/1"))
                .andExpect(status().isNoContent());

        verify(roomService).deleteRoom(1L);
    }

    @Test
    void deleteRoom_returns404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Room", 1L))
                .when(roomService)
                .deleteRoom(1L);

        mockMvc.perform(delete("/api/rooms/1"))
                .andExpect(status().isNotFound());
    }
}