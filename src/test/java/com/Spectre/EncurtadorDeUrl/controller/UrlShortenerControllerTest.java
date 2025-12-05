package com.Spectre.EncurtadorDeUrl.controller;

import com.Spectre.EncurtadorDeUrl.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlShortenerService urlShortenerService;

    @InjectMocks
    private UrlShortenerController urlShortenerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(urlShortenerController).build();
    }

    @Test
    void shortenUrl_ShouldReturnId() throws Exception {
        String originalUrl = "https://example.com";
        String generatedId = "abc1234";

        when(urlShortenerService.shortenUrl(anyString())).thenReturn(generatedId);

        mockMvc.perform(post("/api/v1/shorten")
                .contentType(MediaType.TEXT_PLAIN)
                .content(originalUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(generatedId));
    }

    @Test
    void redirect_ShouldReturn302_WhenIdExists() throws Exception {
        String id = "abc1234";
        String originalUrl = "https://example.com";

        when(urlShortenerService.getOriginalUrl(id)).thenReturn(originalUrl);

        mockMvc.perform(get("/api/v1/{id}", id))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", originalUrl));
    }
}
