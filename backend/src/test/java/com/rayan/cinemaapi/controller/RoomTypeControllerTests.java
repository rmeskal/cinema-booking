package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.service.RoomTypeService;
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

@WebMvcTest(RoomTypeController.class)
class RoomTypeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private RoomTypeService roomTypeService;

    @Test
    void getRoomTypes_returnsRoomTypes() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        when(roomTypeService.getRoomTypes())
                .thenReturn(List.of(roomType));

        mockMvc.perform(get("/api/room-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("IMAX"));
    }

    @Test
    void getRoomType_returnsRoomTypeWhenFound() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        when(roomTypeService.getRoomType(1L))
                .thenReturn(roomType);

        mockMvc.perform(get("/api/room-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("IMAX"));
    }

    @Test
    void getRoomType_returns404WhenNotFound() throws Exception {
        when(roomTypeService.getRoomType(1L))
                .thenThrow(new EntityNotFoundException("RoomType", 1L));

        mockMvc.perform(get("/api/room-types/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRoomType_returns201() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        when(roomTypeService.createRoomType(any(RoomType.class)))
                .thenReturn(roomType);

        String request = """
                {
                    "name": "IMAX"
                }
                """;

        mockMvc.perform(post("/api/room-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("IMAX"));
    }

    @Test
    void createRoomType_returns400WhenNameIsBlank() throws Exception {
        String request = """
                {
                    "name": ""
                }
                """;

        mockMvc.perform(post("/api/room-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(roomTypeService, never()).createRoomType(any());
    }

    @Test
    void updateRoomType_returnsUpdatedRoomType() throws Exception {
        RoomType roomType = new RoomType();
        roomType.setId(1L);
        roomType.setName("IMAX");

        when(roomTypeService.updateRoomType(any(RoomType.class)))
                .thenReturn(roomType);

        String request = """
                {
                    "name": "IMAX"
                }
                """;

        mockMvc.perform(put("/api/room-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("IMAX"));

        verify(roomTypeService).updateRoomType(argThat(roomTypeArg ->
                roomTypeArg.getId().equals(1L)
                        && roomTypeArg.getName().equals("IMAX")
        ));
    }

    @Test
    void updateRoomType_returns400WhenNameIsBlank() throws Exception {
        String request = """
                {
                    "name": ""
                }
                """;

        mockMvc.perform(put("/api/room-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        verify(roomTypeService, never()).updateRoomType(any());
    }

    @Test
    void updateRoomType_returns404WhenNotFound() throws Exception {
        when(roomTypeService.updateRoomType(any(RoomType.class)))
                .thenThrow(new EntityNotFoundException("RoomType", 1L));

        String request = """
                {
                    "name": "IMAX"
                }
                """;

        mockMvc.perform(put("/api/room-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRoomType_returns204() throws Exception {
        doNothing().when(roomTypeService).deleteRoomType(1L);

        mockMvc.perform(delete("/api/room-types/1"))
                .andExpect(status().isNoContent());

        verify(roomTypeService).deleteRoomType(1L);
    }

    @Test
    void deleteRoomType_returns404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("RoomType", 1L))
                .when(roomTypeService).deleteRoomType(1L);

        mockMvc.perform(delete("/api/room-types/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRoomType_returns409WhenInUse() throws Exception {
        doThrow(new EntityInUseException("RoomType", 1L))
                .when(roomTypeService)
                .deleteRoomType(1L);

        mockMvc.perform(delete("/api/room-types/1"))
                .andExpect(status().isConflict());
    }
}