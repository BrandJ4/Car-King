package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.NonNull; 

import com.example.demo.repository.*;
import com.example.demo.dto.ReservaRequestDTO;
import com.example.demo.entity.*;

import java.time.LocalDateTime;
import java.time.Duration; // Necesario para la duración de Checkout
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional; // Necesario para Optional

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
    
    // Método para la Reserva del Conductor (pago anticipado o pendiente)
    @SuppressWarnings("null")
    public Reserva crearReservaConductor(@NonNull ReservaRequestDTO dto) {
        Long plazaId = Objects.requireNonNull(dto.getPlazaId(), "Plaza id no puede ser null");
        Plaza plaza = plazaRepository.findById(plazaId).orElseThrow(
            () -> new IllegalStateException("Plaza no encontrada")
        );
        if (plaza.isOcupada()) throw new IllegalStateException("La plaza " + plaza.getCodigo() + " ya está ocupada.");
        
        // 1. Crear o actualizar Usuario (busca en la lista y toma el primero si existe)
        List<Usuario> usuariosExistentes = usuarioRepository.findByNombreCompletoAndPlacaVehiculo(dto.getNombreConductor(), dto.getPlacaVehiculo());
        Usuario conductor = usuariosExistentes.isEmpty() ? usuarioRepository.save(Usuario.builder()
                .nombre("Cond-" + dto.getPlacaVehiculo())
                .rol("CONDUCTOR")
                .nombreCompleto(dto.getNombreConductor())
                .placaVehiculo(dto.getPlacaVehiculo())
                .build()) : usuariosExistentes.get(0);

        // 2. Crear Reserva
        LocalDateTime ingreso = LocalDateTime.parse(dto.getHoraIngreso());
        LocalDateTime salida = dto.getHoraSalida() != null ? LocalDateTime.parse(dto.getHoraSalida()) : null;
        
        boolean pagoAnticipado = !dto.isUnsure() && salida != null;
        
        Reserva reserva = Reserva.builder()
            .usuario(conductor)
            .plaza(plaza)
            .horaIngreso(ingreso)
            .horaSalida(salida)
            .activa(true)
            .pagado(pagoAnticipado)
            .metodoPago(pagoAnticipado ? dto.getMetodoPago() : "Efectivo_Pendiente") // Asigna método si es anticipado
            .unsure(dto.isUnsure())
            .build();
        
        final Reserva reservaGuardada = reservaRepository.save(reserva);
        
        // 3. Actualizar estado de la Plaza
        plaza.setOcupada(true);
        plazaRepository.save(plaza);

        // 4. Crear Boleta si el pago es anticipado
        if (pagoAnticipado) {
            long horas = calculateDurationHours(ingreso, salida);
            double montoTotal = horas * PRECIO_POR_HORA;
            
            Boleta boletaNueva = Boleta.builder()
                .reserva(reservaGuardada)
                .monto(montoTotal)
                .fechaEmision(LocalDateTime.now())
                .metodoPago(dto.getMetodoPago())
                .build();
            boletaRepository.save(boletaNueva);
        }
        
        return reservaGuardada;
    }


    // Método para Check-in Manual del Recepcionista
    @SuppressWarnings("null")
    public Reserva crearReservaManual(@NonNull ReservaRequestDTO dto) {
        Long plazaId = Objects.requireNonNull(dto.getPlazaId(), "Plaza id no puede ser null");
        Plaza plaza = plazaRepository.findById(plazaId).orElseThrow(
            () -> new IllegalStateException("Plaza no encontrada.")
        );
        if (plaza.isOcupada()) throw new IllegalStateException("La plaza " + plaza.getCodigo() + " ya está ocupada.");

        // Buscar o crear Usuario (Conductor) - toma el primero si existen múltiples
        List<Usuario> usuariosExistentes = usuarioRepository.findByNombreCompletoAndPlacaVehiculo(dto.getNombreConductor(), dto.getPlacaVehiculo());
        Usuario conductor = usuariosExistentes.isEmpty() ? usuarioRepository.save(Usuario.builder()
                .nombre("Cond-" + dto.getPlacaVehiculo())
                .rol("CONDUCTOR")
                .nombreCompleto(dto.getNombreConductor())
                .placaVehiculo(dto.getPlacaVehiculo())
                .build()) : usuariosExistentes.get(0);

        // Crear Reserva
        Reserva reserva = Reserva.builder()
            .usuario(conductor)
            .plaza(plaza)
            .horaIngreso(LocalDateTime.now()) // Ingreso al momento de registrar
            .horaSalida(null) 
            .activa(true)
            .pagado(false) // Siempre pendiente
            .metodoPago("Efectivo_Pendiente")
            .unsure(true)
            .build();
        
        final Reserva reservaGuardada = reservaRepository.save(reserva);
        
        // Actualizar estado de Plaza
        plaza.setOcupada(true);
        plazaRepository.save(plaza);

        return reservaGuardada;
    }


    // Método para Checkout y Pago Final (Recepcionista)
    public Boleta finalizarCheckout(@NonNull Long reservaId, @NonNull String metodoPago) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalStateException("Reserva no encontrada."));

        if (!reserva.isActiva()) {
            throw new IllegalStateException("La reserva ya ha sido finalizada.");
        }

        // 1. Cálculo de duración y costo (lógica de cronómetro)
        LocalDateTime horaIngreso = reserva.getHoraIngreso();
        LocalDateTime horaSalida = LocalDateTime.now(); // Hora de salida actual
        
        long durationHours = calculateDurationHours(horaIngreso, horaSalida);
        double monto = durationHours * PRECIO_POR_HORA; 

        // 2. Actualizar Reserva
        reserva.setHoraSalida(horaSalida);
        reserva.setActiva(false);
        reserva.setPagado(true);
        reserva.setMetodoPago(metodoPago);
        reservaRepository.save(reserva);

        // 3. Actualizar estado de Plaza
        Plaza plaza = reserva.getPlaza();
        plaza.setOcupada(false);
        plazaRepository.save(plaza);

        // 4. Crear Boleta
        Boleta boleta = Boleta.builder()
            .fechaEmision(horaSalida)
            .monto(monto)
            .metodoPago(metodoPago)
            .reserva(reserva)
            .build();
        
        return boletaRepository.save(boleta);
    }
    
    // Helper para calcular la duración, redondeando al entero superior, mínimo 1 hora.
    private long calculateDurationHours(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) return 1;
        Duration duration = Duration.between(inicio, fin);
        long minutes = duration.toMinutes();
        // Redondea al entero superior.
        return Math.max(1, (long) Math.ceil(minutes / 60.0)); 
    }
}