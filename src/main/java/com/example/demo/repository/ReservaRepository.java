package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Reserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    // 1. Método general para buscar todas las reservas por ID de Plaza
    List<Reserva> findByPlazaId(Long plazaId);
    
    // 2. Método CRÍTICO: Usado por ColorUtils para determinar si la plaza está OCUPADA O LIBRE
    // Busca reservas activas en una plaza específica para verificar disponibilidad
    List<Reserva> findByPlazaIdAndActivaTrue(Long plazaId);

    // 3. Método CRÍTICO: Usado para la lista de reservas activas del Recepcionista (Checkout)
    List<Reserva> findByPlazaCocheraIdAndActivaTrue(Long cocheraId);
}