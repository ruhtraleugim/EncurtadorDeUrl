package com.Spectre.EncurtadorDeUrl.service;

import com.Spectre.EncurtadorDeUrl.domain.Url;
import com.Spectre.EncurtadorDeUrl.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shortenUrl_ShouldGenerateIdAndSave() {
        // Arrange
        String originalUrl = "https://example.com";
        when(valueOperations.increment("global_id_counter")).thenReturn(100L); // 100 in Base62 is "1C" (approx) ->
                                                                               // actually 1*62 + 38 = 100 -> 1c

        // Act
        String id = urlShortenerService.shortenUrl(originalUrl);

        // Assert
        assertNotNull(id);
        verify(urlRepository).save(any(Url.class));
        verify(valueOperations).set(eq("url:" + id), eq(originalUrl), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getOriginalUrl_ShouldReturnFromCache_WhenPresent() {
        // Arrange
        String id = "abc";
        String cachedUrl = "https://cached.com";
        when(valueOperations.get("url:" + id)).thenReturn(cachedUrl);

        // Act
        String result = urlShortenerService.getOriginalUrl(id);

        // Assert
        assertEquals(cachedUrl, result);
        verify(urlRepository, never()).findById(anyString());
    }

    @Test
    void getOriginalUrl_ShouldReturnFromRepoAndCache_WhenNotCached() {
        // Arrange
        String id = "abc";
        String originalUrl = "https://repo.com";
        Url url = Url.builder().id(id).originalUrl(originalUrl).build();

        when(valueOperations.get("url:" + id)).thenReturn(null);
        when(urlRepository.findById(id)).thenReturn(Optional.of(url));

        // Act
        String result = urlShortenerService.getOriginalUrl(id);

        // Assert
        assertEquals(originalUrl, result);
        verify(valueOperations).set(eq("url:" + id), eq(originalUrl), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getOriginalUrl_ShouldThrowException_WhenNotFound() {
        // Arrange
        String id = "missing";
        when(valueOperations.get("url:" + id)).thenReturn(null);
        when(urlRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> urlShortenerService.getOriginalUrl(id));
    }
}
