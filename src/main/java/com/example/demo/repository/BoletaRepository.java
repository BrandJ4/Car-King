package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.Boleta;

public interface BoletaRepository extends JpaRepository<Boleta, Long> { }
