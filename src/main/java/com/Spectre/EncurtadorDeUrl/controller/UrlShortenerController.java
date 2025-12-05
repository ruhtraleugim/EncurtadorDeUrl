package com.Spectre.EncurtadorDeUrl.controller;

import com.Spectre.EncurtadorDeUrl.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody String originalUrl) {
        String id = urlShortenerService.shortenUrl(originalUrl);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Void> redirect(@PathVariable String id) {
        String originalUrl = urlShortenerService.getOriginalUrl(id);
        return ResponseEntity.status(302).location(URI.create(originalUrl)).build();
    }
}