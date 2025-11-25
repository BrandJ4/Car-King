package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.NonNull; 

import com.example.demo.repository.*;
import com.example.demo.dto.ReservaRequestDTO;
import com.example.demo.entity.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
// import java.util.Optional; <-- ELIMINADO (Ya no se usa directamente)
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservaService {
    private final ReservaRepository reservaRepository;
    private final PlazaRepository plazaRepository;
    private final BoletaRepository boletaRepository;
    private final UsuarioRepository usuarioRepository; 

    private static final double PRECIO_POR_HORA = 5.0;

    public List<Reserva> obtenerTodas() {
        return reservaRepository.findAll();
    }

    public Reserva guardar(@NonNull Reserva reserva) {
        return reservaRepository.save(reserva);
    }
    
    public Boleta crearReservaYBoleta(@NonNull ReservaRequestDTO dto) {
        // 1. Obtener Plaza (Corregido para evitar warnings de Long)
        Long plazaId = Objects.requireNonNull(dto.getPlazaId(), "Plaza id no puede ser null");
        Plaza plaza = plazaRepository.findById(plazaId).orElseThrow(
            () -> new RuntimeException("Plaza no encontrada")
        );
        
        // 2. Crear o actualizar Usuario (Corregido con inicialización no ambigua)
        Usuario conductor = usuarioRepository.findByNombreCompletoAndPlacaVehiculo(dto.getNombreConductor(), dto.getPlacaVehiculo());
        
        if (conductor == null) {
            Usuario nuevoConductor = Usuario.builder()
                        .nombre("Conductor-" + dto.getPlacaVehiculo())
                        .rol("CONDUCTOR")
                        .nombreCompleto(dto.getNombreConductor())
                        .placaVehiculo(dto.getPlacaVehiculo())
                        .build();
            @SuppressWarnings("null")
            Usuario savedConductor = Objects.requireNonNull(usuarioRepository.save(nuevoConductor), "usuarioRepository.save returned null");
            conductor = savedConductor;
        }

        // 3. Crear Reserva
        LocalDateTime ingreso = LocalDateTime.parse(dto.getHoraIngreso());
        LocalDateTime salida = dto.getHoraSalida() != null ? LocalDateTime.parse(dto.getHoraSalida()) : null;

        Reserva reserva = Reserva.builder()
                .usuario(conductor)
                .plaza(plaza)
                .horaIngreso(ingreso)
                .horaSalida(salida)
                .build();
        @SuppressWarnings("null")
        final Reserva reservaGuardada = Objects.requireNonNull(reservaRepository.save(reserva), "reservaRepository.save returned null"); // Uso de 'final' y garantía de no-null
        
        // 4. Actualizar estado de la Plaza
        plaza.setOcupada(true);
        plazaRepository.save(plaza);

        // 5. Crear Boleta
        if (!dto.isUnsure() && salida != null) {
            long horas = ChronoUnit.HOURS.between(ingreso, salida);
            if (horas == 0) horas = 1;

            double montoTotal = horas * PRECIO_POR_HORA;

            Boleta boletaNueva = Boleta.builder()
                    .reserva(reservaGuardada)
                    .monto(montoTotal)
                    .fechaEmision(LocalDateTime.now())
                    .metodoPago(dto.getMetodoPago())
                    .build();
            @SuppressWarnings("null")
            final Boleta boletaGuardada = Objects.requireNonNull(boletaRepository.save(boletaNueva), "boletaRepository.save returned null"); // Asignación no ambigua con garantía de no-null
            return boletaGuardada;
        }

        return null;
    }
    
    // El @NonNull garantiza que Long y String no son null.
    public Boleta finalizarReservaYGenerarBoleta(@NonNull Long reservaId, @NonNull String metodoPago) {
        Reserva reserva = reservaRepository.findById(reservaId).orElseThrow(
            () -> new RuntimeException("Reserva no encontrada")
        );
        
        if (reserva.getHoraSalida() != null) {
             throw new RuntimeException("La reserva ya ha finalizado.");
        }

        // 1. Establecer hora de salida y actualizar reserva
        LocalDateTime horaSalidaReal = LocalDateTime.now();
        reserva.setHoraSalida(horaSalidaReal);
        final Reserva reservaFinalizada = reservaRepository.save(reserva); 

        // 2. Calcular monto final
        long minutosTranscurridos = ChronoUnit.MINUTES.between(reservaFinalizada.getHoraIngreso(), horaSalidaReal);
        BigDecimal minutos = BigDecimal.valueOf(minutosTranscurridos);
        BigDecimal horas = minutos.divide(BigDecimal.valueOf(60), 0, RoundingMode.CEILING);
        
        if (horas.compareTo(BigDecimal.ONE) < 0) {
            horas = BigDecimal.ONE;
        }

        double montoTotal = horas.doubleValue() * PRECIO_POR_HORA;
        
        // 3. Crear Boleta
        Boleta boletaNueva = Boleta.builder()
                .reserva(reservaFinalizada)
                .monto(montoTotal)
                .fechaEmision(LocalDateTime.now())
                .metodoPago(metodoPago)
                .build();
        
        @SuppressWarnings("null")
        final Boleta boletaGuardada = Objects.requireNonNull(boletaRepository.save(boletaNueva), "boletaRepository.save returned null"); // Asignación final con garantía de no-null
        
        // 4. Liberar Plaza
        Plaza plaza = reservaFinalizada.getPlaza();
        plaza.setOcupada(false);
        plazaRepository.save(plaza);

        return boletaGuardada;
    }
}