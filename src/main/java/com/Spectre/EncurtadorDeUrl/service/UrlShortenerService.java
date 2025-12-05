package com.Spectre.EncurtadorDeUrl.service;

import com.Spectre.EncurtadorDeUrl.domain.Url;
import com.Spectre.EncurtadorDeUrl.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ID_COUNTER_KEY = "global_id_counter";
    private static final String URL_CACHE_PREFIX = "url:";

    public String shortenUrl(String originalUrl) {
        String id = generateId();
        Url url = Url.builder()
                .id(id)
                .originalUrl(originalUrl)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(10)) // 10 years expiration
                .build();

        urlRepository.save(url);

        // Cache the result (Write-through)
        redisTemplate.opsForValue().set(URL_CACHE_PREFIX + id, originalUrl, 10, TimeUnit.MINUTES); // Cache for 10 mins
                                                                                                   // (hot data)

        return id;
    }

    public String getOriginalUrl(String id) {
        // Check cache first
        String cachedUrl = redisTemplate.opsForValue().get(URL_CACHE_PREFIX + id);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        // Fallback to Cassandra
        String originalUrl = urlRepository.findById(id)
                .map(Url::getOriginalUrl)
                .orElseThrow(() -> new RuntimeException("URL not found, please try again"));

        // Populate cache (Read-through)
        redisTemplate.opsForValue().set(URL_CACHE_PREFIX + id, originalUrl, 10, TimeUnit.MINUTES);

        return originalUrl;
    }

    private String generateId() {
        Long counter = redisTemplate.opsForValue().increment(ID_COUNTER_KEY);
        return encodeBase62(counter);
    }

    private String encodeBase62(Long value) {
        if (value == null)
            return "0";
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(BASE62.charAt((int) (value % 62)));
            value /= 62;
        }
        return sb.reverse().toString();
    }
}