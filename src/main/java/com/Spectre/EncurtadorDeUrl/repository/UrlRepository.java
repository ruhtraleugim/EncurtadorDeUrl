package com.Spectre.EncurtadorDeUrl.repository;

import com.Spectre.EncurtadorDeUrl.domain.Url;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlRepository extends CassandraRepository<Url, String> {
}