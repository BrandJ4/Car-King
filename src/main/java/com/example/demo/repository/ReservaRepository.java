package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Reserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    // Nuevo método para obtener reservas activas (sin hora de salida) por Cochera
    List<Reserva> findByPlazaId(Long plazaId);
    List<Reserva> findByPlazaCocheraIdAndHoraSalidaIsNull(Long cocheraId);
}