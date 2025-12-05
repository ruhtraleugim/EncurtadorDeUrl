package com.Spectre.EncurtadorDeUrl.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("urls")
public class Url {

    @PrimaryKey
    private String id;

    private String originalUrl;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}