package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Plaza;

@Repository
public interface PlazaRepository extends JpaRepository<Plaza, Long> {
    List<Plaza> findByCocheraId(Long cocheraId);
}